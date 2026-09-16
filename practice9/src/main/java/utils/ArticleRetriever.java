package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ArticleRetriever {

    public static String getArticleText(long globalDocID) {

        int fileIndex = (int) (globalDocID >>> AppConfig.FILE_SHIFT);
        int lineIndex = (int) (globalDocID & AppConfig.LINE_MASK);

        String fileName = "wiki_chunk_" + fileIndex + ".txt";
        File file = new File(AppConfig.INPUT_DIR, fileName);

        if (!file.exists()) {
            return "Error: File " + fileName + " not found.";
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int currentLine = 0;

            while ((line = br.readLine()) != null) {
                if (currentLine == lineIndex) {
                    if (line.startsWith("BEGIN{") && line.endsWith("}END")) {
                        return line.substring(6, line.length() - 4);
                    }
                    return line;
                }
                currentLine++;
            }
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }

        return "Error: Article not found (Line " + lineIndex + " outside of file).";
    }

    public static void main(String[] args) {
        System.out.println(ArticleRetriever.getArticleText(3));
    }

}