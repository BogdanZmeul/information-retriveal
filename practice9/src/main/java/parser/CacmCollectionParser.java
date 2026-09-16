package parser;

import model.Zone;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CacmCollectionParser {
    public static class CacmDocument {
        public int id;
        public Map<Zone, StringBuilder> zones = new EnumMap<>(Zone.class);

        public CacmDocument(int id) {
            this.id = id;
            for (Zone z : Zone.values()) {
                zones.put(z, new StringBuilder());
            }
        }
    }

    public static List<CacmDocument> parse(File file) {
        List<CacmDocument> documents = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            CacmDocument currentDoc = null;
            Zone currentZone = null;

            while ((line = br.readLine()) != null) {
                if (line.startsWith(".I ")) {
                    int id = Integer.parseInt(line.substring(3).trim());
                    currentDoc = new CacmDocument(id);
                    documents.add(currentDoc);
                    currentZone = null;
                } else if (line.startsWith(".T")) {
                    currentZone = Zone.TITLE;
                } else if (line.startsWith(".W")) {
                    currentZone = Zone.BODY;
                } else if (line.startsWith(".B")) {
                    currentZone = Zone.BODY;
                } else if (line.startsWith(".A")) {
                    currentZone = Zone.AUTHOR;
                } else if (line.startsWith(".N") || line.startsWith(".X") || line.startsWith(".K")) {
                    currentZone = null;
                } else if (currentZone != null && currentDoc != null) {
                    currentDoc.zones.get(currentZone).append(" ").append(line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return documents;
    }

    public static Map<Integer, String> parseQueries(File file) {
        Map<Integer, String> queries = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int currentId = -1;
            StringBuilder currentQuery = new StringBuilder();
            boolean inQuery = false;

            while ((line = br.readLine()) != null) {
                if (line.startsWith(".I ")) {
                    if (currentId != -1 && !currentQuery.toString().trim().isEmpty()) {
                        queries.put(currentId, currentQuery.toString().trim());
                    }
                    currentId = Integer.parseInt(line.substring(3).trim());
                    currentQuery = new StringBuilder();
                    inQuery = false;
                } else if (line.startsWith(".W")) {
                    inQuery = true;
                } else if (line.startsWith(".N") || line.startsWith(".A")) {
                    inQuery = false;
                } else if (inQuery) {
                    currentQuery.append(" ").append(line.trim());
                }
            }
            if (currentId != -1 && !currentQuery.toString().trim().isEmpty()) {
                queries.put(currentId, currentQuery.toString().trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queries;
    }

    public static Set<String> parseCommonWords(File file) {
        Set<String> words = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    words.add(line.trim().toLowerCase());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return words;
    }

    public static Map<Integer, Set<Integer>> parseQrels(File file) {
        Map<Integer, Set<Integer>> qrels = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        int queryId = Integer.parseInt(parts[0]);
                        int docId = Integer.parseInt(parts[1]);
                        qrels.computeIfAbsent(queryId, k -> new HashSet<>()).add(docId);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return qrels;
    }
}
