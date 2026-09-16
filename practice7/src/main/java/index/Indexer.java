package index;

import model.Zone;
import parser.DocumentParser;
import utils.tokenizer.Tokenizer;
import java.io.File;
import java.util.List;
import java.util.Map;

public class Indexer {
    private final InvertedIndex index;
    private final Tokenizer tokenizer;
    private final DocumentParser parser;

    public Indexer(InvertedIndex index, Tokenizer tokenizer, DocumentParser parser) {
        this.index = index;
        this.tokenizer = tokenizer;
        this.parser = parser;
    }

    public void indexFiles(String[] filePaths) {
        for (String path : filePaths) {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("File not found: " + path);
                continue;
            }

            int docId = index.registerDocument(path, file.getName());
            //System.out.println("Indexing doc: " + docId + " - " + file.getName());

            Map<Zone, StringBuilder> zonesContent = parser.parse(file);

            for (Map.Entry<Zone, StringBuilder> entry : zonesContent.entrySet()) {
                Zone zone = entry.getKey();
                String text = entry.getValue().toString();

                List<String> tokens = tokenizer.tokenize(text);
                for (String token : tokens) {
                    index.addTerm(token, docId, zone);
                }
            }
        }
    }
}