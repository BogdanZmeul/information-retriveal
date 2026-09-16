package dictionary.booleanDictionary;

import java.io.Serializable;
import java.util.List;

public interface BooleanDictionary<T> extends Serializable {
    T getDocVector(String word);
    void addWord(String word, int docId);
    int addDocument (String word);

    T and(T a, T b);
    T or(T a, T b);
    T not(T a);

    List<String> getDocuments(T res);
}
