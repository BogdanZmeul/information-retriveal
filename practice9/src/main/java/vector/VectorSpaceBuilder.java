package vector;

import index.InvertedIndex;
import model.Posting;
import model.Zone;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VectorSpaceBuilder {

    public static Map<Integer, DocumentVector> buildVectors(InvertedIndex index) {
        Map<Integer, DocumentVector> documentVectors = new HashMap<>();
        int N = index.getTotalDocuments();

        for (String term : index.getVocabulary()) {
            List<Posting> postings = index.getPostings(term);
            int df = postings.size();

            double idf = Math.log10((double) N / df) + 1.0;

            for (Posting p : postings) {
                int docId = p.getDocumentId();
                documentVectors.putIfAbsent(docId, new DocumentVector(docId));

                for (Zone zone : Zone.values()) {
                    int tf = p.getTfForZone(zone);
                    if (tf > 0) {
                        double tfIdf = (1.0 + Math.log10(tf)) * idf;
                        documentVectors.get(docId).addWeight(zone, term, tfIdf);
                    }
                }
            }
        }
        return documentVectors;
    }
}