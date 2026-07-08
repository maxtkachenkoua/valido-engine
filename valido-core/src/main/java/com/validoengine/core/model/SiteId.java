package com.validoengine.core.model;

public record SiteId(String value) implements Comparable<SiteId> {
    public SiteId {
        value = IdentifierRules.requirePattern(value, IdentifierRules.LOWERCASE_ID, "site id");
    }

    @Override
    public int compareTo(SiteId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
