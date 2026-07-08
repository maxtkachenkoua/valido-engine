package com.validoengine.core.validation;

import java.util.Objects;
import java.util.Optional;

public record ValidationResult<T>(T value, ValidationReport report) {
    public ValidationResult {
        report = report == null ? ValidationReport.empty() : report;
    }

    public static <T> ValidationResult<T> success(T value) {
        return new ValidationResult<>(Objects.requireNonNull(value, "value"), ValidationReport.empty());
    }

    public static <T> ValidationResult<T> of(T value, ValidationReport report) {
        return new ValidationResult<>(value, report);
    }

    public Optional<T> optionalValue() {
        return Optional.ofNullable(value);
    }

    public boolean isValid() {
        return !report.hasErrors();
    }
}
