package com.validoengine.core.model;

import java.util.Objects;
import java.util.regex.Pattern;

final class IdentifierRules {
    static final Pattern LOWERCASE_ID = Pattern.compile("[a-z0-9][a-z0-9-]*");
    static final Pattern ALGORITHM_ID = Pattern.compile("[a-z0-9][a-z0-9.-]*");
    static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z][A-Z0-9]*");
    static final Pattern FORM_INPUT_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");
    static final Pattern LOCALE_CODE = Pattern.compile("[a-z]{2,3}(-[A-Za-z0-9]+)*");

    private IdentifierRules() {
    }

    static String requirePattern(String value, Pattern pattern, String label) {
        String normalized = requireText(value, label);
        if (!pattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid " + label + ": " + normalized);
        }
        return normalized;
    }

    static String requireText(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
