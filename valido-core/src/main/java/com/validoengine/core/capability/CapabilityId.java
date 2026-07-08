package com.validoengine.core.capability;

import java.util.Objects;
import java.util.regex.Pattern;

public record CapabilityId(String value) implements Comparable<CapabilityId> {
    private static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

    public static final CapabilityId VALIDATE = new CapabilityId("validate");
    public static final CapabilityId GENERATE = new CapabilityId("generate");
    public static final CapabilityId PARSE = new CapabilityId("parse");
    public static final CapabilityId FORMAT = new CapabilityId("format");
    public static final CapabilityId CONVERT = new CapabilityId("convert");
    public static final CapabilityId EXPLAIN = new CapabilityId("explain");
    public static final CapabilityId LOOKUP = new CapabilityId("lookup");
    public static final CapabilityId CALCULATE = new CapabilityId("calculate");
    public static final CapabilityId DECODE = new CapabilityId("decode");
    public static final CapabilityId ENCODE = new CapabilityId("encode");
    public static final CapabilityId BULK_VALIDATE = new CapabilityId("bulk-validate");
    public static final CapabilityId BULK_GENERATE = new CapabilityId("bulk-generate");

    public CapabilityId {
        value = normalize(value, "capability id");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid capability id: " + value);
        }
    }

    private static String normalize(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }

    @Override
    public int compareTo(CapabilityId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
