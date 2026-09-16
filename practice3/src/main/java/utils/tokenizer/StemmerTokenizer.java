package utils.tokenizer;

import opennlp.tools.stemmer.PorterStemmer;

import java.util.ArrayList;
import java.util.List;

public class StemmerTokenizer implements Tokenizer {
    private final PorterStemmer stemmer;

    public StemmerTokenizer() {
        this.stemmer = new PorterStemmer();
    }

    @Override
    public List<String> tokenize(String text) {
        String[] words = text.split("[^\\p{L}0-9]+");
        List<String> tokens = new ArrayList<>();

        for (String word : words) {
            if (word.isBlank()) continue;
            String lower = word.toLowerCase();
            String stemmed = stemmer.stem(lower);
            tokens.add(stemmed);
        }
        return tokens;
    }
}
