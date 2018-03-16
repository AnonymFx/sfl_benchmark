import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryAnalysis {
    private static final String RESULT_FILE_PREFIX = "Result";
    private static final String RESULT_FILE_EXTENSION = ".txt";

    private String benchmarkName;
    private FeatureLocationTechnique featureLocationTechnique;
    private File sourcesDir;
    private String query;
    private Integer queryId;
    private Set<String> goldSet = new HashSet<>();
    private OutputStreamWriter resultWriter;
    private File resultsDir;

    QueryAnalysis(String benchmarkName, FeatureLocationTechnique technique,
                  Integer queryId, File queryFile, File sourcesDir, File goldSetFile, File resultsDir) {
        this.benchmarkName = benchmarkName;
        featureLocationTechnique = technique;
        this.queryId = queryId;
        this.sourcesDir = sourcesDir;
        readQuery(queryFile);
        readGoldSet(goldSetFile);
        this.resultsDir = resultsDir;
    }

    QueryResult run() {
        try {
            featureLocationTechnique.prepareCodebase(sourcesDir);
        } catch (IOException e) {
            Logger.debugLog("Error preparing Codebase");
            e.printStackTrace();
            return null;
        }

        Set<SearchResult> results = featureLocationTechnique.locate(query);
        featureLocationTechnique.teardown();

        if (resultsDir != null) {
            try {
                writeResultsToFile(results);
            } catch (IOException e) {
                Logger.debugLog(benchmarkName, "Could not write result for query " + queryId);
                e.printStackTrace();
            }
        } else {
            Logger.debugLog("Could not find result folder");
        }

        return calculateQueryResult(results);
    }

    private void readQuery(File queryFile) {
        try {
            this.query = new String(Files.readAllBytes(Paths.get(queryFile.getPath())));
        } catch (IOException e) {
            Logger.debugLog(benchmarkName, "Error reading " + queryFile.getName());
            e.printStackTrace();
        }
    }

    private void readGoldSet(File goldSetFile) {
        try (Stream<String> lines = Files.lines(Paths.get(goldSetFile.getPath()))) {
            for (String line : (Iterable<String>) lines::iterator) {
                goldSet.add(formatGoldSetElement(line));
            }
        } catch (IOException e) {
            Logger.debugLog(benchmarkName, "Could not read " + goldSetFile.getName());
            e.printStackTrace();
        }
    }

    private String formatGoldSetElement(String element) {
        String formattted = element;
        // If gold Set is on method level, remove method part
        if (element.contains("(")) {
            String[] splits = element.split("\\.");
            formattted = String.join(".", Arrays.copyOfRange(
                    splits, 0, splits.length - 1));
        }

        return formattted.toLowerCase();
    }

    private void writeResultsToFile(Set<SearchResult> results) throws IOException {
        openResultWriter();
        List<SearchResult> resultList = new ArrayList<>(results);
        Collections.sort(resultList);

        for (SearchResult item : resultList) {
            resultWriter.write(item.toString() + "\n");
        }

        flushAndCloseResultWriter();

    }

    private void openResultWriter() throws IOException {
        File outputFile = new File(resultsDir.getPath() + File.separator + RESULT_FILE_PREFIX +
                queryId + RESULT_FILE_EXTENSION);

        resultWriter = new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8);
    }

    private void flushAndCloseResultWriter() throws IOException {
        resultWriter.flush();
        resultWriter.close();
    }

    private QueryResult calculateQueryResult(Set<SearchResult> results) {
        // Prepare result list
        Set<String> resultClasses = results.stream().map(SearchResult::getClassName).collect(Collectors.toSet());
        int numberOfMatches = 0;
        Set<String> lowerCaseResults = resultClasses.stream().map(String::toLowerCase).collect(Collectors.toSet());

        // Prepare top5 list
        List<SearchResult> orderedResults = new ArrayList<>(results);
        Collections.sort(orderedResults);
        List<String> top5List = orderedResults
                .subList(0, (orderedResults.size() > 5) ? 5 : orderedResults.size())
                .stream()
                .map(SearchResult::getClassName)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        int top5Matches = 0;

        for (String goldSetElement : goldSet) {
            goldSetElement = goldSetElement.toLowerCase();
            if (lowerCaseResults.contains(goldSetElement)) {
                numberOfMatches++;
            }
            if(top5List.contains(goldSetElement)) {
                top5Matches++;
            }
        }

        int overheadResults = 0;
        for (String result : lowerCaseResults) {
            if (!goldSet.contains(result)) {
                overheadResults++;
            }
        }


        return new QueryResult(queryId,
                goldSet.size(),
                resultClasses.size(),
                numberOfMatches,
                overheadResults,
                top5Matches);
    }
}
