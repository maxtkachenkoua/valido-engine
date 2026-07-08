package com.validoengine.core.capability;

import com.validoengine.core.model.LocalizedText;

import java.util.Objects;

public record CapabilityModel(
        CapabilityId id,
        LocalizedText name,
        LocalizedText summary
) {
    public CapabilityModel {
        Objects.requireNonNull(id, "id");
        name = name == null ? LocalizedText.empty() : name;
        summary = summary == null ? LocalizedText.empty() : summary;
    }
}
