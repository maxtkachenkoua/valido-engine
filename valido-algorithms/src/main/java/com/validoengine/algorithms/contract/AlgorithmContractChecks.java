package com.validoengine.algorithms.contract;

import com.validoengine.core.algorithm.AlgorithmExample;
import com.validoengine.core.algorithm.AlgorithmMetadata;
import com.validoengine.core.algorithm.ValidoAlgorithm;

import java.util.HashSet;
import java.util.Objects;

public final class AlgorithmContractChecks {
    private AlgorithmContractChecks() {
    }

    public static void verify(ValidoAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        AlgorithmMetadata metadata = Objects.requireNonNull(algorithm.metadata(), "algorithm metadata");
        if (!algorithm.id().equals(metadata.algorithmId())) {
            throw new AlgorithmContractException("Algorithm id must match metadata.algorithmId.");
        }
        if (algorithm.capabilities() == null || algorithm.capabilities().isEmpty()) {
            throw new AlgorithmContractException("Algorithm capabilities must not be empty.");
        }
        if (!metadata.capabilities().containsAll(algorithm.capabilities())) {
            throw new AlgorithmContractException("Metadata capabilities must include algorithm capabilities.");
        }
        for (AlgorithmExample example : metadata.examples()) {
            if (!metadata.capabilities().contains(example.capability())) {
                throw new AlgorithmContractException("Example capability is not declared in metadata: " + example.capability());
            }
        }
        if (new HashSet<>(metadata.capabilities()).size() != metadata.capabilities().size()) {
            throw new AlgorithmContractException("Metadata capabilities must be unique.");
        }
    }
}
