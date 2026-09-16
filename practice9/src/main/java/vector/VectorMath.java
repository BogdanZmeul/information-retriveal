package vector;

import model.Zone;
import java.util.Map;

public class VectorMath {

    public static double cosineSimilarity(Map<String, Double> w1, double norm1, Map<String, Double> w2, double norm2) {
        if (norm1 == 0 || norm2 == 0) return 0.0;

        double dotProduct = 0.0;
        if (w1.size() > w2.size()) {
            Map<String, Double> temp = w1;
            w1 = w2;
            w2 = temp;
        }

        for (Map.Entry<String, Double> entry : w1.entrySet()) {
            String term = entry.getKey();
            if (w2.containsKey(term)) {
                dotProduct += entry.getValue() * w2.get(term);
            }
        }

        return dotProduct / (norm1 * norm2);
    }

    public static double weightedDocumentSimilarity(DocumentVector v1, DocumentVector v2, Map<Zone, Double> zoneWeights) {
        double finalScore = 0.0;
        for (Zone zone : Zone.values()) {
            double weight = zoneWeights.getOrDefault(zone, 0.0);
            if (weight > 0) {
                double sim = cosineSimilarity(
                        v1.getWeightsForZone(zone), v1.getNormForZone(zone),
                        v2.getWeightsForZone(zone), v2.getNormForZone(zone)
                );
                finalScore += weight * sim;
            }
        }
        return finalScore;
    }

    public static double weightedQuerySimilarity(Map<String, Double> queryWeights, double queryNorm, DocumentVector doc, Map<Zone, Double> zoneWeights) {
        double finalScore = 0.0;
        for (Zone zone : Zone.values()) {
            double weight = zoneWeights.getOrDefault(zone, 0.0);
            if (weight > 0) {
                double sim = cosineSimilarity(
                        queryWeights, queryNorm,
                        doc.getWeightsForZone(zone), doc.getNormForZone(zone)
                );
                finalScore += weight * sim;
            }
        }
        return finalScore;
    }
}