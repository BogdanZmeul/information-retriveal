import dictionary.booleanDictionary.BooleanDictionary;
import dictionary.booleanDictionary.CoordinateIndex;
import dictionary.booleanDictionary.InvertedIndex;
import search.ComplexBooleanSearch;
import search.Search;
import utils.Indexer;
import utils.io.GzipStorage;
import utils.tokenizer.PhraseTokenizer;
import utils.tokenizer.StemmerTokenizer;

import java.io.IOException;

public class Main {
    private static InvertedIndex invertedIndex = new InvertedIndex();
    private static CoordinateIndex coordinateIndex = new CoordinateIndex();
    private static final PhraseTokenizer tokenizer = new PhraseTokenizer();
    private static final StemmerTokenizer stemmerTokenizer = new StemmerTokenizer();
    private static final Search index = new ComplexBooleanSearch<>(invertedIndex, tokenizer);
    private static final Search position = new ComplexBooleanSearch<>(coordinateIndex, tokenizer);

    public static void main(String[] args) {
        String[] collection = Indexer.generatePaths("input/test", 10, ".txt");

        Indexer.index(collection, invertedIndex, tokenizer);
        Indexer.index(collection, coordinateIndex, stemmerTokenizer);
        
        String search1 = "all day";
        printSearchResult(search1);

        String search2 = "all day AND the day";
        printSearchResult(search2);

        String search3 = "all day AND (the day AND NOT motel room)";
        printSearchResult(search3);

        String search4 = "kid /3 memory /5 pleasant /3 regret";
        printPositionSearch(search4);

        String search5 = "all /1 day";
        printPositionSearch(search5);
    }

    private static <T> T loadDic(Class<T> clazz) {
        GzipStorage storage = new GzipStorage();
        try {
            String fileName = "result/" + clazz.getSimpleName() + ".gzip";
            return storage.load(fileName, clazz);
        } catch (IOException e) {
            System.err.println("Error loading gzip storage: " + e.getMessage());
        }
        return null;
    }

    private static void saveDic(BooleanDictionary<?> dic){
        GzipStorage gzipStorage = new GzipStorage();
        try {
            gzipStorage.save(dic, "result/"+dic.getClass().getSimpleName()+".gzip");
        } catch (IOException e) {
            System.err.println("Error saving gzip storage");
        }
    }

    private static void printPositionSearch(String query) {
        System.out.println("Query: " + query);
        System.out.println("CoordinateIndex result:\n" + position.search(query));
        System.out.println();
    }

    private static void printSearchResult(String query){
        System.out.println("Query: " + query);
        printTimeComparison(query, 10);
        System.out.println("InvertedIndex result:\n" + index.search(query));
        System.out.println("CoordinateIndex result:\n" + position.search(query));
        System.out.println();
    }

    private static void printTimeComparison(String query, int runs) {
        long total = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            index.search(query);
            total += System.nanoTime() - start;
        }
        double ms = (total / (double) runs) / 1000000.0;
        System.out.printf("%s: %.3f ms%n", "InvertedIndex time", ms);

        total = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            position.search(query);
            total += System.nanoTime() - start;
        }
        ms = (total / (double) runs) / 1000000.0;
        System.out.printf("%s: %.3f ms%n", "CoordinateIndex time", ms);
    }

}
