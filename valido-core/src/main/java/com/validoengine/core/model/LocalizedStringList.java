package com.validoengine.core.model;

import java.util.List;
import java.util.Map;

public record LocalizedStringList(Map<LocaleCode, List<String>> values) {
    public LocalizedStringList {
        values = values == null ? Map.of() : copy(values);
    }

    private static Map<LocaleCode, List<String>> copy(Map<LocaleCode, List<String>> source) {
        return source.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }

    public static LocalizedStringList empty() {
        return new LocalizedStringList(Map.of());
    }

    public List<String> getOrEmpty(LocaleCode locale) {
        return values.getOrDefault(locale, List.of());
    }
}
