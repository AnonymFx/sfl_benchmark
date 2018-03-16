public class SearchResult implements Comparable<SearchResult> {
    private String className;
    private double score;


    SearchResult(String className, double score) {
        this.className = className;
        this.score = score;
    }

    public String getClassName() {
        return className;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(SearchResult searchResult) {
        double diff = searchResult.score - score;

        if (diff < 0) {
            return -1;
        } else if (diff > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SearchResult)) {
            return false;
        }
        SearchResult other = (SearchResult) obj;
        return className.equals(other.className);
    }

    @Override
    public String toString() {
        return String.format("%s -> %.10f", this.className, this.score);
    }
}
