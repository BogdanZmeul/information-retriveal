package dictionary.wildcardDictionary;

import java.util.*;

public class RotatedTermIndex implements WildcardDictionary {
    private final TreeMap<String, String> permutermMap;
    private final Map<String, List<Integer>> postingsMap;
    private final List<String> documents;

    private static final String SYMBOL = "$";

    public RotatedTermIndex() {
        this.permutermMap = new TreeMap<>();
        this.postingsMap = new HashMap<>();
        this.documents = new ArrayList<>();
    }

    @Override
    public int addDocument(String documentName) {
        documents.add(documentName);
        return documents.size() - 1;
    }

    @Override
    public String getDocumentName(int docId) {
        if (docId >= 0 && docId < documents.size()) {
            return documents.get(docId);
        }
        return null;
    }

    @Override
    public void addWord(String word, int docId) {
        postingsMap.computeIfAbsent(word, k -> new ArrayList<>()).add(docId);

        String termWithSymbol = word + SYMBOL;

        if (!permutermMap.containsKey(termWithSymbol)) {
            for (int i = 0; i < termWithSymbol.length(); i++) {
                String rotated = termWithSymbol.substring(i) + termWithSymbol.substring(0, i);
                permutermMap.put(rotated, word);
            }
        }
    }

    @Override
    public List<Integer> getPostings(String term) {
        return postingsMap.getOrDefault(term, Collections.emptyList());
    }

    @Override
    public List<String> findMatchingTerms(String query) {
        if (!query.contains("*")) {
            return postingsMap.containsKey(query) ? List.of(query) : Collections.emptyList();
        }

        int starIndex = query.indexOf("*");
        String prefix = query.substring(0, starIndex);
        String suffix = query.substring(starIndex + 1);

        String searchKey = suffix + SYMBOL + prefix;

        return searchPrefixInMap(searchKey);
    }

    private List<String> searchPrefixInMap(String prefix) {
        char nextChar = (char) (prefix.charAt(prefix.length() - 1) + 1);
        String endBound = prefix.substring(0, prefix.length() - 1) + nextChar;

        NavigableMap<String, String> subMap = permutermMap.subMap(prefix, true, endBound, false);

        Set<String> uniqueMatches = new HashSet<>(subMap.values());

        return new ArrayList<>(uniqueMatches);
    }
}