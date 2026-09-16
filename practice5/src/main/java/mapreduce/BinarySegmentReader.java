package mapreduce;

import utils.AppConfig;
import java.io.*;

public class BinarySegmentReader implements Comparable<BinarySegmentReader>, AutoCloseable {
    private final DataInputStream in;

    public int currentTermID;
    public long currentDocID;

    public BinarySegmentReader(File file) throws IOException {
        this.in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), AppConfig.SEGMENT_READER_BUFFER_BYTES));
    }

    public boolean reload() throws IOException {
        try {
            currentTermID = in.readInt();
            currentDocID = in.readLong();
            return true;
        } catch (EOFException e) {
            return false;
        }
    }

    @Override
    public int compareTo(BinarySegmentReader other) {
        if (this.currentTermID != other.currentTermID) {
            return Integer.compare(this.currentTermID, other.currentTermID);
        }
        return Long.compare(this.currentDocID, other.currentDocID);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}