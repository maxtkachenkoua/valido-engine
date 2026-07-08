package com.validoengine.content.validation;

import com.validoengine.content.dsl.AlgorithmRegistryDsl;
import com.validoengine.content.dsl.CategoryDsl;
import com.validoengine.content.dsl.CountryDsl;
import com.validoengine.content.dsl.SiteDsl;
import com.validoengine.content.dsl.StructuredContentBlockDsl;
import com.validoengine.content.dsl.ToolDsl;
import com.validoengine.content.dsl.UnknownFieldAware;
import com.validoengine.content.model.RawContentWorkspace;
import com.validoengine.content.model.SourceDocument;
import com.validoengine.core.diagnostic.Diagnostic;
import com.validoengine.core.diagnostic.DiagnosticLocation;
import com.validoengine.core.diagnostic.DiagnosticSeverity;
import com.validoengine.core.validation.ValidationReport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ContentValidator {
    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final Pattern LOWERCASE_ID = Pattern.compile("[a-z0-9][a-z0-9-]*");
    private static final Pattern ALGORITHM_ID = Pattern.compile("[a-z0-9][a-z0-9.-]*");
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z][A-Z0-9]*");
    private static final Pattern FORM_INPUT_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");
    private static final Set<String> KNOWN_CAPABILITIES = Set.of(
            "validate", "generate", "parse", "format", "convert", "explain",
            "lookup", "calculate", "decode", "encode", "bulk-validate", "bulk-generate"
    );

    public ValidationReport validate(RawContentWorkspace workspace) {
        Objects.requireNonNull(workspace, "workspace");
        List<Diagnostic> diagnostics = new ArrayList<>();
        String mode = siteMode(workspace.site());

        validateSite(workspace.site(), diagnostics, mode);
        validateCategories(workspace.categories(), diagnostics, mode);
        validateCountries(workspace.countries(), diagnostics, mode);
        validateAlgorithmRegistries(workspace.algorithmRegistries(), diagnostics, mode);
        validateTools(workspace.tools(), workspace, diagnostics, mode);
        validateStructuredContent(workspace.structuredContentBlocks(), diagnostics, mode);
        validateMarkdownContent(workspace, diagnostics);

        return new ValidationReport(diagnostics);
    }

    private static void validateSite(SourceDocument<SiteDsl> site, List<Diagnostic> diagnostics, String mode) {
        if (site == null) {
            diagnostics.add(error("SITE_MISSING", "Missing site.yaml.", null));
            return;
        }
        SiteDsl dsl = site.document();
        unknownFields(dsl, site.path(), "site", mode, diagnostics);
        schemaVersion(dsl.schemaVersion, site.path(), "site", diagnostics);
        required(dsl.id, site.path(), "site.id", diagnostics);
        pattern(dsl.id, LOWERCASE_ID, site.path(), "site.id", diagnostics);
        required(dsl.name, site.path(), "site.name", diagnostics);
        required(dsl.baseUrl, site.path(), "site.baseUrl", diagnostics);
        required(dsl.defaultLocale, site.path(), "site.defaultLocale", diagnostics);
        nonEmpty(dsl.locales, site.path(), "site.locales", diagnostics);
        if (dsl.defaultLocale != null && dsl.locales != null && !dsl.locales.contains(dsl.defaultLocale)) {
            diagnostics.add(error("SITE_DEFAULT_LOCALE_MISSING", "site.locales must contain site.defaultLocale.", site.path()));
        }
        if (dsl.defaultLocale != null && !"en".equals(dsl.defaultLocale)) {
            diagnostics.add(error("SITE_DEFAULT_LOCALE_UNSUPPORTED", "MVP default locale must be en.", site.path()));
        }
        enumValue(dsl.mode, Set.of("demo", "production"), site.path(), "site.mode", diagnostics);
        nonEmpty(dsl.modules, site.path(), "site.modules", diagnostics);
        if (dsl.output == null || blank(dsl.output.directory)) {
            diagnostics.add(error("SITE_OUTPUT_DIRECTORY_REQUIRED", "site.output.directory is required.", site.path()));
        } else if (Path.of(dsl.output.directory).isAbsolute() || dsl.output.directory.contains("..")) {
            diagnostics.add(error("SITE_OUTPUT_DIRECTORY_INVALID", "site.output.directory must be relative and must not escape the project.", site.path()));
        }
        if ("production".equals(dsl.mode) && (dsl.baseUrl == null || !dsl.baseUrl.startsWith("https://"))) {
            diagnostics.add(error("SITE_BASE_URL_HTTPS_REQUIRED", "Production mode requires an absolute HTTPS baseUrl.", site.path()));
        }
        if (dsl.output != null) {
            unknownFields(dsl.output, site.path(), "site.output", mode, diagnostics);
        }
        if (dsl.routing != null) {
            unknownFields(dsl.routing, site.path(), "site.routing", mode, diagnostics);
        }
        if (dsl.generation != null) {
            unknownFields(dsl.generation, site.path(), "site.generation", mode, diagnostics);
        }
        if (dsl.seo != null) {
            unknownFields(dsl.seo, site.path(), "site.seo", mode, diagnostics);
        }
    }

    private static void validateCategories(List<SourceDocument<CategoryDsl>> categories, List<Diagnostic> diagnostics, String mode) {
        Set<String> ids = new HashSet<>();
        Set<String> slugs = new HashSet<>();
        Set<String> allIds = categories.stream().map(SourceDocument::document).map(category -> category.id).filter(Objects::nonNull).collect(Collectors.toSet());
        for (SourceDocument<CategoryDsl> source : categories) {
            CategoryDsl dsl = source.document();
            unknownFields(dsl, source.path(), "category", mode, diagnostics);
            schemaVersion(dsl.schemaVersion, source.path(), "category", diagnostics);
            required(dsl.id, source.path(), "category.id", diagnostics);
            pattern(dsl.id, LOWERCASE_ID, source.path(), "category.id", diagnostics);
            required(dsl.slug, source.path(), "category.slug", diagnostics);
            required(dsl.name, source.path(), "category.name", diagnostics);
            required(dsl.summary, source.path(), "category.summary", diagnostics);
            duplicate(ids, dsl.id, source.path(), "CATEGORY_ID_DUPLICATE", "Duplicate category id.", diagnostics);
            duplicate(slugs, dsl.slug, source.path(), "CATEGORY_SLUG_DUPLICATE", "Duplicate category slug.", diagnostics);
            if (dsl.parent != null && !allIds.contains(dsl.parent)) {
                diagnostics.add(error("CATEGORY_PARENT_MISSING", "Category parent reference does not exist: " + dsl.parent, source.path()));
            }
            if (dsl.seo != null) {
                unknownFields(dsl.seo, source.path(), "category.seo", mode, diagnostics);
            }
        }
    }

    private static void validateCountries(List<SourceDocument<CountryDsl>> countries, List<Diagnostic> diagnostics, String mode) {
        Set<String> codes = new HashSet<>();
        Set<String> slugs = new HashSet<>();
        Set<String> allCodes = countries.stream().map(SourceDocument::document).map(country -> country.code).filter(Objects::nonNull).collect(Collectors.toSet());
        for (SourceDocument<CountryDsl> source : countries) {
            CountryDsl dsl = source.document();
            unknownFields(dsl, source.path(), "country", mode, diagnostics);
            schemaVersion(dsl.schemaVersion, source.path(), "country", diagnostics);
            required(dsl.code, source.path(), "country.code", diagnostics);
            pattern(dsl.code, COUNTRY_CODE, source.path(), "country.code", diagnostics);
            required(dsl.slug, source.path(), "country.slug", diagnostics);
            required(dsl.name, source.path(), "country.name", diagnostics);
            duplicate(codes, dsl.code, source.path(), "COUNTRY_CODE_DUPLICATE", "Duplicate country code.", diagnostics);
            duplicate(slugs, dsl.slug, source.path(), "COUNTRY_SLUG_DUPLICATE", "Duplicate country slug.", diagnostics);
            for (String related : nullToEmpty(dsl.relatedCountries)) {
                if (!allCodes.contains(related)) {
                    diagnostics.add(modeDiagnostic(mode, "COUNTRY_RELATED_MISSING", "Related country reference does not exist: " + related, source.path()));
                }
            }
        }
    }

    private static void validateAlgorithmRegistries(List<SourceDocument<AlgorithmRegistryDsl>> registries, List<Diagnostic> diagnostics, String mode) {
        Set<String> ids = new HashSet<>();
        for (SourceDocument<AlgorithmRegistryDsl> source : registries) {
            AlgorithmRegistryDsl dsl = source.document();
            unknownFields(dsl, source.path(), "algorithmRegistry", mode, diagnostics);
            schemaVersion(dsl.schemaVersion, source.path(), "algorithmRegistry", diagnostics);
            nonEmpty(dsl.algorithms, source.path(), "algorithmRegistry.algorithms", diagnostics);
            for (AlgorithmRegistryDsl.AlgorithmDsl algorithm : nullToEmpty(dsl.algorithms)) {
                unknownFields(algorithm, source.path(), "algorithmRegistry.algorithms[]", mode, diagnostics);
                required(algorithm.algorithmId, source.path(), "algorithm.algorithmId", diagnostics);
                pattern(algorithm.algorithmId, ALGORITHM_ID, source.path(), "algorithm.algorithmId", diagnostics);
                required(algorithm.javaClass, source.path(), "algorithm.javaClass", diagnostics);
                if (algorithm.version == null || algorithm.version < 1) {
                    diagnostics.add(error("ALGORITHM_VERSION_INVALID", "algorithm.version must be greater than zero.", source.path()));
                }
                nonEmpty(algorithm.capabilities, source.path(), "algorithm.capabilities", diagnostics);
                duplicate(ids, algorithm.algorithmId, source.path(), "ALGORITHM_ID_DUPLICATE", "Duplicate algorithm id.", diagnostics);
                for (String capability : nullToEmpty(algorithm.capabilities)) {
                    knownCapability(capability, source.path(), diagnostics);
                }
            }
        }
    }

    private static void validateTools(List<SourceDocument<ToolDsl>> tools, RawContentWorkspace workspace, List<Diagnostic> diagnostics, String mode) {
        Set<String> categoryIds = workspace.categories().stream().map(SourceDocument::document).map(category -> category.id).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> countryCodes = workspace.countries().stream().map(SourceDocument::document).map(country -> country.code).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> toolIds = tools.stream().map(SourceDocument::document).map(tool -> tool.id).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, Set<String>> algorithms = algorithmCapabilities(workspace.algorithmRegistries());
        Set<String> seenToolIds = new HashSet<>();

        for (SourceDocument<ToolDsl> source : tools) {
            ToolDsl dsl = source.document();
            unknownFields(dsl, source.path(), "tool", mode, diagnostics);
            schemaVersion(dsl.schemaVersion, source.path(), "tool", diagnostics);
            required(dsl.id, source.path(), "tool.id", diagnostics);
            pattern(dsl.id, LOWERCASE_ID, source.path(), "tool.id", diagnostics);
            enumValue(dsl.kind, Set.of("tool"), source.path(), "tool.kind", diagnostics);
            required(dsl.module, source.path(), "tool.module", diagnostics);
            required(dsl.category, source.path(), "tool.category", diagnostics);
            enumValue(dsl.status, Set.of("draft", "active", "deprecated"), source.path(), "tool.status", diagnostics);
            required(dsl.name, source.path(), "tool.name", diagnostics);
            required(dsl.summary, source.path(), "tool.summary", diagnostics);
            nonEmpty(dsl.capabilities, source.path(), "tool.capabilities", diagnostics);
            duplicate(seenToolIds, dsl.id, source.path(), "TOOL_ID_DUPLICATE", "Duplicate tool id.", diagnostics);
            if (dsl.category != null && !categoryIds.contains(dsl.category)) {
                diagnostics.add(error("TOOL_CATEGORY_MISSING", "Tool category does not exist: " + dsl.category, source.path()));
            }
            if (dsl.country != null && !countryCodes.contains(dsl.country)) {
                diagnostics.add(error("TOOL_COUNTRY_MISSING", "Tool country does not exist: " + dsl.country, source.path()));
            }
            for (String capability : nullToEmpty(dsl.capabilities)) {
                knownCapability(capability, source.path(), diagnostics);
            }
            validateAlgorithmBinding(dsl, algorithms, mode, source.path(), diagnostics);
            validateForms(dsl, algorithms, source.path(), mode, diagnostics);
            validateRelated(dsl, toolIds, source.path(), mode, diagnostics);
            nestedUnknowns(dsl, source.path(), mode, diagnostics);
        }
    }

    private static void validateStructuredContent(List<SourceDocument<StructuredContentBlockDsl>> blocks, List<Diagnostic> diagnostics, String mode) {
        for (SourceDocument<StructuredContentBlockDsl> source : blocks) {
            StructuredContentBlockDsl dsl = source.document();
            unknownFields(dsl, source.path(), "contentBlock", mode, diagnostics);
            schemaVersion(dsl.schemaVersion, source.path(), "contentBlock", diagnostics);
            required(dsl.toolId, source.path(), "contentBlock.toolId", diagnostics);
            enumValue(dsl.block, Set.of("explanation", "examples", "faq", "references", "developer-examples"), source.path(), "contentBlock.block", diagnostics);
            required(dsl.locale, source.path(), "contentBlock.locale", diagnostics);
        }
    }

    private static void validateMarkdownContent(RawContentWorkspace workspace, List<Diagnostic> diagnostics) {
        Set<String> toolIds = workspace.tools().stream().map(SourceDocument::document).map(tool -> tool.id).filter(Objects::nonNull).collect(Collectors.toSet());
        workspace.markdownContentBlocks().forEach(block -> {
            if (!toolIds.contains(block.toolId().value())) {
                diagnostics.add(warning("CONTENT_TOOL_MISSING", "Markdown content references missing tool: " + block.toolId(), block.path()));
            }
        });
    }

    private static void validateAlgorithmBinding(ToolDsl dsl, Map<String, Set<String>> algorithms, String mode, Path path, List<Diagnostic> diagnostics) {
        if (dsl.algorithm == null || blank(dsl.algorithm.algorithmId)) {
            diagnostics.add(algorithmDiagnostic(dsl, mode, "TOOL_ALGORITHM_MISSING", "Tool algorithm.algorithmId is required.", path));
            return;
        }
        if (dsl.algorithm.javaClass != null) {
            diagnostics.add(error("TOOL_ALGORITHM_JAVA_CLASS_FORBIDDEN", "Tool DSL must not contain algorithm.javaClass.", path));
        }
        pattern(dsl.algorithm.algorithmId, ALGORITHM_ID, path, "tool.algorithm.algorithmId", diagnostics);
        Set<String> algorithmCaps = algorithms.get(dsl.algorithm.algorithmId);
        if (algorithmCaps == null) {
            diagnostics.add(algorithmDiagnostic(dsl, mode, "TOOL_ALGORITHM_UNRESOLVED", "Tool algorithmId is not registered: " + dsl.algorithm.algorithmId, path));
            return;
        }
        for (String capability : nullToEmpty(dsl.capabilities)) {
            if (!algorithmCaps.contains(capability)) {
                diagnostics.add(algorithmDiagnostic(dsl, mode, "TOOL_ALGORITHM_INCOMPATIBLE", "Tool capability is not supported by algorithm: " + capability, path));
            }
        }
    }

    private static void validateForms(ToolDsl dsl, Map<String, Set<String>> algorithms, Path path, String mode, List<Diagnostic> diagnostics) {
        Set<String> toolCaps = new HashSet<>(nullToEmpty(dsl.capabilities));
        Set<String> algorithmCaps = dsl.algorithm == null ? Set.of() : algorithms.getOrDefault(dsl.algorithm.algorithmId, Set.of());
        if (dsl.forms == null || dsl.forms.isEmpty()) {
            diagnostics.add(error("TOOL_FORMS_REQUIRED", "tool.forms is required and must not be empty.", path));
            return;
        }
        dsl.forms.forEach((capability, form) -> {
            knownCapability(capability, path, diagnostics);
            if (!toolCaps.contains(capability)) {
                diagnostics.add(error("TOOL_FORM_CAPABILITY_UNDECLARED", "Form capability is not declared by tool: " + capability, path));
            }
            unknownFields(form, path, "tool.forms." + capability, mode, diagnostics);
            nonEmpty(form.actions, path, "tool.forms." + capability + ".actions", diagnostics);
            for (String action : nullToEmpty(form.actions)) {
                if (!toolCaps.contains(action)) {
                    diagnostics.add(error("TOOL_FORM_ACTION_UNDECLARED", "Form action is not declared by tool: " + action, path));
                }
                if (!algorithmCaps.isEmpty() && !algorithmCaps.contains(action)) {
                    diagnostics.add(algorithmDiagnostic(dsl, mode, "TOOL_FORM_ACTION_UNSUPPORTED", "Form action is not supported by algorithm: " + action, path));
                }
            }
            for (ToolDsl.InputDsl input : nullToEmpty(form.inputs)) {
                unknownFields(input, path, "tool.forms." + capability + ".inputs[]", mode, diagnostics);
                enumValue(input.type, Set.of("text", "textarea", "number", "select", "checkbox"), path, "input.type", diagnostics);
                required(input.name, path, "input.name", diagnostics);
                pattern(input.name, FORM_INPUT_NAME, path, "input.name", diagnostics);
                if ("select".equals(input.type) && input.options == null) {
                    diagnostics.add(error("TOOL_INPUT_OPTIONS_REQUIRED", "Select inputs require options.", path));
                }
            }
        });
    }

    private static void validateRelated(ToolDsl dsl, Set<String> toolIds, Path path, String mode, List<Diagnostic> diagnostics) {
        if (dsl.related == null) {
            return;
        }
        unknownFields(dsl.related, path, "tool.related", mode, diagnostics);
        if (dsl.related.auto != null) {
            unknownFields(dsl.related.auto, path, "tool.related.auto", mode, diagnostics);
        }
        for (String related : nullToEmpty(dsl.related.explicit)) {
            if (!toolIds.contains(related)) {
                diagnostics.add(modeDiagnostic(mode, "TOOL_RELATED_MISSING", "Explicit related tool does not exist: " + related, path));
            }
        }
    }

    private static void nestedUnknowns(ToolDsl dsl, Path path, String mode, List<Diagnostic> diagnostics) {
        if (dsl.algorithm != null) {
            unknownFields(dsl.algorithm, path, "tool.algorithm", mode, diagnostics);
        }
        if (dsl.seo != null) {
            unknownFields(dsl.seo, path, "tool.seo", mode, diagnostics);
        }
    }

    private static Map<String, Set<String>> algorithmCapabilities(List<SourceDocument<AlgorithmRegistryDsl>> registries) {
        Map<String, Set<String>> result = new HashMap<>();
        for (SourceDocument<AlgorithmRegistryDsl> registry : registries) {
            for (AlgorithmRegistryDsl.AlgorithmDsl algorithm : nullToEmpty(registry.document().algorithms)) {
                if (algorithm.algorithmId != null) {
                    result.put(algorithm.algorithmId, new HashSet<>(nullToEmpty(algorithm.capabilities)));
                }
            }
        }
        return result;
    }

    private static String siteMode(SourceDocument<SiteDsl> site) {
        if (site == null || site.document().mode == null) {
            return "demo";
        }
        return "production".equals(site.document().mode) ? "production" : "demo";
    }

    private static Diagnostic algorithmDiagnostic(ToolDsl tool, String mode, String code, String message, Path path) {
        boolean productionActive = "production".equals(mode) && "active".equals(tool.status);
        return productionActive ? error(code, message, path) : warning(code, message, path);
    }

    private static Diagnostic modeDiagnostic(String mode, String code, String message, Path path) {
        return "production".equals(mode) ? error(code, message, path) : warning(code, message, path);
    }

    private static void unknownFields(UnknownFieldAware node, Path path, String context, String mode, List<Diagnostic> diagnostics) {
        node.unknownFields().keySet().stream()
                .filter(name -> !name.startsWith("x-"))
                .forEach(name -> diagnostics.add(modeDiagnostic(mode, "UNKNOWN_FIELD", "Unknown field " + context + "." + name, path)));
    }

    private static void schemaVersion(Integer version, Path path, String context, List<Diagnostic> diagnostics) {
        if (version == null) {
            diagnostics.add(error("SCHEMA_VERSION_REQUIRED", context + ".schemaVersion is required.", path));
        } else if (version != SUPPORTED_SCHEMA_VERSION) {
            diagnostics.add(error("SCHEMA_VERSION_UNSUPPORTED", context + ".schemaVersion is not supported: " + version, path));
        }
    }

    private static void required(Object value, Path path, String field, List<Diagnostic> diagnostics) {
        if (value == null || (value instanceof String text && blank(text)) || (value instanceof Map<?, ?> map && map.isEmpty())) {
            diagnostics.add(error("REQUIRED_FIELD_MISSING", field + " is required.", path));
        }
    }

    private static void nonEmpty(List<?> value, Path path, String field, List<Diagnostic> diagnostics) {
        if (value == null || value.isEmpty()) {
            diagnostics.add(error("REQUIRED_FIELD_MISSING", field + " is required and must not be empty.", path));
        }
    }

    private static void enumValue(String value, Set<String> allowed, Path path, String field, List<Diagnostic> diagnostics) {
        required(value, path, field, diagnostics);
        if (value != null && !allowed.contains(value)) {
            diagnostics.add(error("ENUM_VALUE_INVALID", field + " has invalid value: " + value, path));
        }
    }

    private static void pattern(String value, Pattern pattern, Path path, String field, List<Diagnostic> diagnostics) {
        if (value != null && !pattern.matcher(value).matches()) {
            diagnostics.add(error("PATTERN_INVALID", field + " has invalid value: " + value, path));
        }
    }

    private static void knownCapability(String capability, Path path, List<Diagnostic> diagnostics) {
        if (capability != null && !KNOWN_CAPABILITIES.contains(capability)) {
            diagnostics.add(error("CAPABILITY_UNKNOWN", "Unknown capability: " + capability, path));
        }
    }

    private static void duplicate(Set<String> seen, String value, Path path, String code, String message, List<Diagnostic> diagnostics) {
        if (value != null && !seen.add(value)) {
            diagnostics.add(error(code, message + " " + value, path));
        }
    }

    private static Diagnostic error(String code, String message, Path path) {
        return new Diagnostic(DiagnosticSeverity.ERROR, code, message, location(path), Map.of());
    }

    private static Diagnostic warning(String code, String message, Path path) {
        return new Diagnostic(DiagnosticSeverity.WARNING, code, message, location(path), Map.of());
    }

    private static DiagnosticLocation location(Path path) {
        return path == null ? DiagnosticLocation.none() : DiagnosticLocation.source(path.toString());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> List<T> nullToEmpty(List<T> value) {
        return value == null ? List.of() : value;
    }
}
