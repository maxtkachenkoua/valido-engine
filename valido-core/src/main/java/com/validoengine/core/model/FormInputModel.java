package com.validoengine.core.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record FormInputModel(
        FormInputType type,
        String name,
        LocalizedText label,
        boolean required,
        Object defaultValue,
        List<InputOptionModel> options,
        BigDecimal min,
        BigDecimal max,
        String pattern,
        LocalizedText placeholder,
        LocalizedText help
) {
    public FormInputModel {
        Objects.requireNonNull(type, "type");
        name = IdentifierRules.requirePattern(name, IdentifierRules.FORM_INPUT_NAME, "form input name");
        label = label == null ? LocalizedText.empty() : label;
        options = options == null ? List.of() : List.copyOf(options);
        if (type == FormInputType.SELECT && options.isEmpty()) {
            throw new IllegalArgumentException("select inputs require options");
        }
        if (pattern != null && pattern.isBlank()) {
            throw new IllegalArgumentException("pattern must not be blank when present");
        }
        placeholder = placeholder == null ? LocalizedText.empty() : placeholder;
        help = help == null ? LocalizedText.empty() : help;
    }
}
