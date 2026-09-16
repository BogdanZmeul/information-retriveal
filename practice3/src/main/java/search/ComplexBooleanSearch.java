package search;

import dictionary.booleanDictionary.BooleanDictionary;
import dictionary.booleanDictionary.ComplexDictionary;
import utils.tokenizer.Tokenizer;
import java.util.List;

public class ComplexBooleanSearch<T> extends BooleanSearch<T> {

    public ComplexBooleanSearch(BooleanDictionary<T> dictionary, Tokenizer tokenizer) {
        super(dictionary, tokenizer);
    }

    @Override
    public List<String> search(String query) {
        if (dictionary instanceof ComplexDictionary<T> pd && query.contains("/")) {
            return positionQuery(query, pd);
        }

        String[] raw = query.trim().split("\\s+");
        StringBuilder rewritten = new StringBuilder();

        int i = 0;
        while (i < raw.length) {
            String token = raw[i];
            if (isOperator(token.toUpperCase()) || token.equals("(") || token.equals(")")) {
                rewritten.append(token).append(" ");
                i++;
                continue;
            }
            if (i + 1 < raw.length) {
                String next = raw[i + 1];
                if (!isOperator(next.toUpperCase()) && !next.equals("(")
                        && !next.equals(")")) {
                    rewritten.append(token).append("___").append(next).append(" ");
                    i += 2;
                    continue;
                }
            }
            rewritten.append(token).append(" ");
            i++;
        }
        return super.search(rewritten.toString().trim());
    }

    private List<String> positionQuery(String query, ComplexDictionary<T> dict) {
        String[] tokens = query.trim().split("\\s+");

        T current = dict.getDocVector(tokens[0]);

        for (int i = 1; i < tokens.length; i += 2) {
            int k = Integer.parseInt(tokens[i].substring(1));
            String nextTerm = tokens[i + 1];

            T next = dict.getDocVector(tokenizer.tokenize(nextTerm).getFirst());

            current = dict.getProximalVector(current, next, k);
        }

        return dict.getDocuments(current);
    }
}
