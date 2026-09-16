package dictionary.booleanDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvertedIndex implements BooleanDictionary<List<Integer>> {
    private Map<String, List<Integer>> dictionary;
    private List<String> documents;

    public InvertedIndex(){
        dictionary = new HashMap<>();
        documents = new ArrayList<>();
    }

    public int addDocument(String documentName) {
        documents.add(documentName);
        return documents.size()-1;
    }

    public void addWord(String word, int documentIndex) {
        if (!dictionary.containsKey(word)) {
            dictionary.put(word, new ArrayList<>());
        }
        List<Integer> indexes = dictionary.get(word);
        if(indexes.isEmpty() || indexes.getLast() != documentIndex){
            indexes.add(documentIndex);
        }
    }

    @Override
    public List<Integer> getDocVector(String word) {
        if (dictionary.containsKey(word)) {
            return new ArrayList<>(dictionary.get(word));
        }
        return new ArrayList<>();
    }

    @Override
    public List<Integer> and(List<Integer> list1, List<Integer> list2) {
        List<Integer> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < list1.size() && j < list2.size()) {
            int document1 = list1.get(i);
            int document2 = list2.get(j);

            if (document1 == document2) {
                result.add(document1);
                i++;
                j++;
            } else if (document1 < document2) {
                i++;
            }else {
                j++;
            }
        }
        return result;
    }

    @Override
    public List<Integer> or(List<Integer> list1, List<Integer> list2) {
        List<Integer> result = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < list1.size() && j < list2.size()) {
            int document1 = list1.get(i);
            int document2 = list2.get(j);

            if (document1 == document2) {
                result.add(document1);
                i++;
                j++;
            } else if (document1 < document2) {
                result.add(document1);
                i++;
            } else {
                result.add(document2);
                j++;
            }
        }

        while (i < list1.size()) {
            result.add(list1.get(i));
            i++;
        }
        while (j < list2.size()) {
            result.add(list2.get(j));
            j++;
        }
        return result;
    }

    @Override
    public List<Integer> not(List<Integer> list) {
        List<Integer> result = new ArrayList<>();
        int total = documents.size();
        int ind = 0;

        for (int i = 0; i < total; i++) {
            if (ind <  list.size() && list.get(ind) == i) {
                ind++;
            } else {
                result.add(i);
            }
        }
        return result;
    }

    @Override
    public List<String> getDocuments(List<Integer> list) {
        List<String> result = new ArrayList<>();
        for (Integer i : list) {
            result.add(documents.get(i));
        }
        return result;
    }

}
