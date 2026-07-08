package com.validoengine.core.algorithm;

import com.validoengine.core.capability.CapabilityId;
import com.validoengine.core.model.AlgorithmId;

import java.util.Set;

public interface ValidoAlgorithm {
    AlgorithmId id();

    Set<CapabilityId> capabilities();

    AlgorithmMetadata metadata();

    CapabilityResult execute(CapabilityId capability, CapabilityInput input);
}
