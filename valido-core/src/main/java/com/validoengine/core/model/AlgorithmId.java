package com.validoengine.core.model;

public record AlgorithmId(String value) implements Comparable<AlgorithmId> {
    public AlgorithmId {
        value = IdentifierRules.requirePattern(value, IdentifierRules.ALGORITHM_ID, "algorithm id");
    }

    @Override
    public int compareTo(AlgorithmId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
