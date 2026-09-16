package dictionary;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DictionaryZmeul implements Serializable{
    private Map<String, Integer> dictionary;
    private long totalWordsCount;

    public DictionaryZmeul() {
        dictionary = new HashMap<>();
    }

    public void addWord(String word) {
        word = word.trim();
        checkWord(word);

        if (dictionary.containsKey(word)) {
            dictionary.put(word, dictionary.get(word) + 1);
        }else  {
            dictionary.put(word, 1);
        }
        totalWordsCount++;
    }

    public void putWord(String word, int count) {
        word = word.trim();
        checkWord(word);
        dictionary.put(word, count);
    }

    public long getTotalWordsCount() {
        return totalWordsCount;
    }

    public void setTotalWordsCount(long totalWordsCount) {
        this.totalWordsCount = totalWordsCount;
    }

    public long getUniqueWordsCount() {
        return dictionary.size();
    }

    public Map<String, Integer> getDictionary() {
        return dictionary;
    }

    public void setDictionary(Map<String, Integer> dictionary) {
        this.dictionary = dictionary;
    }

    private void checkWord(String word) {
        if (word.isEmpty())
            throw new IllegalArgumentException("The word is empty!");
    }

}
