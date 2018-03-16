public class TfidfConfiguration {

    private String stopWordFile = null;
    private boolean removeDuplicateQueryTerms = false;
    private float scoreCutoff = -1;

    public String getStopWordFile() {
        return stopWordFile;
    }

    public TfidfConfiguration setStopWordFile(String stopWordFile) {
        this.stopWordFile = stopWordFile;
        return this;
    }

    public boolean isRemoveDuplicateQueryTerms() {
        return removeDuplicateQueryTerms;
    }

    public TfidfConfiguration setRemoveDuplicateQueryTerms(boolean removeDuplicateQueryTerms) {
        this.removeDuplicateQueryTerms = removeDuplicateQueryTerms;
        return this;
    }

    public float getScoreCutoff() {
        return scoreCutoff;
    }

    public TfidfConfiguration setScoreCutoff(float scoreCutoff) {
        this.scoreCutoff = scoreCutoff;
        return this;
    }
}
