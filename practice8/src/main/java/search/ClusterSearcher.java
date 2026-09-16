package search;

import cluster.Cluster;
import index.InvertedIndex;
import model.Zone;
import utils.tokenizer.Tokenizer;
import vector.DocumentVector;
import vector.VectorMath;

import java.util.*;

public class ClusterSearcher implements SearchEngine {
    private final InvertedIndex index;
    private final Tokenizer tokenizer;
    private final List<Cluster> clusters;
    private final Map<Zone, Double> zoneWeights;

    public ClusterSearcher(InvertedIndex index, Tokenizer tokenizer, List<Cluster> clusters, Map<Zone, Double> zoneWeights) {
        this.index = index;
        this.tokenizer = tokenizer;
        this.clusters = clusters;
        this.zoneWeights = zoneWeights;
    }

    public List<SearchResult> search(String query, int limit, int b2_leadersToSearch) {
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

        PriorityQueue<ClusterScore> leaderQueue = new PriorityQueue<>(
                Comparator.comparingDouble(cs -> -cs.score)
        );

        for (Cluster cluster : clusters) {
            double sim = VectorMath.weightedQuerySimilarity(queryWeights, queryNorm, cluster.getLeader(), zoneWeights);
            leaderQueue.add(new ClusterScore(cluster, sim));
        }

        int actualB2 = b2_leadersToSearch;

        if (!leaderQueue.isEmpty() && leaderQueue.peek().score == 0.0) {
            System.out.println("Query didn't match any leader so check all documents");
            actualB2 = clusters.size();
        }

        Set<DocumentVector> candidates = new HashSet<>();
        for (int i = 0; i < actualB2 && !leaderQueue.isEmpty(); i++) {
            Cluster bestCluster = leaderQueue.poll().cluster;
            candidates.addAll(bestCluster.getAllDocuments());
        }

        List<SearchResult> finalResults = new ArrayList<>();
        for (DocumentVector candidate : candidates) {
            double sim = VectorMath.weightedQuerySimilarity(queryWeights, queryNorm, candidate, zoneWeights);
            if (sim > 0) {
                String docName = index.getDocument(candidate.getDocId()).getFileName();
                finalResults.add(new SearchResult(candidate.getDocId(), docName, sim));
            }
        }

        Collections.sort(finalResults);
        return finalResults.subList(0, Math.min(finalResults.size(), limit));
    }

    private static class ClusterScore {
        Cluster cluster;
        double score;
        ClusterScore(Cluster cluster, double score) {
            this.cluster = cluster;
            this.score = score;
        }
    }
}