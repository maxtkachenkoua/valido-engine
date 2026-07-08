package com.validoengine.generator.model;

import com.validoengine.core.validation.ValidationReport;

import java.time.Instant;
import java.util.Objects;

public record GenerationReport(
        Instant generatedAt,
        GenerationPhase phase,
        boolean successful,
        ValidationReport validationReport,
        int toolCount,
        int categoryCount,
        int countryCount,
        int localeCount,
        int algorithmCount,
        int routeCount,
        int exporterCount
) {
    public GenerationReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        Objects.requireNonNull(phase, "phase");
        validationReport = validationReport == null ? ValidationReport.empty() : validationReport;
        if (toolCount < 0 || categoryCount < 0 || countryCount < 0 || localeCount < 0
                || algorithmCount < 0 || routeCount < 0 || exporterCount < 0) {
            throw new IllegalArgumentException("generation report counts must not be negative");
        }
    }
}
