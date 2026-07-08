package com.validoengine.cli;

import java.nio.file.Path;
import java.util.Optional;

public record CliOptions(
        EngineCommand command,
        Path sitePath,
        String modeOverride,
        Path outputOverride,
        OutputFormat format
) {
    public CliOptions {
        java.util.Objects.requireNonNull(command, "command");
        sitePath = sitePath == null ? Path.of("site.yaml") : sitePath;
        format = format == null ? OutputFormat.TEXT : format;
        if (modeOverride != null && !modeOverride.equals("demo") && !modeOverride.equals("production")) {
            throw new IllegalArgumentException("mode must be demo or production");
        }
    }

    public Optional<String> optionalModeOverride() {
        return Optional.ofNullable(modeOverride);
    }

    public Optional<Path> optionalOutputOverride() {
        return Optional.ofNullable(outputOverride);
    }
}
