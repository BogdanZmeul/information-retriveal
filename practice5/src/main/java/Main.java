import mapreduce.MapperTask;
import mapreduce.ReducerTask;
import mapreduce.GlobalDictionary;
import utils.AppConfig;
import utils.SimpleLogger;
import utils.tokenizer.SmartStemmerTokenizer;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        AppConfig.initDirectories();
        SimpleLogger.init();

        SimpleLogger.log("SYSTEM STARTED with " + AppConfig.NUM_THREADS + " threads.");

        File inputDir = new File(AppConfig.INPUT_DIR);
        File[] inputFiles = inputDir.listFiles((dir, name) -> name.endsWith(".txt") && name.startsWith("wiki_chunk_"));

        if (inputFiles == null || inputFiles.length == 0) {
            System.out.println("Error: No 'wiki_chunk_*.txt' files in " + AppConfig.INPUT_DIR);
            SimpleLogger.close();
            return;
        }

        long totalInputBytes = 0;
        for (File f : inputFiles) {
            totalInputBytes += f.length();
        }
        double totalInputGB = totalInputBytes / (1024.0 * 1024.0 * 1024.0);

        long globalStart = System.currentTimeMillis();

        long mapStart = System.currentTimeMillis();
        System.out.println("Starting MAP Phase...");
        SimpleLogger.log("PHASE: MAP STARTED. Found " + inputFiles.length + " files.");

        ExecutorService mapExecutor = Executors.newFixedThreadPool(AppConfig.NUM_THREADS);

        Pattern numberPattern = Pattern.compile("wiki_chunk_(\\d+)\\.txt");

        for (File file : inputFiles) {
            Matcher matcher = numberPattern.matcher(file.getName());
            int fileIndex = -1;

            if (matcher.find()) {
                fileIndex = Integer.parseInt(matcher.group(1));
            } else {
                System.err.println("Skipping file with wrong format: " + file.getName());
                continue;
            }

            mapExecutor.submit(new MapperTask(file, fileIndex, new SmartStemmerTokenizer()));
        }

        mapExecutor.shutdown();
        try {
            mapExecutor.awaitTermination(10, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long mapEnd = System.currentTimeMillis();
        long mapDuration = mapEnd - mapStart;
        SimpleLogger.log(String.format("PHASE: MAP FINISHED in %d min %d sec (%d ms).", mapDuration / 60000, (mapDuration / 1000) % 60, mapDuration));

        GlobalDictionary.getInstance().saveToDisk();

        long reduceStart = System.currentTimeMillis();
        System.out.println("Starting REDUCE Phase...");
        SimpleLogger.log("PHASE: REDUCE STARTED with " + AppConfig.NUM_PARTITIONS + " partitions.");

        ExecutorService reduceExecutor = Executors.newFixedThreadPool(AppConfig.NUM_THREADS);

        for (int i = 0; i < AppConfig.NUM_PARTITIONS; i++) {
            reduceExecutor.submit(new ReducerTask(i));
        }

        reduceExecutor.shutdown();
        try {
            reduceExecutor.awaitTermination(10, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long reduceEnd = System.currentTimeMillis();
        long reduceDuration = reduceEnd - reduceStart;
        SimpleLogger.log(String.format("PHASE: REDUCE FINISHED in %d min %d sec (%d ms).", reduceDuration / 60000, (reduceDuration / 1000) % 60, reduceDuration));

        long globalEnd = System.currentTimeMillis();
        long totalDuration = globalEnd - globalStart;

        long totalOutputBytes = 0;
        File outputDir = new File(AppConfig.OUTPUT_DIR);
        if (outputDir.exists()) {
            File[] outFiles = outputDir.listFiles();
            if (outFiles != null) {
                for (File f : outFiles) totalOutputBytes += f.length();
            }
        }
        double totalOutputGB = totalOutputBytes / (1024.0 * 1024.0 * 1024.0);
        int vocabSize = GlobalDictionary.getInstance().getVocabularySize();

        StringBuilder report = new StringBuilder();
        report.append(String.format("Input Data:       %d files (%.2f GB)%n", inputFiles.length, totalInputGB));
        report.append(String.format("Vocabulary Size:  %,d terms%n", vocabSize));
        report.append(String.format("Index Size:       %.2f GB%n", totalOutputGB));
        report.append(String.format("MAP Time:         %d min %d sec%n", mapDuration / 60000, (mapDuration / 1000) % 60));
        report.append(String.format("REDUCE Time:      %d min %d sec%n", reduceDuration / 60000, (reduceDuration / 1000) % 60));
        report.append(String.format("TOTAL Time:       %d min %d sec%n", totalDuration / 60000, (totalDuration / 1000) % 60));

        System.out.println(report);
        SimpleLogger.log(report.toString());

        SimpleLogger.close();
    }
}