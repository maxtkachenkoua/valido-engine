package com.validoengine.core.model;

import com.validoengine.core.capability.CapabilityId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ToolModel(
        ToolId id,
        ModuleId module,
        CountryCode country,
        CategoryId category,
        ToolStatus status,
        LocalizedText name,
        LocalizedText summary,
        Set<CapabilityId> capabilities,
        AlgorithmBindingModel algorithmBinding,
        Map<CapabilityId, FormModel> forms,
        LocalizedStringList aliases,
        SeoModel seo,
        Map<LocaleCode, RouteModel> routes,
        List<RelatedLinkModel> relatedLinks
) {
    public ToolModel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(summary, "summary");
        capabilities = Set.copyOf(capabilities);
        if (capabilities.isEmpty()) {
            throw new IllegalArgumentException("tool capabilities must not be empty");
        }
        Objects.requireNonNull(algorithmBinding, "algorithmBinding");
        forms = forms == null ? Map.of() : Map.copyOf(forms);
        aliases = aliases == null ? LocalizedStringList.empty() : aliases;
        seo = seo == null ? SeoModel.empty() : seo;
        routes = routes == null ? Map.of() : Map.copyOf(routes);
        relatedLinks = relatedLinks == null ? List.of() : List.copyOf(relatedLinks);
    }

    public Optional<CountryCode> optionalCountry() {
        return Optional.ofNullable(country);
    }
}
