package search;

public class SearchResult implements Comparable<SearchResult> {
    private final int docId;
    private final String docName;
    private final double score;

    public SearchResult(int docId, String docName, double score) {
        this.docId = docId;
        this.docName = docName;
        this.score = score;
    }

    @Override
    public String toString() {
        return String.format("[DocID: %d] %s (Score: %.4f)", docId, docName, score);
    }

    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.score, this.score);
    }
}