package utils.compression;

import utils.AppConfig;
import utils.SimpleLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CompressionManager {

    public static void compressPartition(int partitionId) {
        File inputFile = new File(AppConfig.OUTPUT_DIR, "index_part_" + partitionId + ".txt");
        if (!inputFile.exists()) return;

        File outDictFile = new File(AppConfig.COMPRESSED_DIR, "part_" + partitionId + ".dic");
        File outPostingsFile = new File(AppConfig.COMPRESSED_DIR, "part_" + partitionId + ".idx");

        List<String> termsList = new ArrayList<>();
        List<Long> offsetsList = new ArrayList<>();
        List<Integer> docCountsList = new ArrayList<>();
        List<Long> tempPostings = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedOutputStream bosPostings = new BufferedOutputStream(new FileOutputStream(outPostingsFile))) {

            long currentOffset = 0;
            String line;
            while ((line = br.readLine()) != null) {
                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) continue;

                String termStr = line.substring(0, colonIndex);
                String postingsStr = line.substring(colonIndex + 1);

                tempPostings.clear();
                String[] ids = postingsStr.split(",");
                for (String id : ids) {
                    if (!id.isEmpty()) tempPostings.add(Long.parseLong(id));
                }

                byte[] compressedPostings = VByteCompressor.encode(tempPostings);
                bosPostings.write(compressedPostings);

                termsList.add(termStr);
                offsetsList.add(currentOffset);
                docCountsList.add(compressedPostings.length);

                currentOffset += compressedPostings.length;
            }
            bosPostings.flush();
            FrontCodedDictionary.saveDictionary(outDictFile, termsList, offsetsList, docCountsList);

        } catch (IOException e) {
            SimpleLogger.log("COMPRESSION ERROR (Part " + partitionId + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean verifyPartition(int partitionId) {
        File originalFile = new File(AppConfig.OUTPUT_DIR, "index_part_" + partitionId + ".txt");
        File dictFile = new File(AppConfig.COMPRESSED_DIR, "part_" + partitionId + ".dic");
        File postingsFile = new File(AppConfig.COMPRESSED_DIR, "part_" + partitionId + ".idx");

        if (!originalFile.exists()) return true;

        try {
            List<FrontCodedDictionary.DictionaryEntry> dictionary = FrontCodedDictionary.readDictionaryWithMetadata(dictFile);

            try (RandomAccessFile raf = new RandomAccessFile(postingsFile, "r");
                 BufferedReader brOriginal = new BufferedReader(new FileReader(originalFile))) {

                String line;
                int termIndex = 0;

                while ((line = brOriginal.readLine()) != null) {
                    int colonIndex = line.indexOf(':');
                    String origTerm = line.substring(0, colonIndex);
                    String origPostingsStr = line.substring(colonIndex + 1);

                    if (termIndex >= dictionary.size()) {
                        SimpleLogger.log("VERIFY FAIL: Original file has more terms than compressed dictionary.");
                        return false;
                    }

                    FrontCodedDictionary.DictionaryEntry entry = dictionary.get(termIndex);
                    if (!origTerm.equals(entry.term)) {
                        SimpleLogger.log("VERIFY FAIL: Term mismatch! Original: " + origTerm + ", Compressed: " + entry.term);
                        return false;
                    }

                    byte[] compressedData = new byte[entry.length];
                    raf.seek(entry.offset);
                    raf.readFully(compressedData);

                    List<Long> decompressedIds = VByteCompressor.decode(compressedData);

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < decompressedIds.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(decompressedIds.get(i));
                    }

                    if (!origPostingsStr.equals(sb.toString())) {
                        SimpleLogger.log("VERIFY FAIL: Postings mismatch for term: " + origTerm);
                        SimpleLogger.log("Original: " + (origPostingsStr.length() > 50 ? origPostingsStr.substring(0,50)+"..." : origPostingsStr));
                        SimpleLogger.log("Decoded:  " + (sb.length() > 50 ? sb.substring(0,50)+"..." : sb.toString()));
                        return false;
                    }

                    termIndex++;
                }
            }
            return true;

        } catch (Exception e) {
            SimpleLogger.log("VERIFY EXCEPTION (Part " + partitionId + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}