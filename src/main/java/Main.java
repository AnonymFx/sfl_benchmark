import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify the benchmark folder");
            return;
        }
        String benchmarkFolder = args[0];
        List<BenchmarkSuite> benchmarks = new ArrayList<>();

        TfidfConfiguration config = new TfidfConfiguration()
                .setScoreCutoff(0.4f)
                .setStopWordFile("stopwords.txt");
        benchmarks.add(
                new BenchmarkSuite(new TfidfFeatureLocation(config),
                        benchmarkFolder));
        // benchmarks.add(...) add more FLTs here

        for (BenchmarkSuite suite : benchmarks) {
            suite.runBenchmarks();
        }
    }
}
