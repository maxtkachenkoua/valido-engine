package com.validoengine.core.model;

public record CategoryId(String value) implements Comparable<CategoryId> {
    public CategoryId {
        value = IdentifierRules.requirePattern(value, IdentifierRules.LOWERCASE_ID, "category id");
    }

    @Override
    public int compareTo(CategoryId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
