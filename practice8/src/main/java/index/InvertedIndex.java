package index;

import model.Document;
import model.Posting;
import model.Zone;

import java.util.*;

public class InvertedIndex {
    private final Map<String, Map<Integer, Posting>> index = new HashMap<>();
    private final Map<Integer, Document> docRegistry = new HashMap<>();
    private int docCounter = 0;

    public int registerDocument(String filePath, String fileName) {
        int id = ++docCounter;
        docRegistry.put(id, new Document(id, filePath, fileName));
        return id;
    }

    public Document getDocument(int id) { return docRegistry.get(id); }

    public int getTotalDocuments() { return docCounter; }

    public void addTerm(String term, int docId, Zone zone) {
        index.putIfAbsent(term, new HashMap<>());
        Map<Integer, Posting> postingsMap = index.get(term);

        if (!postingsMap.containsKey(docId)) {
            postingsMap.put(docId, new Posting(docId));
        }
        postingsMap.get(docId).addZoneOccurrence(zone);
    }

    public List<Posting> getPostings(String term) {
        if (!index.containsKey(term)) return Collections.emptyList();
        return new ArrayList<>(index.get(term).values());
    }

    public int getDocumentFrequency(String term) {
        if (!index.containsKey(term)) return 0;
        return index.get(term).size();
    }

    public Set<String> getVocabulary() {
        return index.keySet();
    }
}