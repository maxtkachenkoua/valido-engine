package com.validoengine.generator;

import com.validoengine.generator.model.GenerationPhase;
import com.validoengine.generator.model.GenerationRequest;
import com.validoengine.core.model.ExporterId;
import com.validoengine.core.model.RelatedReason;
import com.validoengine.core.model.ToolId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        assertEquals(1, result.report().exporterCount());
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
        assertEquals(2, model.relatedLinks().size());
        assertEquals(List.of(new ExporterId("html")), model.exportPlan().exporters());
        assertEquals(Path.of("generated", "validohub"), model.exportPlan().targetDirectories().get(new ExporterId("html")));
        List<String> plannedArtifacts = model.exportPlan().generatedArtifacts().stream().map(Path::toString).toList();
        assertEquals(model.routes().size() + 3, plannedArtifacts.size());
        assertTrue(plannedArtifacts.containsAll(model.routes().stream().map(route -> route.outputFile().toString()).toList()));
        assertTrue(plannedArtifacts.contains(Path.of("generated", "validohub", "sitemap.xml").toString()));
        assertTrue(plannedArtifacts.contains(Path.of("generated", "validohub", "robots.txt").toString()));
        assertTrue(plannedArtifacts.contains(Path.of("generated", "validohub", "search-index.json").toString()));
        assertTrue(model.exportPlan().reportPaths().isEmpty());
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

    @Test
    void resolvesRelatedLinksInDeterministicOrderAndDeduplicates() throws IOException {
        writeRelatedProject(tempDir);

        var result = new ValidoGenerator().generate(GenerationRequest.forProjectRoot(tempDir));

        assertTrue(result.successful());
        var model = result.optionalProjectModel().orElseThrow();
        var source = model.tools().stream()
                .filter(tool -> tool.id().equals(new ToolId("source-tool")))
                .findFirst()
                .orElseThrow();

        assertEquals(
                List.of(
                        "country-tool",
                        "country-second-tool",
                        "category-alpha-tool",
                        "category-zulu-tool",
                        "capability-tool",
                        "module-tool"
                ),
                source.relatedLinks().stream().map(link -> link.toolId().value()).toList()
        );
        assertEquals(
                List.of(
                        RelatedReason.EXPLICIT,
                        RelatedReason.SAME_COUNTRY,
                        RelatedReason.SAME_CATEGORY,
                        RelatedReason.SAME_CATEGORY,
                        RelatedReason.SAME_CAPABILITY,
                        RelatedReason.SAME_MODULE
                ),
                source.relatedLinks().stream().map(link -> link.reason()).toList()
        );
        assertEquals(1, source.relatedLinks().stream()
                .filter(link -> link.toolId().equals(new ToolId("country-tool")))
                .count());
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

    private static void writeRelatedProject(Path root) throws IOException {
        write(root.resolve("site.yaml"), """
                schemaVersion: 1
                id: validohub
                name:
                  en: ValidoHub
                baseUrl: https://validohub.example
                defaultLocale: en
                locales: [en]
                mode: demo
                modules: [validohub, otherhub]
                output:
                  directory: generated/validohub
                """);
        write(root.resolve("categories/source.yaml"), """
                schemaVersion: 1
                id: source-category
                slug: source-category
                name:
                  en: Source
                summary:
                  en: Source category.
                """);
        write(root.resolve("categories/other.yaml"), """
                schemaVersion: 1
                id: other-category
                slug: other-category
                name:
                  en: Other
                summary:
                  en: Other category.
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
                  - algorithmId: validohub.validate
                    javaClass: com.validohub.algorithms.ValidateAlgorithm
                    version: 1
                    capabilities: [validate]
                  - algorithmId: validohub.generate
                    javaClass: com.validohub.algorithms.GenerateAlgorithm
                    version: 1
                    capabilities: [generate]
                  - algorithmId: validohub.decode
                    javaClass: com.validohub.algorithms.DecodeAlgorithm
                    version: 1
                    capabilities: [decode]
                """);

        writeTool(root, "source-tool", "Source Tool", "validohub", "source-category", "PL", "validate", "validohub.validate", """
                related:
                  explicit:
                    - country-tool
                    - country-tool
                  auto:
                    sameCountry: true
                    sameCategory: true
                    sameCapability: true
                    sameModule: true
                """);
        writeTool(root, "country-tool", "Country Tool", "otherhub", "other-category", "PL", "generate", "validohub.generate", "");
        writeTool(root, "country-second-tool", "Second Country Tool", "otherhub", "other-category", "PL", "generate", "validohub.generate", "");
        writeTool(root, "category-zulu-tool", "Zulu Category Tool", "otherhub", "source-category", null, "decode", "validohub.decode", "");
        writeTool(root, "category-alpha-tool", "Alpha Category Tool", "otherhub", "source-category", null, "generate", "validohub.generate", "");
        writeTool(root, "capability-tool", "Capability Tool", "otherhub", "other-category", null, "validate", "validohub.validate", "");
        writeTool(root, "module-tool", "Module Tool", "validohub", "other-category", null, "generate", "validohub.generate", "");
    }

    private static void writeTool(
            Path root,
            String id,
            String title,
            String module,
            String category,
            String country,
            String capability,
            String algorithmId,
            String related
    ) throws IOException {
        String countryLine = country == null ? "" : "country: " + country + "\n";
        write(root.resolve("tools/" + id + ".yaml"), """
                schemaVersion: 1
                id: %s
                kind: tool
                module: %s
                %scategory: %s
                status: active
                name:
                  en: %s
                summary:
                  en: %s summary.
                capabilities: [%s]
                algorithm:
                  algorithmId: %s
                forms:
                  %s:
                    inputs:
                      - type: text
                        name: input
                        label:
                          en: Input
                    actions: [%s]
                %s""".formatted(id, module, countryLine, category, title, title, capability, algorithmId, capability, capability, related));
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
