package com.validoengine.generator;

import com.validoengine.content.model.ContentProject;
import com.validoengine.core.capability.CapabilityModel;
import com.validoengine.core.model.AlgorithmId;
import com.validoengine.core.model.AlgorithmBindingModel;
import com.validoengine.core.algorithm.AlgorithmMetadata;
import com.validoengine.core.model.ContentBlockModel;
import com.validoengine.core.model.ExportPlan;
import com.validoengine.core.model.LocaleCode;
import com.validoengine.core.model.LocaleModel;
import com.validoengine.core.model.ProjectModel;
import com.validoengine.core.model.RouteModel;
import com.validoengine.core.model.ToolId;
import com.validoengine.core.model.ToolModel;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProjectModelAssembler {
    public ProjectModel assemble(ContentProject contentProject, List<RouteModel> routes) {
        return assemble(contentProject, routes, ExportPlan.empty());
    }

    public ProjectModel assemble(ContentProject contentProject, List<RouteModel> routes, ExportPlan exportPlan) {
        Objects.requireNonNull(contentProject, "contentProject");
        routes = routes == null ? List.of() : List.copyOf(routes);
        exportPlan = exportPlan == null ? ExportPlan.empty() : exportPlan;
        Map<ToolId, AlgorithmBindingModel> algorithmBindings = contentProject.tools().stream()
                .collect(Collectors.toUnmodifiableMap(ToolModel::id, ToolModel::algorithmBinding));
        Map<AlgorithmId, AlgorithmMetadata> algorithmMetadata = contentProject.algorithmRegistry().all().stream()
                .collect(Collectors.toUnmodifiableMap(entry -> entry.algorithmId(), entry -> entry.toMetadata()));
        return new ProjectModel(
                contentProject.site(),
                contentProject.tools(),
                contentProject.categories(),
                contentProject.countries(),
                locales(contentProject),
                contentProject.content(),
                algorithmBindings,
                algorithmMetadata,
                routes,
                List.of(),
                capabilities(contentProject),
                exportPlan
        );
    }

    private static List<LocaleModel> locales(ContentProject contentProject) {
        Map<LocaleCode, Set<ToolId>> toolsWithContent = contentProject.content().blocksByTool().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(block -> Map.entry(block.locale(), entry.getKey())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableSet())
                ));
        return contentProject.site().locales().stream()
                .map(locale -> new LocaleModel(
                        locale,
                        locale.equals(contentProject.site().defaultLocale()),
                        toolsWithContent.getOrDefault(locale, Set.of())
                ))
                .toList();
    }

    private static List<CapabilityModel> capabilities(ContentProject contentProject) {
        return contentProject.tools().stream()
                .flatMap(tool -> tool.capabilities().stream())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .map(capability -> new CapabilityModel(capability, null, null))
                .toList();
    }
}
