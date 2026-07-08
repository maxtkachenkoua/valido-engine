package com.validoengine.content.dsl;

import java.util.List;
import java.util.Map;

public class AlgorithmRegistryDsl extends UnknownFieldAware {
    public Integer schemaVersion;
    public List<AlgorithmDsl> algorithms;

    public static class AlgorithmDsl extends UnknownFieldAware {
        public String algorithmId;
        public String javaClass;
        public Integer version;
        public List<String> capabilities;
        public Map<String, String> name;
        public Map<String, String> summary;
        public Map<String, List<FieldDsl>> inputs;
        public Map<String, List<FieldDsl>> outputs;
        public List<ErrorDsl> errors;
        public List<ExampleDsl> examples;
    }

    public static class FieldDsl extends UnknownFieldAware {
        public String name;
        public String type;
        public Boolean required;
        public Object label;
        public Map<String, Object> constraints;
        public List<FieldOptionDsl> options;
    }

    public static class FieldOptionDsl extends UnknownFieldAware {
        public String value;
        public Object label;
    }

    public static class ErrorDsl extends UnknownFieldAware {
        public String code;
        public Map<String, String> message;
    }

    public static class ExampleDsl extends UnknownFieldAware {
        public String capability;
        public Map<String, Object> input;
        public Map<String, Object> expected;
    }
}
