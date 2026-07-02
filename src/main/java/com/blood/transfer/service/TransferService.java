package com.blood.transfer.service;

import com.blood.transfer.dto.CompleteTransferRequest;
import com.blood.transfer.dto.CreateTransferRequest;
import com.blood.transfer.dto.RejectTransferRequest;
import com.blood.transfer.dto.TransferResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TransferService {

    /** TRANS-1: Submit a blood transfer request. */
    TransferResponse createRequest(CreateTransferRequest request, Authentication auth);

    /** TRANS-2: Approve a pending request (source hospital officer). */
    TransferResponse approveRequest(Long transferId, Authentication auth);

    /** TRANS-3: Reject a pending request with a mandatory reason. */
    TransferResponse rejectRequest(Long transferId, RejectTransferRequest request, Authentication auth);

    /** TRANS-4: Confirm receipt of blood units (destination hospital). */
    TransferResponse completeTransfer(Long transferId, CompleteTransferRequest request, Authentication auth);

    /** TRANS-5: Cancel a pending or approved request (requester or elevated role). */
    TransferResponse cancelRequest(Long transferId, Authentication auth);

    /** Returns all transfer requests for the authenticated user's hospital. */
    List<TransferResponse> getMyRequests(Authentication auth);

    /** Returns all pending transfer requests targeting the officer's hospital. */
    List<TransferResponse> getPendingForMyHospital(Authentication auth);
}
