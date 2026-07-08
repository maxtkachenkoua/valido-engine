package com.validoengine.core.model;

import java.util.Objects;

public record InputOptionModel(
        String value,
        LocalizedText label
) {
    public InputOptionModel {
        value = IdentifierRules.requireText(value, "input option value");
        label = label == null ? LocalizedText.empty() : label;
    }

    public static InputOptionModel scalar(String value) {
        return new InputOptionModel(value, LocalizedText.of(LocaleCode.EN, Objects.requireNonNull(value, "value")));
    }
}
