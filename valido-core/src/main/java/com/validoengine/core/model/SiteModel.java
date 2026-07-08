package com.validoengine.core.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record SiteModel(
        SiteId id,
        LocalizedText name,
        String baseUrl,
        LocaleCode defaultLocale,
        List<LocaleCode> locales,
        SiteMode mode,
        List<ModuleId> modules,
        Path outputDirectory,
        String defaultToolPath,
        boolean trailingSlash,
        boolean includeDrafts,
        SeoModel seo
) {
    public SiteModel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        baseUrl = IdentifierRules.requireText(baseUrl, "base URL");
        Objects.requireNonNull(defaultLocale, "defaultLocale");
        locales = List.copyOf(locales);
        if (locales.isEmpty()) {
            throw new IllegalArgumentException("locales must not be empty");
        }
        if (!locales.contains(defaultLocale)) {
            throw new IllegalArgumentException("locales must contain defaultLocale");
        }
        Objects.requireNonNull(mode, "mode");
        modules = List.copyOf(modules);
        if (modules.isEmpty()) {
            throw new IllegalArgumentException("modules must not be empty");
        }
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        defaultToolPath = IdentifierRules.requireText(defaultToolPath, "default tool path");
        seo = seo == null ? SeoModel.empty() : seo;
    }
}
