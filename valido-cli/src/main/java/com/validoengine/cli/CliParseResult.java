package com.validoengine.cli;

import java.util.Optional;

public record CliParseResult(CliOptions options, String errorMessage) {
    public static CliParseResult success(CliOptions options) {
        return new CliParseResult(options, null);
    }

    public static CliParseResult error(String message) {
        return new CliParseResult(null, message);
    }

    public boolean successful() {
        return options != null;
    }

    public Optional<CliOptions> optionalOptions() {
        return Optional.ofNullable(options);
    }
}
