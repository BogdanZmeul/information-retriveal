package utils.compression;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VByteCompressor {

    public static byte[] encode(List<Long> docIds) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long previousId = 0;

        for (long currentId : docIds) {
            long delta = currentId - previousId;
            writeVByte(out, delta);
            previousId = currentId;
        }
        return out.toByteArray();
    }

    public static List<Long> decode(byte[] data) {
        List<Long> docIds = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        long previousId = 0;

        while (buffer.hasRemaining()) {
            long delta = readVByte(buffer);
            long currentId = previousId + delta;
            docIds.add(currentId);
            previousId = currentId;
        }
        return docIds;
    }

    private static void writeVByte(ByteArrayOutputStream out, long value) {
        while (true) {
            byte b = (byte) (value & 0x7F);
            value >>>= 7;

            if (value == 0) {
                b |= 0x80;
                out.write(b);
                return;
            }
            out.write(b);
        }
    }

    private static long readVByte(ByteBuffer buffer) {
        long value = 0;
        int shift = 0;
        while (true) {
            byte b = buffer.get();
            long data = b & 0x7F;
            value |= (data << shift);

            if ((b & 0x80) != 0) {
                return value;
            }
            shift += 7;
        }
    }
}