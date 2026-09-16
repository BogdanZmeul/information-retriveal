package dictionary.booleanDictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;

public class MatrixDictionaryZmeul implements BooleanDictionary<BitSet> {
    private Map<String, BitSet> map;
    private List<String> documents;

    public MatrixDictionaryZmeul() {
        map = new HashMap<>();
        documents = new ArrayList<>();
    }

    public int addDocument(String documentName) {
        documents.add(documentName);
        return  documents.size()-1;
    }

    public void addWord(String word, int documentIndex) {
        if (!map.containsKey(word)) {
            map.put(word, new BitSet());
        }
        map.get(word).set(documentIndex);
    }

    @Override
    public BitSet getDocVector(String word) {
        if (map.containsKey(word)) {
            return (BitSet) map.get(word).clone();
        }
        return new BitSet();
    }

    @Override
    public BitSet and(BitSet bs, BitSet bs2) {
        BitSet result = (BitSet) bs.clone();
        result.and(bs2);
        return result;
    }

    @Override
    public BitSet or(BitSet bs, BitSet bs2) {
        BitSet result = (BitSet) bs.clone();
        result.or(bs2);
        return result;
    }

    @Override
    public BitSet not(BitSet bs) {
        BitSet result = (BitSet) bs.clone();
        result.flip(0, documents.size());
        return result;
    }

    @Override
    public List<String> getDocuments(BitSet bs) {
        List<String> list = new ArrayList<>();
        for (int i = bs.nextSetBit(0); i >= 0 && i < documents.size(); i = bs.nextSetBit(i + 1)) {
            list.add(documents.get(i));
        }
        return list;
    }

}
