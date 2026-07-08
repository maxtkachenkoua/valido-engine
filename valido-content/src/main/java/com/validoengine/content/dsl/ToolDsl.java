package com.validoengine.content.dsl;

import java.util.List;
import java.util.Map;

public class ToolDsl extends UnknownFieldAware {
    public Integer schemaVersion;
    public String id;
    public String kind;
    public String module;
    public String country;
    public String category;
    public String status;
    public Map<String, String> name;
    public Map<String, String> summary;
    public List<String> capabilities;
    public AlgorithmRefDsl algorithm;
    public Map<String, FormDsl> forms;
    public Map<String, List<String>> aliases;
    public SeoDsl seo;
    public RelatedDsl related;
    public Object content;

    public static class AlgorithmRefDsl extends UnknownFieldAware {
        public String algorithmId;
        public String javaClass;
    }

    public static class FormDsl extends UnknownFieldAware {
        public List<InputDsl> inputs;
        public List<String> actions;
    }

    public static class InputDsl extends UnknownFieldAware {
        public String type;
        public String name;
        public Object label;
        public Boolean required;
        public Object defaultValue;
        public Object options;
        public Number min;
        public Number max;
        public String pattern;
        public Object placeholder;
        public Object help;

        public void setDefault(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    public static class RelatedDsl extends UnknownFieldAware {
        public List<String> explicit;
        public RelatedAutoDsl auto;
    }

    public static class RelatedAutoDsl extends UnknownFieldAware {
        public Boolean sameCountry;
        public Boolean sameCategory;
        public Boolean sameCapability;
        public Boolean sameModule;
    }
}
