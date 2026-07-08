package com.validoengine.content.model;

import com.validoengine.core.validation.ValidationReport;

import java.util.Objects;
import java.util.Optional;

public record ContentLoadResult(ContentProject project, ValidationReport report, RawContentWorkspace rawWorkspace) {
    public ContentLoadResult {
        report = report == null ? ValidationReport.empty() : report;
        Objects.requireNonNull(rawWorkspace, "rawWorkspace");
    }

    public Optional<ContentProject> optionalProject() {
        return Optional.ofNullable(project);
    }

    public boolean successful() {
        return project != null && !report.hasErrors();
    }
}
