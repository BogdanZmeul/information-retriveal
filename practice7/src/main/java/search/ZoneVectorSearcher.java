package search;

import index.InvertedIndex;
import model.Zone;
import utils.tokenizer.Tokenizer;
import vector.DocumentVector;
import vector.VectorMath;

import java.util.*;

public class ZoneVectorSearcher implements SearchEngine {
    private final InvertedIndex index;
    private final Tokenizer tokenizer;
    private final Map<Integer, DocumentVector> documentVectors;
    private final Map<Zone, Double> zoneWeights;

    public ZoneVectorSearcher(InvertedIndex index, Tokenizer tokenizer,
                              Map<Integer, DocumentVector> documentVectors,
                              Map<Zone, Double> zoneWeights) {
        this.index = index;
        this.tokenizer = tokenizer;
        this.documentVectors = documentVectors;
        this.zoneWeights = zoneWeights;
    }

    public List<SearchResult> search(String query, int limit) {
        List<String> queryTokens = tokenizer.tokenize(query);
        Map<String, Double> queryVector = new HashMap<>();

        Map<String, Integer> queryTf = new HashMap<>();
        for (String token : queryTokens) {
            queryTf.put(token, queryTf.getOrDefault(token, 0) + 1);
        }

        int N = index.getTotalDocuments();
        double queryNormSq = 0.0;

        for (Map.Entry<String, Integer> entry : queryTf.entrySet()) {
            String term = entry.getKey();
            int df = index.getDocumentFrequency(term);
            if (df > 0) {
                double idf = Math.log10((double) N / df) + 1.0;
                double tf = 1.0 + Math.log10(entry.getValue());
                double tfIdf = tf * idf;
                queryVector.put(term, tfIdf);
                queryNormSq += tfIdf * tfIdf;
            }
        }

        double queryNorm = Math.sqrt(queryNormSq);
        if (queryVector.isEmpty()) return Collections.emptyList();

        List<SearchResult> results = new ArrayList<>();

        for (DocumentVector docVec : documentVectors.values()) {
            double finalScore = 0.0;

            for (Zone zone : Zone.values()) {
                double zoneWeight = zoneWeights.getOrDefault(zone, 0.0);
                if (zoneWeight == 0.0) continue;

                double cosSim = VectorMath.cosineSimilarity(
                        queryVector, queryNorm,
                        docVec.getWeightsForZone(zone), docVec.getNormForZone(zone)
                );

                finalScore += zoneWeight * cosSim;
            }

            if (finalScore > 0) {
                String docName = index.getDocument(docVec.getDocId()).getFileName();
                results.add(new SearchResult(docVec.getDocId(), docName, finalScore));
            }
        }

        Collections.sort(results);
        return results.subList(0, Math.min(results.size(), limit));
    }
}