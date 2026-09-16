import cluster.Cluster;
import cluster.ClusterManager;
import index.Indexer;
import index.InvertedIndex;
import model.Zone;
import parser.Fb2Parser;
import search.ClusterSearcher;
import search.SearchResult;
import utils.tokenizer.SimpleTokenizer;
import utils.tokenizer.Tokenizer;
import vector.DocumentVector;
import vector.VectorSpaceBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Tokenizer tokenizer = new SimpleTokenizer();
        InvertedIndex index = new InvertedIndex();
        Fb2Parser fb2Parser = new Fb2Parser();
        Indexer indexer = new Indexer(index, tokenizer, fb2Parser);

        File dir = new File("books");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".fb2"));
            if (files != null && files.length > 0) {
                String[] filePaths = new String[files.length];
                for (int i = 0; i < files.length; i++) filePaths[i] = files[i].getPath();
                indexer.indexFiles(filePaths);
            } else {
                System.out.println("No files.fb2 found");
                return;
            }
        } else {
            System.out.println("Folder is not found");
            return;
        }

        System.out.println("Create vectors (TF-IDF)");
        Map<Integer, DocumentVector> documentVectors = VectorSpaceBuilder.buildVectors(index);

        Map<Zone, Double> weights = new HashMap<>();
        weights.put(Zone.TITLE, 0.5);
        weights.put(Zone.AUTHOR, 0.3);
        weights.put(Zone.BODY, 0.2);

        System.out.println("Create Clusters");
        List<Cluster> clusters = ClusterManager.buildClusters(documentVectors, weights);

        System.out.println("Clusters created: " + clusters.size());

        ClusterSearcher searcher = new ClusterSearcher(index, tokenizer, clusters, weights);
        int b2 = 2;

        printQuery(searcher, "People", b2);

        printQuery(searcher, "Gump", b2);
    }
    private static void printQuery(ClusterSearcher searcher, String query, int b2) {
        System.out.println("\nQuery: '" + query + "'");
        List<SearchResult> results = searcher.search(query, 10, b2);
        for (SearchResult res : results) {
            System.out.println(res);
        }
        System.out.println();
    }
}