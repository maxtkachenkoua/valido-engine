package com.validoengine.content.model;

import com.validoengine.core.algorithm.AlgorithmRegistry;
import com.validoengine.core.model.CategoryModel;
import com.validoengine.core.model.ContentModel;
import com.validoengine.core.model.CountryModel;
import com.validoengine.core.model.SiteModel;
import com.validoengine.core.model.ToolModel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ContentProject(
        Path projectRoot,
        SiteModel site,
        List<CategoryModel> categories,
        List<CountryModel> countries,
        List<ToolModel> tools,
        AlgorithmRegistry algorithmRegistry,
        ContentModel content
) {
    public ContentProject {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(site, "site");
        categories = categories == null ? List.of() : List.copyOf(categories);
        countries = countries == null ? List.of() : List.copyOf(countries);
        tools = tools == null ? List.of() : List.copyOf(tools);
        algorithmRegistry = algorithmRegistry == null ? AlgorithmRegistry.empty() : algorithmRegistry;
        content = content == null ? ContentModel.empty() : content;
    }
}
