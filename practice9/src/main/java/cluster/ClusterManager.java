package cluster;

import model.Zone;
import vector.DocumentVector;
import vector.VectorMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ClusterManager {

    public static List<Cluster> buildClusters(Map<Integer, DocumentVector> allDocuments, Map<Zone, Double> zoneWeights) {
        int N = allDocuments.size();
        if (N == 0) return new ArrayList<>();

        int numLeaders = (int) Math.max(1, Math.sqrt(N));
        System.out.println("Building clusters. Total docs: " + N + ", Leaders: " + numLeaders);

        List<DocumentVector> docsList = new ArrayList<>(allDocuments.values());

        Collections.shuffle(docsList, new Random(42));

        List<Cluster> clusters = new ArrayList<>();

        for (int i = 0; i < numLeaders; i++) {
            clusters.add(new Cluster(docsList.get(i)));
        }

        for (int i = numLeaders; i < N; i++) {
            DocumentVector follower = docsList.get(i);
            Cluster bestCluster = null;
            double maxSimilarity = -1.0;

            for (Cluster cluster : clusters) {
                double similarity = VectorMath.weightedDocumentSimilarity(follower, cluster.getLeader(), zoneWeights);
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    bestCluster = cluster;
                }
            }

            if (bestCluster != null) {
                bestCluster.addFollower(follower);
            }
        }

        return clusters;
    }
}