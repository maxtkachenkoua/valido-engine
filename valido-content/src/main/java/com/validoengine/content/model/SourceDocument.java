package com.validoengine.content.model;

import java.nio.file.Path;
import java.util.Objects;

public record SourceDocument<T>(Path path, T document) {
    public SourceDocument {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(document, "document");
    }
}
