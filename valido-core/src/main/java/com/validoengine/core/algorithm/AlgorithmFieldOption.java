package com.validoengine.core.algorithm;

import com.validoengine.core.model.LocalizedText;

public record AlgorithmFieldOption(
        String value,
        LocalizedText label
) {
    public AlgorithmFieldOption {
        value = requireText(value, "field option value");
        label = label == null ? LocalizedText.empty() : label;
    }

    private static String requireText(String value, String label) {
        String normalized = java.util.Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
