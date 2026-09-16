package utils;

import dictionary.Dictionary;
import utils.tokenizer.Tokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class Indexer {
    public static void index(String[] filePaths, Dictionary dictionary, Tokenizer tokenizer) {
        for (String path : filePaths) {
            indexSingleFile(path, dictionary, tokenizer);
        }
    }

    private static void indexSingleFile(String filePath, Dictionary dictionary, Tokenizer tokenizer) {
        File f = new File(filePath);
        if (!f.exists()) {
            System.err.println("File not found: " + filePath);
            return;
        }
        int documentId = dictionary.addDocument(f.getName());

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<String> tokens = tokenizer.tokenize(line.trim());
                for (String word : tokens) {
                    dictionary.addWord(word.toLowerCase(), documentId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error indexing file " + filePath + ": " + e.getMessage());
        }
    }

    public static String[] generatePaths(String prefix, int count, String extension) {
        String[] paths = new String[count];
        for (int i = 0; i < count; i++) {
            paths[i] = prefix + (i + 1) + extension;
        }
        return paths;
    }

}
