package com.validoengine.core.model;

import java.nio.file.Path;
import java.util.Objects;

public record RouteModel(
        String path,
        Path outputFile,
        String canonicalUrl,
        LocaleCode locale,
        RouteType pageType,
        String sourceAggregateId
) {
    public RouteModel {
        path = IdentifierRules.requireText(path, "route path");
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("route path must start with '/'");
        }
        if (!path.endsWith("/")) {
            throw new IllegalArgumentException("public page route path must end with '/'");
        }
        Objects.requireNonNull(outputFile, "outputFile");
        canonicalUrl = IdentifierRules.requireText(canonicalUrl, "canonical URL");
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(pageType, "pageType");
        sourceAggregateId = IdentifierRules.requireText(sourceAggregateId, "source aggregate id");
    }
}
