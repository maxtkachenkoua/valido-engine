package com.validoengine.exporter.html;

import java.nio.file.Path;
import java.util.List;

public record HtmlExportResult(List<Path> writtenFiles) {
    public HtmlExportResult {
        writtenFiles = writtenFiles == null ? List.of() : List.copyOf(writtenFiles);
    }
}
