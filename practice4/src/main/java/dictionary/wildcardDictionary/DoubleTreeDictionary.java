package dictionary.wildcardDictionary;

import java.util.*;

public class DoubleTreeDictionary implements WildcardDictionary {
    private final TreeMap<String, List<Integer>> forwardMap;
    private final TreeMap<String, String> backwardMap;
    private List<String> documents;

    public DoubleTreeDictionary() {
        this.forwardMap = new TreeMap<>();
        this.backwardMap = new TreeMap<>();
        this.documents = new ArrayList<>();
    }

    @Override
    public int addDocument(String documentName) {
        documents.add(documentName);
        return documents.size()-1;
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
        forwardMap.computeIfAbsent(word, k -> new ArrayList<>()).add(docId);
        String reversed = new StringBuilder(word).reverse().toString();
        backwardMap.put(reversed, word);
    }

    @Override
    public List<Integer> getPostings(String term) {
        return forwardMap.getOrDefault(term, Collections.emptyList());
    }

    @Override
    public List<String> findMatchingTerms(String query) {
        if (!query.contains("*")) {
            return forwardMap.containsKey(query) ? List.of(query) : Collections.emptyList();
        }

        boolean startsWithStar = query.startsWith("*");
        boolean endsWithStar = query.endsWith("*");
        int starIndex = query.indexOf("*");

        if (startsWithStar && !endsWithStar) {
            String suffix = query.substring(1);
            String reversedPrefix = new StringBuilder(suffix).reverse().toString();
            return searchPrefixInMap(backwardMap, reversedPrefix, true);
        }

        if (endsWithStar && !startsWithStar) {
            String prefix = query.substring(0, query.length() - 1);
            return searchPrefixInMap(forwardMap, prefix, false);
        }

        String prefix = query.substring(0, starIndex);
        String suffix = query.substring(starIndex + 1);

        List<String> forwardMatches = searchPrefixInMap(forwardMap, prefix, false);

        String reversedSuffix = new StringBuilder(suffix).reverse().toString();
        List<String> backwardMatches = searchPrefixInMap(backwardMap, reversedSuffix, true);

        Set<String> intersection = new HashSet<>(forwardMatches);
        intersection.retainAll(backwardMatches);

        return new ArrayList<>(intersection);
    }

    private List<String> searchPrefixInMap(TreeMap<String, ?> map, String prefix, boolean returnValues) {
        if (prefix.isEmpty()) return new ArrayList<>();

        char nextChar = (char) (prefix.charAt(prefix.length() - 1) + 1);
        String endBound = prefix.substring(0, prefix.length() - 1) + nextChar;

        NavigableMap<String, ?> subMap = map.subMap(prefix, true, endBound, false);

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, ?> entry : subMap.entrySet()) {
            if (returnValues) {
                result.add((String) entry.getValue());
            } else {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}