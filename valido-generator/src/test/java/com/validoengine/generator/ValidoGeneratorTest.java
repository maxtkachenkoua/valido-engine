package com.validoengine.generator;

import com.validoengine.generator.model.GenerationPhase;
import com.validoengine.generator.model.GenerationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidoGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void constructsPhaseOneProjectModelWithoutRoutesOrExporters() throws IOException {
        writeProject(tempDir, "demo", "validate", "validate");

        var result = new ValidoGenerator().generate(GenerationRequest.forProjectRoot(tempDir));

        assertTrue(result.successful());
        assertEquals(GenerationPhase.PROJECT_MODEL_ASSEMBLY, result.report().phase());
        var model = result.optionalProjectModel().orElseThrow();
        assertEquals("validohub", model.site().id().value());
        assertEquals(1, model.tools().size());
        assertEquals(1, model.categories().size());
        assertEquals(1, model.locales().size());
        assertEquals(1, model.capabilities().size());
        assertTrue(model.routes().isEmpty());
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
        write(root.resolve("algorithms/algorithms.yaml"), """
                schemaVersion: 1
                algorithms:
                  - algorithmId: validohub.base64
                    javaClass: com.validohub.algorithms.encoding.Base64Algorithm
                    version: 1
                    capabilities: [%s]
                """.formatted(algorithmCapability));
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
        write(root.resolve("content/tools/base64-encoder/explanation.en.md"), "Base64 content.\n");
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
