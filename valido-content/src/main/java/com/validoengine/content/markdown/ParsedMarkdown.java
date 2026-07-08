package com.validoengine.content.markdown;

public record ParsedMarkdown(MarkdownFrontMatter frontMatter, String body) {
    public ParsedMarkdown {
        body = body == null ? "" : body;
    }
}
