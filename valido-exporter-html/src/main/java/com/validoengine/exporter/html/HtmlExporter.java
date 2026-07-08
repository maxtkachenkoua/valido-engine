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
import org.thymeleaf.templateresolver.StringTemplateResolver;
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

public final class HtmlExporter {
    private static final ExporterId HTML_EXPORTER_ID = HtmlExporterDescriptor.ID;
    private static final String TEMPLATE = """
            <!doctype html>
            <html th:lang="${locale}">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title th:text="${title}"></title>
              <meta name="description" th:attr="content=${description}">
              <link rel="canonical" th:attr="href=${canonical}">
              <link th:each="alternate : ${hreflang}" rel="alternate" th:attr="hreflang=${alternate.locale},href=${alternate.href}">
            </head>
            <body>
              <header>
                <a th:href="${homePath}" th:text="${siteName}"></a>
              </header>
              <main>
                <h1 th:text="${heading}"></h1>
                <p th:if="${summary != ''}" th:text="${summary}"></p>
                <nav th:if="${links.size() > 0}">
                  <ul>
                    <li th:each="link : ${links}"><a th:href="${link.path}" th:text="${link.title}"></a></li>
                  </ul>
                </nav>
                <section th:each="section : ${sections}">
                  <h2 th:text="${section.title}"></h2>
                  <pre th:text="${section.body}"></pre>
                </section>
              </main>
            </body>
            </html>
            """;

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
        variables.put("title", page.title());
        variables.put("description", page.description());
        variables.put("canonical", route.canonicalUrl());
        variables.put("hreflang", hreflang(projectModel, route));
        variables.put("heading", page.heading());
        variables.put("summary", page.summary());
        variables.put("links", page.links());
        variables.put("sections", page.sections());
        context.setVariables(variables);
        String html = templateEngine.process(TEMPLATE, context);
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
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    public record PageView(
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
    }

    public record HreflangView(String locale, String href) {
    }
}
