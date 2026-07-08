package com.validoengine.core.model;

import com.validoengine.core.capability.CapabilityId;

import java.util.List;
import java.util.Objects;

public record FormModel(
        CapabilityId capability,
        List<FormInputModel> inputs,
        List<CapabilityId> actions
) {
    public FormModel {
        Objects.requireNonNull(capability, "capability");
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        actions = List.copyOf(actions);
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("form actions must not be empty");
        }
    }
}
