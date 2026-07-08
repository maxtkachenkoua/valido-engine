package com.validoengine.generator.model;

import com.validoengine.content.model.ContentLoadResult;
import com.validoengine.core.model.ProjectModel;

import java.util.Objects;
import java.util.Optional;

public record GenerationResult(
        ProjectModel projectModel,
        GenerationReport report,
        ContentLoadResult contentLoadResult
) {
    public GenerationResult {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(contentLoadResult, "contentLoadResult");
    }

    public Optional<ProjectModel> optionalProjectModel() {
        return Optional.ofNullable(projectModel);
    }

    public boolean successful() {
        return projectModel != null && report.successful();
    }
}
