package com.blood.donor.exception;

public class DuplicatePhoneException extends RuntimeException {
    public DuplicatePhoneException(String phone) {
        super("This number is already registered — recover your profile instead? (phone: " + phone + ")");
    }
}
