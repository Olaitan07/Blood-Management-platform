package com.blood.auth.model;

public enum Role {
    DONOR(false),
    CLINICIAN(true),
    OFFICER(true),
    ADMIN(false);

    private final boolean requiresHospital;

    Role(boolean requiresHospital) {
        this.requiresHospital = requiresHospital;
    }

    public boolean requiresHospital() {
        return requiresHospital;
    }
}
