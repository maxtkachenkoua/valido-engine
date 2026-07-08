package com.validoengine.content.load;

import com.validoengine.content.dsl.AlgorithmRegistryDsl;
import com.validoengine.content.dsl.CategoryDsl;
import com.validoengine.content.dsl.CountryDsl;
import com.validoengine.content.dsl.SiteDsl;
import com.validoengine.content.dsl.StructuredContentBlockDsl;
import com.validoengine.content.dsl.ToolDsl;
import com.validoengine.content.markdown.MarkdownContentReader;
import com.validoengine.content.markdown.ParsedMarkdown;
import com.validoengine.content.model.MarkdownContentBlock;
import com.validoengine.content.model.RawContentWorkspace;
import com.validoengine.content.model.SourceDocument;
import com.validoengine.core.model.ContentBlockType;
import com.validoengine.core.model.ContentStatus;
import com.validoengine.core.model.LocaleCode;
import com.validoengine.core.model.LocalizedText;
import com.validoengine.core.model.ToolId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ContentDiscovery {
    private final YamlDslReader yamlReader;
    private final MarkdownContentReader markdownReader;

    public ContentDiscovery(YamlDslReader yamlReader) {
        this.yamlReader = yamlReader;
        this.markdownReader = new MarkdownContentReader(yamlReader);
    }

    public RawContentWorkspace discover(Path projectRoot, Path sitePath) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize();
        SourceDocument<SiteDsl> site = readSite(root, sitePath);
        List<SourceDocument<CategoryDsl>> categories = readCategories(root);
        List<SourceDocument<CountryDsl>> countries = readCountries(root);
        List<SourceDocument<ToolDsl>> tools = readTools(root);
        List<SourceDocument<AlgorithmRegistryDsl>> algorithms = readAlgorithmRegistries(root);
        List<SourceDocument<StructuredContentBlockDsl>> structuredBlocks = readStructuredContentBlocks(root);
        List<MarkdownContentBlock> markdownBlocks = readMarkdownBlocks(root);
        return new RawContentWorkspace(root, site, categories, countries, tools, algorithms, structuredBlocks, markdownBlocks);
    }

    private SourceDocument<SiteDsl> readSite(Path root, Path sitePath) throws IOException {
        Path resolved = sitePath == null ? root.resolve("site.yaml") : root.resolve(sitePath).normalize();
        if (!Files.exists(resolved)) {
            Path exampleSite = root.resolve("examples/site.yaml");
            resolved = Files.exists(exampleSite) ? exampleSite : resolved;
        }
        if (!Files.exists(resolved)) {
            return null;
        }
        return new SourceDocument<>(resolved, yamlReader.read(resolved, SiteDsl.class));
    }

    private List<SourceDocument<CategoryDsl>> readCategories(Path root) throws IOException {
        List<Path> paths = new ArrayList<>();
        paths.addAll(yamlFiles(root.resolve("categories")));
        paths.addAll(yamlFiles(root.resolve("examples/categories")));
        return readDocuments(paths, CategoryDsl.class);
    }

    private List<SourceDocument<CountryDsl>> readCountries(Path root) throws IOException {
        List<Path> paths = new ArrayList<>();
        paths.addAll(yamlFiles(root.resolve("countries")));
        paths.addAll(yamlFiles(root.resolve("examples/countries")));
        return readDocuments(paths, CountryDsl.class);
    }

    private List<SourceDocument<ToolDsl>> readTools(Path root) throws IOException {
        List<Path> paths = new ArrayList<>();
        paths.addAll(yamlFiles(root.resolve("tools")));
        paths.addAll(classifiedExampleYaml(root, "kind", "tool"));
        return readDocuments(paths, ToolDsl.class);
    }

    private List<SourceDocument<AlgorithmRegistryDsl>> readAlgorithmRegistries(Path root) throws IOException {
        List<Path> paths = new ArrayList<>();
        paths.addAll(yamlFiles(root.resolve("algorithms")));
        Path exampleRegistry = root.resolve("examples/algorithms.yaml");
        if (Files.exists(exampleRegistry)) {
            paths.add(exampleRegistry);
        }
        return readDocuments(paths, AlgorithmRegistryDsl.class);
    }

    private List<SourceDocument<StructuredContentBlockDsl>> readStructuredContentBlocks(Path root) throws IOException {
        List<Path> paths = new ArrayList<>();
        paths.addAll(yamlFiles(root.resolve("content/tools")));
        paths.addAll(classifiedExampleYaml(root, "toolId", null));
        return readDocuments(paths, StructuredContentBlockDsl.class);
    }

    private List<MarkdownContentBlock> readMarkdownBlocks(Path root) throws IOException {
        List<Path> paths = new ArrayList<>();
        paths.addAll(markdownFiles(root.resolve("content/tools")));
        paths.addAll(markdownFiles(root.resolve("examples")));
        List<MarkdownContentBlock> blocks = new ArrayList<>();
        for (Path path : paths) {
            inferMarkdownIdentity(root, path).ifPresent(identity -> {
                try {
                    ParsedMarkdown parsed = markdownReader.read(path);
                    blocks.add(new MarkdownContentBlock(
                            path,
                            new ToolId(identity.toolId()),
                            identity.block(),
                            new LocaleCode(identity.locale()),
                            toLocalizedText(parsed.frontMatter().title()),
                            parsed.frontMatter().order(),
                            parseContentStatus(parsed.frontMatter().status()),
                            parsed.body()
                    ));
                } catch (IOException ignored) {
                    throw new IllegalStateException("Unable to read markdown file: " + path, ignored);
                }
            });
        }
        return List.copyOf(blocks);
    }

    private <T> List<SourceDocument<T>> readDocuments(List<Path> paths, Class<T> type) throws IOException {
        List<SourceDocument<T>> documents = new ArrayList<>();
        for (Path path : paths.stream().distinct().sorted().toList()) {
            documents.add(new SourceDocument<>(path, yamlReader.read(path, type)));
        }
        return List.copyOf(documents);
    }

    private List<Path> classifiedExampleYaml(Path root, String requiredKey, String requiredValue) throws IOException {
        List<Path> result = new ArrayList<>();
        for (Path path : yamlFiles(root.resolve("examples"))) {
            if (path.getFileName().toString().equals("site.yaml") || path.getFileName().toString().equals("algorithms.yaml")) {
                continue;
            }
            Map<String, Object> map = yamlReader.readMap(path);
            Object value = map.get(requiredKey);
            if (value != null && (requiredValue == null || requiredValue.equals(String.valueOf(value)))) {
                result.add(path);
            }
        }
        return result;
    }

    private static List<Path> yamlFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".yaml") || name.endsWith(".yml");
                    })
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    private static List<Path> markdownFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    private static Optional<MarkdownIdentity> inferMarkdownIdentity(Path root, Path path) {
        Path relative = root.relativize(path);
        int count = relative.getNameCount();
        if (count >= 5 && "content".equals(relative.getName(0).toString()) && "tools".equals(relative.getName(1).toString())) {
            return parseMarkdownFileName(relative.getName(2).toString(), relative.getFileName().toString());
        }
        if (count >= 3 && "examples".equals(relative.getName(0).toString())) {
            String toolId = relative.getName(1).toString();
            if (!List.of("categories", "countries").contains(toolId)) {
                return parseMarkdownFileName(toolId, relative.getFileName().toString());
            }
        }
        return Optional.empty();
    }

    private static Optional<MarkdownIdentity> parseMarkdownFileName(String toolId, String fileName) {
        if (!fileName.endsWith(".md")) {
            return Optional.empty();
        }
        String base = fileName.substring(0, fileName.length() - 3);
        int separator = base.lastIndexOf('.');
        if (separator < 1 || separator == base.length() - 1) {
            return Optional.empty();
        }
        String blockId = base.substring(0, separator);
        String locale = base.substring(separator + 1);
        return contentBlockType(blockId).map(block -> new MarkdownIdentity(toolId, block, locale));
    }

    private static Optional<ContentBlockType> contentBlockType(String value) {
        for (ContentBlockType type : ContentBlockType.values()) {
            if (type.id().equals(value)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private static LocalizedText toLocalizedText(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return LocalizedText.empty();
        }
        return new LocalizedText(source.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> new LocaleCode(entry.getKey()),
                        Map.Entry::getValue
                )));
    }

    private static ContentStatus parseContentStatus(String value) {
        return "draft".equalsIgnoreCase(value) ? ContentStatus.DRAFT : ContentStatus.ACTIVE;
    }

    private record MarkdownIdentity(String toolId, ContentBlockType block, String locale) {
    }
}
