package com.validoengine.core.algorithm;

import java.util.List;
import java.util.Map;

public record CapabilityResult(
        boolean success,
        Map<String, Object> output,
        List<AlgorithmError> errors
) {
    public CapabilityResult {
        output = output == null ? Map.of() : Map.copyOf(output);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static CapabilityResult success(Map<String, Object> output) {
        return new CapabilityResult(true, output, List.of());
    }

    public static CapabilityResult failure(List<AlgorithmError> errors) {
        return new CapabilityResult(false, Map.of(), errors);
    }
}
