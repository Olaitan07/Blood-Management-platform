package com.blood.hospital.exception;

public class HospitalNotFoundException extends RuntimeException {

    public HospitalNotFoundException(String message) {
        super(message);
    }
}
