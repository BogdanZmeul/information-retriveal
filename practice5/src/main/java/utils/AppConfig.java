package utils;

import java.io.File;

public class AppConfig {
    public static final String INPUT_DIR = "E:\\\\Homeworks_Academy\\\\2 year\\\\2 term\\\\Information Retriveal\\\\wiki-data";
    public static final String TEMP_DIR = "data/temp";
    public static final String OUTPUT_DIR = "data/output";

    public static final int NUM_THREADS = 8;
    public static final int NUM_PARTITIONS = 16;
    public static final long MEMORY_BUFFER_LIMIT_BYTES = 256L * 1024 * 1024;
    public static final int SEGMENT_READER_BUFFER_BYTES = 64 * 1024;
    public static final int INITIAL_DICTIONARY_SIZE = 8_700_000;

    public static final int LINE_BITS = 20;
    public static final int FILE_BITS = 14;
    public static final int TERM_BITS = 30;

    public static final int FILE_SHIFT = LINE_BITS;
    public static final int TERM_SHIFT = LINE_BITS + FILE_BITS;
    public static final long LINE_MASK = (1L << LINE_BITS) - 1;
    public static final long DOC_ID_MASK = (1L << (FILE_BITS + LINE_BITS)) - 1;

    public static void initDirectories() {
        new File(TEMP_DIR).mkdirs();
        new File(OUTPUT_DIR).mkdirs();
    }
}