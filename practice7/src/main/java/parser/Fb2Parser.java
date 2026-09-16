package parser;

import model.Zone;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class Fb2Parser implements DocumentParser {

    @Override
    public Map<Zone, StringBuilder> parse(File file) {
        Map<Zone, StringBuilder> contentMap = new EnumMap<>(Zone.class);
        for (Zone z : Zone.values()) {
            contentMap.put(z, new StringBuilder());
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            Zone currentZone = Zone.BODY;
            boolean insideDescription = false;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.contains("<description>") || trimmed.contains("<title-info>")) {
                    insideDescription = true;
                }
                if (trimmed.contains("</description>") || trimmed.contains("</title-info>")) {
                    insideDescription = false;
                    currentZone = Zone.BODY;
                }

                if (insideDescription) {
                    if (trimmed.contains("<book-title>")) currentZone = Zone.TITLE;
                    else if (trimmed.contains("<author>") || trimmed.contains("<first-name>") || trimmed.contains("<last-name>")) currentZone = Zone.AUTHOR;
                } else {
                    if (trimmed.contains("<body>")) currentZone = Zone.BODY;
                }

                String textContent = removeTags(trimmed);
                if (!textContent.isEmpty()) {
                    contentMap.get(currentZone).append(" ").append(textContent);
                }
            }
        } catch (IOException e) {
            System.err.println("Error parsing FB2: " + e.getMessage());
        }
        return contentMap;
    }

    private String removeTags(String text) {
        return text.replaceAll("<[^>]*>", " ").trim();
    }
}