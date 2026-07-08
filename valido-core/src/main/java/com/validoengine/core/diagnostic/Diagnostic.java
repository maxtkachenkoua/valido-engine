package com.validoengine.core.diagnostic;

import java.util.Map;
import java.util.Objects;

public record Diagnostic(
        DiagnosticSeverity severity,
        String code,
        String message,
        DiagnosticLocation location,
        Map<String, String> details
) {
    public Diagnostic {
        Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        message = requireText(message, "message");
        location = location == null ? DiagnosticLocation.none() : location;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static Diagnostic error(String code, String message) {
        return new Diagnostic(DiagnosticSeverity.ERROR, code, message, DiagnosticLocation.none(), Map.of());
    }

    public static Diagnostic warning(String code, String message) {
        return new Diagnostic(DiagnosticSeverity.WARNING, code, message, DiagnosticLocation.none(), Map.of());
    }

    public boolean isError() {
        return severity == DiagnosticSeverity.ERROR;
    }

    private static String requireText(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
