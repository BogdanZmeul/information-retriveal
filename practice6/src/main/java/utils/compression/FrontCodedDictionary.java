package utils.compression;

import utils.AppConfig;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FrontCodedDictionary {

    private static final int BLOCK_SIZE = AppConfig.FRONT_CODING_BLOCK_SIZE;

    public static class DictionaryEntry {
        public String term;
        public long offset;
        public int length;

        public DictionaryEntry(String term, long offset, int length) {
            this.term = term;
            this.offset = offset;
            this.length = length;
        }
    }

    public static void saveDictionary(File file, List<String> terms, List<Long> offsets, List<Integer> lengths) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            for (int i = 0; i < terms.size(); i += BLOCK_SIZE) {
                int currentBlockSize = Math.min(BLOCK_SIZE, terms.size() - i);
                dos.writeInt(currentBlockSize);

                String firstWord = terms.get(i);
                byte[] firstBytes = firstWord.getBytes(StandardCharsets.UTF_8);
                dos.writeByte(firstBytes.length);
                dos.write(firstBytes);
                writeVLong(dos, offsets.get(i));
                writeVInt(dos, lengths.get(i));

                for (int j = 1; j < currentBlockSize; j++) {
                    int idx = i + j;
                    String prev = terms.get(idx - 1);
                    String curr = terms.get(idx);

                    int prefixLen = 0;
                    int minLen = Math.min(prev.length(), curr.length());
                    while (prefixLen < minLen && prev.charAt(prefixLen) == curr.charAt(prefixLen)) {
                        prefixLen++;
                    }

                    String suffix = curr.substring(prefixLen);
                    byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);

                    dos.writeByte(prefixLen);
                    dos.writeByte(suffixBytes.length);
                    dos.write(suffixBytes);

                    writeVLong(dos, offsets.get(idx));
                    writeVInt(dos, lengths.get(idx));
                }
            }
        }
    }

    public static List<DictionaryEntry> readDictionaryWithMetadata(File file) throws IOException {
        List<DictionaryEntry> entries = new ArrayList<>();

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while (dis.available() > 0) {
                int blockSize = dis.readInt();

                int firstLen = dis.readUnsignedByte();
                byte[] firstBytes = new byte[firstLen];
                dis.readFully(firstBytes);
                String currentWord = new String(firstBytes, StandardCharsets.UTF_8);

                long offset = readVLong(dis);
                int length = readVInt(dis);

                entries.add(new DictionaryEntry(currentWord, offset, length));

                for (int j = 1; j < blockSize; j++) {
                    int prefixLen = dis.readUnsignedByte();
                    int suffixLen = dis.readUnsignedByte();

                    byte[] suffixBytes = new byte[suffixLen];
                    dis.readFully(suffixBytes);
                    String suffix = new String(suffixBytes, StandardCharsets.UTF_8);

                    currentWord = currentWord.substring(0, prefixLen) + suffix;

                    long currOffset = readVLong(dis);
                    int currLength = readVInt(dis);

                    entries.add(new DictionaryEntry(currentWord, currOffset, currLength));
                }
            }
        }
        return entries;
    }

    private static void writeVLong(DataOutputStream out, long value) throws IOException {
        while ((value & ~0x7FL) != 0) {
            out.writeByte((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.writeByte((int) value);
    }

    private static void writeVInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static long readVLong(DataInputStream in) throws IOException {
        long value = 0;
        int shift = 0;
        while (true) {
            byte b = in.readByte();
            value |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return value;
    }

    private static int readVInt(DataInputStream in) throws IOException {
        int value = 0;
        int shift = 0;
        while (true) {
            byte b = in.readByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return value;
    }
}