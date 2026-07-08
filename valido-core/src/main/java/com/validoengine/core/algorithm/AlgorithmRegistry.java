package com.validoengine.core.algorithm;

import com.validoengine.core.model.AlgorithmId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public record AlgorithmRegistry(Map<AlgorithmId, AlgorithmRegistryEntry> entries) {
    public AlgorithmRegistry {
        entries = entries == null ? Map.of() : Map.copyOf(entries);
    }

    public static AlgorithmRegistry empty() {
        return new AlgorithmRegistry(Map.of());
    }

    public Optional<AlgorithmRegistryEntry> find(AlgorithmId algorithmId) {
        return Optional.ofNullable(entries.get(algorithmId));
    }

    public Collection<AlgorithmRegistryEntry> all() {
        return entries.values();
    }
}
