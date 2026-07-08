package com.validoengine.content.markdown;

import java.util.Map;

public record MarkdownFrontMatter(
        Map<String, String> title,
        Integer order,
        String status,
        Map<String, Object> unknownFields
) {
    public MarkdownFrontMatter {
        title = title == null ? Map.of() : Map.copyOf(title);
        unknownFields = unknownFields == null ? Map.of() : Map.copyOf(unknownFields);
    }
}
