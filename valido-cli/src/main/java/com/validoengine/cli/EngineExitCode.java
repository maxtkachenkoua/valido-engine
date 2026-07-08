package com.validoengine.cli;

public final class EngineExitCode {
    public static final int SUCCESS = 0;
    public static final int VALIDATION_OR_GENERATION_ERROR = 1;
    public static final int INVALID_USAGE = 2;
    public static final int UNEXPECTED_ENGINE_FAILURE = 3;

    private EngineExitCode() {
    }
}
