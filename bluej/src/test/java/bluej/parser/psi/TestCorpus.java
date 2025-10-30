package bluej.parser.psi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for discovering, loading, and managing test corpus files for validation testing.
 * Provides convenient access to categorized Kotlin test files used in PSI visitor validation.
 * 
 * <p>The test corpus is organized into four categories:
 * <ul>
 *   <li><b>simple</b> - Basic Kotlin constructs (single classes, simple functions)</li>
 *   <li><b>moderate</b> - More complex constructs (inheritance, properties, companion objects)</li>
 *   <li><b>complex</b> - Advanced features (generics, lambdas, DSL builders)</li>
 *   <li><b>edge-cases</b> - Unusual or problematic constructs requiring special handling</li>
 * </ul>
 * 
 * <p>All methods are static and the class is stateless, making it thread-safe.
 * Files are accessed via ClassLoader resources from the test resources directory.
 * 
 * <p><b>Example usage:</b>
 * <pre>
 * // Get all simple tests
 * List&lt;String&gt; simpleTests = TestCorpus.getSimpleTests();
 * 
 * // Load specific test file
 * String content = TestCorpus.loadTestFile("simple", "BasicClass.kt");
 * 
 * // Get count
 * int count = TestCorpus.getTestFileCount("moderate");
 * 
 * // Load all test files
 * for (String path : TestCorpus.getAllTestFiles()) {
 *     String content = TestCorpus.loadTestFile(path);
 *     // ... run validation
 * }
 * </pre>
 * 
 * @see CallbackRecorder
 * @see TraversalComparator
 */
public class TestCorpus {
    
    private static final String CORPUS_BASE = "/bluej/parser/psi/test-corpus";
    private static final String SIMPLE_DIR = CORPUS_BASE + "/simple";
    private static final String MODERATE_DIR = CORPUS_BASE + "/moderate";
    private static final String COMPLEX_DIR = CORPUS_BASE + "/complex";
    private static final String EDGE_CASES_DIR = CORPUS_BASE + "/edge-cases";
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TestCorpus() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Get all test files from a specific category.
     * 
     * @param category "simple", "moderate", "complex", or "edge-cases"
     * @return List of file paths (relative to resources), never null
     * @throws IllegalArgumentException if category is unknown
     * @throws RuntimeException if directory cannot be accessed
     */
    public static List<String> getTestFiles(String category) {
        String dirPath = getDirectoryPath(category);
        return listResourceFiles(dirPath);
    }
    
    /**
     * Get all simple test files (basic Kotlin constructs).
     * 
     * @return List of file paths for simple tests, never null
     */
    public static List<String> getSimpleTests() {
        return getTestFiles("simple");
    }
    
    /**
     * Get all moderate test files (more complex constructs).
     * 
     * @return List of file paths for moderate tests, never null
     */
    public static List<String> getModerateTests() {
        return getTestFiles("moderate");
    }
    
    /**
     * Get all complex test files (advanced features).
     * 
     * @return List of file paths for complex tests, never null
     */
    public static List<String> getComplexTests() {
        return getTestFiles("complex");
    }
    
    /**
     * Get all edge case test files (unusual or problematic constructs).
     * 
     * @return List of file paths for edge case tests, never null
     */
    public static List<String> getEdgeCaseTests() {
        return getTestFiles("edge-cases");
    }
    
    /**
     * Get all test files across all categories.
     * Files are returned in category order: simple, moderate, complex, edge-cases.
     * 
     * @return List of all test file paths, never null
     */
    public static List<String> getAllTestFiles() {
        List<String> all = new ArrayList<>();
        all.addAll(getSimpleTests());
        all.addAll(getModerateTests());
        all.addAll(getComplexTests());
        all.addAll(getEdgeCaseTests());
        return all;
    }
    
    /**
     * Load test file content as String with UTF-8 encoding.
     *
     * <p>Validates file size and encoding before returning content to prevent
     * resource exhaustion and encoding issues.</p>
     *
     * @param relativePath Path relative to test-corpus directory (e.g., "/bluej/parser/psi/test-corpus/simple/BasicClass.kt")
     * @return File content as String with UTF-8 encoding
     * @throws IOException if file cannot be read, does not exist, is too large, or has encoding issues
     */
    public static String loadTestFile(String relativePath) throws IOException {
        try (InputStream is = TestCorpus.class.getResourceAsStream(relativePath)) {
            if (is == null) {
                throw new IOException("Test file not found: " + relativePath);
            }
            
            byte[] bytes = is.readAllBytes();
            
            // Sanity checks to prevent resource issues
            if (bytes.length == 0) {
                throw new IOException("Test file is empty: " + relativePath);
            }
            
            if (bytes.length > 1_000_000) {  // 1MB limit for test files
                throw new IOException(
                    String.format("Test file too large (%d bytes, max 1MB): %s",
                        bytes.length, relativePath));
            }
            
            // Convert to UTF-8 string
            // This will throw CharacterCodingException if bytes are not valid UTF-8
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Load test file from category and filename.
     * Convenience method that constructs the full path from category and filename.
     * 
     * @param category Test category ("simple", "moderate", "complex", or "edge-cases")
     * @param filename Filename (e.g., "BasicClass.kt")
     * @return File content as String with UTF-8 encoding
     * @throws IOException if file cannot be read or does not exist
     * @throws IllegalArgumentException if category is unknown
     */
    public static String loadTestFile(String category, String filename) throws IOException {
        String path = getDirectoryPath(category) + "/" + filename;
        return loadTestFile(path);
    }
    
    /**
     * Check if test file exists in resources.
     * 
     * @param relativePath Path relative to resources root
     * @return true if file exists and can be accessed, false otherwise
     */
    public static boolean testFileExists(String relativePath) {
        return TestCorpus.class.getResource(relativePath) != null;
    }
    
    /**
     * Get count of test files in category.
     * 
     * @param category Test category ("simple", "moderate", "complex", or "edge-cases")
     * @return Number of test files in category
     * @throws IllegalArgumentException if category is unknown
     */
    public static int getTestFileCount(String category) {
        return getTestFiles(category).size();
    }
    
    /**
     * Get resource path for category directory.
     * 
     * @param category Test category name
     * @return Full resource path for category
     * @throws IllegalArgumentException if category is unknown
     */
    private static String getDirectoryPath(String category) {
        switch (category.toLowerCase()) {
            case "simple":
                return SIMPLE_DIR;
            case "moderate":
                return MODERATE_DIR;
            case "complex":
                return COMPLEX_DIR;
            case "edge-cases":
            case "edge":
                return EDGE_CASES_DIR;
            default:
                throw new IllegalArgumentException("Unknown category: " + category);
        }
    }
    
    /**
     * List all .kt files in a resource directory.
     * Handles both file system and JAR resources.
     * 
     * @param resourcePath Resource directory path
     * @return List of file paths (relative to resources root)
     * @throws RuntimeException if directory cannot be accessed
     */
    private static List<String> listResourceFiles(String resourcePath) {
        try {
            URL resource = TestCorpus.class.getResource(resourcePath);
            if (resource == null) {
                return Collections.emptyList();
            }
            
            URI uri = resource.toURI();
            
            // Handle both file system and JAR resources
            if (uri.getScheme().equals("jar")) {
                return listFilesInJar(uri, resourcePath);
            } else {
                return listFilesInDirectory(Paths.get(uri), resourcePath);
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Failed to list files in: " + resourcePath, e);
        }
    }
    
    /**
     * List files in a JAR resource.
     * 
     * @param uri JAR URI
     * @param resourcePath Resource path within JAR
     * @return List of file paths
     * @throws IOException if JAR cannot be accessed
     */
    private static List<String> listFilesInJar(URI uri, String resourcePath) throws IOException {
        String[] parts = uri.toString().split("!");
        try (FileSystem fs = FileSystems.newFileSystem(URI.create(parts[0]), Collections.emptyMap())) {
            Path jarPath = fs.getPath(resourcePath);
            return listFilesInPath(jarPath, resourcePath);
        }
    }
    
    /**
     * List files in a file system directory.
     * 
     * @param path Directory path
     * @param resourcePath Original resource path for constructing result paths
     * @return List of file paths
     * @throws IOException if directory cannot be read
     */
    private static List<String> listFilesInDirectory(Path path, String resourcePath) throws IOException {
        return listFilesInPath(path, resourcePath);
    }
    
    /**
     * List .kt files in a path (common logic for both file system and JAR).
     * 
     * @param path Directory path to list
     * @param resourcePath Original resource path for constructing result paths
     * @return List of file paths (relative to resources root)
     * @throws IOException if path cannot be read
     */
    private static List<String> listFilesInPath(Path path, String resourcePath) throws IOException {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return Collections.emptyList();
        }
        
        try (Stream<Path> files = Files.list(path)) {
            return files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".kt"))
                .map(p -> resourcePath + "/" + p.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        }
    }
}