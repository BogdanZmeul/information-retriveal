package search;

import index.InvertedIndex;
import model.Zone;
import utils.tokenizer.Tokenizer;
import vector.DocumentVector;
import vector.VectorMath;

import java.util.*;

public class RankedSearcher implements SearchEngine {
    private final InvertedIndex index;
    private final Tokenizer tokenizer;
    private final Map<Integer, DocumentVector> documentVectors;
    private final Map<Zone, Double> zoneWeights;

    public RankedSearcher(InvertedIndex index, Tokenizer tokenizer, Map<Integer, DocumentVector> documentVectors, Map<Zone, Double> zoneWeights) {
        this.index = index;
        this.tokenizer = tokenizer;
        this.documentVectors = documentVectors;
        this.zoneWeights = zoneWeights;
    }

    public List<SearchResult> search(String query, int limit) {
        List<String> queryTokens = tokenizer.tokenize(query);
        Map<String, Integer> queryTf = new HashMap<>();
        for (String token : queryTokens) {
            queryTf.put(token, queryTf.getOrDefault(token, 0) + 1);
        }

        Map<String, Double> queryWeights = new HashMap<>();
        double queryNormSq = 0.0;
        int N = index.getTotalDocuments();

        for (Map.Entry<String, Integer> entry : queryTf.entrySet()) {
            String term = entry.getKey();
            int df = index.getDocumentFrequency(term);

            if (df > 0) {
                double idf = Math.log10((double) N / df) + 1.0;
                double tf = 1.0 + Math.log10(entry.getValue());
                double weight = tf * idf;
                queryWeights.put(term, weight);
                queryNormSq += weight * weight;
            }
        }

        if (queryWeights.isEmpty()) {
            return Collections.emptyList();
        }

        double queryNorm = Math.sqrt(queryNormSq);
        List<SearchResult> finalResults = new ArrayList<>();

        for (DocumentVector candidate : documentVectors.values()) {
            double sim = VectorMath.weightedQuerySimilarity(queryWeights, queryNorm, candidate, zoneWeights);
            if (sim > 0) {
                String docName = index.getDocument(candidate.getDocId()).getFileName();
                finalResults.add(new SearchResult(candidate.getDocId(), docName, sim));
            }
        }

        Collections.sort(finalResults);
        return finalResults.subList(0, Math.min(finalResults.size(), limit));
    }
}
