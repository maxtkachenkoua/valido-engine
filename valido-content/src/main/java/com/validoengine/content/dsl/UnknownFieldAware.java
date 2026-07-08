package com.validoengine.content.dsl;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class UnknownFieldAware {
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void putUnknownField(String name, Object value) {
        unknownFields.put(name, value);
    }

    @JsonIgnore
    public Map<String, Object> unknownFields() {
        return Map.copyOf(unknownFields);
    }
}
