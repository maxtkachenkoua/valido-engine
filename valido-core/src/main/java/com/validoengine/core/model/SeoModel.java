package com.validoengine.core.model;

import java.util.Map;

public record SeoModel(
        LocalizedText title,
        LocalizedText description,
        String canonicalUrl,
        Map<LocaleCode, String> hreflangUrls,
        LocalizedText openGraphTitle,
        LocalizedText openGraphDescription,
        LocalizedText twitterTitle,
        LocalizedText twitterDescription,
        boolean jsonLdEligible
) {
    public SeoModel {
        title = title == null ? LocalizedText.empty() : title;
        description = description == null ? LocalizedText.empty() : description;
        if (canonicalUrl != null && canonicalUrl.isBlank()) {
            throw new IllegalArgumentException("canonicalUrl must not be blank when present");
        }
        hreflangUrls = hreflangUrls == null ? Map.of() : Map.copyOf(hreflangUrls);
        openGraphTitle = openGraphTitle == null ? LocalizedText.empty() : openGraphTitle;
        openGraphDescription = openGraphDescription == null ? LocalizedText.empty() : openGraphDescription;
        twitterTitle = twitterTitle == null ? LocalizedText.empty() : twitterTitle;
        twitterDescription = twitterDescription == null ? LocalizedText.empty() : twitterDescription;
    }

    public static SeoModel empty() {
        return new SeoModel(
                LocalizedText.empty(),
                LocalizedText.empty(),
                null,
                Map.of(),
                LocalizedText.empty(),
                LocalizedText.empty(),
                LocalizedText.empty(),
                LocalizedText.empty(),
                false
        );
    }
}
