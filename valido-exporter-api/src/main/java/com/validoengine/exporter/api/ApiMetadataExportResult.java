package com.validoengine.exporter.api;

import java.nio.file.Path;

public record ApiMetadataExportResult(Path writtenFile) {
    public ApiMetadataExportResult {
        java.util.Objects.requireNonNull(writtenFile, "writtenFile");
    }
}
