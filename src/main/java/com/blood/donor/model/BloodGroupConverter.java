package com.blood.donor.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BloodGroupConverter implements AttributeConverter<BloodGroup, String> {

    @Override
    public String convertToDatabaseColumn(BloodGroup bloodGroup) {
        return bloodGroup == null ? null : bloodGroup.getValue();
    }

    @Override
    public BloodGroup convertToEntityAttribute(String value) {
        return value == null ? null : BloodGroup.fromValue(value);
    }
}
