package com.validoengine.exporter.html;

import com.validoengine.core.model.ExporterId;

public final class HtmlExporterDescriptor {
    public static final ExporterId ID = new ExporterId("html");
    public static final String ARTIFACT_KIND = "static-html";
    public static final String TEMPLATE_ENGINE = "thymeleaf";

    private HtmlExporterDescriptor() {
    }

    public static ExporterId id() {
        return ID;
    }

    public static String artifactKind() {
        return ARTIFACT_KIND;
    }

    public static String templateEngine() {
        return TEMPLATE_ENGINE;
    }
}
