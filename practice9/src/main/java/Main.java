import cluster.Cluster;
import cluster.ClusterManager;
import dictionary.booleanDictionary.InvertedIndex;
import dictionary.booleanDictionary.MatrixDictionary;
import dictionary.wildcardDictionary.DoubleTreeDictionary;
import dictionary.wildcardDictionary.KGramIndex;
import dictionary.wildcardDictionary.RotatedTermIndex;

import mapreduce.GlobalDictionary;
import mapreduce.MapperTask;
import mapreduce.ReducerTask;
import model.Zone;
import parser.CacmCollectionParser;
import search.BooleanSearch;
import search.ClusterSearcher;
import search.SearchResult;
import search.WildcardSearch;
import utils.AppConfig;
import utils.Evaluator;
import utils.SimpleLogger;
import utils.tokenizer.StemmerTokenizer;
import utils.tokenizer.SmartStemmerTokenizer;
import utils.tokenizer.Tokenizer;
import vector.DocumentVector;
import vector.VectorSpaceBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        SimpleLogger.init();
        SimpleLogger.log("CACM Information Retrieval System Testing");

        File cacmFile = new File("cacm/cacm.all");
        File queriesFile = new File("cacm/query.text");
        File qrelsFile = new File("cacm/qrels.text");
        File commonWordsFile = new File("cacm/common_words");

        SimpleLogger.log("Parsing CACM files...");
        List<CacmCollectionParser.CacmDocument> documents = CacmCollectionParser.parse(cacmFile);
        Map<Integer, String> queries = CacmCollectionParser.parseQueries(queriesFile);
        Map<Integer, Set<Integer>> qrels = CacmCollectionParser.parseQrels(qrelsFile);
        Set<String> stopWords = CacmCollectionParser.parseCommonWords(commonWordsFile);

        Tokenizer simpleTokenizer = new StemmerTokenizer();

        for (Map.Entry<Integer, String> entry : queries.entrySet()) {
            StringBuilder filteredQuery = new StringBuilder();
            for (String word : simpleTokenizer.tokenize(entry.getValue())) {
                if (!stopWords.contains(word.toLowerCase())) {
                    filteredQuery.append(word).append(" ");
                }
            }
            entry.setValue(filteredQuery.toString().trim());
        }

        int totalDocs = documents.size();
        long totalWords = 0;
        Set<String> uniqueWords = new HashSet<>();

        for (CacmCollectionParser.CacmDocument doc : documents) {
            for (StringBuilder sb : doc.zones.values()) {
                List<String> tokens = simpleTokenizer.tokenize(sb.toString());
                totalWords += tokens.size();
                uniqueWords.addAll(tokens);
            }
        }

        SimpleLogger.log(String.format("Collection Size: %d documents", totalDocs));
        SimpleLogger.log(String.format("Total words: %d", totalWords));
        SimpleLogger.log(String.format("Total unique words (vocabulary): %d", uniqueWords.size()));
        System.out.println("Collection stats logged.");

        testBooleanInvertedIndex(documents, queries, qrels, totalDocs);
        testMatrixDictionary(documents, queries, qrels, totalDocs);
        testDoubleTreeDictionary(documents, queries, qrels, totalDocs);
        testKGramIndex(documents, queries, qrels, totalDocs);
        testRotatedTermIndex(documents, queries, qrels, totalDocs);
        testRankedSearcher(documents, queries, qrels, totalDocs);
        testClusterSearcher(documents, queries, qrels, totalDocs);
        testMapReduce(documents);

        SimpleLogger.close();
        System.out.println("All testing finished. Check processing.log");
    }

    private static void testBooleanInvertedIndex(List<CacmCollectionParser.CacmDocument> docs,
            Map<Integer, String> queries, Map<Integer, Set<Integer>> qrels, int totalDocs) {
        SimpleLogger.log("\nTesting Boolean InvertedIndex");
        InvertedIndex index = new InvertedIndex();
        Tokenizer tokenizer = new StemmerTokenizer();

        long startBuild = System.nanoTime();
        for (CacmCollectionParser.CacmDocument doc : docs) {
            int dictId = index.addDocument(String.valueOf(doc.id));
            for (StringBuilder sb : doc.zones.values()) {
                for (String word : tokenizer.tokenize(sb.toString())) {
                    index.addWord(word, dictId);
                }
            }
        }
        long buildTime = (System.nanoTime() - startBuild) / 1000000;
        SimpleLogger.log("Index built in " + buildTime + " ms");
        SimpleLogger.log("Approximate vocabulary size passed: " + uniqueVocabEstimate(docs));
        serializeIndex(index, "InvertedIndex");

        BooleanSearch<List<Integer>> searcher = new BooleanSearch<>(index, tokenizer);
        SimpleLogger.log("Mode: ALTERNATING");
        runBooleanSearchEval(searcher, queries, qrels, totalDocs, "ALTERNATING");
        SimpleLogger.log("Mode: AND");
        runBooleanSearchEval(searcher, queries, qrels, totalDocs, "AND");
        SimpleLogger.log("Mode: OR");
        runBooleanSearchEval(searcher, queries, qrels, totalDocs, "OR");
    }

    private static void testMatrixDictionary(List<CacmCollectionParser.CacmDocument> docs, Map<Integer, String> queries,
            Map<Integer, Set<Integer>> qrels, int totalDocs) {
        SimpleLogger.log("\nTesting MatrixDictionary");
        MatrixDictionary index = new MatrixDictionary();
        Tokenizer tokenizer = new StemmerTokenizer();

        long startBuild = System.nanoTime();
        for (CacmCollectionParser.CacmDocument doc : docs) {
            int dictId = index.addDocument(String.valueOf(doc.id));
            for (StringBuilder sb : doc.zones.values()) {
                for (String word : tokenizer.tokenize(sb.toString())) {
                    index.addWord(word, dictId);
                }
            }
        }
        long buildTime = (System.nanoTime() - startBuild) / 1000000;
        SimpleLogger.log("Index built in " + buildTime + " ms");
        serializeIndex(index, "MatrixDictionary");

        BooleanSearch<BitSet> searcher = new BooleanSearch<>(index, tokenizer);
        SimpleLogger.log("Mode: ALTERNATING");
        runBooleanSearchEval(searcher, queries, qrels, totalDocs, "ALTERNATING");
        SimpleLogger.log("Mode: AND");
        runBooleanSearchEval(searcher, queries, qrels, totalDocs, "AND");
        SimpleLogger.log("Mode: OR");
        runBooleanSearchEval(searcher, queries, qrels, totalDocs, "OR");
    }

    private static void testDoubleTreeDictionary(List<CacmCollectionParser.CacmDocument> docs,
            Map<Integer, String> queries, Map<Integer, Set<Integer>> qrels, int totalDocs) {
        SimpleLogger.log("\nTesting DoubleTreeDictionary");
        DoubleTreeDictionary index = new DoubleTreeDictionary();
        Tokenizer tokenizer = new StemmerTokenizer();

        long startBuild = System.nanoTime();
        for (CacmCollectionParser.CacmDocument doc : docs) {
            int dictId = index.addDocument(String.valueOf(doc.id));
            for (StringBuilder sb : doc.zones.values()) {
                for (String word : tokenizer.tokenize(sb.toString())) {
                    index.addWord(word, dictId);
                }
            }
        }
        long buildTime = (System.nanoTime() - startBuild) / 1000000;
        SimpleLogger.log("Index built in " + buildTime + " ms");
        serializeIndex(index, "DoubleTreeDictionary");

        WildcardSearch searcher = new WildcardSearch(index);
        SimpleLogger.log("Mode: ALTERNATING");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "ALTERNATING");
        SimpleLogger.log("Mode: AND");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "AND");
        SimpleLogger.log("Mode: OR");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "OR");
    }

    private static void testKGramIndex(List<CacmCollectionParser.CacmDocument> docs, Map<Integer, String> queries,
            Map<Integer, Set<Integer>> qrels, int totalDocs) {
        SimpleLogger.log("\nTesting KGramIndex");
        KGramIndex index = new KGramIndex();
        Tokenizer tokenizer = new StemmerTokenizer();

        long startBuild = System.nanoTime();
        for (CacmCollectionParser.CacmDocument doc : docs) {
            int dictId = index.addDocument(String.valueOf(doc.id));
            for (StringBuilder sb : doc.zones.values()) {
                for (String word : tokenizer.tokenize(sb.toString())) {
                    index.addWord(word, dictId);
                }
            }
        }
        long buildTime = (System.nanoTime() - startBuild) / 1000000;
        SimpleLogger.log("Index built in " + buildTime + " ms");
        serializeIndex(index, "KGramIndex");

        WildcardSearch searcher = new WildcardSearch(index);
        SimpleLogger.log("Mode: ALTERNATING");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "ALTERNATING");
        SimpleLogger.log("Mode: AND");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "AND");
        SimpleLogger.log("Mode: OR");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "OR");
    }

    private static void testRotatedTermIndex(List<CacmCollectionParser.CacmDocument> docs, Map<Integer, String> queries,
            Map<Integer, Set<Integer>> qrels, int totalDocs) {
        SimpleLogger.log("\nTesting RotatedTermIndex");
        RotatedTermIndex index = new RotatedTermIndex();
        Tokenizer tokenizer = new StemmerTokenizer();

        long startBuild = System.nanoTime();
        for (CacmCollectionParser.CacmDocument doc : docs) {
            int dictId = index.addDocument(String.valueOf(doc.id));
            for (StringBuilder sb : doc.zones.values()) {
                for (String word : tokenizer.tokenize(sb.toString())) {
                    index.addWord(word, dictId);
                }
            }
        }
        long buildTime = (System.nanoTime() - startBuild) / 1000000;
        SimpleLogger.log("Index built in " + buildTime + " ms");
        serializeIndex(index, "RotatedTermIndex");

        WildcardSearch searcher = new WildcardSearch(index);
        SimpleLogger.log("Mode: ALTERNATING");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "ALTERNATING");
        SimpleLogger.log("Mode: AND");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "AND");
        SimpleLogger.log("Mode: OR");
        runWildcardSearchEval(searcher, queries, qrels, totalDocs, "OR");
    }

    private static void testRankedSearcher(List<CacmCollectionParser.CacmDocument> docs, Map<Integer, String> queries,
            Map<Integer, Set<Integer>> qrels, int totalDocs) {
        SimpleLogger.log("\nTesting RankedSearcher (Without Clustering)");
        index.InvertedIndex index = new index.InvertedIndex();
        Tokenizer tokenizer = new StemmerTokenizer();

        long startBuild = System.nanoTime();
        for (CacmCollectionParser.CacmDocument doc : docs) {
            int docId = index.registerDocument(String.valueOf(doc.id), String.valueOf(doc.id));
            for (Map.Entry<Zone, StringBuilder> entry : doc.zones.entrySet()) {
                for (String word : tokenizer.tokenize(entry.getValue().toString())) {
                    index.addTerm(word, docId, entry.getKey());
                }
            }
        }
        Map<Integer, DocumentVector> documentVectors = VectorSpaceBuilder.buildVectors(index);
        Map<Zone, Double> weights = new HashMap<>();
        weights.put(Zone.TITLE, 0.9);
        weights.put(Zone.AUTHOR, 0.0);
        weights.put(Zone.BODY, 0.1);

        search.RankedSearcher searcher = new search.RankedSearcher(index, tokenizer, documentVectors, weights);
        long buildTime = (System.nanoTime() - startBuild) / 1000000;

        SimpleLogger.log("Ranked Index built in " + buildTime + " ms");
        serializeIndex(index, "Ranked_InvertedIndex");
        serializeIndex(documentVectors, "Ranked_DocumentVectors");

        Evaluator.RankedMetrics totalMetrics = new Evaluator.RankedMetrics();
        int count = 0;

        for (Map.Entry<Integer, String> entry : queries.entrySet()) {
            int queryId = entry.getKey();
            String queryText = entry.getValue();
            Set<Integer> gt = qrels.get(queryId);
            if (gt == null || gt.isEmpty())
                continue;

            long startQ = System.nanoTime();
            List<SearchResult> searchResults = searcher.search(queryText, 52);
            long qTime = System.nanoTime() - startQ;

            List<Integer> retIds = new ArrayList<>();
            for (SearchResult res : searchResults) {
                retIds.add(Integer.parseInt(res.getDocName()));
            }

            Evaluator.RankedMetrics m = Evaluator.evaluateRanked(gt, retIds);
            m.durationNs = qTime;
            totalMetrics.addTo(m);
            count++;
        }

        totalMetrics.divideBy(count);
        SimpleLogger.log("Metrics (Averaged over " + count + " queries):\n" + totalMetrics.toString());
    }

    private static void testClusterSearcher(List<CacmCollectionParser.CacmDocument> docs, Map<Integer, String> queries,
            Map<Integer, Set<Integer>> qrels, int totalDocs) {
        SimpleLogger.log("\nTesting ClusterSearcher (Ranked Retrieval)");
        index.InvertedIndex index = new index.InvertedIndex();
        Tokenizer tokenizer = new StemmerTokenizer();

        long startBuild = System.nanoTime();
        for (CacmCollectionParser.CacmDocument doc : docs) {
            int docId = index.registerDocument(String.valueOf(doc.id), String.valueOf(doc.id));
            for (Map.Entry<Zone, StringBuilder> entry : doc.zones.entrySet()) {
                for (String word : tokenizer.tokenize(entry.getValue().toString())) {
                    index.addTerm(word, docId, entry.getKey());
                }
            }
        }
        Map<Integer, DocumentVector> documentVectors = VectorSpaceBuilder.buildVectors(index);
        Map<Zone, Double> weights = new HashMap<>();
        weights.put(Zone.TITLE, 0.9);
        weights.put(Zone.AUTHOR, 0.0);
        weights.put(Zone.BODY, 0.1);

        List<Cluster> clusters = ClusterManager.buildClusters(documentVectors, weights);
        ClusterSearcher searcher = new ClusterSearcher(index, tokenizer, clusters, weights);
        long buildTime = (System.nanoTime() - startBuild) / 1000000;

        SimpleLogger.log("Cluster Index built in " + buildTime + " ms. Clusters created: " + clusters.size());
        serializeIndex(index, "Cluster_InvertedIndex");
        serializeIndex(documentVectors, "Cluster_DocumentVectors");
        serializeIndex(clusters, "Cluster_Clusters");

        Evaluator.RankedMetrics totalMetrics = new Evaluator.RankedMetrics();
        int b2 = Math.min(6, clusters.size());
        int count = 0;

        for (Map.Entry<Integer, String> entry : queries.entrySet()) {
            int queryId = entry.getKey();
            String queryText = entry.getValue();
            Set<Integer> gt = qrels.get(queryId);
            if (gt == null || gt.isEmpty())
                continue;

            long startQ = System.nanoTime();
            List<SearchResult> searchResults = searcher.search(queryText, 52, b2);
            long qTime = System.nanoTime() - startQ;

            List<Integer> retIds = new ArrayList<>();
            for (SearchResult res : searchResults) {
                retIds.add(Integer.parseInt(res.getDocName()));
            }

            Evaluator.RankedMetrics m = Evaluator.evaluateRanked(gt, retIds);
            m.durationNs = qTime;
            totalMetrics.addTo(m);
            count++;
        }

        totalMetrics.divideBy(count);
        SimpleLogger.log("Metrics (Averaged over " + count + " queries):\n" + totalMetrics.toString());
    }

    private static void testMapReduce(List<CacmCollectionParser.CacmDocument> docs) throws Exception {
        SimpleLogger.log("\nTesting MapReduce Indexing");
        AppConfig.initDirectories();
        SmartStemmerTokenizer mtTokenizer = new SmartStemmerTokenizer();

        File mrTempFile = new File("data/cacm_mr_input.txt");
        mrTempFile.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mrTempFile))) {
            for (CacmCollectionParser.CacmDocument doc : docs) {
                StringBuilder content = new StringBuilder();
                for (StringBuilder sb : doc.zones.values()) {
                    content.append(sb).append(" ");
                }
                bw.write("BEGIN{" + content.toString().replace("\n", " ").trim() + "}END\n");
            }
        }

        long startMR = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(AppConfig.NUM_THREADS);
        MapperTask mapper = new MapperTask(mrTempFile, 1, mtTokenizer);
        executor.submit(mapper);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        ExecutorService reduceExecutor = Executors.newFixedThreadPool(AppConfig.NUM_THREADS);
        for (int i = 0; i < AppConfig.NUM_PARTITIONS; i++) {
            reduceExecutor.submit(new ReducerTask(i));
        }
        reduceExecutor.shutdown();
        reduceExecutor.awaitTermination(5, TimeUnit.MINUTES);

        GlobalDictionary.getInstance().saveToDisk();

        SimpleLogger.log("Compressing generated index parts...");
        long startCompress = System.nanoTime();
        ExecutorService compExecutor = Executors.newFixedThreadPool(AppConfig.NUM_THREADS);
        for (int i = 0; i < AppConfig.NUM_PARTITIONS; i++) {
            final int partId = i;
            compExecutor.submit(() -> utils.compression.CompressionManager.compressPartition(partId));
        }
        compExecutor.shutdown();
        compExecutor.awaitTermination(5, TimeUnit.MINUTES);
        long compTime = (System.nanoTime() - startCompress) / 1000000;
        SimpleLogger.log("MapReduce Compression time: " + compTime + " ms");

        long totalSize = 0;
        File cDir = new File(AppConfig.COMPRESSED_DIR);
        if (cDir.exists() && cDir.isDirectory()) {
            for (File f : cDir.listFiles()) {
                totalSize += f.length();
            }
        }
        double mbSize = totalSize / (1024.0 * 1024.0);
        SimpleLogger.log(String.format(java.util.Locale.US, "Compressed Data Total Size: %.2f MB", mbSize));

        long buildTime = (System.nanoTime() - startMR) / 1000000;
        SimpleLogger.log("MapReduce Total Indexing (including compression) time: " + buildTime + " ms");
        mrTempFile.delete();
    }

    private static void runBooleanSearchEval(BooleanSearch<?> searcher, Map<Integer, String> queries,
            Map<Integer, Set<Integer>> qrels, int totalDocs, String operatorMode) {
        Evaluator.Metrics totalMetrics = new Evaluator.Metrics();
        int count = 0;
        Tokenizer tokenizer = new StemmerTokenizer();

        for (Map.Entry<Integer, String> entry : queries.entrySet()) {
            int queryId = entry.getKey();
            String queryText = entry.getValue();
            Set<Integer> gt = qrels.get(queryId);
            if (gt == null || gt.isEmpty())
                continue;

            List<String> rawTokens = tokenizer.tokenize(queryText);
            List<String> tokens = new ArrayList<>();
            for (String t : rawTokens) {
                String upper = t.toUpperCase();
                if (!upper.equals("AND") && !upper.equals("OR") && !upper.equals("NOT")) {
                    tokens.add(t);
                }
            }
            if (tokens.isEmpty())
                continue;

            StringBuilder boolQuery = new StringBuilder();
            boolQuery.append(tokens.get(0));
            boolean useAnd = "AND".equals(operatorMode);
            if ("ALTERNATING".equals(operatorMode)) useAnd = true;

            for (int i = 1; i < tokens.size(); i++) {
                if ("ALTERNATING".equals(operatorMode)) {
                    boolQuery.append(useAnd ? " AND " : " OR ");
                    useAnd = !useAnd;
                } else if ("AND".equals(operatorMode)) {
                    boolQuery.append(" AND ");
                } else {
                    boolQuery.append(" OR ");
                }
                boolQuery.append(tokens.get(i));
            }

            long startQ = System.nanoTime();
            List<String> searchResults;
            try {
                searchResults = searcher.search(boolQuery.toString());
            } catch (Exception e) {
                continue;
            }
            long qTime = System.nanoTime() - startQ;

            Set<Integer> retIds = new HashSet<>();
            for (String res : searchResults) {
                try {
                    retIds.add(Integer.parseInt(res));
                } catch (NumberFormatException ignored) {
                }
            }

            Evaluator.Metrics m = Evaluator.evaluate(gt, retIds, totalDocs);
            m.durationNs = qTime;
            totalMetrics.addTo(m);
            count++;
        }

        totalMetrics.divideBy(count);
        SimpleLogger.log("Metrics (Averaged over " + count + " queries): " + totalMetrics.toString());
    }

    private static void runWildcardSearchEval(WildcardSearch searcher, Map<Integer, String> queries,
            Map<Integer, Set<Integer>> qrels, int totalDocs, String operatorMode) {
        Evaluator.Metrics totalMetrics = new Evaluator.Metrics();
        int count = 0;
        Tokenizer tokenizer = new StemmerTokenizer();

        for (Map.Entry<Integer, String> entry : queries.entrySet()) {
            int queryId = entry.getKey();
            String queryText = entry.getValue();
            Set<Integer> gt = qrels.get(queryId);
            if (gt == null || gt.isEmpty())
                continue;

            List<String> tokens = tokenizer.tokenize(queryText);
            if (tokens.isEmpty())
                continue;

            long qTimeTotal = 0;
            Set<Integer> retIds = null;
            boolean useAnd = "AND".equals(operatorMode);
            if ("ALTERNATING".equals(operatorMode)) useAnd = true;

            for (String token : tokens) {
                if (token.length() >= 3 && !token.endsWith("*"))
                    token = token + "*";

                long startQ = System.nanoTime();
                List<String> searchResults = searcher.search(token);
                qTimeTotal += (System.nanoTime() - startQ);

                Set<Integer> currentIds = new HashSet<>();
                for (String res : searchResults) {
                    try {
                        currentIds.add(Integer.parseInt(res));
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (retIds == null) {
                    retIds = currentIds;
                } else {
                    if (useAnd) {
                        retIds.retainAll(currentIds);
                    } else {
                        retIds.addAll(currentIds);
                    }
                    if ("ALTERNATING".equals(operatorMode)) {
                        useAnd = !useAnd;
                    }
                }
            }

            if (retIds == null) {
                retIds = new HashSet<>();
            }

            Evaluator.Metrics m = Evaluator.evaluate(gt, retIds, totalDocs);
            m.durationNs = qTimeTotal;
            totalMetrics.addTo(m);
            count++;
        }

        totalMetrics.divideBy(count);
        SimpleLogger.log("Metrics (Averaged over " + count + " queries): " + totalMetrics.toString());
    }

    private static int uniqueVocabEstimate(List<CacmCollectionParser.CacmDocument> docs) {
        Set<String> set = new HashSet<>();
        Tokenizer t = new StemmerTokenizer();
        for (CacmCollectionParser.CacmDocument d : docs) {
            for (StringBuilder b : d.zones.values()) {
                set.addAll(t.tokenize(b.toString()));
            }
        }
        return set.size();
    }

    private static void serializeIndex(Object indexObj, String name) {
        File dir = new File("indexes");
        if (!dir.exists()) dir.mkdir();
        File file = new File(dir, name + ".ser");
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(file))) {
            oos.writeObject(indexObj);
            double mbSize = file.length() / (1024.0 * 1024.0);
            SimpleLogger.log(String.format(java.util.Locale.US, "Serialized %s size: %.2f MB", name, mbSize));
        } catch (Exception e) {
            SimpleLogger.log("Serialization failed for " + name + ": " + e.getMessage());
        }
    }
}