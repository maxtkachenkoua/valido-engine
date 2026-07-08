package com.validoengine.generator;

import com.validoengine.core.capability.CapabilityId;
import com.validoengine.core.model.LocaleCode;
import com.validoengine.core.model.ProjectModel;
import com.validoengine.core.model.RelatedConfigModel;
import com.validoengine.core.model.RelatedLinkModel;
import com.validoengine.core.model.RelatedReason;
import com.validoengine.core.model.RouteModel;
import com.validoengine.core.model.RouteType;
import com.validoengine.core.model.SiteMode;
import com.validoengine.core.model.ToolId;
import com.validoengine.core.model.ToolModel;
import com.validoengine.core.model.ToolStatus;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class RelatedLinkResolver {
    public ProjectModel resolve(ProjectModel projectModel) {
        Objects.requireNonNull(projectModel, "projectModel");
        Map<ToolId, ToolModel> publicToolsById = publicToolsById(projectModel);
        Map<String, RouteModel> defaultLocaleToolRoutes = defaultLocaleToolRoutes(projectModel);
        List<ToolModel> tools = projectModel.tools().stream()
                .map(tool -> withRelatedLinks(tool, publicToolsById, defaultLocaleToolRoutes, projectModel.site().defaultLocale()))
                .toList();

        return new ProjectModel(
                projectModel.site(),
                tools,
                projectModel.categories(),
                projectModel.countries(),
                projectModel.locales(),
                projectModel.content(),
                projectModel.algorithmBindings(),
                projectModel.algorithmMetadata(),
                projectModel.routes(),
                tools.stream().flatMap(tool -> tool.relatedLinks().stream()).toList(),
                projectModel.capabilities(),
                projectModel.exportPlan()
        );
    }

    private static Map<ToolId, ToolModel> publicToolsById(ProjectModel projectModel) {
        boolean includeDrafts = projectModel.site().mode() == SiteMode.DEMO && projectModel.site().includeDrafts();
        return projectModel.tools().stream()
                .filter(tool -> tool.status() == ToolStatus.ACTIVE || (includeDrafts && tool.status() == ToolStatus.DRAFT))
                .collect(Collectors.toUnmodifiableMap(ToolModel::id, tool -> tool));
    }

    private static Map<String, RouteModel> defaultLocaleToolRoutes(ProjectModel projectModel) {
        return projectModel.routes().stream()
                .filter(route -> route.pageType() == RouteType.TOOL)
                .filter(route -> route.locale().equals(projectModel.site().defaultLocale()))
                .collect(Collectors.toUnmodifiableMap(RouteModel::sourceAggregateId, route -> route));
    }

    private static ToolModel withRelatedLinks(
            ToolModel tool,
            Map<ToolId, ToolModel> publicToolsById,
            Map<String, RouteModel> defaultLocaleToolRoutes,
            LocaleCode defaultLocale
    ) {
        if (!publicToolsById.containsKey(tool.id())) {
            return copy(tool, List.of());
        }

        LinkedHashMap<ToolId, Candidate> candidates = new LinkedHashMap<>();
        addExplicit(tool, publicToolsById, defaultLocaleToolRoutes, defaultLocale, candidates);
        addAutomatic(tool, publicToolsById, defaultLocaleToolRoutes, defaultLocale, candidates);

        List<RelatedLinkModel> relatedLinks = candidates.values().stream()
                .sorted(Comparator
                        .comparingInt(Candidate::orderBucket)
                        .thenComparing(Candidate::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(candidate -> candidate.toolId().value()))
                .map(Candidate::toModel)
                .toList();
        return copy(tool, relatedLinks);
    }

    private static void addExplicit(
            ToolModel source,
            Map<ToolId, ToolModel> publicToolsById,
            Map<String, RouteModel> defaultLocaleToolRoutes,
            LocaleCode defaultLocale,
            Map<ToolId, Candidate> candidates
    ) {
        for (ToolId targetId : source.relatedConfig().explicit()) {
            addCandidate(source, targetId, publicToolsById, defaultLocaleToolRoutes, defaultLocale, candidates, RelatedReason.EXPLICIT, 1);
        }
    }

    private static void addAutomatic(
            ToolModel source,
            Map<ToolId, ToolModel> publicToolsById,
            Map<String, RouteModel> defaultLocaleToolRoutes,
            LocaleCode defaultLocale,
            Map<ToolId, Candidate> candidates
    ) {
        RelatedConfigModel config = source.relatedConfig();
        if (config.sameCountry()) {
            addMatching(source, publicToolsById, defaultLocaleToolRoutes, defaultLocale, candidates, RelatedReason.SAME_COUNTRY, 2, target ->
                    source.optionalCountry().isPresent() && source.optionalCountry().equals(target.optionalCountry()));
        }
        if (config.sameCategory()) {
            addMatching(source, publicToolsById, defaultLocaleToolRoutes, defaultLocale, candidates, RelatedReason.SAME_CATEGORY, 3, target ->
                    source.category().equals(target.category()));
        }
        if (config.sameCapability()) {
            addMatching(source, publicToolsById, defaultLocaleToolRoutes, defaultLocale, candidates, RelatedReason.SAME_CAPABILITY, 4, target ->
                    intersects(source.capabilities(), target.capabilities()));
        }
        if (config.sameModule()) {
            addMatching(source, publicToolsById, defaultLocaleToolRoutes, defaultLocale, candidates, RelatedReason.SAME_MODULE, 5, target ->
                    source.module().equals(target.module()));
        }
    }

    private static void addMatching(
            ToolModel source,
            Map<ToolId, ToolModel> publicToolsById,
            Map<String, RouteModel> defaultLocaleToolRoutes,
            LocaleCode defaultLocale,
            Map<ToolId, Candidate> candidates,
            RelatedReason reason,
            int orderBucket,
            Predicate<ToolModel> predicate
    ) {
        publicToolsById.values().stream()
                .filter(target -> !target.id().equals(source.id()))
                .filter(predicate)
                .forEach(target -> addCandidate(source, target.id(), publicToolsById, defaultLocaleToolRoutes, defaultLocale, candidates, reason, orderBucket));
    }

    private static void addCandidate(
            ToolModel source,
            ToolId targetId,
            Map<ToolId, ToolModel> publicToolsById,
            Map<String, RouteModel> defaultLocaleToolRoutes,
            LocaleCode defaultLocale,
            Map<ToolId, Candidate> candidates,
            RelatedReason reason,
            int orderBucket
    ) {
        if (source.id().equals(targetId) || candidates.containsKey(targetId)) {
            return;
        }
        ToolModel target = publicToolsById.get(targetId);
        RouteModel route = defaultLocaleToolRoutes.get(targetId.value());
        if (target == null || route == null) {
            return;
        }
        candidates.put(targetId, new Candidate(
                targetId,
                reason,
                orderBucket,
                target.name().resolve(defaultLocale, defaultLocale),
                route
        ));
    }

    private static boolean intersects(Set<CapabilityId> source, Set<CapabilityId> target) {
        for (CapabilityId capability : source) {
            if (target.contains(capability)) {
                return true;
            }
        }
        return false;
    }

    private static ToolModel copy(ToolModel tool, List<RelatedLinkModel> relatedLinks) {
        return new ToolModel(
                tool.id(),
                tool.module(),
                tool.country(),
                tool.category(),
                tool.status(),
                tool.name(),
                tool.summary(),
                tool.capabilities(),
                tool.algorithmBinding(),
                tool.forms(),
                tool.aliases(),
                tool.seo(),
                tool.relatedConfig(),
                tool.routes(),
                relatedLinks
        );
    }

    private record Candidate(
            ToolId toolId,
            RelatedReason reason,
            int orderBucket,
            String title,
            RouteModel route
    ) {
        private RelatedLinkModel toModel() {
            return new RelatedLinkModel(toolId, reason, orderBucket, title, route);
        }
    }
}
