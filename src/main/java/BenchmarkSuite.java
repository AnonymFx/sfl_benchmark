import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BenchmarkSuite {
    private static final String RESULTS_FILE_NAME_PREFIX = "benchmark_results_";
    private static final String RESULTS_FILE_EXTENSION = ".csv";

    private File rootFolder;
    private OutputStreamWriter resultWriter;
    private FeatureLocationTechnique featureLocationTechnique;

    public BenchmarkSuite(FeatureLocationTechnique technique, String benchmarkSuiteFolder) {
        this.featureLocationTechnique = technique;
        this.rootFolder = new File(benchmarkSuiteFolder);
        if (!this.rootFolder.exists() || !this.rootFolder.isDirectory()) {
            throw new IllegalArgumentException("Root folder has to be a directory");
        }
    }

    public List<QueryResult> runBenchmarks() {
        Logger.debugLog("Running benchmark with " + featureLocationTechnique.getId());
        File[] benchmarkDirs = rootFolder.listFiles(File::isDirectory);
        if (benchmarkDirs == null) return null;

        Logger.debugLog(String.format("Found %d benchmark folders: %s", benchmarkDirs.length, Arrays.toString(benchmarkDirs)));

        try {
            openResultWriter();
        } catch (IOException e) {
            Logger.debugLog("Could not open result file");
            e.printStackTrace();
        }

        Arrays.sort(benchmarkDirs, (file1, file2) -> {
            String fileName1 = file1.getName().toLowerCase();
            String fileName2 = file2.getName().toLowerCase();
            return fileName1.compareTo(fileName2);
        });

        List<QueryResult> suiteResults = new ArrayList<>();
        for (File benchmarkDir : benchmarkDirs) {
            Logger.verboseLog("-------------------------------------------------");
            Logger.verboseLog(String.format("Running benchmark %s", benchmarkDir));
            Benchmark benchmark = new Benchmark(featureLocationTechnique, benchmarkDir);
            List<QueryResult> results = benchmark.run();
            suiteResults.addAll(results);

            for (QueryResult result : results) {
                try {
                    resultWriter.append(String.format("%s,%s\n",
                            benchmarkDir.getName().replaceAll(",",""),
                            result.toString()));
                    resultWriter.flush();
                } catch (IOException e) {
                    Logger.debugLog("Could not write to result file");
                    e.printStackTrace();
                }
            }
        }

        try {
            flushAndCloseResultWriter();
        } catch (IOException e) {
            Logger.debugLog("Could not write/close result file");
            e.printStackTrace();
        }

        return suiteResults;
    }

    private void openResultWriter() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        File outputFile = new File(rootFolder.getPath() + File.separator +
                RESULTS_FILE_NAME_PREFIX + featureLocationTechnique.getId() + "_" + timeStamp +
                RESULTS_FILE_EXTENSION);
        boolean existed = !outputFile.createNewFile();

        resultWriter = new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8);

        // Write header
        List<String> fieldNames = Arrays.stream(QueryResult.class.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toList());
        Field[] fields = QueryResult.class.getFields();
        resultWriter.write("BenchmarkName," + String.join(",", fieldNames) + "\n");
        resultWriter.flush();
    }

    private void flushAndCloseResultWriter() throws IOException {
        resultWriter.flush();
        resultWriter.close();
    }


}
