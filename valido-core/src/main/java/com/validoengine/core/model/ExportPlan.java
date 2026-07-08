package com.validoengine.core.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ExportPlan(
        List<ExporterId> exporters,
        Map<ExporterId, Path> targetDirectories,
        List<Path> generatedArtifacts,
        Map<String, Path> reportPaths
) {
    public ExportPlan {
        exporters = exporters == null ? List.of() : List.copyOf(exporters);
        targetDirectories = targetDirectories == null ? Map.of() : Map.copyOf(targetDirectories);
        generatedArtifacts = generatedArtifacts == null ? List.of() : List.copyOf(generatedArtifacts);
        reportPaths = reportPaths == null ? Map.of() : Map.copyOf(reportPaths);
    }

    public static ExportPlan empty() {
        return new ExportPlan(List.of(), Map.of(), List.of(), Map.of());
    }
}
