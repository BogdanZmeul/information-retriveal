package vector;

import model.Zone;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import java.io.Serializable;

public class DocumentVector implements Serializable {
    private final int docId;
    private final Map<Zone, Map<String, Double>> zoneWeights;
    private final Map<Zone, Double> zoneNorms;

    public DocumentVector(int docId) {
        this.docId = docId;
        this.zoneWeights = new EnumMap<>(Zone.class);
        this.zoneNorms = new EnumMap<>(Zone.class);

        for (Zone z : Zone.values()) {
            zoneWeights.put(z, new HashMap<>());
            zoneNorms.put(z, -1.0);
        }
    }

    public void addWeight(Zone zone, String term, double weight) {
        zoneWeights.get(zone).put(term, weight);
    }

    public Map<String, Double> getWeightsForZone(Zone zone) {
        return zoneWeights.get(zone);
    }

    public int getDocId() {
        return docId;
    }

    public double getNormForZone(Zone zone) {
        if (zoneNorms.get(zone) < 0) {
            double sum = 0.0;
            for (double weight : zoneWeights.get(zone).values()) {
                sum += weight * weight;
            }
            zoneNorms.put(zone, Math.sqrt(sum));
        }
        return zoneNorms.get(zone);
    }
}