package mapreduce;

import utils.AppConfig;
import utils.SimpleLogger;
import utils.tokenizer.StreamTokenizer;

import java.io.*;
import java.util.Arrays;

public class MapperTask implements Runnable {
    private final File inputFile;
    private final int fileIndex;
    private final StreamTokenizer tokenizer;

    private final int[] bufferCounts;
    private static final int INITIAL_BUFFER_SIZE = 10000;
    private final long[][] primitiveBuffers;

    private int spillCounter = 0;
    private long currentBufferSize = 0;

    public MapperTask(File inputFile, int fileIndex, StreamTokenizer tokenizer) {
        this.inputFile = inputFile;
        this.fileIndex = fileIndex;
        this.tokenizer = tokenizer;

        this.bufferCounts = new int[AppConfig.NUM_PARTITIONS];
        this.primitiveBuffers = new long[AppConfig.NUM_PARTITIONS][];

        for (int i = 0; i < AppConfig.NUM_PARTITIONS; i++) {
            primitiveBuffers[i] = new long[INITIAL_BUFFER_SIZE];
        }
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        SimpleLogger.log("MAP-START: Processing file " + inputFile.getName() + " (ID: " + fileIndex + ")");
        GlobalDictionary dictionary = GlobalDictionary.getInstance();

        long fileIdPart = (long) fileIndex << AppConfig.FILE_SHIFT;

        int localLineCounter = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("BEGIN{") || !line.endsWith("}END")) {
                    continue;
                }

                String cleanText = line.substring(6, line.length() - 4);
                long currentDocID = fileIdPart | localLineCounter;

                localLineCounter++;

                tokenizer.tokenize(cleanText, (term) -> {
                    int termID = dictionary.getOrAdd(term);

                    if (termID < 0 || termID >= (1 << AppConfig.TERM_BITS)) {
                        return;
                    }

                    int partitionId = (termID & 0x7FFFFFFF) % AppConfig.NUM_PARTITIONS;

                    long packedEntry = ((long) termID << AppConfig.TERM_SHIFT) | currentDocID;

                    addToBuffer(partitionId, packedEntry);
                    currentBufferSize += 12;

                    if (currentBufferSize >= AppConfig.MEMORY_BUFFER_LIMIT_BYTES) {
                        flushToDisk();
                    }
                });
            }
            flushToDisk();
            long mapEnd = System.currentTimeMillis();
            long mapDuration = mapEnd - startTime;

            SimpleLogger.log(String.format("PHASE: MAP FINISHED in %d min %d sec (%d ms).", mapDuration / 60000, (mapDuration / 1000) % 60, mapDuration));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addToBuffer(int partitionId, long value) {
        int count = bufferCounts[partitionId];
        long[] buffer = primitiveBuffers[partitionId];

        if (count >= buffer.length) {
            long[] newBuffer = new long[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            primitiveBuffers[partitionId] = newBuffer;
            buffer = newBuffer;
        }

        buffer[count] = value;
        bufferCounts[partitionId] = count + 1;
    }

    private void flushToDisk() throws IOException {
        spillCounter++;
        for (int pId = 0; pId < AppConfig.NUM_PARTITIONS; pId++) {
            int count = bufferCounts[pId];
            if (count == 0) continue;

            long[] buffer = primitiveBuffers[pId];

            Arrays.sort(buffer, 0, count);

            String fileName = String.format("m%d_p%d_s%d.dat", fileIndex, pId, spillCounter);
            File outputFile = new File(AppConfig.TEMP_DIR, fileName);

            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
                for (int i = 0; i < count; i++) {
                    long packed = buffer[i];
                    int tId = (int) (packed >>> AppConfig.TERM_SHIFT);
                    long dId = packed & AppConfig.DOC_ID_MASK;

                    dos.writeInt(tId);
                    dos.writeLong(dId);
                }
            }
            bufferCounts[pId] = 0;
        }
        currentBufferSize = 0;
    }
}