package com.validoengine.generator;

import com.validoengine.content.model.ContentProject;
import com.validoengine.core.model.ExportPlan;
import com.validoengine.core.model.ExporterId;
import com.validoengine.core.model.RouteModel;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ExportPlanBuilder {
    public static final ExporterId HTML_EXPORTER_ID = new ExporterId("html");

    public ExportPlan buildHtmlPlan(ContentProject contentProject, List<RouteModel> routes) {
        Objects.requireNonNull(contentProject, "contentProject");
        routes = routes == null ? List.of() : List.copyOf(routes);
        List<Path> plannedArtifacts = routes.stream()
                .map(RouteModel::outputFile)
                .distinct()
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        return new ExportPlan(
                List.of(HTML_EXPORTER_ID),
                Map.of(HTML_EXPORTER_ID, contentProject.site().outputDirectory()),
                plannedArtifacts,
                Map.of()
        );
    }
}
