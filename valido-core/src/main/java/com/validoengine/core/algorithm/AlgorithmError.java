package com.validoengine.core.algorithm;

import java.util.Map;

public record AlgorithmError(
        String code,
        String message,
        Map<String, Object> details
) {
    public AlgorithmError {
        code = requireText(code, "algorithm error code");
        message = message == null ? "" : message;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    private static String requireText(String value, String label) {
        String normalized = java.util.Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
