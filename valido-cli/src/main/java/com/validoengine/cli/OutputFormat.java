package com.validoengine.cli;

import java.util.Locale;
import java.util.Optional;

public enum OutputFormat {
    TEXT("text"),
    JSON("json");

    private final String token;

    OutputFormat(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public static Optional<OutputFormat> fromToken(String token) {
        String normalized = token == null ? "" : token.toLowerCase(Locale.ROOT);
        for (OutputFormat format : values()) {
            if (format.token.equals(normalized)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
