package utils.tokenizer;

import java.util.ArrayList;
import java.util.List;

public class PhraseTokenizer implements Tokenizer {
    @Override
    public List<String> tokenize(String text) {
        StemmerTokenizer stemmerTokenizer = new StemmerTokenizer();
        List<String> tokens = stemmerTokenizer.tokenize(text);

        List<String> phrases = new ArrayList<>();
        for (int i = 0; i < tokens.size() - 1; i++) {
            String w1 = tokens.get(i);
            String w2 = tokens.get(i + 1);
            phrases.add(w1 + "___" + w2);
        }
        return phrases.isEmpty() ? tokens : phrases;
    }
}
