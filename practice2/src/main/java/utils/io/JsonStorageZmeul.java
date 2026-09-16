package utils.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class JsonStorageZmeul implements StorageZmeul {
    private final ObjectMapper mapper;

    public JsonStorageZmeul() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void save(Object obj, String fileName) throws IOException {
        mapper.writeValue(new File(fileName), obj);
    }

    @Override
    public <T> T load(String fileName, Class<T> clazz) throws IOException {
        return mapper.readValue(new File(fileName), clazz);
    }
}
