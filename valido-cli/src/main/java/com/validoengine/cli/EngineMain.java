package com.validoengine.cli;

public final class EngineMain {
    private EngineMain() {
    }

    public static void main(String[] args) {
        int exitCode = new EngineCli().run(args, System.out, System.err);
        System.exit(exitCode);
    }
}
