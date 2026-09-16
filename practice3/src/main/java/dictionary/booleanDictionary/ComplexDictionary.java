package dictionary.booleanDictionary;

import java.util.List;

public interface ComplexDictionary<T> extends BooleanDictionary<T> {
    T getPhraseVector(String w1, String w2);
    T getProximalVector(T left, T right, int range);
}
