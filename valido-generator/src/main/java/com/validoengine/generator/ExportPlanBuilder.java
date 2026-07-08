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
    public static final ExporterId API_METADATA_EXPORTER_ID = new ExporterId("api-metadata");

    public ExportPlan buildHtmlPlan(ContentProject contentProject, List<RouteModel> routes) {
        Objects.requireNonNull(contentProject, "contentProject");
        routes = routes == null ? List.of() : List.copyOf(routes);
        Path targetDirectory = contentProject.projectRoot().resolve(contentProject.site().outputDirectory()).normalize();
        List<Path> plannedArtifacts = new ArrayList<>(routes.stream()
                .map(RouteModel::outputFile)
                .distinct()
                .sorted(Comparator.comparing(Path::toString))
                .toList());
        plannedArtifacts.add(targetDirectory.resolve("sitemap.xml"));
        plannedArtifacts.add(targetDirectory.resolve("robots.txt"));
        plannedArtifacts.add(targetDirectory.resolve("search-index.json"));
        plannedArtifacts.add(targetDirectory.resolve("api-metadata.json"));
        return new ExportPlan(
                List.of(HTML_EXPORTER_ID, API_METADATA_EXPORTER_ID),
                Map.of(
                        HTML_EXPORTER_ID, targetDirectory,
                        API_METADATA_EXPORTER_ID, targetDirectory
                ),
                plannedArtifacts.stream().distinct().sorted(Comparator.comparing(Path::toString)).toList(),
                Map.of()
        );
    }
}
