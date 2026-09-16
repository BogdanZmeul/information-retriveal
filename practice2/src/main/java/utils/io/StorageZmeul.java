package utils.io;

import java.io.IOException;

public interface StorageZmeul {
    void save(Object obj, String fileName) throws IOException;
    <T> T load(String fileName, Class<T> clazz) throws IOException;
}
