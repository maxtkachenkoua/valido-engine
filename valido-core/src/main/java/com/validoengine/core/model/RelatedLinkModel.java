package com.validoengine.core.model;

import java.util.Objects;

public record RelatedLinkModel(
        ToolId toolId,
        RelatedReason reason,
        int orderBucket,
        String title,
        RouteModel route
) {
    public RelatedLinkModel {
        Objects.requireNonNull(toolId, "toolId");
        Objects.requireNonNull(reason, "reason");
        title = IdentifierRules.requireText(title, "related link title");
        Objects.requireNonNull(route, "route");
    }
}
