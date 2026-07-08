package com.validoengine.core.validation;

import com.validoengine.core.diagnostic.Diagnostic;
import com.validoengine.core.diagnostic.DiagnosticSeverity;

import java.util.List;

public record ValidationReport(List<Diagnostic> diagnostics) {
    public ValidationReport {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public static ValidationReport empty() {
        return new ValidationReport(List.of());
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(Diagnostic::isError);
    }

    public long count(DiagnosticSeverity severity) {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == severity)
                .count();
    }

    public ValidationReport append(Diagnostic diagnostic) {
        var merged = new java.util.ArrayList<>(diagnostics);
        merged.add(diagnostic);
        return new ValidationReport(merged);
    }

    public ValidationReport merge(ValidationReport other) {
        var merged = new java.util.ArrayList<>(diagnostics);
        merged.addAll(other.diagnostics());
        return new ValidationReport(merged);
    }
}
