import dictionary.booleanDictionary.BooleanDictionary;
import dictionary.booleanDictionary.InvertedIndexZmeul;
import dictionary.booleanDictionary.MatrixDictionaryZmeul;
import search.BooleanSearchZmeul;
import utils.IndexerZmeul;
import utils.io.GzipStorageZmeul;
import utils.tokenizer.NLPTokenizerZmeul;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;

public class MainZmeul {
    private static InvertedIndexZmeul invertedIndexZmeul = new InvertedIndexZmeul();
    private static MatrixDictionaryZmeul matrixDictionaryZmeul = new MatrixDictionaryZmeul();
    private static final NLPTokenizerZmeul tokenizer = new NLPTokenizerZmeul();
    private static final BooleanSearchZmeul<List<Integer>> index = new BooleanSearchZmeul<>(invertedIndexZmeul, tokenizer);
    private static final BooleanSearchZmeul<BitSet> matrix = new BooleanSearchZmeul<>(matrixDictionaryZmeul, tokenizer);

    public static void main(String[] args) {
        String[] collection = IndexerZmeul.generatePaths("input/test", 10, ".txt");

        IndexerZmeul.index(collection, invertedIndexZmeul, tokenizer);
        IndexerZmeul.index(collection, matrixDictionaryZmeul, tokenizer);

        //saveDic(invertedIndexZmeul);
        //saveDic(matrixDictionaryZmeul);

        //invertedIndexZmeul = loadDic(InvertedIndexZmeul.class);
        //matrixDictionaryZmeul = loadDic(MatrixDictionaryZmeul.class);
        
        String search1 = "animal";
        printSearchResult(search1);

        String search2 = "animal AND bar";
        printSearchResult(search2);

        String search3 = "animal AND bar AND NOT barely";
        printSearchResult(search3);

        String search4 = "(animal OR bar) AND NOT (barely AND kitchen)";
        printSearchResult(search4);

        String search5 = "animal AND (NOT bar AND ((NOT (barely OR beloved)) OR (NOT a OR the)))";
        printSearchResult(search5);
    }

    private static <T> T loadDic(Class<T> clazz) {
        GzipStorageZmeul storage = new GzipStorageZmeul();
        try {
            String fileName = "result/" + clazz.getSimpleName() + ".gzip";
            return storage.load(fileName, clazz);
        } catch (IOException e) {
            System.err.println("Error loading gzip storage: " + e.getMessage());
        }
        return null;
    }

    private static void saveDic(BooleanDictionary<?> dic){
        GzipStorageZmeul gzipStorageZmeul = new GzipStorageZmeul();
        try {
            gzipStorageZmeul.save(dic, "result/"+dic.getClass().getSimpleName()+".gzip");
        } catch (IOException e) {
            System.err.println("Error saving gzip storage");
        }
    }

    private static void printSearchResult(String query){
        System.out.println("Query: " + query);
        printTimeComparison(query, 100);
        System.out.println("InvertedIndex result:\n" + index.search(query));
        System.out.println("Matrix result:\n" + matrix.search(query));
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
            matrix.search(query);
            total += System.nanoTime() - start;
        }
        ms = (total / (double) runs) / 1000000.0;
        System.out.printf("%s: %.3f ms%n", "Matrix time", ms);
    }

}
