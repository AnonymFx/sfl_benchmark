import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Benchmark {
    private static final String QUERY_PATTERN = "Query[\\d]*\\.txt";

    private static final String QUERY_FOLDER_NAME = "Queries";
    private static final String QUERY_FILE_PREFIX = "Query";
    private static final String QUERY_FILE_POSTFIX = ".txt";

    private static final String SOURCES_FOLDER_NAME = "Sources";
    private static final String SOURCE_FILE_PREFIX = "Source";

    private static final String GOLD_SETS_FOLDER_NAME = "GoldSets";
    private static final String GOLD_SET_FILE_PREFIX = "GoldSet";
    private static final String GOLD_SET_FILE_POSTFIX = ".txt";

    private static final String RESULT_FOLDER_NAME = "Results";

    private FeatureLocationTechnique featureLocationTechnique;
    private File rootFolder;
    private File resultDir;
    private String benchmarkName;
    private Map<Integer, File> queryFiles = new HashMap<>();
    private Map<Integer, File> sourceFolders = new HashMap<>();
    private Map<Integer, File> goldSetFiles = new HashMap<>();

    Benchmark(FeatureLocationTechnique technique, File benchmarkFolder) {
        this.featureLocationTechnique = technique;
        this.rootFolder = benchmarkFolder;
        benchmarkName = rootFolder.getName();
        initBenchmarkFiles();
    }

    List<QueryResult> run() {
        List<QueryResult> results = new ArrayList<>();
        int queryNumber = 1;
        int numQueries = queryFiles.keySet().size();

        List<Integer> sortedQueries = new ArrayList<>(queryFiles.keySet());
        Collections.sort(sortedQueries);

        for (Integer id : sortedQueries) {
            Logger.verboseLog(benchmarkName, String.format("Running query %d of %d with id %d",
                    queryNumber++, numQueries, id));
            File queryFile = queryFiles.get(id);

            File sourceFolder = sourceFolders.get(id);
            if (sourceFolder == null) {
                Logger.debugLog(benchmarkName, "Missing source folder for query " + id);
                continue;
            }

            File goldSetFile = goldSetFiles.get(id);
            if (goldSetFile == null) {
                Logger.debugLog(benchmarkName, "Missing gold set for query " + id);
                continue;
            }


            QueryAnalysis queryAnalysis = new QueryAnalysis(
                    benchmarkName, featureLocationTechnique,
                    id, queryFile, sourceFolder, goldSetFile, resultDir);

            QueryResult queryResult = queryAnalysis.run();
            if (queryResult != null) {
                results.add(queryResult);
            }
        }

        Collections.sort(results);

        return results;
    }

    private void initBenchmarkFiles() {
        getQueries();
        getSourcesForQueries();
        getGoldSetsForQueries();
        getResultDir();
    }

    private void getResultDir() {
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        File resultsDir = new File(rootFolder.getPath() + File.separator +
                RESULT_FOLDER_NAME + "_" + featureLocationTechnique.getId() + "_" + timeStamp);
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
            this.resultDir = resultsDir;
            return;
        }

        try {
            FileUtils.cleanDirectory(resultsDir);
        } catch (IOException e) {
            Logger.debugLog(benchmarkName, "Could not clear result folder");
            e.printStackTrace();
            return;
        }
        this.resultDir = resultsDir;
    }

    private void getQueries() {
        File queryFolder = new File(rootFolder.getPath() + File.separator + QUERY_FOLDER_NAME);

        if (!queryFolder.exists() || !queryFolder.isDirectory()) {
            Logger.debugLog(benchmarkName, "Could not find the Query folder");
            return;
        }

        File[] queryFiles = queryFolder.listFiles((dir, filename) -> {
            Pattern queryPattern = Pattern.compile(QUERY_PATTERN);
            Matcher queryMatcher = queryPattern.matcher(filename);
            return queryMatcher.matches();
        });

        if (queryFiles == null) {
            Logger.debugLog("Could not find queries for " + benchmarkName + "");
            return;
        }

        for (File queryFile : queryFiles) {
            String fileName = queryFile.getName();
            String idString = fileName.substring(
                    QUERY_FILE_PREFIX.length(), fileName.length() - QUERY_FILE_POSTFIX.length());
            Integer queryID = Integer.valueOf(idString);
            this.queryFiles.put(queryID, queryFile);
        }
    }

    private void getSourcesForQueries() {
        File sourcesFolder = new File(rootFolder.getPath() + File.separator + SOURCES_FOLDER_NAME);
        File defaultSourceFolder = new File(
                sourcesFolder.getPath() + File.separator + SOURCE_FILE_PREFIX);

        if (!sourcesFolder.exists() || !sourcesFolder.isDirectory()) {
            Logger.debugLog(benchmarkName, "Could not find sources folder, " +
                    "searching for a setup with singular source folder");

            if (!defaultSourceFolder.exists() || !defaultSourceFolder.isDirectory()) {
                Logger.debugLog(benchmarkName, "Could also not find singular source folder," +
                        "omitting sources. Benchmark will not run");
                return;
            }

        }

        for (Integer queryId : queryFiles.keySet()) {
            File sourceForQuery = new File(
                    sourcesFolder.getPath() + File.separator + SOURCE_FILE_PREFIX + queryId);

            if (!sourceForQuery.exists() || !sourceForQuery.isDirectory()) {
                Logger.debugLog(benchmarkName, "Could not find sources for query, using default Source");
                sourceForQuery = defaultSourceFolder;
            }

            sourceFolders.put(queryId, sourceForQuery);
        }
    }

    private void getGoldSetsForQueries() {
        File goldSetFolder = new File(rootFolder.getPath() + File.separator + GOLD_SETS_FOLDER_NAME);

        if (!goldSetFolder.exists() || !goldSetFolder.isDirectory()) {
            Logger.debugLog(benchmarkName, "Could not find GoldSet Folder");
            return;
        }

        for (Integer queryId : queryFiles.keySet()) {
            File goldSetForQuery = new File(goldSetFolder.getPath() + File.separator +
                    GOLD_SET_FILE_PREFIX + queryId + GOLD_SET_FILE_POSTFIX);

            if (!goldSetForQuery.exists()) {
                Logger.debugLog(benchmarkName, "Could not find GoldSet for Query" + queryId);
                continue;
            }

            goldSetFiles.put(queryId, goldSetForQuery);
        }
    }
}

