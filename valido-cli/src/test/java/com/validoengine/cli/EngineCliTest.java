package com.validoengine.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineCliTest {
    @TempDir
    Path tempDir;

    @Test
    void doctorReturnsSuccessForValidProject() throws IOException {
        writeProject(tempDir);

        CliRun run = run("engine", "doctor", "--site", tempDir.resolve("site.yaml").toString());

        assertEquals(EngineExitCode.SUCCESS, run.exitCode());
        assertTrue(run.stdout().contains("Valido Engine doctor"));
        assertTrue(run.stdout().contains("diagnostics: none"));
    }

    @Test
    void statsReturnsProjectCounts() throws IOException {
        writeProject(tempDir);

        CliRun run = run("stats", "--site", tempDir.resolve("site.yaml").toString());

        assertEquals(EngineExitCode.SUCCESS, run.exitCode());
        assertTrue(run.stdout().contains("tools: 1"));
        assertTrue(run.stdout().contains("categories: 1"));
        assertTrue(run.stdout().contains("algorithms: 1"));
    }

    @Test
    void publishGeneratesStaticSiteFromExamples() throws IOException {
        Path examples = copyExamplesToTempProject();

        CliRun run = run("engine", "publish", "--site", examples.resolve("site.yaml").toString());

        Path output = examples.resolve(Path.of("generated", "validohub"));
        assertEquals(EngineExitCode.SUCCESS, run.exitCode());
        assertTrue(run.stdout().contains("Valido Engine publish"));
        assertTrue(run.stdout().contains("writtenArtifacts:"));
        assertTrue(Files.isRegularFile(output.resolve(Path.of("en", "index.html"))));
        assertTrue(Files.isRegularFile(output.resolve(Path.of("en", "tools", "base64-encoder", "index.html"))));
        assertTrue(Files.isRegularFile(output.resolve("sitemap.xml")));
        assertTrue(Files.isRegularFile(output.resolve("robots.txt")));
        assertTrue(Files.isRegularFile(output.resolve("search-index.json")));
        assertTrue(Files.notExists(output.resolve("api-metadata.json")));
    }

    @Test
    void unknownCommandReturnsInvalidUsage() {
        CliRun run = run("engine", "architect");

        assertEquals(EngineExitCode.INVALID_USAGE, run.exitCode());
        assertTrue(run.stderr().contains("Unknown command"));
    }

    private CliRun run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EngineCli().run(
                args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8)
        );
        return new CliRun(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8)
        );
    }

    private static void writeProject(Path root) throws IOException {
        write(root.resolve("site.yaml"), """
                schemaVersion: 1
                id: validohub
                name:
                  en: ValidoHub
                baseUrl: https://validohub.example
                defaultLocale: en
                locales: [en]
                mode: demo
                modules: [validohub]
                output:
                  directory: generated/validohub
                """);
        write(root.resolve("categories/encoding.yaml"), """
                schemaVersion: 1
                id: encoding
                slug: encoding
                name:
                  en: Encoding
                summary:
                  en: Encoding tools.
                """);
        write(root.resolve("algorithms/algorithms.yaml"), """
                schemaVersion: 1
                algorithms:
                  - algorithmId: validohub.base64
                    javaClass: com.validohub.algorithms.encoding.Base64Algorithm
                    version: 1
                    capabilities: [validate]
                """);
        write(root.resolve("tools/base64.yaml"), """
                schemaVersion: 1
                id: base64-encoder
                kind: tool
                module: validohub
                category: encoding
                status: active
                name:
                  en: Base64 Toolkit
                summary:
                  en: Encode and validate Base64.
                capabilities: [validate]
                algorithm:
                  algorithmId: validohub.base64
                forms:
                  validate:
                    inputs:
                      - type: text
                        name: input
                        label:
                          en: Input
                    actions: [validate]
                """);
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private Path copyExamplesToTempProject() throws IOException {
        Path source = examplesRoot();
        Path target = tempDir.resolve("examples");
        try (var paths = Files.walk(source)) {
            for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination);
                }
            }
        }
        return target;
    }

    private static Path examplesRoot() {
        Path moduleRelative = Path.of("..", "valido-examples", "src", "main", "resources").toAbsolutePath().normalize();
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative;
        }
        return Path.of("valido-examples", "src", "main", "resources").toAbsolutePath().normalize();
    }

    private record CliRun(int exitCode, String stdout, String stderr) {
    }
}
