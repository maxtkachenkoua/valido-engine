package com.validoengine.core.model;

import java.util.Map;
import java.util.Optional;

public record LocalizedText(Map<LocaleCode, String> values) {
    public LocalizedText {
        values = values == null ? Map.of() : Map.copyOf(values);
        values.forEach((locale, value) -> {
            if (locale == null) {
                throw new IllegalArgumentException("localized text locale must not be null");
            }
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("localized text value must not be blank");
            }
        });
    }

    public static LocalizedText empty() {
        return new LocalizedText(Map.of());
    }

    public static LocalizedText of(LocaleCode locale, String value) {
        return new LocalizedText(Map.of(locale, value));
    }

    public Optional<String> get(LocaleCode locale) {
        return Optional.ofNullable(values.get(locale));
    }

    public String resolve(LocaleCode preferred, LocaleCode fallback) {
        return get(preferred)
                .or(() -> get(fallback))
                .orElse("");
    }
}
