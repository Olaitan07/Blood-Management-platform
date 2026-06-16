package com.blood.audit.listener;

import com.blood.audit.model.AuditRecord;
import com.blood.audit.repository.AuditRecordRepository;
import com.blood.auth.event.AdminAuditEvent;
import com.blood.auth.event.UserRegisteredEvent;
import com.blood.donor.event.DonorRegisteredEvent;
import com.blood.hospital.event.HospitalDeactivatedEvent;
import com.blood.hospital.event.HospitalRegisteredEvent;
import com.blood.transfer.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Immutable audit trail consumer.
 *
 * Spring Modulith's @ApplicationModuleListener runs in a new transaction.
 * The event_publication outbox guarantees delivery even after a restart —
 * so missed events are replayed automatically (zero-gap trail).
 *
 * Ordering: occurredAt from the event payload is persisted verbatim so that
 * clock skew between the publishing service and this listener does not corrupt
 * the chronological order of the trail. The DB sequence id is the tiebreaker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditRecordRepository auditRecordRepository;

    // ── Transfer events ────────────────────────────────────────────────────────

    @ApplicationModuleListener
    public void on(BloodTransferRequestedEvent e) {
        save("BloodTransferRequestedEvent", e.requestedBy(), e.transferId(), "TRANSFER",
                String.format("Transfer #%d requested: %dx%s from hospital #%d to hospital #%d",
                        e.transferId(), e.quantity(), e.bloodGroup(),
                        e.requestingHospitalId(), e.sourceHospitalId()),
                e.occurredAt());
    }

    @ApplicationModuleListener
    public void on(BloodTransferApprovedEvent e) {
        save("BloodTransferApprovedEvent", e.approvedBy(), e.transferId(), "TRANSFER",
                String.format("Transfer #%d approved by %s: %dx%s from hospital #%d",
                        e.transferId(), e.approvedBy(), e.quantity(), e.bloodGroup(), e.sourceHospitalId()),
                e.occurredAt());
    }

    @ApplicationModuleListener
    public void on(BloodTransferRejectedEvent e) {
        save("BloodTransferRejectedEvent", e.rejectedBy(), e.transferId(), "TRANSFER",
                String.format("Transfer #%d rejected by hospital #%d. Reason: %s",
                        e.transferId(), e.sourceHospitalId(), e.reason()),
                e.occurredAt());
    }

    @ApplicationModuleListener
    public void on(BloodTransferCompletedEvent e) {
        save("BloodTransferCompletedEvent", e.completedBy(), e.transferId(), "TRANSFER",
                String.format("Transfer #%d completed: %d/%d units of %s received",
                        e.transferId(), e.quantityReceived(), e.quantityApproved(), e.bloodGroup()),
                e.occurredAt());
    }

    @ApplicationModuleListener
    public void on(BloodTransferCancelledEvent e) {
        save("BloodTransferCancelledEvent", e.cancelledBy(), e.transferId(), "TRANSFER",
                String.format("Transfer #%d cancelled by %s. Reservation released: %b",
                        e.transferId(), e.cancelledBy(), e.wasApproved()),
                e.occurredAt());
    }

    // ── Donor events ───────────────────────────────────────────────────────────

    @ApplicationModuleListener
    public void on(DonorRegisteredEvent e) {
        save("DonorRegisteredEvent", e.fullName(), e.donorId(), "DONOR",
                String.format("Donor #%d registered: %s (blood group %s)",
                        e.donorId(), e.fullName(), e.bloodGroup()),
                e.occurredOn());
    }

    // ── Hospital events ────────────────────────────────────────────────────────

    @ApplicationModuleListener
    public void on(HospitalRegisteredEvent e) {
        save("HospitalRegisteredEvent", "system", e.hospitalId(), "HOSPITAL",
                String.format("Hospital #%d registered: %s, %s, %s",
                        e.hospitalId(), e.name(), e.city(), e.state()),
                e.registeredAt().toInstant(java.time.ZoneOffset.UTC));
    }

    @ApplicationModuleListener
    public void on(HospitalDeactivatedEvent e) {
        save("HospitalDeactivatedEvent", "system", e.hospitalId(), "HOSPITAL",
                String.format("Hospital #%d deactivated: %s", e.hospitalId(), e.name()),
                e.deactivatedAt().toInstant(java.time.ZoneOffset.UTC));
    }

    // ── Auth / admin events ────────────────────────────────────────────────────

    @ApplicationModuleListener
    public void on(UserRegisteredEvent e) {
        save("UserRegisteredEvent", e.email(), e.userId(), "USER",
                String.format("User #%d registered: %s (%s) assigned to hospital #%d",
                        e.userId(), e.name(), e.role(), e.hospitalId()),
                Instant.now());
    }

    @ApplicationModuleListener
    public void on(AdminAuditEvent e) {
        save("AdminAuditEvent", "admin#" + e.adminId(), e.targetUserId(), "USER",
                String.format("Admin action [%s] by admin #%d on user #%d: %s",
                        e.action(), e.adminId(), e.targetUserId(), e.detail()),
                Instant.now());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void save(String eventType, String actor, Long targetId, String targetType,
                      String payload, Instant occurredAt) {
        auditRecordRepository.save(AuditRecord.builder()
                .eventType(eventType)
                .actor(actor != null ? actor : "system")
                .targetId(targetId != null ? targetId.toString() : null)
                .targetType(targetType)
                .payload(payload)
                .occurredAt(occurredAt != null ? occurredAt : Instant.now())
                .receivedAt(Instant.now())
                .build());
        log.debug("Audit recorded: eventType={} actor={} targetId={}", eventType, actor, targetId);
    }
}
