package model;

import java.util.EnumMap;
import java.util.Map;

public class Posting {
    private final int documentId;
    private final Map<Zone, Integer> zoneTfs;

    public Posting(int documentId) {
        this.documentId = documentId;
        this.zoneTfs = new EnumMap<>(Zone.class);
    }

    public void addZoneOccurrence(Zone zone) {
        zoneTfs.put(zone, zoneTfs.getOrDefault(zone, 0) + 1);
    }

    public int getDocumentId() {
        return documentId;
    }

    public int getTfForZone(Zone zone) {
        return zoneTfs.getOrDefault(zone, 0);
    }
}