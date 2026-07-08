package com.validoengine.core.algorithm;

import com.validoengine.core.capability.CapabilityId;
import com.validoengine.core.model.AlgorithmId;
import com.validoengine.core.model.LocalizedText;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AlgorithmMetadata(
        AlgorithmId algorithmId,
        int version,
        LocalizedText name,
        LocalizedText summary,
        Set<CapabilityId> capabilities,
        Map<CapabilityId, List<AlgorithmFieldMetadata>> inputs,
        Map<CapabilityId, List<AlgorithmFieldMetadata>> outputs,
        List<AlgorithmErrorMetadata> errors,
        List<AlgorithmExample> examples
) {
    public AlgorithmMetadata {
        Objects.requireNonNull(algorithmId, "algorithmId");
        if (version < 1) {
            throw new IllegalArgumentException("algorithm version must be greater than zero");
        }
        name = name == null ? LocalizedText.empty() : name;
        summary = summary == null ? LocalizedText.empty() : summary;
        capabilities = Set.copyOf(capabilities);
        if (capabilities.isEmpty()) {
            throw new IllegalArgumentException("algorithm capabilities must not be empty");
        }
        inputs = inputs == null ? Map.of() : copyFieldMap(inputs);
        outputs = outputs == null ? Map.of() : copyFieldMap(outputs);
        errors = errors == null ? List.of() : List.copyOf(errors);
        examples = examples == null ? List.of() : List.copyOf(examples);
    }

    private static Map<CapabilityId, List<AlgorithmFieldMetadata>> copyFieldMap(
            Map<CapabilityId, List<AlgorithmFieldMetadata>> source
    ) {
        return source.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }
}
