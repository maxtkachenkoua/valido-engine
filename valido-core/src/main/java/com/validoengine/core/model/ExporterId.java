package com.validoengine.core.model;

public record ExporterId(String value) implements Comparable<ExporterId> {
    public ExporterId {
        value = IdentifierRules.requirePattern(value, IdentifierRules.LOWERCASE_ID, "exporter id");
    }

    @Override
    public int compareTo(ExporterId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
