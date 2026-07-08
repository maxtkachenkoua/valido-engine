package com.validoengine.generator;

import com.validoengine.generator.model.GenerationPhase;
import com.validoengine.generator.model.GenerationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidoGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDeterministicPhaseTwoOneRoutes() throws IOException {
        writeProject(tempDir, "demo", "validate", "validate");

        var result = new ValidoGenerator().generate(GenerationRequest.forProjectRoot(tempDir));

        assertTrue(result.successful());
        assertEquals(GenerationPhase.PROJECT_MODEL_ASSEMBLY, result.report().phase());
        var model = result.optionalProjectModel().orElseThrow();
        assertEquals("validohub", model.site().id().value());
        assertEquals(2, model.tools().size());
        assertEquals(1, model.categories().size());
        assertEquals(1, model.locales().size());
        assertEquals(1, model.capabilities().size());
        assertEquals(5, model.routes().size());
        assertEquals(5, result.report().routeCount());
        assertEquals(
                java.util.Set.of(
                        "/en/",
                        "/en/tools/base64-encoder/",
                        "/en/poland/pesel-validator/",
                        "/en/categories/encoding/",
                        "/en/poland/"
                ),
                model.routes().stream().map(route -> route.path()).collect(Collectors.toSet())
        );
        String base64Output = Path.of("generated", "validohub", "en", "tools", "base64-encoder", "index.html").toString();
        String peselOutput = Path.of("generated", "validohub", "en", "poland", "pesel-validator", "index.html").toString();
        assertTrue(model.routes().stream().anyMatch(route ->
                route.path().equals("/en/tools/base64-encoder/")
                        && route.outputFile().toString().equals(base64Output)
                        && route.canonicalUrl().equals("https://validohub.example/en/tools/base64-encoder/")));
        assertTrue(model.routes().stream().anyMatch(route ->
                route.path().equals("/en/poland/pesel-validator/")
                        && route.outputFile().toString().equals(peselOutput)
                        && route.canonicalUrl().equals("https://validohub.example/en/poland/pesel-validator/")));
        assertTrue(model.relatedLinks().isEmpty());
        assertTrue(model.exportPlan().exporters().isEmpty());
        assertTrue(model.exportPlan().generatedArtifacts().isEmpty());
    }

    @Test
    void stopsBeforeProjectModelWhenContentValidationFails() throws IOException {
        writeProject(tempDir, "production", "parse", "validate");

        var result = new ValidoGenerator().generate(GenerationRequest.forProjectRoot(tempDir));

        assertFalse(result.successful());
        assertTrue(result.optionalProjectModel().isEmpty());
        assertEquals(GenerationPhase.CONTENT_VALIDATION, result.report().phase());
        assertTrue(result.report().validationReport().hasErrors());
    }

    @Test
    void stopsBeforeProjectModelWhenRouteCollisionIsDetected() throws IOException {
        writeCollisionProject(tempDir);

        var result = new ValidoGenerator().generate(GenerationRequest.forProjectRoot(tempDir));

        assertFalse(result.successful());
        assertTrue(result.optionalProjectModel().isEmpty());
        assertEquals(GenerationPhase.ROUTE_GENERATION, result.report().phase());
        assertTrue(result.report().validationReport().diagnostics().stream()
                .anyMatch(diagnostic -> "ROUTE_COLLISION".equals(diagnostic.code())));
    }

    private static void writeProject(Path root, String mode, String toolCapability, String algorithmCapability) throws IOException {
        write(root.resolve("site.yaml"), """
                schemaVersion: 1
                id: validohub
                name:
                  en: ValidoHub
                baseUrl: https://validohub.example
                defaultLocale: en
                locales: [en]
                mode: %s
                modules: [validohub]
                output:
                  directory: generated/validohub
                """.formatted(mode));
        write(root.resolve("categories/encoding.yaml"), """
                schemaVersion: 1
                id: encoding
                slug: encoding
                name:
                  en: Encoding
                summary:
                  en: Encoding tools.
                """);
        write(root.resolve("countries/poland.yaml"), """
                schemaVersion: 1
                code: PL
                slug: poland
                name:
                  en: Poland
                """);
        write(root.resolve("algorithms/algorithms.yaml"), """
                schemaVersion: 1
                algorithms:
                  - algorithmId: validohub.base64
                    javaClass: com.validohub.algorithms.encoding.Base64Algorithm
                    version: 1
                    capabilities: [%s]
                  - algorithmId: validohub.pesel
                    javaClass: com.validohub.algorithms.poland.PeselAlgorithm
                    version: 1
                    capabilities: [%s]
                """.formatted(algorithmCapability, algorithmCapability));
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
                capabilities: [%s]
                algorithm:
                  algorithmId: validohub.base64
                forms:
                  %s:
                    inputs:
                      - type: text
                        name: input
                        label:
                          en: Input
                    actions: [%s]
                """.formatted(toolCapability, toolCapability, toolCapability));
        write(root.resolve("tools/pesel.yaml"), """
                schemaVersion: 1
                id: pesel-validator
                kind: tool
                module: validohub
                country: PL
                category: encoding
                status: active
                name:
                  en: PESEL Validator
                summary:
                  en: Validate Polish PESEL identifiers.
                capabilities: [%s]
                algorithm:
                  algorithmId: validohub.pesel
                forms:
                  %s:
                    inputs:
                      - type: text
                        name: input
                        label:
                          en: Input
                    actions: [%s]
                """.formatted(toolCapability, toolCapability, toolCapability));
        write(root.resolve("content/tools/base64-encoder/explanation.en.md"), "Base64 content.\n");
        write(root.resolve("content/tools/pesel-validator/explanation.en.md"), "PESEL content.\n");
    }

    private static void writeCollisionProject(Path root) throws IOException {
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
        write(root.resolve("countries/poland.yaml"), """
                schemaVersion: 1
                code: PL
                slug: categories
                name:
                  en: Poland
                """);
        write(root.resolve("algorithms/algorithms.yaml"), """
                schemaVersion: 1
                algorithms:
                  - algorithmId: validohub.example
                    javaClass: com.validohub.algorithms.ExampleAlgorithm
                    version: 1
                    capabilities: [validate]
                """);
        write(root.resolve("tools/colliding.yaml"), """
                schemaVersion: 1
                id: encoding
                kind: tool
                module: validohub
                country: PL
                category: encoding
                status: active
                name:
                  en: Colliding Tool
                summary:
                  en: Tool route intentionally collides with category route.
                capabilities: [validate]
                algorithm:
                  algorithmId: validohub.example
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
}
