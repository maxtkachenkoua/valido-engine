package com.validoengine.generator;

import com.validoengine.content.model.ContentProject;
import com.validoengine.core.model.ExportPlan;
import com.validoengine.core.model.ExporterId;
import com.validoengine.core.model.RouteModel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ExportPlanBuilder {
    public static final ExporterId HTML_EXPORTER_ID = new ExporterId("html");

    public ExportPlan buildHtmlPlan(ContentProject contentProject, List<RouteModel> routes) {
        Objects.requireNonNull(contentProject, "contentProject");
        routes = routes == null ? List.of() : List.copyOf(routes);
        List<Path> plannedArtifacts = new ArrayList<>(routes.stream()
                .map(RouteModel::outputFile)
                .distinct()
                .sorted(Comparator.comparing(Path::toString))
                .toList());
        plannedArtifacts.add(contentProject.site().outputDirectory().resolve("sitemap.xml"));
        plannedArtifacts.add(contentProject.site().outputDirectory().resolve("robots.txt"));
        plannedArtifacts.add(contentProject.site().outputDirectory().resolve("search-index.json"));
        return new ExportPlan(
                List.of(HTML_EXPORTER_ID),
                Map.of(HTML_EXPORTER_ID, contentProject.site().outputDirectory()),
                plannedArtifacts.stream().distinct().sorted(Comparator.comparing(Path::toString)).toList(),
                Map.of()
        );
    }
}
