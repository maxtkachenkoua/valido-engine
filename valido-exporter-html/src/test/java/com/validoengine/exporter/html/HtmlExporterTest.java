package com.validoengine.exporter.html;

import com.validoengine.generator.ValidoGenerator;
import com.validoengine.generator.model.GenerationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void exportsStaticHtmlPagesFromValidoExamplesProjectModel() throws IOException {
        Path projectRoot = copyExamplesToTempProject();

        var generation = new ValidoGenerator().generate(GenerationRequest.forProjectRoot(projectRoot));
        assertTrue(generation.successful(), () -> generation.report().validationReport().diagnostics().toString());
        var projectModel = generation.optionalProjectModel().orElseThrow();
        deleteRecursively(Path.of("target", "html-exporter-test"));

        HtmlExportResult result = new HtmlExporter().export(projectModel);

        assertEquals(projectModel.routes().size() + 3, result.writtenFiles().size());
        assertTrue(Files.isRegularFile(Path.of("target", "html-exporter-test", "en", "index.html")));
        assertTrue(Files.isRegularFile(Path.of("target", "html-exporter-test", "en", "tools", "base64-encoder", "index.html")));
        assertTrue(Files.isRegularFile(Path.of("target", "html-exporter-test", "en", "categories", "encoding", "index.html")));
        assertTrue(Files.isRegularFile(Path.of("target", "html-exporter-test", "en", "poland", "index.html")));
        assertTrue(Files.isRegularFile(Path.of("target", "html-exporter-test", "sitemap.xml")));
        assertTrue(Files.isRegularFile(Path.of("target", "html-exporter-test", "robots.txt")));
        assertTrue(Files.isRegularFile(Path.of("target", "html-exporter-test", "search-index.json")));

        String toolHtml = Files.readString(Path.of("target", "html-exporter-test", "en", "tools", "base64-encoder", "index.html"));
        assertTrue(toolHtml.contains("<title>Base64 Encoder, Decoder, and Validator</title>"));
        assertTrue(toolHtml.contains("name=\"description\""));
        assertTrue(toolHtml.contains("rel=\"canonical\" href=\"https://validohub.example/en/tools/base64-encoder/\""));
        assertTrue(toolHtml.contains("rel=\"alternate\" hreflang=\"en\" href=\"https://validohub.example/en/tools/base64-encoder/\""));
        assertTrue(toolHtml.contains("Base64 represents binary data as ASCII text."));

        String sitemap = Files.readString(Path.of("target", "html-exporter-test", "sitemap.xml"));
        assertTrue(sitemap.contains("<loc>https://validohub.example/en/tools/base64-encoder/</loc>"));
        assertTrue(sitemap.contains("<loc>https://validohub.example/en/poland/</loc>"));

        String robots = Files.readString(Path.of("target", "html-exporter-test", "robots.txt"));
        assertTrue(robots.contains("User-agent: *"));
        assertTrue(robots.contains("Sitemap: https://validohub.example/sitemap.xml"));

        String searchIndex = Files.readString(Path.of("target", "html-exporter-test", "search-index.json"));
        assertTrue(searchIndex.contains("\"toolId\": \"base64-encoder\""));
        assertTrue(searchIndex.contains("\"title\": \"Base64 Toolkit\""));
        assertTrue(searchIndex.contains("\"route\": \"/en/tools/base64-encoder/\""));
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
                .replace("directory: generated/validohub", "directory: target/html-exporter-test");
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

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
