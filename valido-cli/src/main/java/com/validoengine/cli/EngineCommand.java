package com.validoengine.cli;

import java.util.Locale;
import java.util.Optional;

public enum EngineCommand {
    DOCTOR("doctor"),
    STATS("stats"),
    PUBLISH("publish"),
    BRAIN("brain");

    private final String token;

    EngineCommand(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public static Optional<EngineCommand> fromToken(String token) {
        String normalized = token == null ? "" : token.toLowerCase(Locale.ROOT);
        for (EngineCommand command : values()) {
            if (command.token.equals(normalized)) {
                return Optional.of(command);
            }
        }
        return Optional.empty();
    }
}
