package com.validoengine.examples;

import com.validoengine.generator.ValidoGenerator;
import com.validoengine.generator.model.GenerationRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidoExamplesTest {
    @Test
    void examplesValidateThroughGeneratorFlow() {
        Path examplesRoot = examplesRoot();

        var result = new ValidoGenerator().generate(GenerationRequest.forProjectRoot(examplesRoot));

        assertTrue(result.successful(), () -> result.report().validationReport().diagnostics().toString());
        var model = result.optionalProjectModel().orElseThrow();
        assertEquals("validohub", model.site().id().value());
        assertEquals(3, model.tools().size());
        assertEquals(3, model.categories().size());
        assertEquals(1, model.countries().size());
        assertEquals(3, result.report().algorithmCount());
        assertEquals(8, model.capabilities().size());
    }

    private static Path examplesRoot() {
        Path moduleRelative = Path.of("src/main/resources");
        if (moduleRelative.toFile().isDirectory()) {
            return moduleRelative.toAbsolutePath().normalize();
        }
        return Path.of("valido-examples/src/main/resources").toAbsolutePath().normalize();
    }
}
