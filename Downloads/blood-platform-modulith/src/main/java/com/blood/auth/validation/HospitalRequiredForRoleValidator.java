package com.blood.auth.validation;

import com.blood.auth.dto.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HospitalRequiredForRoleValidator implements ConstraintValidator<HospitalRequiredForRole, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null || request.role() == null) {
            return true;
        }
        if (request.role().requiresHospital()) {
            return request.hospitalId() != null;
        }
        return true;
    }
}
