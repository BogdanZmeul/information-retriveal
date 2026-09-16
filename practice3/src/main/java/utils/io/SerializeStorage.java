package utils.io;

import java.io.*;

public class SerializeStorage implements Storage {
    @Override
    public void save(Object obj, String fileName) throws IOException {
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(obj);
        }
    }

    @Override
    public <T> T load(String fileName, Class<T> clazz) throws IOException {
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            return (T) ois.readObject();
        } catch(ClassNotFoundException e) {
            throw new IOException("Not found class for serialize " + e);
        }
    }
}
