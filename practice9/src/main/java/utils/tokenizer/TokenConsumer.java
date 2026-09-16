package utils.tokenizer;

@FunctionalInterface
public interface TokenConsumer {
    void accept(String token) throws Exception;
}