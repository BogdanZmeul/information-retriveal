package utils.tokenizer;

public class SmartSimpleTokenizer implements StreamTokenizer {

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
                    consumer.accept(text.substring(start, i).toLowerCase());
                    start = -1;
                }
            }
        }

        if (start != -1) {
            consumer.accept(text.substring(start, length).toLowerCase());
        }
    }
}
