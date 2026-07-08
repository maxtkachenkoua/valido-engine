package com.validoengine.core.algorithm;

import java.util.Map;

public record CapabilityInput(Map<String, Object> values) {
    public CapabilityInput {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static CapabilityInput empty() {
        return new CapabilityInput(Map.of());
    }
}
