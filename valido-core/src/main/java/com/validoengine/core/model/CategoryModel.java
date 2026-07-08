package com.validoengine.core.model;

import java.util.Objects;
import java.util.Optional;

public record CategoryModel(
        CategoryId id,
        String slug,
        LocalizedText name,
        LocalizedText summary,
        CategoryId parent,
        int order,
        SeoModel seo
) {
    public CategoryModel {
        Objects.requireNonNull(id, "id");
        slug = IdentifierRules.requireText(slug, "category slug");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(summary, "summary");
        seo = seo == null ? SeoModel.empty() : seo;
    }

    public Optional<CategoryId> optionalParent() {
        return Optional.ofNullable(parent);
    }
}
