package com.validoengine.exporter.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validoengine.generator.ValidoGenerator;
import com.validoengine.generator.model.GenerationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiMetadataExporterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void exportsApiMetadataFromValidoExamplesProjectModel() throws IOException {
        Path projectRoot = copyExamplesToTempProject();

        var generation = new ValidoGenerator().generate(GenerationRequest.forProjectRoot(projectRoot));
        assertTrue(generation.successful(), () -> generation.report().validationReport().diagnostics().toString());
        var projectModel = generation.optionalProjectModel().orElseThrow();

        ApiMetadataExportResult result = new ApiMetadataExporter().export(projectModel);

        Path outputFile = projectRoot.resolve(Path.of("target", "api-metadata-exporter-test", "api-metadata.json"));
        assertEquals(outputFile, result.writtenFile());
        assertTrue(Files.isRegularFile(outputFile));

        String json = Files.readString(outputFile);
        JsonNode root = objectMapper.readTree(json);
        assertEquals(ApiMetadataExporterDescriptor.METADATA_VERSION, root.path("metadataVersion").asInt());
        assertEquals("validohub", root.path("siteId").asText());
        assertFalse(root.path("generatedAt").asText().isBlank());

        JsonNode base64 = findTool(root, "base64-encoder");
        assertEquals("validohub.base64", base64.path("algorithmId").asText());
        assertEquals(1, base64.path("version").asInt());
        assertTrue(containsText(base64.path("capabilities"), "encode"));
        assertTrue(containsText(base64.path("capabilities"), "decode"));
        assertTrue(containsText(base64.path("capabilities"), "validate"));
        assertTrue(containsText(base64.path("capabilities"), "explain"));
        assertTrue(containsText(base64.path("capabilities"), "format"));
        assertEquals("input", base64.path("inputs").path("encode").get(0).path("name").asText());
        assertEquals("output", base64.path("outputs").path("encode").get(0).path("name").asText());
        assertTrue(containsField(base64.path("errors"), "code", "INVALID_BASE64"));
        assertTrue(containsField(base64.path("examples"), "capability", "encode"));
        assertEquals("/en/tools/base64-encoder/", base64.path("routes").path("en").asText());

        JsonNode pesel = findTool(root, "pesel-validator");
        assertEquals("validohub.pesel", pesel.path("algorithmId").asText());
        assertTrue(containsField(pesel.path("errors"), "code", "INVALID_CHECKSUM"));

        assertFalse(json.contains("javaClass"));
        assertFalse(json.contains("com.validohub.algorithms"));
        assertFalse(json.contains("PeselAlgorithm"));
        assertFalse(json.contains("Base64Algorithm"));
        assertFalse(json.contains("UuidAlgorithm"));
        assertFalse(json.contains("content/tools"));
        assertFalse(json.contains("site.yaml"));
    }

    private static JsonNode findTool(JsonNode root, String toolId) {
        for (JsonNode tool : root.path("tools")) {
            if (toolId.equals(tool.path("toolId").asText())) {
                return tool;
            }
        }
        throw new AssertionError("Missing tool metadata for " + toolId);
    }

    private static boolean containsText(JsonNode array, String value) {
        for (JsonNode item : array) {
            if (value.equals(item.asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsField(JsonNode array, String field, String value) {
        for (JsonNode item : array) {
            if (value.equals(item.path(field).asText())) {
                return true;
            }
        }
        return false;
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
        Path site = target.resolve("site.yaml");
        String siteYaml = Files.readString(site)
                .replace("directory: generated/validohub", "directory: target/api-metadata-exporter-test");
        Files.writeString(site, siteYaml);
        return target;
    }

    private static Path examplesRoot() {
        Path moduleRelative = Path.of("..", "valido-examples", "src", "main", "resources").toAbsolutePath().normalize();
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative;
        }
        return Path.of("valido-examples", "src", "main", "resources").toAbsolutePath().normalize();
    }
}
