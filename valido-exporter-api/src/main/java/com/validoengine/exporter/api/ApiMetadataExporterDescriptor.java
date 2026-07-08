package com.validoengine.exporter.api;

import com.validoengine.core.model.ExporterId;

public final class ApiMetadataExporterDescriptor {
    public static final ExporterId ID = new ExporterId("api-metadata");
    public static final int METADATA_VERSION = 1;
    public static final String ARTIFACT_NAME = "api-metadata.json";

    private ApiMetadataExporterDescriptor() {
    }

    public static ExporterId id() {
        return ID;
    }

    public static int metadataVersion() {
        return METADATA_VERSION;
    }

    public static String artifactName() {
        return ARTIFACT_NAME;
    }
}
