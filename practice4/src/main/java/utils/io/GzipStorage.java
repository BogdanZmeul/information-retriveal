package utils.io;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class GzipStorage implements Storage {
    @Override
    public void save(Object obj, String fileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new java.util.zip.GZIPOutputStream(new FileOutputStream(fileName)))) {
            oos.writeObject(obj);
        }
    }

    @Override
    public <T> T load(String fileName, Class<T> clazz) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(fileName)))){
            return (T) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Not found class for gzip " + e);
        }
    }

}
