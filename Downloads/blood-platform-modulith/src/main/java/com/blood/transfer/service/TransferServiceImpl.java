package com.blood.transfer.service;

import com.blood.hospital.service.HospitalService;
import com.blood.inventory.transfer.InventorySlotDto;
import com.blood.inventory.transfer.TransferInventoryPort;
import com.blood.transfer.dto.CompleteTransferRequest;
import com.blood.transfer.dto.CreateTransferRequest;
import com.blood.transfer.dto.RejectTransferRequest;
import com.blood.transfer.dto.TransferResponse;
import com.blood.transfer.events.*;
import com.blood.transfer.exception.TransferNotFoundException;
import com.blood.transfer.model.BloodTransferRequest;
import com.blood.transfer.model.TransferStatus;
import com.blood.transfer.repository.BloodTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
class TransferServiceImpl implements TransferService {

    private final BloodTransferRepository transferRepository;
    private final HospitalService hospitalService;
    private final TransferInventoryPort inventoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ── TRANS-1 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransferResponse createRequest(CreateTransferRequest req, Authentication auth) {
        Long requestingHospitalId = extractHospitalId(auth);
        Long userId = extractUserId(auth);

        if (requestingHospitalId.equals(req.sourceHospitalId())) {
            throw new IllegalArgumentException("Cannot request transfer from your own hospital");
        }

        if (req.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if (!hospitalService.hospitalExists(req.sourceHospitalId())) {
            throw new IllegalStateException("Hospital no longer available");
        }

        if (transferRepository.existsByIdempotencyKey(req.idempotencyKey())) {
            throw new IllegalStateException("Duplicate request detected — this transfer was already submitted");
        }

        // Real-time availability check at request time (search may be stale)
        InventorySlotDto slot = inventoryPort.findAndReserve(
                req.sourceHospitalId(), req.bloodGroup(), req.quantity(), auth.getName());

        BloodTransferRequest transfer = BloodTransferRequest.builder()
                .requestingHospitalId(requestingHospitalId)
                .sourceHospitalId(req.sourceHospitalId())
                .bloodGroup(req.bloodGroup())
                .quantity(req.quantity())
                .status(TransferStatus.PENDING)
                .requestDate(LocalDateTime.now(clock))
                .idempotencyKey(req.idempotencyKey())
                .requestedByUserId(userId)
                .sourceInventoryId(slot.inventoryId())
                .build();

        BloodTransferRequest saved = transferRepository.save(transfer);

        eventPublisher.publishEvent(new BloodTransferRequestedEvent(
                saved.getId(), requestingHospitalId, req.sourceHospitalId(),
                req.bloodGroup(), req.quantity(), auth.getName(), Instant.now(clock)));

        log.info("Transfer request created: id={} {} x{} from hospitalId={} by {}",
                saved.getId(), req.bloodGroup(), req.quantity(), req.sourceHospitalId(), auth.getName());
        return TransferResponse.from(saved);
    }

    // ── TRANS-2 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransferResponse approveRequest(Long transferId, Authentication auth) {
        Long officerHospitalId = extractHospitalId(auth);
        Long userId = extractUserId(auth);

        BloodTransferRequest transfer = transferRepository.findByIdAndSourceHospitalId(transferId, officerHospitalId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        if (transfer.getStatus() == TransferStatus.CANCELLED) {
            throw new IllegalStateException("Request was cancelled");
        }

        transfer.transitionTo(TransferStatus.APPROVED);
        transfer.setApprovalDate(LocalDateTime.now(clock));
        transfer.setApprovedByUserId(userId);

        BloodTransferRequest saved = transferRepository.save(transfer);

        eventPublisher.publishEvent(new BloodTransferApprovedEvent(
                saved.getId(), saved.getRequestingHospitalId(), saved.getSourceHospitalId(),
                saved.getBloodGroup(), saved.getQuantity(), auth.getName(), Instant.now(clock)));

        log.info("Transfer approved: id={} by {}", saved.getId(), auth.getName());
        return TransferResponse.from(saved);
    }

    // ── TRANS-3 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransferResponse rejectRequest(Long transferId, RejectTransferRequest req, Authentication auth) {
        Long officerHospitalId = extractHospitalId(auth);

        BloodTransferRequest transfer = transferRepository.findByIdAndSourceHospitalId(transferId, officerHospitalId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        transfer.transitionTo(TransferStatus.REJECTED);
        transfer.setRejectionReason(req.reason());

        // Release reservation since we reserved at request time
        if (transfer.getSourceInventoryId() != null) {
            inventoryPort.release(transfer.getSourceInventoryId(), transfer.getQuantity(), auth.getName());
        }

        BloodTransferRequest saved = transferRepository.save(transfer);

        eventPublisher.publishEvent(new BloodTransferRejectedEvent(
                saved.getId(), saved.getRequestingHospitalId(), saved.getSourceHospitalId(),
                saved.getBloodGroup(), saved.getQuantity(), req.reason(), auth.getName(), Instant.now(clock)));

        log.info("Transfer rejected: id={} reason='{}' by {}", saved.getId(), req.reason(), auth.getName());
        return TransferResponse.from(saved);
    }

    // ── TRANS-4 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransferResponse completeTransfer(Long transferId, CompleteTransferRequest req, Authentication auth) {
        Long destHospitalId = extractHospitalId(auth);

        BloodTransferRequest transfer = transferRepository.findByIdAndRequestingHospitalId(transferId, destHospitalId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        transfer.transitionTo(TransferStatus.COMPLETED);
        transfer.setCompletionDate(LocalDateTime.now(clock));
        transfer.setUnitsReceived(req.unitsReceived());

        int received = req.unitsReceived();
        int approved = transfer.getQuantity();

        if (received > 0) {
            // Check if expiry has passed during transit
            inventoryPort.finalizeTransfer(
                    transfer.getSourceInventoryId(), approved,
                    destHospitalId, auth.getName());

            if (received < approved) {
                log.warn("Partial receipt: transferId={} approved={} received={} — discrepancy of {} units logged",
                        transferId, approved, received, (approved - received));
            }
        } else {
            // All expired in transit — just release the reservation without adding to dest
            inventoryPort.release(transfer.getSourceInventoryId(), approved, auth.getName() + " [expired-in-transit]");
            log.warn("All units expired in transit for transferId={}", transferId);
        }

        BloodTransferRequest saved = transferRepository.save(transfer);

        eventPublisher.publishEvent(new BloodTransferCompletedEvent(
                saved.getId(), saved.getRequestingHospitalId(), saved.getSourceHospitalId(),
                saved.getBloodGroup(), approved, received, auth.getName(), Instant.now(clock)));

        log.info("Transfer completed: id={} approved={} received={}", saved.getId(), approved, received);
        return TransferResponse.from(saved);
    }

    // ── TRANS-5 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransferResponse cancelRequest(Long transferId, Authentication auth) {
        Long hospitalId = extractHospitalId(auth);

        // Requester's hospital can cancel; officers at source can also cancel elevated
        BloodTransferRequest transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        boolean isRequester = transfer.getRequestingHospitalId().equals(hospitalId);
        boolean isSource = transfer.getSourceHospitalId().equals(hospitalId);

        if (!isRequester && !isSource) {
            throw new AccessDeniedException("You can only cancel requests from your own hospital");
        }

        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed transfer");
        }

        boolean wasApproved = transfer.getStatus() == TransferStatus.APPROVED;
        transfer.transitionTo(TransferStatus.CANCELLED);

        // Release reservation on cancel
        if (transfer.getSourceInventoryId() != null) {
            inventoryPort.release(transfer.getSourceInventoryId(), transfer.getQuantity(), auth.getName() + " [cancelled]");
        }

        BloodTransferRequest saved = transferRepository.save(transfer);

        eventPublisher.publishEvent(new BloodTransferCancelledEvent(
                saved.getId(), saved.getRequestingHospitalId(), saved.getSourceHospitalId(),
                saved.getBloodGroup(), saved.getQuantity(), wasApproved, auth.getName(), Instant.now(clock)));

        log.info("Transfer cancelled: id={} wasApproved={} by {}", saved.getId(), wasApproved, auth.getName());
        return TransferResponse.from(saved);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getMyRequests(Authentication auth) {
        Long hospitalId = extractHospitalId(auth);
        return transferRepository.findByRequestingHospitalId(hospitalId)
                .stream().map(TransferResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getPendingForMyHospital(Authentication auth) {
        Long hospitalId = extractHospitalId(auth);
        return transferRepository.findBySourceHospitalIdAndStatus(hospitalId, TransferStatus.PENDING)
                .stream().map(TransferResponse::from).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long extractHospitalId(Authentication auth) {
        if (auth.getDetails() instanceof Map<?, ?> details) {
            Object id = details.get("hospitalId");
            if (id instanceof Number n) return n.longValue();
        }
        throw new AccessDeniedException("Hospital scope not found in token");
    }

    private Long extractUserId(Authentication auth) {
        if (auth.getDetails() instanceof Map<?, ?> details) {
            Object id = details.get("userId");
            if (id instanceof Number n) return n.longValue();
        }
        return 0L;
    }
}
