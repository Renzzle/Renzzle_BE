package com.renzzle.backend.global.util;

import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

public class ErrorUtils {

    private ErrorUtils() {}

    public static String getErrorMessages(BindingResult bindingResult) {
        StringBuilder errorMessages = new StringBuilder();
        bindingResult.getAllErrors()
                .forEach(error -> errorMessages.append(error.getDefaultMessage()).append(". "));
        return errorMessages.toString();
    }

    public static String getErrorMessages(BindException e) {
        StringBuilder errorMessages = new StringBuilder();
        e.getAllErrors()
                .forEach(error -> errorMessages.append(error.getDefaultMessage()).append(". "));
        return errorMessages.toString();
    }

}