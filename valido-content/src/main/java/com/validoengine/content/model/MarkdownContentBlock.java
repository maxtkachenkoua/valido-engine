package com.validoengine.content.model;

import com.validoengine.core.model.ContentBlockType;
import com.validoengine.core.model.ContentStatus;
import com.validoengine.core.model.LocaleCode;
import com.validoengine.core.model.LocalizedText;
import com.validoengine.core.model.ToolId;

import java.nio.file.Path;
import java.util.Objects;

public record MarkdownContentBlock(
        Path path,
        ToolId toolId,
        ContentBlockType block,
        LocaleCode locale,
        LocalizedText title,
        int order,
        ContentStatus status,
        String markdown
) {
    public MarkdownContentBlock {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(toolId, "toolId");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(locale, "locale");
        title = title == null ? LocalizedText.empty() : title;
        Objects.requireNonNull(status, "status");
        markdown = markdown == null ? "" : markdown;
    }
}
