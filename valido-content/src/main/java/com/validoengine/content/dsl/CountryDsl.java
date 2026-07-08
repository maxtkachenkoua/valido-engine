package com.validoengine.content.dsl;

import java.util.List;
import java.util.Map;

public class CountryDsl extends UnknownFieldAware {
    public Integer schemaVersion;
    public String code;
    public String slug;
    public Map<String, String> name;
    public String region;
    public List<String> languages;
    public String currency;
    public List<String> relatedCountries;
}
