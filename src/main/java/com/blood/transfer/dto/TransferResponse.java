package com.blood.transfer.dto;

import com.blood.transfer.model.BloodTransferRequest;
import com.blood.transfer.model.TransferStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferResponse(
        Long id,
        Long requestingHospitalId,
        Long sourceHospitalId,
        String bloodGroup,
        int quantity,
        TransferStatus status,
        String requestDate,
        String approvalDate,
        String completionDate,
        String rejectionReason,
        Integer unitsReceived,
        String idempotencyKey
) {
    public static TransferResponse from(BloodTransferRequest r) {
        return new TransferResponse(
                r.getId(),
                r.getRequestingHospitalId(),
                r.getSourceHospitalId(),
                r.getBloodGroup(),
                r.getQuantity(),
                r.getStatus(),
                r.getRequestDate() != null ? r.getRequestDate().toString() : null,
                r.getApprovalDate() != null ? r.getApprovalDate().toString() : null,
                r.getCompletionDate() != null ? r.getCompletionDate().toString() : null,
                r.getRejectionReason(),
                r.getUnitsReceived(),
                r.getIdempotencyKey()
        );
    }
}
