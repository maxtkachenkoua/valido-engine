package com.validoengine.core.model;

public record CountryCode(String value) implements Comparable<CountryCode> {
    public CountryCode {
        value = IdentifierRules.requirePattern(value, IdentifierRules.COUNTRY_CODE, "country code");
    }

    @Override
    public int compareTo(CountryCode other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
