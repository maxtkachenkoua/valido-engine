package com.validoengine.core.model;

import java.util.Map;
import java.util.Objects;

public record ContentBlockModel(
        ToolId toolId,
        ContentBlockType block,
        LocaleCode locale,
        LocalizedText title,
        int order,
        ContentStatus status,
        String markdown,
        Map<String, Object> structuredItems
) {
    public ContentBlockModel {
        Objects.requireNonNull(toolId, "toolId");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(locale, "locale");
        title = title == null ? LocalizedText.empty() : title;
        Objects.requireNonNull(status, "status");
        if (markdown != null && markdown.isBlank()) {
            throw new IllegalArgumentException("markdown must not be blank when present");
        }
        structuredItems = structuredItems == null ? Map.of() : Map.copyOf(structuredItems);
    }
}
