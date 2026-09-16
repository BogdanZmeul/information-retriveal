package utils.tokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SimpleTokenizer implements Tokenizer {
    private final Pattern splitPattern = Pattern.compile("[^\\p{L}0-9]+");
    @Override
    public List<String> tokenize(String text) {
        String[] words = splitPattern.split(text.toLowerCase());
        return words.length==0 ? new ArrayList<>(): Arrays.asList(words);
    }
}
