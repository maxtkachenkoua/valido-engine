package com.validoengine.content.model;

import com.validoengine.content.dsl.AlgorithmRegistryDsl;
import com.validoengine.content.dsl.CategoryDsl;
import com.validoengine.content.dsl.CountryDsl;
import com.validoengine.content.dsl.SiteDsl;
import com.validoengine.content.dsl.StructuredContentBlockDsl;
import com.validoengine.content.dsl.ToolDsl;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record RawContentWorkspace(
        Path projectRoot,
        SourceDocument<SiteDsl> site,
        List<SourceDocument<CategoryDsl>> categories,
        List<SourceDocument<CountryDsl>> countries,
        List<SourceDocument<ToolDsl>> tools,
        List<SourceDocument<AlgorithmRegistryDsl>> algorithmRegistries,
        List<SourceDocument<StructuredContentBlockDsl>> structuredContentBlocks,
        List<MarkdownContentBlock> markdownContentBlocks
) {
    public RawContentWorkspace {
        Objects.requireNonNull(projectRoot, "projectRoot");
        categories = categories == null ? List.of() : List.copyOf(categories);
        countries = countries == null ? List.of() : List.copyOf(countries);
        tools = tools == null ? List.of() : List.copyOf(tools);
        algorithmRegistries = algorithmRegistries == null ? List.of() : List.copyOf(algorithmRegistries);
        structuredContentBlocks = structuredContentBlocks == null ? List.of() : List.copyOf(structuredContentBlocks);
        markdownContentBlocks = markdownContentBlocks == null ? List.of() : List.copyOf(markdownContentBlocks);
    }
}
