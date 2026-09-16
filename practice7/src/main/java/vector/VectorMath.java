package vector;

import java.util.Map;

public class VectorMath {
    public static double cosineSimilarity(Map<String, Double> queryWeights, double queryNorm,
                                          Map<String, Double> docWeights, double docNorm) {
        if (queryNorm == 0.0 || docNorm == 0.0) return 0.0;

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : queryWeights.entrySet()) {
            String term = entry.getKey();
            if (docWeights.containsKey(term)) {
                dotProduct += entry.getValue() * docWeights.get(term);
            }
        }

        return dotProduct / (queryNorm * docNorm);
    }
}