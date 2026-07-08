package com.validoengine.cli;

import com.validoengine.core.diagnostic.Diagnostic;
import com.validoengine.exporter.html.HtmlExportResult;
import com.validoengine.generator.model.GenerationReport;
import com.validoengine.generator.model.GenerationResult;

import java.nio.file.Path;
import java.util.stream.Collectors;

public final class CliOutput {
    public String doctorText(GenerationResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("Valido Engine doctor\n");
        builder.append("phase: ").append(result.report().phase()).append('\n');
        builder.append("status: ").append(result.report().validationReport().hasErrors() ? "errors" : "ok").append('\n');
        if (result.report().validationReport().diagnostics().isEmpty()) {
            builder.append("diagnostics: none\n");
        } else {
            builder.append("diagnostics:\n");
            for (Diagnostic diagnostic : result.report().validationReport().diagnostics()) {
                builder.append("- [")
                        .append(diagnostic.severity())
                        .append("] ")
                        .append(diagnostic.code())
                        .append(": ")
                        .append(diagnostic.message())
                        .append('\n');
            }
        }
        return builder.toString();
    }

    public String statsText(GenerationResult result) {
        GenerationReport report = result.report();
        return """
                Valido Engine stats
                siteId: %s
                mode: %s
                locales: %d
                modules: %d
                tools: %d
                categories: %d
                countries: %d
                algorithms: %d
                exporters: %d
                generatedRoutes: %d
                """.formatted(
                result.optionalProjectModel().map(model -> model.site().id().value()).orElse("unknown"),
                result.optionalProjectModel().map(model -> model.site().mode().name().toLowerCase()).orElse("unknown"),
                report.localeCount(),
                result.optionalProjectModel().map(model -> model.site().modules().size()).orElse(0),
                report.toolCount(),
                report.categoryCount(),
                report.countryCount(),
                report.algorithmCount(),
                report.exporterCount(),
                report.routeCount()
        );
    }

    public String publishText(GenerationResult result, HtmlExportResult exportResult) {
        return """
                Valido Engine publish
                siteId: %s
                outputDirectory: %s
                writtenArtifacts: %d
                """.formatted(
                result.optionalProjectModel().map(model -> model.site().id().value()).orElse("unknown"),
                result.optionalProjectModel()
                        .flatMap(model -> model.exportPlan().targetDirectories().values().stream().findFirst())
                        .map(Path::toString)
                        .orElse("unknown"),
                exportResult.writtenFiles().size()
        );
    }

    public String doctorJson(GenerationResult result) {
        String diagnostics = result.report().validationReport().diagnostics().stream()
                .map(diagnostic -> """
                        {"severity":"%s","code":"%s","message":"%s"}""".formatted(
                        escape(diagnostic.severity().name()),
                        escape(diagnostic.code()),
                        escape(diagnostic.message())
                ))
                .collect(Collectors.joining(","));
        return """
                {"phase":"%s","successful":%s,"hasErrors":%s,"diagnostics":[%s]}
                """.formatted(
                escape(result.report().phase().name()),
                result.successful(),
                result.report().validationReport().hasErrors(),
                diagnostics
        );
    }

    public String statsJson(GenerationResult result) {
        GenerationReport report = result.report();
        return """
                {"siteId":"%s","mode":"%s","locales":%d,"modules":%d,"tools":%d,"categories":%d,"countries":%d,"algorithms":%d,"exporters":%d,"generatedRoutes":%d}
                """.formatted(
                escape(result.optionalProjectModel().map(model -> model.site().id().value()).orElse("unknown")),
                escape(result.optionalProjectModel().map(model -> model.site().mode().name().toLowerCase()).orElse("unknown")),
                report.localeCount(),
                result.optionalProjectModel().map(model -> model.site().modules().size()).orElse(0),
                report.toolCount(),
                report.categoryCount(),
                report.countryCount(),
                report.algorithmCount(),
                report.exporterCount(),
                report.routeCount()
        );
    }

    public String publishJson(GenerationResult result, HtmlExportResult exportResult) {
        String files = exportResult.writtenFiles().stream()
                .map(path -> "\"" + escape(path.toString()) + "\"")
                .collect(Collectors.joining(","));
        return """
                {"siteId":"%s","outputDirectory":"%s","writtenArtifacts":%d,"files":[%s]}
                """.formatted(
                escape(result.optionalProjectModel().map(model -> model.site().id().value()).orElse("unknown")),
                escape(result.optionalProjectModel()
                        .flatMap(model -> model.exportPlan().targetDirectories().values().stream().findFirst())
                        .map(Path::toString)
                        .orElse("unknown")),
                exportResult.writtenFiles().size(),
                files
        );
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
