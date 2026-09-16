package utils.tokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleTokenizer implements Tokenizer {
    @Override
    public List<String> tokenize(String text) {
        String[] words = text.toLowerCase().split("[^\\p{L}0-9]+");
        return words.length==0 ? new ArrayList<>(): Arrays.asList(words);
    }
}
