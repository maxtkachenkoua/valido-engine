package com.validoengine.content.load;

import com.validoengine.core.model.AlgorithmCompatibilityStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentProjectLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsValidDslProject() throws IOException {
        writeValidProject(tempDir, "demo", "validate");

        var result = new ContentProjectLoader().load(tempDir);

        assertTrue(result.successful());
        assertTrue(result.report().diagnostics().isEmpty());
        var project = result.optionalProject().orElseThrow();
        assertEquals("validohub", project.site().id().value());
        assertEquals(1, project.categories().size());
        assertEquals(1, project.tools().size());
        assertEquals(AlgorithmCompatibilityStatus.COMPATIBLE, project.tools().get(0).algorithmBinding().compatibilityStatus());
        assertEquals(1, project.content().blocksByTool().size());
    }

    @Test
    void productionModeFailsIncompatibleAlgorithmBinding() throws IOException {
        writeValidProject(tempDir, "production", "parse");

        var result = new ContentProjectLoader().load(tempDir);

        assertFalse(result.successful());
        assertTrue(result.report().hasErrors());
        assertTrue(result.report().diagnostics().stream()
                .anyMatch(diagnostic -> "TOOL_ALGORITHM_INCOMPATIBLE".equals(diagnostic.code())));
    }

    private static void writeValidProject(Path root, String mode, String toolCapability) throws IOException {
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
                    capabilities: [validate]
                    outputs:
                      validate:
                        - name: valid
                          type: boolean
                    errors: []
                    examples:
                      - capability: validate
                        input:
                          value: abc
                        expected:
                          valid: true
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
                        required: true
                    actions: [%s]
                """.formatted(toolCapability, toolCapability, toolCapability));
        write(root.resolve("content/tools/base64-encoder/explanation.en.md"), """
                ---
                title:
                  en: Base64 explained
                order: 10
                status: active
                ---
                Base64 content.
                """);
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
