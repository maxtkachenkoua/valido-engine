package com.validoengine.generator.model;

import com.validoengine.core.model.RouteModel;
import com.validoengine.core.validation.ValidationReport;

import java.util.List;

public record RouteGenerationResult(List<RouteModel> routes, ValidationReport report) {
    public RouteGenerationResult {
        routes = routes == null ? List.of() : List.copyOf(routes);
        report = report == null ? ValidationReport.empty() : report;
    }

    public boolean successful() {
        return !report.hasErrors();
    }
}
