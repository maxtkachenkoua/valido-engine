package com.validoengine.generator;

import com.validoengine.content.load.ContentProjectLoader;
import com.validoengine.content.model.ContentLoadResult;
import com.validoengine.content.model.ContentProject;
import com.validoengine.core.model.ProjectModel;
import com.validoengine.core.validation.ValidationReport;
import com.validoengine.generator.model.GenerationPhase;
import com.validoengine.generator.model.GenerationReport;
import com.validoengine.generator.model.GenerationRequest;
import com.validoengine.generator.model.GenerationResult;
import com.validoengine.generator.model.RouteGenerationResult;

import java.time.Instant;
import java.util.Objects;

public final class ValidoGenerator {
    private final ContentProjectLoader contentProjectLoader;
    private final ProjectModelAssembler projectModelAssembler;
    private final RouteGenerator routeGenerator;

    public ValidoGenerator() {
        this(new ContentProjectLoader(), new ProjectModelAssembler(), new RouteGenerator());
    }

    public ValidoGenerator(ContentProjectLoader contentProjectLoader, ProjectModelAssembler projectModelAssembler, RouteGenerator routeGenerator) {
        this.contentProjectLoader = Objects.requireNonNull(contentProjectLoader, "contentProjectLoader");
        this.projectModelAssembler = Objects.requireNonNull(projectModelAssembler, "projectModelAssembler");
        this.routeGenerator = Objects.requireNonNull(routeGenerator, "routeGenerator");
    }

    public GenerationResult generate(GenerationRequest request) {
        Objects.requireNonNull(request, "request");
        ContentLoadResult contentLoadResult = contentProjectLoader.load(request.projectRoot(), request.sitePath());
        if (contentLoadResult.report().hasErrors() || contentLoadResult.project() == null) {
            return new GenerationResult(
                    null,
                    report(null, contentLoadResult, GenerationPhase.CONTENT_VALIDATION, false),
                    contentLoadResult
            );
        }

        RouteGenerationResult routeGenerationResult = routeGenerator.generate(contentLoadResult.project());
        ValidationReport generationReport = contentLoadResult.report().merge(routeGenerationResult.report());
        if (!routeGenerationResult.successful()) {
            return new GenerationResult(
                    null,
                    report(null, contentLoadResult, generationReport, GenerationPhase.ROUTE_GENERATION, false),
                    contentLoadResult
            );
        }

        ProjectModel projectModel = projectModelAssembler.assemble(contentLoadResult.project(), routeGenerationResult.routes());
        return new GenerationResult(
                projectModel,
                report(projectModel, contentLoadResult, generationReport, GenerationPhase.PROJECT_MODEL_ASSEMBLY, true),
                contentLoadResult
        );
    }

    private static GenerationReport report(
            ProjectModel projectModel,
            ContentLoadResult contentLoadResult,
            GenerationPhase phase,
            boolean successful
    ) {
        ContentProject contentProject = contentLoadResult.project();
        return new GenerationReport(
                Instant.now(),
                phase,
                successful,
                contentLoadResult.report(),
                contentProject == null ? 0 : contentProject.tools().size(),
                contentProject == null ? 0 : contentProject.categories().size(),
                contentProject == null ? 0 : contentProject.countries().size(),
                projectModel == null ? 0 : projectModel.locales().size(),
                contentProject == null ? 0 : contentProject.algorithmRegistry().entries().size(),
                projectModel == null ? 0 : projectModel.routes().size(),
                projectModel == null ? 0 : projectModel.exportPlan().exporters().size()
        );
    }

    private static GenerationReport report(
            ProjectModel projectModel,
            ContentLoadResult contentLoadResult,
            ValidationReport validationReport,
            GenerationPhase phase,
            boolean successful
    ) {
        ContentProject contentProject = contentLoadResult.project();
        return new GenerationReport(
                Instant.now(),
                phase,
                successful,
                validationReport,
                contentProject == null ? 0 : contentProject.tools().size(),
                contentProject == null ? 0 : contentProject.categories().size(),
                contentProject == null ? 0 : contentProject.countries().size(),
                projectModel == null ? 0 : projectModel.locales().size(),
                contentProject == null ? 0 : contentProject.algorithmRegistry().entries().size(),
                projectModel == null ? 0 : projectModel.routes().size(),
                projectModel == null ? 0 : projectModel.exportPlan().exporters().size()
        );
    }
}
