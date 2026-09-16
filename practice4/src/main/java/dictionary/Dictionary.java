package dictionary;

import java.io.Serializable;

public interface Dictionary extends Serializable {
    void addWord(String word, int docId);
    int addDocument (String word);
}
