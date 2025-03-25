package com.renzzle.backend.global.validation;

import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BoardValidator implements ConstraintValidator<ValidBoardString, String> {

    @Override
    public void initialize(ValidBoardString constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null) return false;
        if(value.isBlank()) return false;

        return BoardUtils.validBoardString(value);
    }

}
