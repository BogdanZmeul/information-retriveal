package parser;

import model.Zone;
import java.io.File;
import java.util.Map;

public interface DocumentParser {
    Map<Zone, StringBuilder> parse(File file);
}