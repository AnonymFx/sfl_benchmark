import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface FeatureLocationTechnique {
    String getId();

    void prepareCodebase(File codeBase) throws IOException;

    Set<SearchResult> locate(String query);

    void teardown();
}
