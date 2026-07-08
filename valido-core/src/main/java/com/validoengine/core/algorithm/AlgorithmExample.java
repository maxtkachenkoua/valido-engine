package com.validoengine.core.algorithm;

import com.validoengine.core.capability.CapabilityId;

import java.util.Map;
import java.util.Objects;

public record AlgorithmExample(
        CapabilityId capability,
        Map<String, Object> input,
        Map<String, Object> expected
) {
    public AlgorithmExample {
        Objects.requireNonNull(capability, "capability");
        input = input == null ? Map.of() : Map.copyOf(input);
        expected = expected == null ? Map.of() : Map.copyOf(expected);
    }
}
