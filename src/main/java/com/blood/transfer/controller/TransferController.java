package com.blood.transfer.controller;

import com.blood.transfer.dto.*;
import com.blood.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /** TRANS-1: Submit a blood transfer request (clinician or officer). */
    @PostMapping
    @PreAuthorize("hasAnyRole('CLINICIAN', 'OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransferResponse>> createRequest(
            @Valid @RequestBody CreateTransferRequest request,
            Authentication auth) {
        TransferResponse response = transferService.createRequest(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Transfer request submitted", response));
    }

    /** TRANS-2: Approve a pending request (source hospital officer). */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransferResponse>> approveRequest(
            @PathVariable Long id,
            Authentication auth) {
        TransferResponse response = transferService.approveRequest(id, auth);
        return ResponseEntity.ok(ApiResponse.ok("Transfer request approved", response));
    }

    /** TRANS-3: Reject a pending request with a reason. */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransferResponse>> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody RejectTransferRequest request,
            Authentication auth) {
        TransferResponse response = transferService.rejectRequest(id, request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Transfer request rejected", response));
    }

    /** TRANS-4: Confirm receipt of blood units (destination hospital). */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OFFICER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransferResponse>> completeTransfer(
            @PathVariable Long id,
            @Valid @RequestBody CompleteTransferRequest request,
            Authentication auth) {
        TransferResponse response = transferService.completeTransfer(id, request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Transfer completed", response));
    }

    /** TRANS-5: Cancel a pending or approved request. */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransferResponse>> cancelRequest(
            @PathVariable Long id,
            Authentication auth) {
        TransferResponse response = transferService.cancelRequest(id, auth);
        return ResponseEntity.ok(ApiResponse.ok("Transfer request cancelled", response));
    }

    /** My hospital's outgoing requests. */
    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getMyRequests(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Requests retrieved", transferService.getMyRequests(auth)));
    }

    /** Pending transfer requests arriving at my hospital (for officers to action). */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getPending(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Pending requests retrieved", transferService.getPendingForMyHospital(auth)));
    }
}
