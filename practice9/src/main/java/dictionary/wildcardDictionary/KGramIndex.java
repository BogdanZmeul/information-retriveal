package dictionary.wildcardDictionary;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KGramIndex implements WildcardDictionary {
    private final int K = 3;
    private static final String SYMBOL = "$";

    private final Map<String, List<Integer>> postingsMap;
    private final Map<String, Set<String>> kGramIndex;
    private final List<String> documents;

    public KGramIndex() {
        this.postingsMap = new HashMap<>();
        this.kGramIndex = new HashMap<>();
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
        String extendedWord = SYMBOL + word + SYMBOL;

        for (int i = 0; i < extendedWord.length() - K + 1; i++) {
            String gram = extendedWord.substring(i, i + K);

            kGramIndex.computeIfAbsent(gram, k -> new HashSet<>()).add(word);
        }
    }

    @Override
    public List<Integer> getPostings(String term) {
        return postingsMap.getOrDefault(term, Collections.emptyList());
    }

    @Override
    public List<String> findMatchingTerms(String wildcardQuery) {
        if (!wildcardQuery.contains("*")) {
            return postingsMap.containsKey(wildcardQuery) ? List.of(wildcardQuery) : Collections.emptyList();
        }

        List<String> queryGrams = extractGramsFromWildcard(wildcardQuery);
        Set<String> candidates;

        if (queryGrams.isEmpty()) {
            candidates = new HashSet<>(postingsMap.keySet());
        } else {
            candidates = new HashSet<>(kGramIndex.getOrDefault(queryGrams.get(0), Collections.emptySet()));

            for (int i = 1; i < queryGrams.size(); i++) {
                Set<String> nextGramMatches = kGramIndex.getOrDefault(queryGrams.get(i), Collections.emptySet());
                if (nextGramMatches.isEmpty()) {
                    return Collections.emptyList();
                }
                candidates.retainAll(nextGramMatches);
            }
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        String regex = "^" + wildcardQuery.replace("*", ".*") + SYMBOL;
        Pattern pattern = Pattern.compile(regex);

        return candidates.stream()
                .filter(term -> pattern.matcher(term).matches())
                .collect(Collectors.toList());
    }

    private List<String> extractGramsFromWildcard(String query) {
        List<String> grams = new ArrayList<>();
        String[] parts = query.split("\\*");

        if (!query.startsWith("*") && parts.length > 0) {
            String firstPart = SYMBOL + parts[0];
            addGramsFromString(firstPart, grams);
        }

        for (int i = 1; i < parts.length - 1; i++) {
            addGramsFromString(parts[i], grams);
        }

        if (!query.endsWith("*") && parts.length > 0) {
            String lastPart = parts[parts.length - 1] + SYMBOL;
            addGramsFromString(lastPart, grams);
        }

        return grams;
    }

    private void addGramsFromString(String text, List<String> grams) {
        if (text.length() >= K) {
            for (int i = 0; i < text.length() - K + 1; i++) {
                grams.add(text.substring(i, i + K));
            }
        }
    }
}