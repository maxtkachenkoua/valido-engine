package com.validoengine.content.load;

import com.validoengine.content.model.ContentLoadResult;
import com.validoengine.content.model.ContentProject;
import com.validoengine.content.model.RawContentWorkspace;
import com.validoengine.content.validation.ContentValidator;
import com.validoengine.core.diagnostic.Diagnostic;
import com.validoengine.core.diagnostic.DiagnosticLocation;
import com.validoengine.core.diagnostic.DiagnosticSeverity;
import com.validoengine.core.validation.ValidationReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ContentProjectLoader {
    private final ContentDiscovery discovery;
    private final ContentValidator validator;
    private final ContentProjectMapper mapper;

    public ContentProjectLoader() {
        this(new YamlDslReader());
    }

    public ContentProjectLoader(YamlDslReader yamlReader) {
        this.discovery = new ContentDiscovery(Objects.requireNonNull(yamlReader, "yamlReader"));
        this.validator = new ContentValidator();
        this.mapper = new ContentProjectMapper();
    }

    public ContentLoadResult load(Path projectRoot) {
        return load(projectRoot, Path.of("site.yaml"));
    }

    public ContentLoadResult load(Path projectRoot, Path sitePath) {
        Objects.requireNonNull(projectRoot, "projectRoot");
        try {
            RawContentWorkspace workspace = discovery.discover(projectRoot, sitePath);
            ValidationReport report = validator.validate(workspace);
            ContentProject project = report.hasErrors() ? null : mapper.map(workspace);
            return new ContentLoadResult(project, report, workspace);
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            RawContentWorkspace workspace = new RawContentWorkspace(projectRoot.toAbsolutePath().normalize(), null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
            return new ContentLoadResult(null, failureReport(exception), workspace);
        }
    }

    private static ValidationReport failureReport(Exception exception) {
        return new ValidationReport(List.of(new Diagnostic(
                DiagnosticSeverity.ERROR,
                "CONTENT_LOAD_FAILED",
                exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                DiagnosticLocation.none(),
                Map.of()
        )));
    }
}
