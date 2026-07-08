package com.validoengine.core.model;

public record LocaleCode(String value) implements Comparable<LocaleCode> {
    public static final LocaleCode EN = new LocaleCode("en");

    public LocaleCode {
        value = IdentifierRules.requirePattern(value, IdentifierRules.LOCALE_CODE, "locale code");
    }

    @Override
    public int compareTo(LocaleCode other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
