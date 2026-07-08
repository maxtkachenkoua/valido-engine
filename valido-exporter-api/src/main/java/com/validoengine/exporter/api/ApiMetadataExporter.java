package com.validoengine.exporter.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.validoengine.core.algorithm.AlgorithmErrorMetadata;
import com.validoengine.core.algorithm.AlgorithmExample;
import com.validoengine.core.algorithm.AlgorithmFieldMetadata;
import com.validoengine.core.algorithm.AlgorithmMetadata;
import com.validoengine.core.capability.CapabilityId;
import com.validoengine.core.model.AlgorithmBindingModel;
import com.validoengine.core.model.ExporterId;
import com.validoengine.core.model.FormInputModel;
import com.validoengine.core.model.FormModel;
import com.validoengine.core.model.LocaleCode;
import com.validoengine.core.model.LocalizedText;
import com.validoengine.core.model.ProjectModel;
import com.validoengine.core.model.RouteModel;
import com.validoengine.core.model.RouteType;
import com.validoengine.core.model.ToolId;
import com.validoengine.core.model.ToolModel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ApiMetadataExporter {
    private static final ExporterId API_METADATA_EXPORTER_ID = ApiMetadataExporterDescriptor.ID;

    private final ObjectMapper objectMapper;

    public ApiMetadataExporter() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ApiMetadataExportResult export(ProjectModel projectModel) {
        Objects.requireNonNull(projectModel, "projectModel");
        Path outputFile = outputFile(projectModel);
        write(outputFile, metadata(projectModel));
        return new ApiMetadataExportResult(outputFile);
    }

    private Map<String, Object> metadata(ProjectModel projectModel) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("metadataVersion", ApiMetadataExporterDescriptor.METADATA_VERSION);
        root.put("siteId", projectModel.site().id().value());
        root.put("generatedAt", Instant.now().toString());
        root.put("tools", projectModel.tools().stream()
                .sorted(Comparator.comparing(tool -> tool.id().value()))
                .map(tool -> tool(projectModel, tool))
                .toList());
        return root;
    }

    private Map<String, Object> tool(ProjectModel projectModel, ToolModel tool) {
        AlgorithmBindingModel binding = tool.algorithmBinding();
        AlgorithmMetadata metadata = binding.algorithmId() == null ? null : projectModel.algorithmMetadata().get(binding.algorithmId());
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("toolId", tool.id().value());
        entry.put("algorithmId", binding.algorithmId() == null ? null : binding.algorithmId().value());
        entry.put("version", binding.algorithmVersion());
        entry.put("status", tool.status().name().toLowerCase());
        entry.put("capabilities", tool.capabilities().stream().sorted().map(CapabilityId::value).toList());
        entry.put("inputs", inputs(projectModel, tool));
        entry.put("outputs", outputs(metadata));
        entry.put("errors", errors(metadata));
        entry.put("examples", examples(metadata));
        entry.put("routes", routes(projectModel, tool.id()));
        return entry;
    }

    private Map<String, Object> inputs(ProjectModel projectModel, ToolModel tool) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        tool.forms().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> inputs.put(entry.getKey().value(), formInputs(projectModel, entry.getValue())));
        return inputs;
    }

    private List<Map<String, Object>> formInputs(ProjectModel projectModel, FormModel form) {
        return form.inputs().stream()
                .map(input -> input(projectModel, input))
                .toList();
    }

    private Map<String, Object> input(ProjectModel projectModel, FormInputModel input) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", input.name());
        entry.put("type", input.type().name().toLowerCase());
        entry.put("required", input.required());
        entry.put("label", localized(input.label(), projectModel.site().locales()));
        return entry;
    }

    private Map<String, Object> outputs(AlgorithmMetadata metadata) {
        Map<String, Object> outputs = new LinkedHashMap<>();
        if (metadata == null) {
            return outputs;
        }
        metadata.outputs().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> outputs.put(entry.getKey().value(), fields(entry.getValue())));
        return outputs;
    }

    private List<Map<String, Object>> fields(List<AlgorithmFieldMetadata> fields) {
        return fields.stream()
                .map(field -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("name", field.name());
                    entry.put("type", field.type());
                    if (field.required()) {
                        entry.put("required", true);
                    }
                    if (!field.label().values().isEmpty()) {
                        entry.put("label", localized(field.label(), field.label().values().keySet()));
                    }
                    return entry;
                })
                .toList();
    }

    private List<Map<String, Object>> errors(AlgorithmMetadata metadata) {
        if (metadata == null) {
            return List.of();
        }
        return metadata.errors().stream()
                .sorted(Comparator.comparing(AlgorithmErrorMetadata::code))
                .map(error -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("code", error.code());
                    entry.put("message", localized(error.message(), error.message().values().keySet()));
                    return entry;
                })
                .toList();
    }

    private List<Map<String, Object>> examples(AlgorithmMetadata metadata) {
        if (metadata == null) {
            return List.of();
        }
        return metadata.examples().stream()
                .sorted(Comparator.comparing(example -> example.capability().value()))
                .map(this::example)
                .toList();
    }

    private Map<String, Object> example(AlgorithmExample example) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("capability", example.capability().value());
        entry.put("input", sortedMap(example.input()));
        entry.put("expected", sortedMap(example.expected()));
        return entry;
    }

    private Map<String, Object> routes(ProjectModel projectModel, ToolId toolId) {
        Map<String, Object> routes = new LinkedHashMap<>();
        projectModel.routes().stream()
                .filter(route -> route.pageType() == RouteType.TOOL)
                .filter(route -> route.sourceAggregateId().equals(toolId.value()))
                .sorted(Comparator.comparing(route -> route.locale().value()))
                .forEach(route -> routes.put(route.locale().value(), route.path()));
        return routes;
    }

    private static Map<String, Object> localized(LocalizedText text, Iterable<LocaleCode> locales) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (LocaleCode locale : locales) {
            text.get(locale).ifPresent(value -> values.put(locale.value(), value));
        }
        return values;
    }

    private static Map<String, Object> sortedMap(Map<String, Object> source) {
        return source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Path outputFile(ProjectModel projectModel) {
        if (!projectModel.exportPlan().exporters().contains(API_METADATA_EXPORTER_ID)) {
            throw new IllegalArgumentException("ProjectModel ExportPlan does not include api-metadata exporter.");
        }
        Path targetDirectory = projectModel.exportPlan().targetDirectories().get(API_METADATA_EXPORTER_ID);
        if (targetDirectory == null) {
            throw new IllegalArgumentException("ProjectModel ExportPlan does not include api-metadata target directory.");
        }
        Path outputFile = targetDirectory.resolve(ApiMetadataExporterDescriptor.ARTIFACT_NAME);
        Set<Path> plannedArtifacts = Set.copyOf(projectModel.exportPlan().generatedArtifacts());
        if (plannedArtifacts.stream().filter(Predicate.isEqual(outputFile)).findFirst().isEmpty()) {
            throw new IllegalArgumentException("ProjectModel ExportPlan is missing api-metadata artifact: " + outputFile);
        }
        return outputFile;
    }

    private void write(Path outputFile, Map<String, Object> metadata) {
        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize API metadata.", exception);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write API metadata.", exception);
        }
    }
}
