package com.vvmonitor.api.exception;

import java.time.Instant;
import java.util.List;

/** Formato padrao de erro devolvido pela API (RNF6). */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }
}
