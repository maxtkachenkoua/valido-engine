package com.validoengine.core.model;

public enum ContentBlockType {
    EXPLANATION("explanation"),
    EXAMPLES("examples"),
    FAQ("faq"),
    REFERENCES("references"),
    DEVELOPER_EXAMPLES("developer-examples");

    private final String id;

    ContentBlockType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
