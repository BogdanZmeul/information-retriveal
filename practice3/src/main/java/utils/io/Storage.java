package utils.io;

import java.io.IOException;

public interface Storage {
    void save(Object obj, String fileName) throws IOException;
    <T> T load(String fileName, Class<T> clazz) throws IOException;
}
