package com.validoengine.content.dsl;

import java.util.List;
import java.util.Map;

public class StructuredContentBlockDsl extends UnknownFieldAware {
    public Integer schemaVersion;
    public String toolId;
    public String block;
    public String locale;
    public Map<String, String> title;
    public Integer order;
    public String status;
    public List<Map<String, Object>> items;
}
