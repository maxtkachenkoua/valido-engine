package com.validoengine.core.algorithm;

import com.validoengine.core.capability.CapabilityId;
import com.validoengine.core.model.AlgorithmId;
import com.validoengine.core.model.LocalizedText;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AlgorithmRegistryEntry(
        AlgorithmId algorithmId,
        String javaClass,
        int version,
        Set<CapabilityId> capabilities,
        LocalizedText name,
        LocalizedText summary,
        Map<CapabilityId, List<AlgorithmFieldMetadata>> inputs,
        Map<CapabilityId, List<AlgorithmFieldMetadata>> outputs,
        List<AlgorithmErrorMetadata> errors,
        List<AlgorithmExample> examples
) {
    public AlgorithmRegistryEntry {
        Objects.requireNonNull(algorithmId, "algorithmId");
        javaClass = requireJavaClass(javaClass);
        if (version < 1) {
            throw new IllegalArgumentException("algorithm version must be greater than zero");
        }
        capabilities = Set.copyOf(capabilities);
        if (capabilities.isEmpty()) {
            throw new IllegalArgumentException("algorithm registry capabilities must not be empty");
        }
        name = name == null ? LocalizedText.empty() : name;
        summary = summary == null ? LocalizedText.empty() : summary;
        inputs = inputs == null ? Map.of() : copyFieldMap(inputs);
        outputs = outputs == null ? Map.of() : copyFieldMap(outputs);
        errors = errors == null ? List.of() : List.copyOf(errors);
        examples = examples == null ? List.of() : List.copyOf(examples);
    }

    public AlgorithmMetadata toMetadata() {
        return new AlgorithmMetadata(
                algorithmId,
                version,
                name,
                summary,
                capabilities,
                inputs,
                outputs,
                errors,
                examples
        );
    }

    private static String requireJavaClass(String value) {
        String normalized = Objects.requireNonNull(value, "javaClass").trim();
        if (normalized.isEmpty() || normalized.startsWith(".") || normalized.endsWith(".") || !normalized.contains(".")) {
            throw new IllegalArgumentException("javaClass must be a fully qualified class name");
        }
        return normalized;
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
