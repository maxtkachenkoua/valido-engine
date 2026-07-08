package com.validoengine.algorithms.contract;

import com.validoengine.core.algorithm.AlgorithmExample;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlgorithmContractChecksTest {
    @Test
    void acceptsConsistentAlgorithmContract() {
        ValidoAlgorithm algorithm = algorithm(
                new AlgorithmId("example.validate"),
                Set.of(CapabilityId.VALIDATE),
                new AlgorithmMetadata(
                        new AlgorithmId("example.validate"),
                        1,
                        null,
                        null,
                        Set.of(CapabilityId.VALIDATE),
                        Map.of(),
                        Map.of(),
                        List.of(),
                        List.of(new AlgorithmExample(CapabilityId.VALIDATE, Map.of("value", "abc"), Map.of("valid", true)))
                )
        );

        assertDoesNotThrow(() -> AlgorithmContractChecks.verify(algorithm));
    }

    @Test
    void rejectsMetadataIdMismatch() {
        ValidoAlgorithm algorithm = algorithm(
                new AlgorithmId("example.validate"),
                Set.of(CapabilityId.VALIDATE),
                new AlgorithmMetadata(
                        new AlgorithmId("example.other"),
                        1,
                        null,
                        null,
                        Set.of(CapabilityId.VALIDATE),
                        Map.of(),
                        Map.of(),
                        List.of(),
                        List.of()
                )
        );

        assertThrows(AlgorithmContractException.class, () -> AlgorithmContractChecks.verify(algorithm));
    }

    private static ValidoAlgorithm algorithm(AlgorithmId id, Set<CapabilityId> capabilities, AlgorithmMetadata metadata) {
        return new ValidoAlgorithm() {
            @Override
            public AlgorithmId id() {
                return id;
            }

            @Override
            public Set<CapabilityId> capabilities() {
                return capabilities;
            }

            @Override
            public AlgorithmMetadata metadata() {
                return metadata;
            }

            @Override
            public CapabilityResult execute(CapabilityId capability, CapabilityInput input) {
                return CapabilityResult.success(Map.of());
            }
        };
    }
}
