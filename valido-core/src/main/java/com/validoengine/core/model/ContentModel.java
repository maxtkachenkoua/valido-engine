package com.validoengine.core.model;

import java.util.List;
import java.util.Map;

public record ContentModel(
        Map<ToolId, List<ContentBlockModel>> blocksByTool
) {
    public ContentModel {
        blocksByTool = blocksByTool == null ? Map.of() : copy(blocksByTool);
    }

    public static ContentModel empty() {
        return new ContentModel(Map.of());
    }

    private static Map<ToolId, List<ContentBlockModel>> copy(Map<ToolId, List<ContentBlockModel>> source) {
        return source.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }
}
