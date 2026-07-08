package com.validoengine.cli;

import com.validoengine.generator.ValidoGenerator;
import com.validoengine.generator.model.GenerationRequest;
import com.validoengine.generator.model.GenerationResult;
import com.validoengine.exporter.html.HtmlExportResult;
import com.validoengine.exporter.html.HtmlExporter;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

public final class EngineCli {
    private final CliParser parser;
    private final ValidoGenerator generator;
    private final HtmlExporter htmlExporter;
    private final CliOutput output;

    public EngineCli() {
        this(new CliParser(), new ValidoGenerator(), new HtmlExporter(), new CliOutput());
    }

    public EngineCli(CliParser parser, ValidoGenerator generator, HtmlExporter htmlExporter, CliOutput output) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.htmlExporter = Objects.requireNonNull(htmlExporter, "htmlExporter");
        this.output = Objects.requireNonNull(output, "output");
    }

    public int run(String[] args, PrintStream stdout, PrintStream stderr) {
        Objects.requireNonNull(stdout, "stdout");
        Objects.requireNonNull(stderr, "stderr");
        try {
            CliParseResult parsed = parser.parse(args);
            if (!parsed.successful()) {
                stderr.println(parsed.errorMessage());
                printUsage(stderr);
                return EngineExitCode.INVALID_USAGE;
            }
            return run(parsed.options(), stdout, stderr);
        } catch (RuntimeException exception) {
            stderr.println("Unexpected engine failure: " + exception.getMessage());
            return EngineExitCode.UNEXPECTED_ENGINE_FAILURE;
        }
    }

    private int run(CliOptions options, PrintStream stdout, PrintStream stderr) {
        if (options.optionalModeOverride().isPresent()) {
            stdout.println("note: --mode is parsed in Phase 1 but generation uses site.yaml mode until generator overrides are implemented.");
        }
        if (options.optionalOutputOverride().isPresent()) {
            stdout.println("note: --output is parsed but generation uses site.yaml output until generator overrides are implemented.");
        }
        return switch (options.command()) {
            case DOCTOR -> runDoctor(options, stdout);
            case STATS -> runStats(options, stdout);
            case PUBLISH -> runPublish(options, stdout);
            case BRAIN -> notImplemented("engine brain", stdout);
        };
    }

    private int runDoctor(CliOptions options, PrintStream stdout) {
        GenerationResult result = generate(options);
        if (options.format() == OutputFormat.JSON) {
            stdout.println(output.doctorJson(result));
        } else {
            stdout.print(output.doctorText(result));
        }
        return result.report().validationReport().hasErrors()
                ? EngineExitCode.VALIDATION_OR_GENERATION_ERROR
                : EngineExitCode.SUCCESS;
    }

    private int runStats(CliOptions options, PrintStream stdout) {
        GenerationResult result = generate(options);
        if (options.format() == OutputFormat.JSON) {
            stdout.println(output.statsJson(result));
        } else {
            stdout.print(output.statsText(result));
        }
        return result.successful()
                ? EngineExitCode.SUCCESS
                : EngineExitCode.VALIDATION_OR_GENERATION_ERROR;
    }

    private int runPublish(CliOptions options, PrintStream stdout) {
        GenerationResult result = generate(options);
        if (!result.successful() || result.optionalProjectModel().isEmpty()) {
            if (options.format() == OutputFormat.JSON) {
                stdout.println(output.doctorJson(result));
            } else {
                stdout.print(output.doctorText(result));
            }
            return EngineExitCode.VALIDATION_OR_GENERATION_ERROR;
        }
        HtmlExportResult exportResult = htmlExporter.export(result.optionalProjectModel().orElseThrow());
        if (options.format() == OutputFormat.JSON) {
            stdout.println(output.publishJson(result, exportResult));
        } else {
            stdout.print(output.publishText(result, exportResult));
        }
        return EngineExitCode.SUCCESS;
    }

    private GenerationResult generate(CliOptions options) {
        Path sitePath = options.sitePath();
        Path projectRoot = sitePath.toAbsolutePath().normalize().getParent();
        if (projectRoot == null) {
            projectRoot = Path.of(".").toAbsolutePath().normalize();
        }
        Path relativeSitePath = projectRoot.relativize(sitePath.toAbsolutePath().normalize());
        return generator.generate(new GenerationRequest(projectRoot, relativeSitePath));
    }

    private int notImplemented(String command, PrintStream stdout) {
        stdout.println(command + " is not implemented in Phase 1.");
        return EngineExitCode.VALIDATION_OR_GENERATION_ERROR;
    }

    private static void printUsage(PrintStream stream) {
        stream.println("Usage: engine <doctor|stats|publish|brain> [--site <path>] [--mode <demo|production>] [--output <directory>] [--format <text|json>]");
    }
}
