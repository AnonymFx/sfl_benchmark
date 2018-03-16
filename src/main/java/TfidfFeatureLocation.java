import com.ibm.icu.text.CharsetDetector;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class TfidfFeatureLocation implements FeatureLocationTechnique {
    private static final String CONTENTS_FIELD = "contents";
    private static final String PATH_FIELD = "path";
    private static final String INDEX_DIR = "lucene_index_tfidf";

    private Directory indexDir;
    private Analyzer englishAnalyzer;
    private Analyzer javaAnalyzer;
    private File searchDir;
    private IndexSearcher indexSearcher;
    private TfidfConfiguration config;

    TfidfFeatureLocation(TfidfConfiguration config) {
        this.config = config;
    }

    @Override
    public String getId() {
        return "TFIDF";
    }

    @Override
    public void prepareCodebase(File codeBase) throws IOException {
        this.searchDir = codeBase;

        // If index already exists, do not overwrite
        File indexFolderFile = new File(codeBase.getPath() + File.separator + INDEX_DIR);
        if (indexFolderFile.exists() && indexFolderFile.isDirectory()) {
            indexDir = FSDirectory.open(Paths.get(codeBase.getPath() + File.separator + INDEX_DIR));
            configureAnalyzers();
            DirectoryReader iReader = DirectoryReader.open(indexDir);
            indexSearcher = new IndexSearcher(iReader);
            return;
        }

        indexDir = FSDirectory.open(Paths.get(codeBase.getPath() + File.separator + INDEX_DIR));
        configureAnalyzers();

        IndexWriterConfig irconfig = new IndexWriterConfig(javaAnalyzer);
        irconfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        irconfig.setSimilarity(SimilarityFactory.createSimilarity(config.getDocumentSimilarity()));
        IndexWriter indexWriter = new IndexWriter(indexDir, irconfig);

        Collection<File> files = getFiles();
        for (File file : files) {
            Document fileDoc = new Document();
            String path = file.getCanonicalPath();
            fileDoc.add(new StringField(PATH_FIELD, path, StringField.Store.YES));
            Charset fileCharset = detectCharSet(file);
            if (fileCharset == null) {
                Logger.debugLog(getId(), String.format("Could not find charset for file %s", file));
                return;
            }
            String contents = String.join(" ", Files.readAllLines(Paths.get(file.getPath()), fileCharset));
            String filteredContents = preprocessText(contents);
            fileDoc.add(new TextField(CONTENTS_FIELD, filteredContents, Field.Store.NO));
            indexWriter.addDocument(fileDoc);

        }

        indexWriter.close();

        DirectoryReader iReader = DirectoryReader.open(indexDir);
        indexSearcher = new IndexSearcher(iReader);
    }

    @Override
    public Set<SearchResult> locate(String feature) {
        String queryString = preprocessText(feature);
        Set<SearchResult> resultList = new HashSet<>();
        try {
            indexSearcher.setSimilarity(SimilarityFactory.createSimilarity(config.getQuerySimilarity()));

            BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
            QueryParser parser = new QueryParser(CONTENTS_FIELD, englishAnalyzer);
            Logger.verboseLog(getId(), String.format("Searching for:\n%s\n", queryString));
            Query query = parser.parse(queryString);

            if (config.isRemoveDuplicateQueryTerms()) {
                if (query instanceof BooleanQuery) {
                    removeDuplicateQueryTerms((BooleanQuery) query);
                } else {
                    Logger.debugLog(getId(), "Error, not a boolean query");
                }
            }


            // Get total hits first
            TotalHitCountCollector totalHitCountCollector = new TotalHitCountCollector();
            indexSearcher.search(query, totalHitCountCollector);


            // Get all hits with the help w/ the previously acquired total hit count
            TopDocs hits = indexSearcher.search(query, totalHitCountCollector.getTotalHits() + 1);

            for (ScoreDoc doc : hits.scoreDocs) {
                String className = parseClassName(doc);
                if (className != null) {
                    if (config.getScoreCutoff() < 0 || doc.score >= config.getScoreCutoff()) {
                        resultList.add(new SearchResult(className, doc.score));
                    }
                }
            }

        } catch (IOException e) {
            Logger.debugLog(this.getId(), "Could not open index for searching");
        } catch (ParseException e) {
            Logger.debugLog(this.getId(), "Could not parse query \"" + queryString + "\"");
            e.printStackTrace();
        }

        return resultList;
    }

    @Override
    public void teardown() {
        try {
            indexDir.close();
            indexSearcher.getIndexReader().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureAnalyzers() throws IOException {
        CustomAnalyzer.Builder analyzerBuilder = CustomAnalyzer.builder(Paths.get(System.getProperty("user.dir")))
                .withTokenizer("standard")
                .addTokenFilter("lowercase");

        if (config.getStopWordFile() != null) {
            analyzerBuilder.addTokenFilter("stop",
                    "ignoreCase", "true",
                    "words", config.getStopWordFile(),
                    "format", StopFilterFactory.FORMAT_WORDSET);
        }

        analyzerBuilder.addTokenFilter("porterstem");

        this.javaAnalyzer = analyzerBuilder.build();
        this.englishAnalyzer = analyzerBuilder.build();
    }

    private Collection<File> getFiles() {
        return FileUtils.listFiles(searchDir, new String[]{"java"}, true);
    }

    /**
     * Replace all non-alphanumerical characters with whitespace
     *
     * @return String containing only alphanumerical characters
     */
    private String preprocessText(String text) {
        String tmp = text.replaceAll("\\W", " ");
        tmp = tmp.replaceAll(String.format("%s|%s|%s", //Camel Case splitter
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
        tmp = tmp.replaceAll("\\s+", " ");

        return tmp;
    }

    private void removeDuplicateQueryTerms(BooleanQuery query) {
        Set<BooleanClause> clauseSet = new HashSet<>(query.clauses());
        query.clauses().clear();
        query.clauses().addAll(clauseSet);
    }

    private String parseClassName(ScoreDoc document) {
        try {
            String className;
            AtomicReference<String> packageName = new AtomicReference<>(null);
            String path = indexSearcher.doc(document.doc).getField(PATH_FIELD).stringValue();
            File sourceFile = new File(path);

            String[] splits = sourceFile.getName().split("\\.");
            className = String.join(".", Arrays.copyOfRange(
                    splits, 0, splits.length - 1));

            Charset fileCharset = detectCharSet(sourceFile);
            if (fileCharset == null) {
                Logger.debugLog(getId(), "Could not find Charset for file " + sourceFile);
                return null;
            }
            try (Stream<String> stream = Files.lines(Paths.get(sourceFile.getPath()), fileCharset)) {
                AtomicBoolean continued = new AtomicBoolean(false);
                try {
                    stream.forEach(line -> {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("package") && packageName.get() == null) {
                            // Remove package tag
                            trimmed = trimmed.substring("package".length(), trimmed.length());

                            packageName.set(trimmed);

                            if (!line.contains(";")) {
                                continued.set(true);
                            }
                        } else if (continued.get()) {
                            if (line.contains(";")) {
                                continued.set(false);
                                packageName.set(packageName.get() + "." + trimmed);
                            }
                            packageName.set(packageName.get() + line.trim());
                        }
                    });
                } catch (UncheckedIOException e) {
                    Logger.debugLog(this.getId(), String.format("Error reading file %s", sourceFile));
                }
            }


            if (packageName.get() != null && className != null) {
                // Remove semicolon(s) and whitespaces
                packageName.set(packageName.get().replaceAll(";", "").replaceAll("\\s", ""));

                return packageName + "." + className;
            }
        } catch (IOException e) {
            Logger.debugLog(this.getId(), "Could not get path from search result");
            e.printStackTrace();
        }

        return null;
    }

    private Charset detectCharSet(File file) {
        try {
            CharsetDetector detector = new CharsetDetector();
            detector.setText(Files.readAllBytes(Paths.get(file.getPath())));
            return Charset.forName(detector.detect().getName());
        } catch (IllegalArgumentException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
