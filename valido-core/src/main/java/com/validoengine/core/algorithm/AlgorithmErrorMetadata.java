package com.validoengine.core.algorithm;

import com.validoengine.core.model.LocalizedText;

import java.util.Objects;

public record AlgorithmErrorMetadata(
        String code,
        LocalizedText message
) {
    public AlgorithmErrorMetadata {
        code = requireText(code, "algorithm error code");
        message = Objects.requireNonNull(message, "message");
    }

    private static String requireText(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
