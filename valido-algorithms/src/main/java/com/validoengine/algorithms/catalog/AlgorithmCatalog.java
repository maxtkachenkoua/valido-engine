package com.validoengine.algorithms.catalog;

import com.validoengine.core.algorithm.ValidoAlgorithm;
import com.validoengine.core.model.AlgorithmId;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AlgorithmCatalog {
    private final Map<AlgorithmId, ValidoAlgorithm> algorithms;

    private AlgorithmCatalog(Map<AlgorithmId, ValidoAlgorithm> algorithms) {
        this.algorithms = Map.copyOf(algorithms);
    }

    public static AlgorithmCatalog empty() {
        return new AlgorithmCatalog(Map.of());
    }

    public static AlgorithmCatalog of(Collection<? extends ValidoAlgorithm> algorithms) {
        Objects.requireNonNull(algorithms, "algorithms");
        Map<AlgorithmId, ValidoAlgorithm> indexed = new LinkedHashMap<>();
        for (ValidoAlgorithm algorithm : algorithms) {
            Objects.requireNonNull(algorithm, "algorithm");
            ValidoAlgorithm existing = indexed.putIfAbsent(algorithm.id(), algorithm);
            if (existing != null) {
                throw new IllegalArgumentException("Duplicate algorithm id: " + algorithm.id());
            }
        }
        return new AlgorithmCatalog(indexed);
    }

    public static AlgorithmCatalog of(ValidoAlgorithm... algorithms) {
        return of(List.of(algorithms));
    }

    public Optional<ValidoAlgorithm> find(AlgorithmId algorithmId) {
        return Optional.ofNullable(algorithms.get(algorithmId));
    }

    public Collection<ValidoAlgorithm> all() {
        return algorithms.values();
    }

    public int size() {
        return algorithms.size();
    }

    public boolean isEmpty() {
        return algorithms.isEmpty();
    }
}
