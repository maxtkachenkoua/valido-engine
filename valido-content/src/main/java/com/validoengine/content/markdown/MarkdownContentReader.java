package com.validoengine.content.markdown;

import com.fasterxml.jackson.core.type.TypeReference;
import com.validoengine.content.load.YamlDslReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MarkdownContentReader {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final YamlDslReader yamlReader;

    public MarkdownContentReader(YamlDslReader yamlReader) {
        this.yamlReader = yamlReader;
    }

    public ParsedMarkdown read(Path path) throws IOException {
        String text = Files.readString(path);
        if (!text.startsWith("---\n") && !text.startsWith("---\r\n")) {
            return new ParsedMarkdown(new MarkdownFrontMatter(Map.of(), 100, "active", Map.of()), text);
        }

        int bodyStart = findClosingFence(text);
        if (bodyStart < 0) {
            return new ParsedMarkdown(new MarkdownFrontMatter(Map.of(), 100, "active", Map.of()), text);
        }

        String yaml = text.substring(firstLineEnd(text), bodyStart);
        String body = text.substring(nextLineStart(text, bodyStart));
        Map<String, Object> raw = yamlReader.mapper().readValue(yaml, MAP_TYPE);
        Map<String, Object> unknown = new LinkedHashMap<>(raw);
        Map<String, String> title = readStringMap(raw.get("title"));
        Integer order = readInteger(raw.get("order"));
        String status = raw.get("status") == null ? null : raw.get("status").toString();
        unknown.remove("title");
        unknown.remove("order");
        unknown.remove("status");
        return new ParsedMarkdown(
                new MarkdownFrontMatter(title, order == null ? 100 : order, status == null ? "active" : status, unknown),
                body
        );
    }

    private static int findClosingFence(String text) {
        int searchFrom = firstLineEnd(text);
        while (searchFrom >= 0 && searchFrom < text.length()) {
            int lineEnd = lineEnd(text, searchFrom);
            String line = text.substring(searchFrom, lineEnd).trim();
            if ("---".equals(line)) {
                return searchFrom;
            }
            searchFrom = nextLineStart(text, lineEnd);
        }
        return -1;
    }

    private static int firstLineEnd(String text) {
        return lineEnd(text, 0);
    }

    private static int lineEnd(String text, int from) {
        int next = text.indexOf('\n', from);
        return next < 0 ? text.length() : next;
    }

    private static int nextLineStart(String text, int lineEnd) {
        if (lineEnd >= text.length()) {
            return text.length();
        }
        return lineEnd + 1;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readStringMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, String> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), String.valueOf(item)));
            return result;
        }
        return Map.of("en", String.valueOf(value));
    }

    private static Integer readInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value.toString());
    }
}
