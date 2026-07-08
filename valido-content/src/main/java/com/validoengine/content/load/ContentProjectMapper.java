package com.validoengine.content.load;

import com.validoengine.content.dsl.AlgorithmRegistryDsl;
import com.validoengine.content.dsl.CategoryDsl;
import com.validoengine.content.dsl.CountryDsl;
import com.validoengine.content.dsl.SeoDsl;
import com.validoengine.content.dsl.SiteDsl;
import com.validoengine.content.dsl.StructuredContentBlockDsl;
import com.validoengine.content.dsl.ToolDsl;
import com.validoengine.content.model.ContentProject;
import com.validoengine.content.model.MarkdownContentBlock;
import com.validoengine.content.model.RawContentWorkspace;
import com.validoengine.content.model.SourceDocument;
import com.validoengine.core.algorithm.AlgorithmErrorMetadata;
import com.validoengine.core.algorithm.AlgorithmExample;
import com.validoengine.core.algorithm.AlgorithmFieldMetadata;
import com.validoengine.core.algorithm.AlgorithmFieldOption;
import com.validoengine.core.algorithm.AlgorithmRegistry;
import com.validoengine.core.algorithm.AlgorithmRegistryEntry;
import com.validoengine.core.capability.CapabilityId;
import com.validoengine.core.diagnostic.Diagnostic;
import com.validoengine.core.model.AlgorithmBindingModel;
import com.validoengine.core.model.AlgorithmCompatibilityStatus;
import com.validoengine.core.model.AlgorithmId;
import com.validoengine.core.model.CategoryId;
import com.validoengine.core.model.CategoryModel;
import com.validoengine.core.model.ContentBlockModel;
import com.validoengine.core.model.ContentBlockType;
import com.validoengine.core.model.ContentModel;
import com.validoengine.core.model.ContentStatus;
import com.validoengine.core.model.CountryCode;
import com.validoengine.core.model.CountryModel;
import com.validoengine.core.model.FormInputModel;
import com.validoengine.core.model.FormInputType;
import com.validoengine.core.model.FormModel;
import com.validoengine.core.model.InputOptionModel;
import com.validoengine.core.model.LocaleCode;
import com.validoengine.core.model.LocalizedStringList;
import com.validoengine.core.model.LocalizedText;
import com.validoengine.core.model.ModuleId;
import com.validoengine.core.model.SeoModel;
import com.validoengine.core.model.SiteId;
import com.validoengine.core.model.SiteMode;
import com.validoengine.core.model.SiteModel;
import com.validoengine.core.model.ToolId;
import com.validoengine.core.model.ToolModel;
import com.validoengine.core.model.ToolStatus;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ContentProjectMapper {
    public ContentProject map(RawContentWorkspace workspace) {
        Objects.requireNonNull(workspace, "workspace");
        AlgorithmRegistry registry = mapAlgorithmRegistry(workspace.algorithmRegistries());
        SiteModel site = mapSite(workspace.site().document());
        List<CategoryModel> categories = workspace.categories().stream().map(source -> mapCategory(source.document())).toList();
        List<CountryModel> countries = workspace.countries().stream().map(source -> mapCountry(source.document())).toList();
        List<ToolModel> tools = workspace.tools().stream().map(source -> mapTool(source.document(), registry)).toList();
        ContentModel content = mapContent(workspace);
        return new ContentProject(workspace.projectRoot(), site, categories, countries, tools, registry, content);
    }

    private static SiteModel mapSite(SiteDsl dsl) {
        SiteDsl.RoutingDsl routing = dsl.routing == null ? new SiteDsl.RoutingDsl() : dsl.routing;
        SiteDsl.GenerationDsl generation = dsl.generation == null ? new SiteDsl.GenerationDsl() : dsl.generation;
        return new SiteModel(
                new SiteId(dsl.id),
                localized(dsl.name),
                dsl.baseUrl,
                new LocaleCode(dsl.defaultLocale),
                dsl.locales.stream().map(LocaleCode::new).toList(),
                "production".equals(dsl.mode) ? SiteMode.PRODUCTION : SiteMode.DEMO,
                dsl.modules.stream().map(ModuleId::new).toList(),
                Path.of(dsl.output.directory),
                routing.defaultToolPath == null ? "tools" : routing.defaultToolPath,
                routing.trailingSlash == null || routing.trailingSlash,
                generation.includeDrafts != null && generation.includeDrafts,
                seo(dsl.seo)
        );
    }

    private static CategoryModel mapCategory(CategoryDsl dsl) {
        return new CategoryModel(
                new CategoryId(dsl.id),
                dsl.slug,
                localized(dsl.name),
                localized(dsl.summary),
                dsl.parent == null ? null : new CategoryId(dsl.parent),
                dsl.order == null ? 100 : dsl.order,
                seo(dsl.seo)
        );
    }

    private static CountryModel mapCountry(CountryDsl dsl) {
        return new CountryModel(
                new CountryCode(dsl.code),
                dsl.slug,
                localized(dsl.name),
                dsl.region,
                nullToEmpty(dsl.languages).stream().map(LocaleCode::new).toList(),
                dsl.currency,
                nullToEmpty(dsl.relatedCountries).stream().map(CountryCode::new).toList()
        );
    }

    private static ToolModel mapTool(ToolDsl dsl, AlgorithmRegistry registry) {
        Set<CapabilityId> capabilities = nullToEmpty(dsl.capabilities).stream().map(CapabilityId::new).collect(Collectors.toUnmodifiableSet());
        AlgorithmBindingModel binding = algorithmBinding(dsl, registry, capabilities);
        Map<CapabilityId, FormModel> forms = new LinkedHashMap<>();
        if (dsl.forms != null) {
            dsl.forms.forEach((capability, form) -> forms.put(new CapabilityId(capability), mapForm(capability, form)));
        }
        return new ToolModel(
                new ToolId(dsl.id),
                new ModuleId(dsl.module),
                dsl.country == null ? null : new CountryCode(dsl.country),
                new CategoryId(dsl.category),
                toolStatus(dsl.status),
                localized(dsl.name),
                localized(dsl.summary),
                capabilities,
                binding,
                forms,
                localizedStringList(dsl.aliases),
                seo(dsl.seo),
                Map.of(),
                List.of()
        );
    }

    private static FormModel mapForm(String capability, ToolDsl.FormDsl form) {
        return new FormModel(
                new CapabilityId(capability),
                nullToEmpty(form.inputs).stream().map(ContentProjectMapper::mapInput).toList(),
                form.actions.stream().map(CapabilityId::new).toList()
        );
    }

    private static FormInputModel mapInput(ToolDsl.InputDsl input) {
        return new FormInputModel(
                formInputType(input.type),
                input.name,
                localizedObject(input.label),
                Boolean.TRUE.equals(input.required),
                input.defaultValue,
                inputOptions(input.options),
                decimal(input.min),
                decimal(input.max),
                input.pattern,
                localizedObject(input.placeholder),
                localizedObject(input.help)
        );
    }

    private static AlgorithmBindingModel algorithmBinding(ToolDsl dsl, AlgorithmRegistry registry, Set<CapabilityId> toolCapabilities) {
        if (dsl.algorithm == null || dsl.algorithm.algorithmId == null) {
            return new AlgorithmBindingModel(null, null, AlgorithmCompatibilityStatus.MISSING, List.of());
        }
        AlgorithmId id = new AlgorithmId(dsl.algorithm.algorithmId);
        return registry.find(id)
                .map(entry -> {
                    boolean compatible = entry.capabilities().containsAll(toolCapabilities);
                    return new AlgorithmBindingModel(
                            id,
                            entry.version(),
                            compatible ? AlgorithmCompatibilityStatus.COMPATIBLE : AlgorithmCompatibilityStatus.INCOMPATIBLE,
                            List.of()
                    );
                })
                .orElseGet(() -> new AlgorithmBindingModel(id, null, AlgorithmCompatibilityStatus.UNRESOLVED, List.of()));
    }

    private static AlgorithmRegistry mapAlgorithmRegistry(List<SourceDocument<AlgorithmRegistryDsl>> registries) {
        Map<AlgorithmId, AlgorithmRegistryEntry> entries = new LinkedHashMap<>();
        for (SourceDocument<AlgorithmRegistryDsl> source : registries) {
            for (AlgorithmRegistryDsl.AlgorithmDsl algorithm : nullToEmpty(source.document().algorithms)) {
                AlgorithmRegistryEntry entry = new AlgorithmRegistryEntry(
                        new AlgorithmId(algorithm.algorithmId),
                        algorithm.javaClass,
                        algorithm.version,
                        nullToEmpty(algorithm.capabilities).stream().map(CapabilityId::new).collect(Collectors.toUnmodifiableSet()),
                        localized(algorithm.name),
                        localized(algorithm.summary),
                        fieldMap(algorithm.inputs),
                        fieldMap(algorithm.outputs),
                        nullToEmpty(algorithm.errors).stream()
                                .map(error -> new AlgorithmErrorMetadata(error.code, localized(error.message)))
                                .toList(),
                        nullToEmpty(algorithm.examples).stream()
                                .map(example -> new AlgorithmExample(new CapabilityId(example.capability), copyMap(example.input), copyMap(example.expected)))
                                .toList()
                );
                entries.put(entry.algorithmId(), entry);
            }
        }
        return new AlgorithmRegistry(entries);
    }

    private static ContentModel mapContent(RawContentWorkspace workspace) {
        Map<ToolId, List<ContentBlockModel>> blocksByTool = new LinkedHashMap<>();
        for (SourceDocument<StructuredContentBlockDsl> source : workspace.structuredContentBlocks()) {
            StructuredContentBlockDsl dsl = source.document();
            ToolId toolId = new ToolId(dsl.toolId);
            blocksByTool.computeIfAbsent(toolId, ignored -> new ArrayList<>()).add(new ContentBlockModel(
                    toolId,
                    contentBlockType(dsl.block),
                    new LocaleCode(dsl.locale),
                    localized(dsl.title),
                    dsl.order == null ? 100 : dsl.order,
                    contentStatus(dsl.status),
                    null,
                    Map.of("items", nullToEmpty(dsl.items))
            ));
        }
        for (MarkdownContentBlock markdown : workspace.markdownContentBlocks()) {
            blocksByTool.computeIfAbsent(markdown.toolId(), ignored -> new ArrayList<>()).add(new ContentBlockModel(
                    markdown.toolId(),
                    markdown.block(),
                    markdown.locale(),
                    markdown.title(),
                    markdown.order(),
                    markdown.status(),
                    markdown.markdown(),
                    Map.of()
            ));
        }
        return new ContentModel(blocksByTool);
    }

    private static Map<CapabilityId, List<AlgorithmFieldMetadata>> fieldMap(Map<String, List<AlgorithmRegistryDsl.FieldDsl>> raw) {
        if (raw == null) {
            return Map.of();
        }
        Map<CapabilityId, List<AlgorithmFieldMetadata>> result = new LinkedHashMap<>();
        raw.forEach((capability, fields) -> result.put(
                new CapabilityId(capability),
                nullToEmpty(fields).stream()
                        .map(field -> new AlgorithmFieldMetadata(
                                field.name,
                                field.type,
                                Boolean.TRUE.equals(field.required),
                                localizedObject(field.label),
                                copyMap(field.constraints),
                                nullToEmpty(field.options).stream()
                                        .map(option -> new AlgorithmFieldOption(option.value, localizedObject(option.label)))
                                        .toList()
                        ))
                        .toList()
        ));
        return result;
    }

    private static List<InputOptionModel> inputOptions(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<InputOptionModel> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object value = map.get("value");
                Object label = map.get("label");
                result.add(new InputOptionModel(String.valueOf(value), localizedObject(label)));
            } else {
                result.add(InputOptionModel.scalar(String.valueOf(item)));
            }
        }
        return List.copyOf(result);
    }

    private static SeoModel seo(SeoDsl seo) {
        if (seo == null) {
            return SeoModel.empty();
        }
        return new SeoModel(localized(seo.title), localized(seo.description), null, Map.of(), null, null, null, null, false);
    }

    private static LocalizedText localized(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return LocalizedText.empty();
        }
        return new LocalizedText(source.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(entry -> new LocaleCode(entry.getKey()), Map.Entry::getValue)));
    }

    @SuppressWarnings("unchecked")
    private static LocalizedText localizedObject(Object source) {
        if (source == null) {
            return LocalizedText.empty();
        }
        if (source instanceof Map<?, ?> map) {
            Map<String, String> values = new LinkedHashMap<>();
            map.forEach((key, value) -> values.put(String.valueOf(key), String.valueOf(value)));
            return localized(values);
        }
        return LocalizedText.of(LocaleCode.EN, String.valueOf(source));
    }

    private static LocalizedStringList localizedStringList(Map<String, List<String>> source) {
        if (source == null) {
            return LocalizedStringList.empty();
        }
        return new LocalizedStringList(source.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(entry -> new LocaleCode(entry.getKey()), entry -> List.copyOf(entry.getValue()))));
    }

    private static FormInputType formInputType(String value) {
        return FormInputType.valueOf(value.toUpperCase().replace('-', '_'));
    }

    private static ToolStatus toolStatus(String value) {
        return ToolStatus.valueOf(value.toUpperCase().replace('-', '_'));
    }

    private static ContentStatus contentStatus(String value) {
        return "draft".equals(value) ? ContentStatus.DRAFT : ContentStatus.ACTIVE;
    }

    private static ContentBlockType contentBlockType(String value) {
        for (ContentBlockType type : ContentBlockType.values()) {
            if (type.id().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported content block type: " + value);
    }

    private static BigDecimal decimal(Number value) {
        return value == null ? null : new BigDecimal(value.toString());
    }

    private static <T> List<T> nullToEmpty(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? Map.of() : Map.copyOf(source);
    }
}
