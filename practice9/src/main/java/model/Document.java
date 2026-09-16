package model;

import java.io.Serializable;

public class Document implements Serializable {
    private final int id;
    private final String filePath;
    private final String fileName;

    public Document(int id, String filePath, String fileName) {
        this.id = id;
        this.filePath = filePath;
        this.fileName = fileName;
    }

    public String getFileName() { return fileName; }
}