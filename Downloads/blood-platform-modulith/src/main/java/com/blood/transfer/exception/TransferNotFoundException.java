package com.blood.transfer.exception;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(Long id) {
        super("Transfer request not found or access denied: id=" + id);
    }
}
