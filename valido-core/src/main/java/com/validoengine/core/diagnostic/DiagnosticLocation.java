package com.validoengine.core.diagnostic;

import java.util.Objects;
import java.util.Optional;

public record DiagnosticLocation(String source, Integer line, Integer column) {
    public DiagnosticLocation {
        if (source != null && source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank when present");
        }
        if (line != null && line < 1) {
            throw new IllegalArgumentException("line must be greater than zero when present");
        }
        if (column != null && column < 1) {
            throw new IllegalArgumentException("column must be greater than zero when present");
        }
    }

    public static DiagnosticLocation none() {
        return new DiagnosticLocation(null, null, null);
    }

    public static DiagnosticLocation source(String source) {
        return new DiagnosticLocation(Objects.requireNonNull(source, "source"), null, null);
    }

    public Optional<String> sourceName() {
        return Optional.ofNullable(source);
    }
}
