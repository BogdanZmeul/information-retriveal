package utils.tokenizer;

import opennlp.tools.stemmer.PorterStemmer;

public class SmartStemmerTokenizer implements StreamTokenizer {
    private final PorterStemmer stemmer;

    public SmartStemmerTokenizer() {
        this.stemmer = new PorterStemmer();
    }

    @Override
    public void tokenize(String text, TokenConsumer consumer) throws Exception {
        int length = text.length();
        int start = -1;

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                if (start == -1) {
                    start = i;
                }
            } else {
                if (start != -1) {
                    processToken(text, start, i, consumer);
                    start = -1;
                }
            }
        }

        if (start != -1) {
            processToken(text, start, length, consumer);
        }
    }

    private void processToken(String text, int start, int end, TokenConsumer consumer) throws Exception {
        String processed = stemmer.stem(text.substring(start, end).toLowerCase());
        if (!processed.isEmpty()) {
            consumer.accept(processed);
        }
    }
}