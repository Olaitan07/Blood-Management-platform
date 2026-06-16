package com.blood.inventory.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String value;

    BloodGroup(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static final List<String> VALID_VALUES = Arrays.stream(values())
            .map(BloodGroup::getValue)
            .toList();

    @JsonCreator
    public static BloodGroup fromValue(String value) {
        return Arrays.stream(values())
                .filter(bg -> bg.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid blood group '" + value + "'. Valid values: " + VALID_VALUES));
    }

    @Converter(autoApply = true)
    public static class BloodGroupConverter implements AttributeConverter<BloodGroup, String> {
        @Override
        public String convertToDatabaseColumn(BloodGroup bg) {
            return bg == null ? null : bg.getValue();
        }

        @Override
        public BloodGroup convertToEntityAttribute(String value) {
            return value == null ? null : BloodGroup.fromValue(value);
        }
    }
}
