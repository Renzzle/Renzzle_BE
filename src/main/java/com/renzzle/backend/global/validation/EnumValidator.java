package com.renzzle.backend.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private ValidEnum annotation;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null && this.annotation.nullable()) return false;
        else if(value == null) return true;

        Object[] enumValues = this.annotation.enumClass().getEnumConstants();
        if(enumValues == null) return false;

        for(Object enumValue : enumValues) {
            if(enumValue.toString().equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }

}
