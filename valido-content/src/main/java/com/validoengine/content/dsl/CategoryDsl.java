package com.validoengine.content.dsl;

import java.util.Map;

public class CategoryDsl extends UnknownFieldAware {
    public Integer schemaVersion;
    public String id;
    public String slug;
    public Map<String, String> name;
    public Map<String, String> summary;
    public String parent;
    public Integer order;
    public SeoDsl seo;
}
