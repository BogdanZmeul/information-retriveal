package mapreduce;

import utils.AppConfig;
import utils.SimpleLogger;

import java.io.*;
import java.util.*;

public class ReducerTask implements Runnable {
    private final int partitionId;

    public ReducerTask(int partitionId) {
        this.partitionId = partitionId;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        SimpleLogger.log("REDUCE-START: Partition " + partitionId);

        File tempDir = new File(AppConfig.TEMP_DIR);
        File[] segmentFiles = tempDir.listFiles((dir, name) -> name.endsWith(".dat") && name.contains("_p" + partitionId + "_"));

        if (segmentFiles == null || segmentFiles.length == 0) return;

        PriorityQueue<BinarySegmentReader> pq = new PriorityQueue<>();
        List<BinarySegmentReader> openReaders = new ArrayList<>();
        File outputFile = new File(AppConfig.OUTPUT_DIR, "index_part_" + partitionId + ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            for (File file : segmentFiles) {
                try {
                    BinarySegmentReader sr = new BinarySegmentReader(file);
                    if (sr.reload()) {
                        pq.add(sr);
                        openReaders.add(sr);
                    } else {
                        sr.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            while (!pq.isEmpty()) {
                int currentTermID = pq.peek().currentTermID;

                StringBuilder docList = new StringBuilder();
                boolean first = true;

                long lastDocID = -1;

                while (!pq.isEmpty() && pq.peek().currentTermID == currentTermID) {
                    BinarySegmentReader sr = pq.poll();

                    if (sr.currentDocID != lastDocID) {
                        if (!first) docList.append(",");
                        docList.append(sr.currentDocID);

                        lastDocID = sr.currentDocID;
                        first = false;
                    }

                    if (sr.reload()) pq.add(sr);
                }

                writer.write(currentTermID + ":" + docList);
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            long reduceEnd = System.currentTimeMillis();
            long reduceDuration = reduceEnd - startTime;
            SimpleLogger.log(String.format("PHASE: REDUCE FINISHED in %d min %d sec (%d ms).", reduceDuration / 60000, (reduceDuration / 1000) % 60, reduceDuration));
            for (BinarySegmentReader sr : openReaders) {
                try {
                    sr.close();
                } catch (IOException e) {}
            }
        }
    }
}