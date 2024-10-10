package com.renzzle.backend.global.util;

import org.springframework.validation.BindingResult;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorUtils {

    public static String getErrorMessages(BindingResult bindingResult) {
        StringBuilder errorMessages = new StringBuilder();
        bindingResult.getAllErrors()
                .forEach(error -> errorMessages.append(error.getDefaultMessage()).append(". "));
        return errorMessages.toString();
    }

    public static String getStakeTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}