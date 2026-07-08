package com.validoengine.core.algorithm;

import com.validoengine.core.model.LocalizedText;

import java.util.List;
import java.util.Map;

public record AlgorithmFieldMetadata(
        String name,
        String type,
        boolean required,
        LocalizedText label,
        Map<String, Object> constraints,
        List<AlgorithmFieldOption> options
) {
    public AlgorithmFieldMetadata {
        name = requireText(name, "field name");
        type = requireText(type, "field type");
        label = label == null ? LocalizedText.empty() : label;
        constraints = constraints == null ? Map.of() : Map.copyOf(constraints);
        options = options == null ? List.of() : List.copyOf(options);
    }

    private static String requireText(String value, String label) {
        String normalized = java.util.Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
