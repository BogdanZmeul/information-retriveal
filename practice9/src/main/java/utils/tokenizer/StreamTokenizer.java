package utils.tokenizer;

public interface StreamTokenizer {
    void tokenize(String text, TokenConsumer consumer) throws Exception;
}