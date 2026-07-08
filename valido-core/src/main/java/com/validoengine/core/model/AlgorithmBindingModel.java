package com.validoengine.core.model;

import com.validoengine.core.diagnostic.Diagnostic;

import java.util.List;
import java.util.Objects;

public record AlgorithmBindingModel(
        AlgorithmId algorithmId,
        Integer algorithmVersion,
        AlgorithmCompatibilityStatus compatibilityStatus,
        List<Diagnostic> diagnostics
) {
    public AlgorithmBindingModel {
        Objects.requireNonNull(compatibilityStatus, "compatibilityStatus");
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        if (compatibilityStatus == AlgorithmCompatibilityStatus.COMPATIBLE && algorithmId == null) {
            throw new IllegalArgumentException("compatible algorithm binding requires algorithmId");
        }
    }
}
