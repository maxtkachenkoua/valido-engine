package com.validoengine.core.model;

import java.util.List;
import java.util.Objects;

public record CountryModel(
        CountryCode code,
        String slug,
        LocalizedText name,
        String region,
        List<LocaleCode> languages,
        String currency,
        List<CountryCode> relatedCountries
) {
    public CountryModel {
        Objects.requireNonNull(code, "code");
        slug = IdentifierRules.requireText(slug, "country slug");
        Objects.requireNonNull(name, "name");
        if (region != null && region.isBlank()) {
            throw new IllegalArgumentException("region must not be blank when present");
        }
        languages = languages == null ? List.of() : List.copyOf(languages);
        if (currency != null && currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank when present");
        }
        relatedCountries = relatedCountries == null ? List.of() : List.copyOf(relatedCountries);
    }
}
