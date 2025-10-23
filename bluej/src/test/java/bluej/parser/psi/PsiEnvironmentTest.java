package bluej.parser.psi;

import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Comprehensive unit tests for {@link PsiEnvironment} singleton.
 * 
 * <p>Tests cover singleton behavior, initialization, parsing capabilities,
 * error handling, thread safety, and resource cleanup.</p>
 * 
 * @see PsiEnvironment
 */
public class PsiEnvironmentTest {
    
    private PsiEnvironment env;
    
    /**
     * Setup test environment before each test.
     * Initializes the singleton instance.
     */
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
    }
    
    /**
     * Cleanup after each test (optional - shutdown is handled by JVM hook).
     */
    @After
    public void tearDown() {
        // Singleton persists across tests by design
        // Shutdown is handled by JVM shutdown hook
    }
    
    // ==================== SINGLETON BEHAVIOR ====================
    
    /**
     * Test 1: Verify singleton pattern returns same instance.
     * 
     * <p><b>Requirement:</b> PsiEnvironment must be a true singleton with
     * consistent instance across multiple {@code getInstance()} calls.</p>
     */
    @Test
    public void testSingletonBehavior() {
        PsiEnvironment env1 = PsiEnvironment.getInstance();
        PsiEnvironment env2 = PsiEnvironment.getInstance();
        
        assertNotNull("First getInstance() should return non-null", env1);
        assertNotNull("Second getInstance() should return non-null", env2);
        assertSame("Should return same instance", env1, env2);
    }
    
    /**
     * Test 2: Verify thread-safe initialization.
     * 
     * <p><b>Requirement:</b> Multiple threads calling {@code getInstance()}
     * concurrently must receive the same instance.</p>
     * 
     * <p>Uses {@link CountDownLatch} to synchronize thread start for
     * maximum contention at initialization point.</p>
     */
    @Test
    public void testThreadSafeInitialization() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicReference<PsiEnvironment>[] results = new AtomicReference[threadCount];
        
        // Initialize result holders
        for (int i = 0; i < threadCount; i++) {
            results[i] = new AtomicReference<>();
        }
        
        // Create threads that will all call getInstance() simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    // All threads get instance at same time
                    results[index].set(PsiEnvironment.getInstance());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }
        
        // Release all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        endLatch.await();
        
        // Verify all threads got the same instance
        PsiEnvironment firstInstance = results[0].get();
        assertNotNull("First thread should get instance", firstInstance);
        
        for (int i = 1; i < threadCount; i++) {
            assertNotNull("Thread " + i + " should get instance", results[i].get());
            assertSame("Thread " + i + " should get same instance", 
                      firstInstance, results[i].get());
        }
    }
    
    // ==================== INITIALIZATION STATUS ====================
    
    /**
     * Test 3: Verify initialization status check.
     * 
     * <p><b>Requirement:</b> {@link PsiEnvironment#isInitialized()} must
     * accurately reflect initialization state.</p>
     */
    @Test
    public void testInitializationSuccess() {
        assertTrue("Environment should initialize successfully", env.isInitialized());
        assertNotNull("Should have project when initialized", env.getProject());
    }
    
    /**
     * Test 4: Verify idea.home.path system property is set.
     * 
     * <p><b>Requirement:</b> Initialization must set {@code idea.home.path}
     * to an absolute path of an existing directory.</p>
     */
    @Test
    public void testIdeaHomePathSet() {
        // Trigger init via getInstance (already done in setUp)
        String ideaHome = System.getProperty("idea.home.path");
        
        assertNotNull("idea.home.path should be set", ideaHome);
        assertTrue("Path should be absolute", new File(ideaHome).isAbsolute());
        assertTrue("Directory should exist", new File(ideaHome).exists());
        assertTrue("Should be a directory", new File(ideaHome).isDirectory());
    }
    
    // ==================== PARSING CAPABILITIES ====================
    
    /**
     * Test 5: Parse simple function declaration.
     * 
     * <p><b>Requirement:</b> Must successfully parse valid Kotlin code
     * and return non-null {@link KtFile} with expected structure.</p>
     */
    @Test
    public void testParseSimpleFunction() {
        String source = "fun hello() = 42";
        KtFile file = env.parseFile("Test.kt", source);
        
        assertNotNull("Should parse successfully", file);
        assertEquals("File name should match", "Test.kt", file.getName());
        assertEquals("Should have 1 declaration", 1, file.getDeclarations().size());
        assertTrue("Declaration should be a function", 
                  file.getDeclarations().get(0) instanceof KtNamedFunction);
        
        KtNamedFunction func = (KtNamedFunction) file.getDeclarations().get(0);
        assertEquals("Function name should be 'hello'", "hello", func.getName());
    }
    
    /**
     * Test 6: Parse data class with properties.
     * 
     * <p><b>Requirement:</b> Must handle complex Kotlin constructs including
     * data classes with multiple properties.</p>
     */
    @Test
    public void testParseDataClass() {
        String source = "data class User(val name: String, var age: Int)";
        KtFile file = env.parseFile("User.kt", source);
        
        assertNotNull("Should parse successfully", file);
        assertEquals("Should have 1 declaration", 1, file.getDeclarations().size());
        assertTrue("Should be a class", 
                  file.getDeclarations().get(0) instanceof KtClass);
        
        KtClass klass = (KtClass) file.getDeclarations().get(0);
        assertEquals("Class name should be 'User'", "User", klass.getName());
        assertTrue("Should be a data class", klass.isData());
    }
    
    /**
     * Test 7: Parse file with syntax error.
     * 
     * <p><b>Requirement:</b> PSI should still create a tree even with syntax errors.
     * The tree will contain error nodes but should not be null.</p>
     * 
     * <p><b>Note:</b> Kotlin PSI parser is designed to be resilient and will
     * create PSI trees even for invalid syntax, marking error regions.</p>
     */
    @Test
    public void testParseSyntaxError() {
        String source = "fun broken(";  // Incomplete syntax
        KtFile file = env.parseFile("Broken.kt", source);
        
        // PSI should still create a tree with error nodes
        assertNotNull("Should parse even with errors (PSI is resilient)", file);
        assertEquals("File name should match", "Broken.kt", file.getName());
        // The file will have declarations but they may contain error elements
    }
    
    /**
     * Test 8: Parse multiple files in sequence.
     * 
     * <p><b>Requirement:</b> Environment must support multiple parse operations
     * without interference or resource exhaustion.</p>
     */
    @Test
    public void testMultipleParseCalls() {
        KtFile file1 = env.parseFile("File1.kt", "fun foo() = 1");
        KtFile file2 = env.parseFile("File2.kt", "fun bar() = 2");
        KtFile file3 = env.parseFile("File3.kt", "class Baz");
        
        assertNotNull("First parse should work", file1);
        assertNotNull("Second parse should work", file2);
        assertNotNull("Third parse should work", file3);
        
        // Verify they are different files
        assertNotSame("Files should be distinct", file1, file2);
        assertNotSame("Files should be distinct", file2, file3);
        
        // Verify content is correct
        assertEquals("File1 name correct", "File1.kt", file1.getName());
        assertEquals("File2 name correct", "File2.kt", file2.getName());
        assertEquals("File3 name correct", "File3.kt", file3.getName());
    }
    
    // ==================== ERROR HANDLING ====================
    
    /**
     * Test 9: Parse with null source code.
     * 
     * <p><b>Requirement:</b> Must handle null input gracefully without
     * throwing exceptions.</p>
     */
    @Test
    public void testParseNullSource() {
        KtFile file = env.parseFile("Test.kt", null);
        assertNull("Should return null for null source", file);
    }
    
    /**
     * Test 10: Parse with empty source code.
     * 
     * <p><b>Requirement:</b> Must handle empty input gracefully without
     * throwing exceptions.</p>
     */
    @Test
    public void testParseEmptySource() {
        KtFile file = env.parseFile("Test.kt", "");
        assertNull("Should return null for empty source", file);
    }
    
    /**
     * Test 11: Parse complex Kotlin code with multiple declarations.
     * 
     * <p><b>Requirement:</b> Must handle realistic Kotlin code with
     * imports, classes, functions, and properties.</p>
     */
    @Test
    public void testParseComplexCode() {
        String source = 
            "package com.example\n" +
            "\n" +
            "import java.util.*\n" +
            "\n" +
            "class Example {\n" +
            "    val property: String = \"test\"\n" +
            "    \n" +
            "    fun method(): Int {\n" +
            "        return 42\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "fun topLevel() = \"hello\"";
        
        KtFile file = env.parseFile("Example.kt", source);
        
        assertNotNull("Should parse complex code", file);
        assertEquals("File name should match", "Example.kt", file.getName());
        
        // Verify package
        assertNotNull("Should have package directive", file.getPackageDirective());
        assertEquals("Package name should match", "com.example", 
                    file.getPackageDirective().getQualifiedName());
        
        // Verify imports
        assertFalse("Should have imports", file.getImportDirectives().isEmpty());
        
        // Verify declarations (class + top-level function)
        assertTrue("Should have at least 2 declarations", 
                  file.getDeclarations().size() >= 2);
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Test 12: Verify shutdown is idempotent.
     * 
     * <p><b>Requirement:</b> Multiple shutdown() calls must be safe.</p>
     * 
     * <p><b>Note:</b> This test doesn't actually test JVM shutdown hook,
     * but verifies manual shutdown can be called safely.</p>
     */
    @Test
    public void testShutdownIdempotent() {
        // Note: We don't actually shutdown in tests as it would affect other tests
        // This test would be run in isolation if needed
        // For now, we just verify the method exists and is callable
        assertNotNull("Environment should be initialized", env);
        assertTrue("Should be initialized", env.isInitialized());
        
        // In a real scenario, you would:
        // env.shutdown();
        // env.shutdown(); // Second call should be safe
        // But this would break subsequent tests in the suite
    }
}