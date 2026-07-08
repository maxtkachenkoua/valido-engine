package com.validoengine.content.dsl;

import java.util.List;
import java.util.Map;

public class SiteDsl extends UnknownFieldAware {
    public Integer schemaVersion;
    public String id;
    public Map<String, String> name;
    public String baseUrl;
    public String defaultLocale;
    public List<String> locales;
    public String mode;
    public List<String> modules;
    public OutputDsl output;
    public RoutingDsl routing;
    public GenerationDsl generation;
    public SeoDsl seo;

    public static class OutputDsl extends UnknownFieldAware {
        public String directory;
    }

    public static class RoutingDsl extends UnknownFieldAware {
        public String defaultToolPath;
        public Boolean trailingSlash;
    }

    public static class GenerationDsl extends UnknownFieldAware {
        public Boolean includeDrafts;
    }
}
