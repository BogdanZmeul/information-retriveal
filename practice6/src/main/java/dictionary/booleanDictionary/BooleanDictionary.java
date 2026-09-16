package dictionary.booleanDictionary;

import dictionary.Dictionary;

import java.io.Serializable;
import java.util.List;

public interface BooleanDictionary<T> extends Dictionary {
    T getDocVector(String word);

    T and(T a, T b);
    T or(T a, T b);
    T not(T a);

    List<String> getDocuments(T bs);
}
