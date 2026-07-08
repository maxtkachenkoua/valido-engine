package com.validoengine.core.model;

import java.util.List;

public record RelatedConfigModel(
        List<ToolId> explicit,
        boolean sameCountry,
        boolean sameCategory,
        boolean sameCapability,
        boolean sameModule
) {
    public RelatedConfigModel {
        explicit = explicit == null ? List.of() : List.copyOf(explicit);
    }

    public static RelatedConfigModel defaults() {
        return new RelatedConfigModel(List.of(), true, true, true, true);
    }
}
