package mapreduce;

import utils.AppConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalDictionary {
    private static final GlobalDictionary INSTANCE = new GlobalDictionary();
    private final ConcurrentHashMap<String, Integer> termToIdMap;
    private final AtomicInteger termIdCounter;

    private GlobalDictionary() {
        this.termToIdMap = new ConcurrentHashMap<>(AppConfig.INITIAL_DICTIONARY_SIZE);
        this.termIdCounter = new AtomicInteger(0);
    }

    public static GlobalDictionary getInstance() { return INSTANCE; }

    public int getOrAdd(String term) {
        return termToIdMap.computeIfAbsent(term, k -> termIdCounter.getAndIncrement());
    }

    public void saveToDisk() {
        File file = new File(AppConfig.OUTPUT_DIR, "term_ids_mapping.txt");

        System.out.println("Sorting dictionary for readable output...");

        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(termToIdMap.entrySet());

        sortedList.sort(Map.Entry.comparingByKey());

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, Integer> entry : sortedList) {
                bw.write(entry.getValue() + "   " + entry.getKey());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Term dictionary saved.");
    }

    public int getVocabularySize() {
        return termIdCounter.get();
    }

}