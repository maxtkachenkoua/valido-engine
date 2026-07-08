package com.validoengine.core.model;

public record ToolId(String value) implements Comparable<ToolId> {
    public ToolId {
        value = IdentifierRules.requirePattern(value, IdentifierRules.LOWERCASE_ID, "tool id");
    }

    @Override
    public int compareTo(ToolId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
