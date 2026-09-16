package search;

import dictionary.booleanDictionary.BooleanDictionary;
import utils.tokenizer.Tokenizer;

import java.util.ArrayDeque;
import java.util.List;

public class BooleanSearch<T> implements Search {
    protected final BooleanDictionary<T> dictionary;
    protected final Tokenizer tokenizer;

    public BooleanSearch(BooleanDictionary<T> dictionary, Tokenizer tokenizer) {
        this.dictionary = dictionary;
        this.tokenizer = tokenizer;
    }

    @Override
    public List<String> search(String query) {
        String[] tokens = query.replace("(", " ( ").replace(")", " ) ").trim().split("\\s+");

        ArrayDeque<T> values = new ArrayDeque<>();
        ArrayDeque<String> ops = new ArrayDeque<>();

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            String t = token.toUpperCase();

            if (t.equals("(")) {
                ops.push(t);
            } else if (t.equals(")")) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) {
                    applyOperator(values, ops.pop());
                }
                if (ops.isEmpty()) {
                    throw new IllegalArgumentException("Mismatched parentheses");
                }
                ops.pop();
                if (!ops.isEmpty() && ops.peek().equals("NOT")) {
                    applyOperator(values, ops.pop());
                }
            } else if (isOperator(t)) {
                if (t.equals("NOT")) {
                    ops.push(t);
                } else {
                    while (!ops.isEmpty()
                            && !ops.peek().equals("(")
                            && priority(ops.peek()) >= priority(t)) {
                        applyOperator(values, ops.pop());
                    }
                    ops.push(t);
                }
            } else {
                String word = tokenizer.tokenize(token).getFirst();
                values.push(dictionary.getDocVector(word));
                if (!ops.isEmpty() && ops.peek().equals("NOT")) {
                    applyOperator(values, ops.pop());
                }
            }
        }

        while (!ops.isEmpty()) {
            String op = ops.pop();
            if (op.equals("(")) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            applyOperator(values, op);
        }

        return dictionary.getDocuments(values.pop());
    }

    protected void applyOperator(ArrayDeque<T> values, String op) {
        if (op.equals("NOT")) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("NOT has no operand");
            }
            T v = values.pop();
            values.push(dictionary.not(v));
        } else {
            if (values.size() < 2) {
                throw new IllegalArgumentException(op + " needs two operands");
            }
            T right = values.pop();
            T left = values.pop();

            if (op.equals("AND")) {
                values.push(dictionary.and(left, right));
            } else if (op.equals("OR")) {
                values.push(dictionary.or(left, right));
            }
        }
    }

    protected boolean isOperator(String t) {
        return t.equals("AND") || t.equals("OR") || t.equals("NOT");
    }

    protected int priority(String op) {
        return switch (op) {
            case "NOT" -> 3;
            case "AND" -> 2;
            case "OR" -> 1;
            default -> 0;
        };
    }
}
