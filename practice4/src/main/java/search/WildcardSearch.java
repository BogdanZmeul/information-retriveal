package search;

import dictionary.wildcardDictionary.WildcardDictionary;

import java.util.*;

public class WildcardSearch implements Search{
    private final WildcardDictionary dictionary;

    public WildcardSearch(WildcardDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public List<String> search(String query) {
        List<String> matchingTerms = dictionary.findMatchingTerms(query);

        if (matchingTerms.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> resultDocNames = new HashSet<>();
        for (String term : matchingTerms) {
            List<Integer> docIds = dictionary.getPostings(term);
            for (Integer docId : docIds) {
                String docName = dictionary.getDocumentName(docId);
                if (docName != null) {
                    resultDocNames.add(docName);
                }
            }
        }
        return new ArrayList<>(resultDocNames);
    }
}
