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
        Path outputDirectory = projectRoot.resolve(Path.of("target", "html-exporter-test"));
        deleteRecursively(outputDirectory);

        HtmlExportResult result = new HtmlExporter().export(projectModel);

        assertEquals(projectModel.routes().size() + 3, result.writtenFiles().size());
        assertTrue(Files.isRegularFile(outputDirectory.resolve(Path.of("en", "index.html"))));
        assertTrue(Files.isRegularFile(outputDirectory.resolve(Path.of("en", "tools", "base64-encoder", "index.html"))));
        assertTrue(Files.isRegularFile(outputDirectory.resolve(Path.of("en", "categories", "encoding", "index.html"))));
        assertTrue(Files.isRegularFile(outputDirectory.resolve(Path.of("en", "poland", "index.html"))));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("sitemap.xml")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("robots.txt")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("search-index.json")));

        String toolHtml = Files.readString(outputDirectory.resolve(Path.of("en", "tools", "base64-encoder", "index.html")));
        assertTrue(toolHtml.contains("<title>Base64 Encoder, Decoder, and Validator</title>"));
        assertTrue(toolHtml.contains("name=\"description\""));
        assertTrue(toolHtml.contains("rel=\"canonical\" href=\"https://validohub.example/en/tools/base64-encoder/\""));
        assertTrue(toolHtml.contains("rel=\"alternate\" hreflang=\"en\" href=\"https://validohub.example/en/tools/base64-encoder/\""));
        assertTrue(toolHtml.contains("class=\"site-header\""));
        assertTrue(toolHtml.contains("class=\"brand-mark\""));
        assertTrue(toolHtml.contains("class=\"primary-nav\""));
        assertTrue(toolHtml.contains("class=\"breadcrumbs\""));
        assertTrue(toolHtml.contains("class=\"site-footer\""));
        assertTrue(toolHtml.contains("position: sticky"));
        assertTrue(toolHtml.contains("class=\"page-shell\""));
        assertTrue(toolHtml.contains("class=\"workbench-card\""));
        assertTrue(toolHtml.contains("<h2>Run the tool</h2>"));
        assertTrue(toolHtml.contains("class=\"doc-accordion\""));
        assertTrue(toolHtml.contains("<summary>How Base64 encoding works</summary>"));
        assertTrue(toolHtml.contains("Continue with related tools"));
        assertTrue(toolHtml.contains("data-algorithm-id=\"validohub.base64\""));
        assertTrue(toolHtml.contains("data-action=\"encode\""));
        assertTrue(toolHtml.contains("data-action=\"decode\""));
        assertTrue(toolHtml.contains("data-action=\"validate\""));
        assertTrue(!toolHtml.contains("data-action=\"format\""));
        assertTrue(!toolHtml.contains("data-action=\"explain\""));
        assertTrue(toolHtml.contains("Copy result"));
        assertTrue(toolHtml.contains("Download result"));
        assertTrue(toolHtml.contains("data-tool-feedback"));
        assertTrue(toolHtml.contains("TextEncoder"));
        assertTrue(toolHtml.contains("TextDecoder"));
        assertTrue(toolHtml.contains("Enter text to encode."));
        assertTrue(toolHtml.contains("Decoded size:"));
        assertTrue(toolHtml.contains("Decoded size"));
        assertTrue(toolHtml.contains("Output length"));
        assertTrue(toolHtml.contains("aria-keyshortcuts"));
        assertTrue(toolHtml.contains("downloadResult"));
        assertTrue(toolHtml.contains("Base64URL"));
        assertTrue(!toolHtml.contains("This action is not available in the static preview."));
        assertTrue(toolHtml.contains("Base64 represents binary data as ASCII text."));
        assertTrue(toolHtml.contains("<p>Base64 represents binary data as ASCII text."));
        assertTrue(toolHtml.indexOf("class=\"workbench-card\"") < toolHtml.indexOf("class=\"content-card\""));

        String sitemap = Files.readString(outputDirectory.resolve("sitemap.xml"));
        assertTrue(sitemap.contains("<loc>https://validohub.example/en/tools/base64-encoder/</loc>"));
        assertTrue(sitemap.contains("<loc>https://validohub.example/en/poland/</loc>"));

        String robots = Files.readString(outputDirectory.resolve("robots.txt"));
        assertTrue(robots.contains("User-agent: *"));
        assertTrue(robots.contains("Sitemap: https://validohub.example/sitemap.xml"));

        String searchIndex = Files.readString(outputDirectory.resolve("search-index.json"));
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
