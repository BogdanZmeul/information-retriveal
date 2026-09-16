package utils.tokenizer;

import opennlp.tools.stemmer.PorterStemmer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StemmerTokenizer implements Tokenizer {
    private final PorterStemmer stemmer;
    private final Pattern splitPattern = Pattern.compile("[^\\p{L}0-9]+");

    public StemmerTokenizer() {
        this.stemmer = new PorterStemmer();
    }

    @Override
    public List<String> tokenize(String text) {
        String[] words = splitPattern.split(text);
        List<String> tokens = new ArrayList<>();

        for (String word : words) {
            if (word.isBlank()) continue;
            tokens.add(stemmer.stem(word.toLowerCase()));
        }
        return tokens;
    }
}