package com.validoengine.exporter.html;

import com.validoengine.core.model.CategoryId;
import com.validoengine.core.model.CategoryModel;
import com.validoengine.core.model.ContentBlockModel;
import com.validoengine.core.model.CountryCode;
import com.validoengine.core.model.CountryModel;
import com.validoengine.core.model.ExporterId;
import com.validoengine.core.model.LocaleCode;
import com.validoengine.core.model.LocalizedText;
import com.validoengine.core.model.ProjectModel;
import com.validoengine.core.model.RouteModel;
import com.validoengine.core.model.RouteType;
import com.validoengine.core.model.ToolId;
import com.validoengine.core.model.ToolModel;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public final class HtmlExporter {
    private static final ExporterId HTML_EXPORTER_ID = HtmlExporterDescriptor.ID;
    private static final String PAGE_TEMPLATE = "valido/page";
    private static final Pattern ORDERED_LIST = Pattern.compile("\\d+\\.\\s+.+");
    private static final String STYLESHEET = readResource("valido/static/styles.css");

    private final TemplateEngine templateEngine;

    public HtmlExporter() {
        this.templateEngine = templateEngine();
    }

    public HtmlExportResult export(ProjectModel projectModel) {
        Objects.requireNonNull(projectModel, "projectModel");
        validatePlan(projectModel);
        Map<ToolId, ToolModel> toolsById = projectModel.tools().stream()
                .collect(Collectors.toUnmodifiableMap(ToolModel::id, tool -> tool));
        Map<CategoryId, CategoryModel> categoriesById = projectModel.categories().stream()
                .collect(Collectors.toUnmodifiableMap(CategoryModel::id, category -> category));
        Map<CountryCode, CountryModel> countriesByCode = projectModel.countries().stream()
                .collect(Collectors.toUnmodifiableMap(CountryModel::code, country -> country));
        List<Path> writtenFiles = projectModel.routes().stream()
                .sorted(Comparator.comparing(RouteModel::path))
                .map(route -> writeRoute(projectModel, route, toolsById, categoriesById, countriesByCode))
                .collect(Collectors.toCollection(ArrayList::new));
        writtenFiles.addAll(writeStaticArtifacts(projectModel, toolsById));
        return new HtmlExportResult(writtenFiles);
    }

    private Path writeRoute(
            ProjectModel projectModel,
            RouteModel route,
            Map<ToolId, ToolModel> toolsById,
            Map<CategoryId, CategoryModel> categoriesById,
            Map<CountryCode, CountryModel> countriesByCode
    ) {
        PageView page = page(projectModel, route, toolsById, categoriesById, countriesByCode);
        Context context = new Context(Locale.forLanguageTag(route.locale().value()));
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("siteName", projectModel.site().name().resolve(route.locale(), projectModel.site().defaultLocale()));
        variables.put("homePath", "/" + route.locale().value() + "/");
        variables.put("locale", route.locale().value());
        variables.put("pageType", page.pageType());
        variables.put("pageLabel", page.pageLabel());
        variables.put("currentPath", route.path());
        variables.put("title", page.title());
        variables.put("description", page.description());
        variables.put("canonical", route.canonicalUrl());
        variables.put("hreflang", hreflang(projectModel, route));
        variables.put("stylesheet", STYLESHEET);
        variables.put("navigation", navigation(projectModel, route.locale()));
        variables.put("breadcrumbs", breadcrumbs(projectModel, route, page.heading()));
        variables.put("heading", page.heading());
        variables.put("summary", page.summary());
        variables.put("links", page.links());
        variables.put("sections", page.sections());
        context.setVariables(variables);
        String html = templateEngine.process(PAGE_TEMPLATE, context);
        try {
            Files.createDirectories(route.outputFile().getParent());
            Files.writeString(route.outputFile(), html);
            return route.outputFile();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write HTML route " + route.path(), exception);
        }
    }

    private PageView page(
            ProjectModel projectModel,
            RouteModel route,
            Map<ToolId, ToolModel> toolsById,
            Map<CategoryId, CategoryModel> categoriesById,
            Map<CountryCode, CountryModel> countriesByCode
    ) {
        return switch (route.pageType()) {
            case HOME -> home(projectModel, route);
            case TOOL -> tool(projectModel, route, toolsById);
            case CATEGORY -> category(projectModel, route, categoriesById);
            case COUNTRY -> country(projectModel, route, countriesByCode);
        };
    }

    private PageView home(ProjectModel projectModel, RouteModel route) {
        List<LinkView> links = projectModel.routes().stream()
                .filter(publicListingRoute())
                .filter(candidate -> candidate.locale().equals(route.locale()))
                .sorted(Comparator.comparing(RouteModel::path))
                .map(candidate -> new LinkView(candidate.path(), titleFor(projectModel, candidate, route.locale())))
                .toList();
        String siteName = projectModel.site().name().resolve(route.locale(), projectModel.site().defaultLocale());
        return new PageView(
                "Home",
                "home",
                seoTitle(projectModel.site().seo().title(), route.locale(), projectModel.site().defaultLocale(), siteName),
                seoDescription(projectModel.site().seo().description(), route.locale(), projectModel.site().defaultLocale(), ""),
                siteName,
                seoDescription(projectModel.site().seo().description(), route.locale(), projectModel.site().defaultLocale(), ""),
                links,
                List.of()
        );
    }

    private PageView tool(ProjectModel projectModel, RouteModel route, Map<ToolId, ToolModel> toolsById) {
        ToolModel tool = require(toolsById.get(new ToolId(route.sourceAggregateId())), route);
        List<SectionView> sections = projectModel.content().blocksByTool().getOrDefault(tool.id(), List.of()).stream()
                .filter(block -> block.locale().equals(route.locale()))
                .sorted(Comparator.comparingInt(ContentBlockModel::order).thenComparing(block -> block.block().id()))
                .map(block -> new SectionView(sectionTitle(block, route.locale(), projectModel.site().defaultLocale()), block.markdown()))
                .toList();
        List<LinkView> links = tool.relatedLinks().stream()
                .map(link -> new LinkView(link.route().path(), link.title()))
                .toList();
        String fallbackTitle = tool.name().resolve(route.locale(), projectModel.site().defaultLocale());
        String fallbackDescription = tool.summary().resolve(route.locale(), projectModel.site().defaultLocale());
        return new PageView(
                "Tool",
                "tool",
                seoTitle(tool.seo().title(), route.locale(), projectModel.site().defaultLocale(), fallbackTitle),
                seoDescription(tool.seo().description(), route.locale(), projectModel.site().defaultLocale(), fallbackDescription),
                fallbackTitle,
                fallbackDescription,
                links,
                sections
        );
    }

    private PageView category(ProjectModel projectModel, RouteModel route, Map<CategoryId, CategoryModel> categoriesById) {
        CategoryModel category = require(categoriesById.get(new CategoryId(route.sourceAggregateId())), route);
        List<LinkView> links = projectModel.tools().stream()
                .filter(tool -> tool.category().equals(category.id()))
                .flatMap(tool -> routeFor(projectModel, route.locale(), tool.id()).stream()
                        .map(toolRoute -> new LinkView(toolRoute.path(), tool.name().resolve(route.locale(), projectModel.site().defaultLocale()))))
                .sorted(Comparator.comparing(LinkView::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
        String fallbackTitle = category.name().resolve(route.locale(), projectModel.site().defaultLocale());
        String fallbackDescription = category.summary().resolve(route.locale(), projectModel.site().defaultLocale());
        return new PageView(
                "Category",
                "category",
                seoTitle(category.seo().title(), route.locale(), projectModel.site().defaultLocale(), fallbackTitle),
                seoDescription(category.seo().description(), route.locale(), projectModel.site().defaultLocale(), fallbackDescription),
                fallbackTitle,
                fallbackDescription,
                links,
                List.of()
        );
    }

    private PageView country(ProjectModel projectModel, RouteModel route, Map<CountryCode, CountryModel> countriesByCode) {
        CountryModel country = require(countriesByCode.get(new CountryCode(route.sourceAggregateId())), route);
        List<LinkView> links = projectModel.tools().stream()
                .filter(tool -> tool.optionalCountry().map(country.code()::equals).orElse(false))
                .flatMap(tool -> routeFor(projectModel, route.locale(), tool.id()).stream()
                        .map(toolRoute -> new LinkView(toolRoute.path(), tool.name().resolve(route.locale(), projectModel.site().defaultLocale()))))
                .sorted(Comparator.comparing(LinkView::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
        String title = country.name().resolve(route.locale(), projectModel.site().defaultLocale());
        return new PageView(
                "Country",
                "country",
                title,
                "Tools for " + title + ".",
                title,
                "Tools for " + title + ".",
                links,
                List.of()
        );
    }

    private static void validatePlan(ProjectModel projectModel) {
        if (!projectModel.exportPlan().exporters().contains(HTML_EXPORTER_ID)) {
            throw new IllegalArgumentException("ProjectModel ExportPlan does not include html exporter.");
        }
        Path targetDirectory = projectModel.exportPlan().targetDirectories().get(HTML_EXPORTER_ID);
        if (targetDirectory == null) {
            throw new IllegalArgumentException("ProjectModel ExportPlan does not include html target directory.");
        }
        Set<Path> plannedArtifacts = Set.copyOf(projectModel.exportPlan().generatedArtifacts());
        List<Path> missing = projectModel.routes().stream()
                .map(RouteModel::outputFile)
                .filter(Predicate.not(plannedArtifacts::contains))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("ProjectModel ExportPlan is missing HTML route artifacts: " + missing);
        }
        List<Path> missingStaticArtifacts = staticArtifactPaths(projectModel).stream()
                .filter(Predicate.not(plannedArtifacts::contains))
                .toList();
        if (!missingStaticArtifacts.isEmpty()) {
            throw new IllegalArgumentException("ProjectModel ExportPlan is missing static artifacts: " + missingStaticArtifacts);
        }
    }

    private static List<Path> writeStaticArtifacts(ProjectModel projectModel, Map<ToolId, ToolModel> toolsById) {
        Path targetDirectory = projectModel.exportPlan().targetDirectories().get(HTML_EXPORTER_ID);
        return List.of(
                writeFile(targetDirectory.resolve("sitemap.xml"), sitemap(projectModel)),
                writeFile(targetDirectory.resolve("robots.txt"), robots(projectModel)),
                writeFile(targetDirectory.resolve("search-index.json"), searchIndex(projectModel, toolsById))
        );
    }

    private static List<Path> staticArtifactPaths(ProjectModel projectModel) {
        Path targetDirectory = projectModel.exportPlan().targetDirectories().get(HTML_EXPORTER_ID);
        return List.of(
                targetDirectory.resolve("sitemap.xml"),
                targetDirectory.resolve("robots.txt"),
                targetDirectory.resolve("search-index.json")
        );
    }

    private static Path writeFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return path;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write static artifact " + path, exception);
        }
    }

    private static String sitemap(ProjectModel projectModel) {
        String urls = projectModel.routes().stream()
                .sorted(Comparator.comparing(RouteModel::canonicalUrl))
                .map(route -> "  <url><loc>" + xml(route.canonicalUrl()) + "</loc></url>")
                .collect(Collectors.joining("\n"));
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                %s
                </urlset>
                """.formatted(urls);
    }

    private static String robots(ProjectModel projectModel) {
        String baseUrl = normalizedBaseUrl(projectModel.site().baseUrl());
        return """
                User-agent: *
                Allow: /
                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }

    private static String searchIndex(ProjectModel projectModel, Map<ToolId, ToolModel> toolsById) {
        LocaleCode locale = projectModel.site().defaultLocale();
        String tools = projectModel.routes().stream()
                .filter(route -> route.pageType() == RouteType.TOOL)
                .filter(route -> route.locale().equals(locale))
                .sorted(Comparator.comparing(route -> titleFor(projectModel, route, locale), String.CASE_INSENSITIVE_ORDER))
                .map(route -> searchEntry(projectModel, toolsById.get(new ToolId(route.sourceAggregateId())), route, locale))
                .collect(Collectors.joining(",\n"));
        return """
                {
                  "tools": [
                %s
                  ]
                }
                """.formatted(tools.isBlank() ? "" : tools.indent(4).stripTrailing());
    }

    private static String searchEntry(ProjectModel projectModel, ToolModel tool, RouteModel route, LocaleCode locale) {
        String country = tool.optionalCountry().map(CountryCode::value).orElse(null);
        List<String> labels = tool.forms().values().stream()
                .flatMap(form -> form.inputs().stream())
                .map(input -> input.label().resolve(locale, projectModel.site().defaultLocale()))
                .filter(label -> !label.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> capabilities = tool.capabilities().stream()
                .sorted()
                .map(capability -> capability.value())
                .toList();
        List<String> aliases = tool.aliases().getOrEmpty(locale);
        return """
                {
                  "toolId": %s,
                  "title": %s,
                  "summary": %s,
                  "country": %s,
                  "category": %s,
                  "capabilities": %s,
                  "aliases": %s,
                  "localizedLabels": %s,
                  "route": %s
                }""".formatted(
                json(tool.id().value()),
                json(tool.name().resolve(locale, projectModel.site().defaultLocale())),
                json(tool.summary().resolve(locale, projectModel.site().defaultLocale())),
                country == null ? "null" : json(country),
                json(tool.category().value()),
                jsonArray(capabilities),
                jsonArray(aliases),
                jsonArray(labels),
                json(route.path())
        );
    }

    private static Predicate<RouteModel> publicListingRoute() {
        return route -> route.pageType() == RouteType.TOOL || route.pageType() == RouteType.CATEGORY || route.pageType() == RouteType.COUNTRY;
    }

    private static Optional<RouteModel> routeFor(ProjectModel projectModel, LocaleCode locale, ToolId toolId) {
        return projectModel.routes().stream()
                .filter(route -> route.pageType() == RouteType.TOOL)
                .filter(route -> route.locale().equals(locale))
                .filter(route -> route.sourceAggregateId().equals(toolId.value()))
                .findFirst();
    }

    private static String titleFor(ProjectModel projectModel, RouteModel route, LocaleCode locale) {
        return switch (route.pageType()) {
            case HOME -> projectModel.site().name().resolve(locale, projectModel.site().defaultLocale());
            case TOOL -> projectModel.tools().stream()
                    .filter(tool -> tool.id().value().equals(route.sourceAggregateId()))
                    .findFirst()
                    .map(tool -> tool.name().resolve(locale, projectModel.site().defaultLocale()))
                    .orElse(route.path());
            case CATEGORY -> projectModel.categories().stream()
                    .filter(category -> category.id().value().equals(route.sourceAggregateId()))
                    .findFirst()
                    .map(category -> category.name().resolve(locale, projectModel.site().defaultLocale()))
                    .orElse(route.path());
            case COUNTRY -> projectModel.countries().stream()
                    .filter(country -> country.code().value().equals(route.sourceAggregateId()))
                    .findFirst()
                    .map(country -> country.name().resolve(locale, projectModel.site().defaultLocale()))
                    .orElse(route.path());
        };
    }

    private static List<LinkView> navigation(ProjectModel projectModel, LocaleCode locale) {
        List<LinkView> links = new ArrayList<>();
        links.add(new LinkView("/" + locale.value() + "/", "Home"));
        projectModel.routes().stream()
                .filter(route -> route.locale().equals(locale))
                .filter(route -> route.pageType() == RouteType.CATEGORY || route.pageType() == RouteType.COUNTRY)
                .sorted(Comparator.comparing(route -> titleFor(projectModel, route, locale), String.CASE_INSENSITIVE_ORDER))
                .map(route -> new LinkView(route.path(), titleFor(projectModel, route, locale)))
                .forEach(links::add);
        return List.copyOf(links);
    }

    private static List<LinkView> breadcrumbs(ProjectModel projectModel, RouteModel route, String title) {
        List<LinkView> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new LinkView("/" + route.locale().value() + "/", "Home"));
        switch (route.pageType()) {
            case HOME -> {
                return List.of();
            }
            case TOOL -> {
                findTool(projectModel, route).ifPresent(tool -> {
                    tool.optionalCountry()
                            .flatMap(country -> projectModel.routes().stream()
                                    .filter(candidate -> candidate.locale().equals(route.locale()))
                                    .filter(candidate -> candidate.pageType() == RouteType.COUNTRY)
                                    .filter(candidate -> candidate.sourceAggregateId().equals(country.value()))
                                    .findFirst())
                            .ifPresent(countryRoute -> breadcrumbs.add(new LinkView(countryRoute.path(), titleFor(projectModel, countryRoute, route.locale()))));
                    projectModel.routes().stream()
                            .filter(candidate -> candidate.locale().equals(route.locale()))
                            .filter(candidate -> candidate.pageType() == RouteType.CATEGORY)
                            .filter(candidate -> candidate.sourceAggregateId().equals(tool.category().value()))
                            .findFirst()
                            .ifPresent(categoryRoute -> breadcrumbs.add(new LinkView(categoryRoute.path(), titleFor(projectModel, categoryRoute, route.locale()))));
                });
            }
            case CATEGORY -> breadcrumbs.add(new LinkView("/" + route.locale().value() + "/", "Categories"));
            case COUNTRY -> breadcrumbs.add(new LinkView("/" + route.locale().value() + "/", "Countries"));
        }
        breadcrumbs.add(new LinkView(route.path(), title));
        return List.copyOf(breadcrumbs);
    }

    private static Optional<ToolModel> findTool(ProjectModel projectModel, RouteModel route) {
        return projectModel.tools().stream()
                .filter(tool -> tool.id().value().equals(route.sourceAggregateId()))
                .findFirst();
    }

    private static List<HreflangView> hreflang(ProjectModel projectModel, RouteModel route) {
        return projectModel.routes().stream()
                .filter(candidate -> candidate.pageType() == route.pageType())
                .filter(candidate -> candidate.sourceAggregateId().equals(route.sourceAggregateId()))
                .sorted(Comparator.comparing(candidate -> candidate.locale().value()))
                .map(candidate -> new HreflangView(candidate.locale().value(), candidate.canonicalUrl()))
                .toList();
    }

    private static String seoTitle(LocalizedText seoTitle, LocaleCode locale, LocaleCode fallback, String defaultValue) {
        String resolved = seoTitle.resolve(locale, fallback);
        return resolved.isBlank() ? defaultValue : resolved;
    }

    private static String seoDescription(LocalizedText seoDescription, LocaleCode locale, LocaleCode fallback, String defaultValue) {
        String resolved = seoDescription.resolve(locale, fallback);
        return resolved.isBlank() ? defaultValue : resolved;
    }

    private static String sectionTitle(ContentBlockModel block, LocaleCode locale, LocaleCode fallback) {
        String title = block.title().resolve(locale, fallback);
        return title.isBlank() ? block.block().id() : title;
    }

    private static String normalizedBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String xml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String jsonArray(List<String> values) {
        return values.stream()
                .map(HtmlExporter::json)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String json(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static <T> T require(T value, RouteModel route) {
        if (value == null) {
            throw new IllegalArgumentException("Unable to resolve source aggregate for route " + route.path());
        }
        return value;
    }

    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static String readResource(String name) {
        try (var stream = HtmlExporter.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing exporter resource: " + name);
            }
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read exporter resource: " + name, exception);
        }
    }

    private static String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        MarkdownRenderer renderer = new MarkdownRenderer(markdown);
        return renderer.render();
    }

    public record PageView(
            String pageLabel,
            String pageType,
            String title,
            String description,
            String heading,
            String summary,
            List<LinkView> links,
            List<SectionView> sections
    ) {
    }

    public record LinkView(String path, String title) {
    }

    public record SectionView(String title, String body) {
        public String html() {
            return markdownToHtml(body);
        }
    }

    public record HreflangView(String locale, String href) {
    }

    private static final class MarkdownRenderer {
        private final List<String> lines;
        private final StringBuilder html = new StringBuilder();
        private int index;

        private MarkdownRenderer(String markdown) {
            this.lines = markdown.replace("\r\n", "\n").replace('\r', '\n').lines().toList();
        }

        private String render() {
            while (index < lines.size()) {
                String line = lines.get(index);
                if (line.isBlank()) {
                    index++;
                } else if (line.startsWith("```")) {
                    codeBlock();
                } else if (isTableStart()) {
                    table();
                } else if (line.startsWith("- ") || line.startsWith("* ")) {
                    unorderedList();
                } else if (ORDERED_LIST.matcher(line).matches()) {
                    orderedList();
                } else if (line.startsWith("### ")) {
                    html.append("<h3>").append(inline(line.substring(4))).append("</h3>\n");
                    index++;
                } else if (line.startsWith("## ")) {
                    html.append("<h3>").append(inline(line.substring(3))).append("</h3>\n");
                    index++;
                } else if (line.startsWith("# ")) {
                    html.append("<h3>").append(inline(line.substring(2))).append("</h3>\n");
                    index++;
                } else {
                    paragraph();
                }
            }
            return html.toString();
        }

        private void codeBlock() {
            index++;
            StringBuilder code = new StringBuilder();
            while (index < lines.size() && !lines.get(index).startsWith("```")) {
                code.append(lines.get(index)).append('\n');
                index++;
            }
            if (index < lines.size()) {
                index++;
            }
            html.append("<pre><code>").append(escape(code.toString().stripTrailing())).append("</code></pre>\n");
        }

        private boolean isTableStart() {
            return index + 1 < lines.size()
                    && lines.get(index).contains("|")
                    && lines.get(index + 1).matches("\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*");
        }

        private void table() {
            List<String> headers = tableCells(lines.get(index));
            index += 2;
            html.append("<div class=\"table-wrap\"><table><thead><tr>");
            headers.forEach(header -> html.append("<th>").append(inline(header)).append("</th>"));
            html.append("</tr></thead><tbody>");
            while (index < lines.size() && lines.get(index).contains("|") && !lines.get(index).isBlank()) {
                html.append("<tr>");
                tableCells(lines.get(index)).forEach(cell -> html.append("<td>").append(inline(cell)).append("</td>"));
                html.append("</tr>");
                index++;
            }
            html.append("</tbody></table></div>\n");
        }

        private static List<String> tableCells(String line) {
            String trimmed = line.trim();
            if (trimmed.startsWith("|")) {
                trimmed = trimmed.substring(1);
            }
            if (trimmed.endsWith("|")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return java.util.Arrays.stream(trimmed.split("\\|"))
                    .map(String::trim)
                    .toList();
        }

        private void unorderedList() {
            html.append("<ul>\n");
            while (index < lines.size() && (lines.get(index).startsWith("- ") || lines.get(index).startsWith("* "))) {
                html.append("<li>").append(inline(lines.get(index).substring(2))).append("</li>\n");
                index++;
            }
            html.append("</ul>\n");
        }

        private void orderedList() {
            html.append("<ol>\n");
            while (index < lines.size() && ORDERED_LIST.matcher(lines.get(index)).matches()) {
                String line = lines.get(index);
                int separator = line.indexOf('.');
                html.append("<li>").append(inline(line.substring(separator + 1).trim())).append("</li>\n");
                index++;
            }
            html.append("</ol>\n");
        }

        private void paragraph() {
            StringBuilder text = new StringBuilder();
            while (index < lines.size()
                    && !lines.get(index).isBlank()
                    && !lines.get(index).startsWith("```")
                    && !lines.get(index).startsWith("# ")
                    && !lines.get(index).startsWith("## ")
                    && !lines.get(index).startsWith("### ")
                    && !lines.get(index).startsWith("- ")
                    && !lines.get(index).startsWith("* ")
                    && !ORDERED_LIST.matcher(lines.get(index)).matches()
                    && !isTableStart()) {
                if (!text.isEmpty()) {
                    text.append(' ');
                }
                text.append(lines.get(index).trim());
                index++;
            }
            html.append("<p>").append(inline(text.toString())).append("</p>\n");
        }

        private static String inline(String value) {
            String escaped = escape(value);
            StringBuilder result = new StringBuilder();
            boolean code = false;
            StringBuilder segment = new StringBuilder();
            for (int i = 0; i < escaped.length(); i++) {
                char character = escaped.charAt(i);
                if (character == '`') {
                    result.append(code ? "<code>" : "");
                    if (code) {
                        result.append(segment).append("</code>");
                    } else {
                        result.append(segment);
                    }
                    segment.setLength(0);
                    code = !code;
                } else {
                    segment.append(character);
                }
            }
            if (code) {
                result.append('`');
            }
            result.append(segment);
            return result.toString();
        }

        private static String escape(String value) {
            return value
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
        }
    }
}
