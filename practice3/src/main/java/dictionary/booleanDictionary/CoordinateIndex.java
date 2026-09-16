package dictionary.booleanDictionary;

import java.util.*;

public class CoordinateIndex implements ComplexDictionary<Map<Integer, List<Long>>> {
    private final Map<String, Map<Integer, List<Long>>> dictionaryAndPositions;
    private final List<String> documents;
    private long position;

    public CoordinateIndex() {
        dictionaryAndPositions = new HashMap<>();
        documents = new ArrayList<>();
        position = 0;
    }

    @Override
    public void addWord(String word, int docId) {
        if (!dictionaryAndPositions.containsKey(word)) {
            dictionaryAndPositions.put(word, new HashMap<>());
        }
        Map<Integer, List<Long>> positions = dictionaryAndPositions.get(word);

        if (!positions.containsKey(docId)) {
            positions.put(docId, new ArrayList<>());
        }
        List<Long> list = positions.get(docId);
        list.add(position);
        position++;
    }

    @Override
    public int addDocument(String document) {
        documents.add(document);
        position = 0;
        return documents.size() - 1;
    }

    @Override
    public Map<Integer, List<Long>> getProximalVector(Map<Integer, List<Long>> left, Map<Integer, List<Long>> right, int range) {
        Map<Integer, List<Long>> result = new HashMap<>();
        for (Integer docId : left.keySet()) {
            if (!right.containsKey(docId)) continue;
            List<Long> p1 = left.get(docId);
            List<Long> p2 = right.get(docId);
            int i = 0, j = 0;
            while (i < p1.size() && j < p2.size()) {
                long pos1 = p1.get(i);
                long pos2 = p2.get(j);
                if (Math.abs(pos1 - pos2) <= range) {
                    if (!result.containsKey(docId)) {
                        result.put(docId, new ArrayList<>());
                        result.get(docId).add(pos2);
                    }
                    i++;
                    j++;
                } else if (pos1 < pos2) {
                    i++;
                } else {
                    j++;
                }
            }
        }
        return result;
    }

    @Override
    public Map<Integer, List<Long>> getPhraseVector(String w1, String w2) {
        Map<Integer, List<Long>> map1 = getDocVector(w1);
        Map<Integer, List<Long>> map2 = getDocVector(w2);

        Map<Integer, List<Long>> result = new HashMap<>();

        for (Integer doc : map1.keySet()) {
            if (!map2.containsKey(doc)) continue;

            List<Long> p1 = map1.get(doc);
            List<Long> p2 = map2.get(doc);

            for (Long pos : p1) {
                if (p2.contains(pos + 1) && !result.containsKey(doc)) {
                    result.put(doc, new ArrayList<>());
                    result.get(doc).add(pos);
                }
            }
        }
        return result;
    }

    @Override
    public Map<Integer, List<Long>> getDocVector(String word) {
        if (word.contains("___")) {
            String[] parts = word.split("___");
            if (parts.length == 2) {
                return getPhraseVector(parts[0], parts[1]);
            }
        }
        String term = word.toLowerCase();
        if (dictionaryAndPositions.containsKey(term)) {
            return new HashMap<>(dictionaryAndPositions.get(term));
        }
        return new HashMap<>();
    }

    @Override
    public Map<Integer, List<Long>> and(Map<Integer, List<Long>> map1, Map<Integer, List<Long>> map2) {
        Map<Integer, List<Long>> result = new HashMap<>();

        Map<Integer, List<Long>> smaller = (map1.size() < map2.size()) ? map1 : map2;
        Map<Integer, List<Long>> bigger = (smaller == map1) ? map2 : map1;

        for (Integer docId : smaller.keySet()) {
            if (bigger.containsKey(docId)) {
                result.put(docId, new ArrayList<>());
            }
        }
        return result;
    }

    @Override
    public Map<Integer, List<Long>> or(Map<Integer, List<Long>> map1, Map<Integer, List<Long>> map2) {
        Map<Integer, List<Long>> result = new HashMap<>(map1);
        result.putAll(map2);
        return result;
    }

    @Override
    public Map<Integer, List<Long>> not(Map<Integer, List<Long>> map) {
        Map<Integer, List<Long>> result = new HashMap<>();
        int totalDocs = documents.size();

        for (int i = 0; i < totalDocs; i++) {
            if (!map.containsKey(i)) {
                result.put(i, new ArrayList<>());
            }
        }
        return result;
    }

    @Override
    public List<String> getDocuments(Map<Integer, List<Long>> map) {
        List<String> resultDocs = new ArrayList<>();
        List<Integer> sortedIds = new ArrayList<>(map.keySet());
        Collections.sort(sortedIds);
        for (Integer docId : sortedIds) {
            resultDocs.add(documents.get(docId));
        }
        return resultDocs;
    }

}
