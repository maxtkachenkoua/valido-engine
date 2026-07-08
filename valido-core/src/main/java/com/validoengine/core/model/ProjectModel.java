package com.validoengine.core.model;

import com.validoengine.core.algorithm.AlgorithmMetadata;
import com.validoengine.core.capability.CapabilityModel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ProjectModel(
        SiteModel site,
        List<ToolModel> tools,
        List<CategoryModel> categories,
        List<CountryModel> countries,
        List<LocaleModel> locales,
        ContentModel content,
        Map<ToolId, AlgorithmBindingModel> algorithmBindings,
        Map<AlgorithmId, AlgorithmMetadata> algorithmMetadata,
        List<RouteModel> routes,
        List<RelatedLinkModel> relatedLinks,
        List<CapabilityModel> capabilities,
        ExportPlan exportPlan
) {
    public ProjectModel {
        Objects.requireNonNull(site, "site");
        tools = tools == null ? List.of() : List.copyOf(tools);
        categories = categories == null ? List.of() : List.copyOf(categories);
        countries = countries == null ? List.of() : List.copyOf(countries);
        locales = locales == null ? List.of() : List.copyOf(locales);
        content = content == null ? ContentModel.empty() : content;
        algorithmBindings = algorithmBindings == null ? Map.of() : Map.copyOf(algorithmBindings);
        algorithmMetadata = algorithmMetadata == null ? Map.of() : Map.copyOf(algorithmMetadata);
        routes = routes == null ? List.of() : List.copyOf(routes);
        relatedLinks = relatedLinks == null ? List.of() : List.copyOf(relatedLinks);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        exportPlan = exportPlan == null ? ExportPlan.empty() : exportPlan;
    }
}
