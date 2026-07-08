package com.validoengine.core.model;

import java.util.Set;

public record LocaleModel(
        LocaleCode code,
        boolean defaultLocale,
        Set<ToolId> toolsWithContent
) {
    public LocaleModel {
        java.util.Objects.requireNonNull(code, "code");
        toolsWithContent = toolsWithContent == null ? Set.of() : Set.copyOf(toolsWithContent);
    }
}
