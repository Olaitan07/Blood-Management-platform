package com.blood.donor.exception;

public class DonorNotFoundException extends RuntimeException {
    public DonorNotFoundException(Long id) {
        super("Donor not found with id: " + id);
    }

    public DonorNotFoundException(String message) {
        super(message);
    }
}
