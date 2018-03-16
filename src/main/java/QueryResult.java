public class QueryResult implements Comparable<QueryResult> {
    private int queryId;
    private int goldSetLength;
    private int resultLength;
    private int numberOfMatches;
    private int overheadResults;
    private int top5Matches;
    private float precision;
    private float recall;
    private float f1Measure;
    private float top5Precision;

    QueryResult(int queryID, int goldSetLength, int resultLength, int numberOfMatches,
                int overheadResults, int top5Matches) {
        this.queryId = queryID;
        this.goldSetLength = goldSetLength;
        this.resultLength = resultLength;
        this.numberOfMatches = numberOfMatches;
        this.overheadResults = overheadResults;
        this.top5Matches = top5Matches;
        this.precision = calculatePrecision();
        this.recall = calculateRecall();
        this.f1Measure = calculateF1Measure();
        this.top5Precision = calculateTop5Precision();
    }

    public int getQueryId() {
        return queryId;
    }

    public int getGoldSetLength() {
        return goldSetLength;
    }

    public int getResultLength() {
        return resultLength;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    public int getOverheadResults() {
        return overheadResults;
    }

    public int getTop5Matches() {
        return top5Matches;
    }

    public float getPrecision() {
        return precision;
    }

    public float getRecall() {
        return recall;
    }

    public float getTop5Precision() {
        return top5Precision;
    }

    public float getF1Measure() {
        return f1Measure;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%.5f,%.5f,%.5f,%5f",
                queryId,
                goldSetLength,
                resultLength,
                numberOfMatches,
                overheadResults,
                top5Matches,
                precision,
                recall,
                f1Measure,
                top5Precision);
    }

    @Override
    public int compareTo(QueryResult queryResult) {
        return this.queryId - queryResult.getQueryId();
    }

    private float calculatePrecision() {
        return resultLength > 0 ? (float) numberOfMatches / resultLength : 0;
    }

    private float calculateRecall() {
        return goldSetLength > 0 ? (float) numberOfMatches / goldSetLength : 0;
    }

    private float calculateF1Measure() {
        return precision + recall > 0 ? 2 * ((precision * recall) / precision + recall) : 0;
    }

    private float calculateTop5Precision() {
        return resultLength > 0 ? (float) top5Matches / (resultLength > 5 ? 5 : resultLength) : 0;
    }
}
