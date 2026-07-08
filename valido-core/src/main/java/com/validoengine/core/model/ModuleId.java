package com.validoengine.core.model;

public record ModuleId(String value) implements Comparable<ModuleId> {
    public ModuleId {
        value = IdentifierRules.requirePattern(value, IdentifierRules.LOWERCASE_ID, "module id");
    }

    @Override
    public int compareTo(ModuleId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
