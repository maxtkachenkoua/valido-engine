package com.validoengine.cli;

import java.nio.file.Path;
import java.util.Arrays;

public final class CliParser {
    public CliParseResult parse(String[] args) {
        if (args == null || args.length == 0) {
            return CliParseResult.error("Missing command. Expected: engine <doctor|stats|publish|brain>.");
        }
        int index = 0;
        if ("engine".equals(args[index])) {
            index++;
        }
        if (index >= args.length) {
            return CliParseResult.error("Missing command. Expected: engine <doctor|stats|publish|brain>.");
        }
        var command = EngineCommand.fromToken(args[index]);
        if (command.isEmpty()) {
            return CliParseResult.error("Unknown command: " + args[index]);
        }
        index++;

        Path sitePath = Path.of("site.yaml");
        String modeOverride = null;
        Path outputOverride = null;
        OutputFormat format = OutputFormat.TEXT;

        while (index < args.length) {
            String option = args[index++];
            switch (option) {
                case "--site" -> {
                    if (index >= args.length) {
                        return CliParseResult.error("--site requires a path.");
                    }
                    sitePath = Path.of(args[index++]);
                }
                case "--mode" -> {
                    if (index >= args.length) {
                        return CliParseResult.error("--mode requires demo or production.");
                    }
                    modeOverride = args[index++];
                    if (!modeOverride.equals("demo") && !modeOverride.equals("production")) {
                        return CliParseResult.error("--mode must be demo or production.");
                    }
                }
                case "--output" -> {
                    if (index >= args.length) {
                        return CliParseResult.error("--output requires a directory.");
                    }
                    outputOverride = Path.of(args[index++]);
                }
                case "--format" -> {
                    if (index >= args.length) {
                        return CliParseResult.error("--format requires text or json.");
                    }
                    var parsed = OutputFormat.fromToken(args[index++]);
                    if (parsed.isEmpty()) {
                        return CliParseResult.error("--format must be text or json.");
                    }
                    format = parsed.get();
                }
                default -> {
                    return CliParseResult.error("Unknown option: " + option + " in " + Arrays.toString(args));
                }
            }
        }

        return CliParseResult.success(new CliOptions(command.get(), sitePath, modeOverride, outputOverride, format));
    }
}
