package com.validoengine.algorithms.catalog;

import com.validoengine.core.algorithm.AlgorithmMetadata;
import com.validoengine.core.algorithm.CapabilityInput;
import com.validoengine.core.algorithm.CapabilityResult;
import com.validoengine.core.algorithm.ValidoAlgorithm;
import com.validoengine.core.capability.CapabilityId;
import com.validoengine.core.model.AlgorithmId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlgorithmCatalogTest {
    @Test
    void indexesAlgorithmsByStableId() {
        ValidoAlgorithm algorithm = fakeAlgorithm("example.validate");

        AlgorithmCatalog catalog = AlgorithmCatalog.of(algorithm);

        assertEquals(1, catalog.size());
        assertTrue(catalog.find(new AlgorithmId("example.validate")).isPresent());
    }

    @Test
    void rejectsDuplicateAlgorithmIds() {
        ValidoAlgorithm first = fakeAlgorithm("example.validate");
        ValidoAlgorithm second = fakeAlgorithm("example.validate");

        assertThrows(IllegalArgumentException.class, () -> AlgorithmCatalog.of(first, second));
    }

    private static ValidoAlgorithm fakeAlgorithm(String id) {
        return new ValidoAlgorithm() {
            private final AlgorithmId algorithmId = new AlgorithmId(id);
            private final Set<CapabilityId> capabilities = Set.of(CapabilityId.VALIDATE);

            @Override
            public AlgorithmId id() {
                return algorithmId;
            }

            @Override
            public Set<CapabilityId> capabilities() {
                return capabilities;
            }

            @Override
            public AlgorithmMetadata metadata() {
                return new AlgorithmMetadata(algorithmId, 1, null, null, capabilities, Map.of(), Map.of(), List.of(), List.of());
            }

            @Override
            public CapabilityResult execute(CapabilityId capability, CapabilityInput input) {
                return CapabilityResult.success(Map.of());
            }
        };
    }
}
