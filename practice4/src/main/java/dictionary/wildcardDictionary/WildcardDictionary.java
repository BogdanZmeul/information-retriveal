package dictionary.wildcardDictionary;

import dictionary.Dictionary;

import java.util.List;

public interface WildcardDictionary extends Dictionary {
    List<String> findMatchingTerms(String wildcardQuery);
    List<Integer> getPostings(String term);
    String getDocumentName(int docId);
}
