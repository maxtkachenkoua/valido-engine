package com.validoengine.exporter.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiMetadataExporterDescriptorTest {
    @Test
    void identifiesApiMetadataExporterBoundary() {
        assertEquals("api-metadata", ApiMetadataExporterDescriptor.id().value());
        assertEquals(1, ApiMetadataExporterDescriptor.metadataVersion());
        assertEquals("api-metadata.json", ApiMetadataExporterDescriptor.artifactName());
    }
}
