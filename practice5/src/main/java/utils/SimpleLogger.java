package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimpleLogger {
    private static BufferedWriter writer;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void init() {
        try {
            File logFile = new File("processing.log");
            writer = new BufferedWriter(new FileWriter(logFile, false));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void log(String message) {
        if (writer == null) return;
        try {
            String timestamp = dtf.format(LocalDateTime.now());
            writer.write(String.format("%s", message));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}