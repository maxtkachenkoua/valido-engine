package com.validoengine.generator.model;

import java.nio.file.Path;
import java.util.Objects;

public record GenerationRequest(Path projectRoot, Path sitePath) {
    public GenerationRequest {
        Objects.requireNonNull(projectRoot, "projectRoot");
        sitePath = sitePath == null ? Path.of("site.yaml") : sitePath;
    }

    public static GenerationRequest forProjectRoot(Path projectRoot) {
        return new GenerationRequest(projectRoot, Path.of("site.yaml"));
    }
}
