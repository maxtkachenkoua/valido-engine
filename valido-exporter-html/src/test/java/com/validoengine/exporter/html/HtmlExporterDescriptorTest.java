package com.validoengine.exporter.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlExporterDescriptorTest {
    @Test
    void identifiesHtmlExporterBoundary() {
        assertEquals("html", HtmlExporterDescriptor.id().value());
        assertEquals("static-html", HtmlExporterDescriptor.artifactKind());
        assertEquals("thymeleaf", HtmlExporterDescriptor.templateEngine());
    }
}
