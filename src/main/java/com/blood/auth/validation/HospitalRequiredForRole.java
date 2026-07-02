package com.blood.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = HospitalRequiredForRoleValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HospitalRequiredForRole {

    String message() default "Hospital ID is required for the selected role";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
