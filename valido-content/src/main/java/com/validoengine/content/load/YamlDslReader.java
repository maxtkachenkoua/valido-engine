package com.validoengine.content.load;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class YamlDslReader {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper mapper;

    public YamlDslReader() {
        this(new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    public YamlDslReader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public <T> T read(Path path, Class<T> type) throws IOException {
        return mapper.readValue(path.toFile(), type);
    }

    public Map<String, Object> readMap(Path path) throws IOException {
        return mapper.readValue(path.toFile(), MAP_TYPE);
    }

    public ObjectMapper mapper() {
        return mapper;
    }
}
