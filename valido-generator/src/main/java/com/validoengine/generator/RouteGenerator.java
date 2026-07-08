package com.validoengine.generator;

import com.validoengine.content.model.ContentProject;
import com.validoengine.core.diagnostic.Diagnostic;
import com.validoengine.core.diagnostic.DiagnosticLocation;
import com.validoengine.core.diagnostic.DiagnosticSeverity;
import com.validoengine.core.model.CategoryId;
import com.validoengine.core.model.CountryCode;
import com.validoengine.core.model.CountryModel;
import com.validoengine.core.model.LocaleCode;
import com.validoengine.core.model.RouteModel;
import com.validoengine.core.model.RouteType;
import com.validoengine.core.model.ToolModel;
import com.validoengine.core.model.ToolStatus;
import com.validoengine.core.validation.ValidationReport;
import com.validoengine.generator.model.RouteGenerationResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class RouteGenerator {
    public RouteGenerationResult generate(ContentProject contentProject) {
        Objects.requireNonNull(contentProject, "contentProject");
        List<RouteModel> routes = new ArrayList<>();
        List<ToolModel> publicTools = publicTools(contentProject);
        Map<CountryCode, CountryModel> countriesByCode = contentProject.countries().stream()
                .collect(Collectors.toUnmodifiableMap(CountryModel::code, country -> country));

        for (LocaleCode locale : contentProject.site().locales()) {
            routes.add(route(contentProject, locale, RouteType.HOME, "/" + locale.value() + "/", contentProject.site().id().value()));
            routes.addAll(toolRoutes(contentProject, locale, publicTools, countriesByCode));
            routes.addAll(categoryRoutes(contentProject, locale, publicTools));
            routes.addAll(countryRoutes(contentProject, locale, publicTools, countriesByCode));
        }

        return new RouteGenerationResult(List.copyOf(routes), validateCollisions(routes));
    }

    private static List<ToolModel> publicTools(ContentProject contentProject) {
        boolean includeDrafts = contentProject.site().mode().name().equals("DEMO") && contentProject.site().includeDrafts();
        return contentProject.tools().stream()
                .filter(tool -> tool.status() == ToolStatus.ACTIVE || (includeDrafts && tool.status() == ToolStatus.DRAFT))
                .sorted(Comparator.comparing(tool -> tool.id().value()))
                .toList();
    }

    private static List<RouteModel> toolRoutes(
            ContentProject contentProject,
            LocaleCode locale,
            List<ToolModel> tools,
            Map<CountryCode, CountryModel> countriesByCode
    ) {
        List<RouteModel> routes = new ArrayList<>();
        for (ToolModel tool : tools) {
            String toolPath = tool.optionalCountry()
                    .flatMap(countryCode -> java.util.Optional.ofNullable(countriesByCode.get(countryCode)))
                    .map(country -> country.slug() + "/" + tool.id().value())
                    .orElse(contentProject.site().defaultToolPath() + "/" + tool.id().value());
            routes.add(route(contentProject, locale, RouteType.TOOL, "/" + locale.value() + "/" + toolPath + "/", tool.id().value()));
        }
        return routes;
    }

    private static List<RouteModel> categoryRoutes(
            ContentProject contentProject,
            LocaleCode locale,
            List<ToolModel> publicTools
    ) {
        Set<CategoryId> categoriesWithTools = publicTools.stream()
                .map(ToolModel::category)
                .collect(Collectors.toUnmodifiableSet());
        return contentProject.categories().stream()
                .filter(category -> categoriesWithTools.contains(category.id()))
                .sorted(Comparator.comparing(category -> category.slug()))
                .map(category -> route(
                        contentProject,
                        locale,
                        RouteType.CATEGORY,
                        "/" + locale.value() + "/categories/" + category.slug() + "/",
                        category.id().value()
                ))
                .toList();
    }

    private static List<RouteModel> countryRoutes(
            ContentProject contentProject,
            LocaleCode locale,
            List<ToolModel> publicTools,
            Map<CountryCode, CountryModel> countriesByCode
    ) {
        Set<CountryCode> countriesWithTools = publicTools.stream()
                .flatMap(tool -> tool.optionalCountry().stream())
                .collect(Collectors.toUnmodifiableSet());
        return contentProject.countries().stream()
                .filter(country -> countriesWithTools.contains(country.code()))
                .sorted(Comparator.comparing(CountryModel::slug))
                .map(country -> route(
                        contentProject,
                        locale,
                        RouteType.COUNTRY,
                        "/" + locale.value() + "/" + country.slug() + "/",
                        country.code().value()
                ))
                .toList();
    }

    private static RouteModel route(ContentProject contentProject, LocaleCode locale, RouteType type, String path, String sourceAggregateId) {
        return new RouteModel(
                path,
                outputFile(contentProject.projectRoot().resolve(contentProject.site().outputDirectory()).normalize(), path),
                canonicalUrl(contentProject.site().baseUrl(), path),
                locale,
                type,
                sourceAggregateId
        );
    }

    private static Path outputFile(Path outputDirectory, String path) {
        String normalized = path.substring(1, path.length() - 1);
        Path output = outputDirectory;
        if (!normalized.isEmpty()) {
            for (String segment : normalized.split("/")) {
                output = output.resolve(segment);
            }
        }
        return output.resolve("index.html");
    }

    private static String canonicalUrl(String baseUrl, String path) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + path;
    }

    private static ValidationReport validateCollisions(List<RouteModel> routes) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        collisionDiagnostics(routes, diagnostics, RouteModel::path, "ROUTE_COLLISION", "Duplicate generated route path: ");
        collisionDiagnostics(routes, diagnostics, route -> route.outputFile().toString(), "ROUTE_OUTPUT_COLLISION", "Duplicate generated route output file: ");
        return new ValidationReport(diagnostics);
    }

    private static void collisionDiagnostics(
            List<RouteModel> routes,
            List<Diagnostic> diagnostics,
            java.util.function.Function<RouteModel, String> keyExtractor,
            String code,
            String message
    ) {
        Map<String, List<RouteModel>> byKey = new LinkedHashMap<>();
        for (RouteModel route : routes) {
            byKey.computeIfAbsent(keyExtractor.apply(route), ignored -> new ArrayList<>()).add(route);
        }
        byKey.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> diagnostics.add(new Diagnostic(
                        DiagnosticSeverity.ERROR,
                        code,
                        message + entry.getKey(),
                        DiagnosticLocation.none(),
                        Map.of()
                )));
    }
}
