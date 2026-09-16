import dictionary.booleanDictionary.BooleanDictionary;
import dictionary.wildcardDictionary.DoubleTreeDictionary;
import dictionary.wildcardDictionary.KGramIndex;
import dictionary.wildcardDictionary.RotatedTermIndex;
import dictionary.wildcardDictionary.WildcardDictionary;
import search.WildcardSearch;
import utils.Indexer;
import utils.io.GzipStorage;
import utils.tokenizer.SimpleTokenizer;
import utils.tokenizer.StemmerTokenizer;

import java.io.IOException;

public class Main {
    private static final StemmerTokenizer stemmerTokenizer = new StemmerTokenizer();
    private static final SimpleTokenizer tokenizer = new SimpleTokenizer();
    private static final WildcardDictionary tree = new DoubleTreeDictionary();
    private static final WildcardSearch treeSearch = new WildcardSearch(tree);
    private static final WildcardDictionary rotatedIndex = new RotatedTermIndex();
    private static final WildcardSearch rotatedIndexSearch = new WildcardSearch(rotatedIndex);
    private static final WildcardDictionary kGramIndex = new KGramIndex();
    private static final WildcardSearch kGramIndexSearch = new WildcardSearch(kGramIndex);

    public static void main(String[] args) {
        String[] collection = Indexer.generatePaths("input/test", 10, ".txt");
        Indexer.index(collection, tree, tokenizer);
        Indexer.index(collection, rotatedIndex, tokenizer);
        Indexer.index(collection, kGramIndex, tokenizer);

        System.out.println(treeSearch.search("hel*o"));
        System.out.println(rotatedIndexSearch.search("hel*o"));
        System.out.println(kGramIndexSearch.search("hel*o"));

        System.out.println();

        System.out.println(treeSearch.search("w*ld"));
        System.out.println(rotatedIndexSearch.search("w*ld"));
        System.out.println(kGramIndexSearch.search("w*ld"));
    }

    private static <T> T loadDic(Class<T> clazz, String filePrefix) {
        GzipStorage storage = new GzipStorage();
        try {
            String fileName = filePrefix + clazz.getSimpleName() + ".gzip";
            return storage.load(fileName, clazz);
        } catch (IOException e) {
            System.err.println("Error loading gzip storage: " + e.getMessage());
        }
        return null;
    }

    private static void saveDic(BooleanDictionary<?> dic, String filePrefix){
        GzipStorage gzipStorage = new GzipStorage();
        try {
            gzipStorage.save(dic, filePrefix+dic.getClass().getSimpleName()+".gzip");
        } catch (IOException e) {
            System.err.println("Error saving gzip storage");
        }
    }

    private static void printPositionSearch(String query) {
        System.out.println("Query: " + query);
        //System.out.println("CoordinateIndex result:\n" + position.search(query));
        System.out.println();
    }

    private static void printSearchResult(String query){
        System.out.println("Query: " + query);
        //printTimeComparison(query, 10);
        //System.out.println("InvertedIndex result:\n" + index.search(query));
        //System.out.println("CoordinateIndex result:\n" + position.search(query));
        System.out.println();
    }

//    private static void printTimeComparison(String query, int runs) {
//        long total = 0;
//        for (int i = 0; i < runs; i++) {
//            long start = System.nanoTime();
//            index.search(query);
//            total += System.nanoTime() - start;
//        }
//        double ms = (total / (double) runs) / 1000000.0;
//        System.out.printf("%s: %.3f ms%n", "InvertedIndex time", ms);
//
//        total = 0;
//        for (int i = 0; i < runs; i++) {
//            long start = System.nanoTime();
//            position.search(query);
//            total += System.nanoTime() - start;
//        }
//        ms = (total / (double) runs) / 1000000.0;
//        System.out.printf("%s: %.3f ms%n", "CoordinateIndex time", ms);
//    }

}
