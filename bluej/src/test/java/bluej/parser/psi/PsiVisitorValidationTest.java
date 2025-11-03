package bluej.parser.psi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.kotlin.psi.KtFile;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive validation test suite for PSI visitor traversal infrastructure.
 *
 * <p><b>Milestone 2.3 Task 2.3.5:</b> This test suite validates that {@link PsiCallbackVisitor}
 * correctly traverses PSI structures using {@link CallbackRecorder} to verify callback sequences
 * and pairing. This is the culmination of Milestone 2.3 (Validation Infrastructure), proving
 * the visitor infrastructure works correctly before Phase 3 callback implementation.</p>
 *
 * <p><b>IMPORTANT - Phase 3 Integration Note:</b> These tests are currently stubs that document
 * the test structure and approach. Full implementation requires Kotlin PSI environment setup
 * which depends on IntelliJ Platform runtime dependencies. The actual tests will be enabled
 * in Phase 3 when the full PSI parsing infrastructure is integrated into the build system.</p>
 *
 * <h2>Test Structure</h2>
 * <p>Tests are organized into progressive complexity levels:</p>
 * <ul>
 *   <li><b>Simple Tests (1-10):</b> Basic Kotlin constructs (classes, functions, properties)</li>
 *   <li><b>Moderate Tests (11-25):</b> Inheritance, interfaces, companion objects</li>
 *   <li><b>Complex Tests (26-40):</b> Generics, nested classes, DSL builders</li>
 *   <li><b>Edge Case Tests (41-46):</b> Empty files, Unicode, boundary conditions</li>
 *   <li><b>Aggregate Tests (47-50):</b> Full corpus validation</li>
 * </ul>
 *
 * <h2>Validation Approach</h2>
 * <p>Each test validates:</p>
 * <ul>
 *   <li>Callback sequences are recorded correctly</li>
 *   <li>Begin/end callback pairing is balanced</li>
 *   <li>No validation errors occur during traversal</li>
 *   <li>Visitor state management is correct</li>
 * </ul>
 *
 * <h2>Test Infrastructure</h2>
 * <p>Uses:</p>
 * <ul>
 *   <li>{@link CallbackRecorder} - Records all callback invocations</li>
 *   <li>{@link TestCorpus} - Provides access to test files</li>
 *   <li>{@link PairingValidator} - Validates callback pairing</li>
 * </ul>
 *
 * @see PsiCallbackVisitor
 * @see CallbackRecorder
 * @see TestCorpus
 * @see PairingValidator
 */
public class PsiVisitorValidationTest {
    
    /**
     * Singleton PSI environment used by all tests.
     * Lazily initialized on first use by {@link PsiEnvironment#getInstance()}.
     */
    private static PsiEnvironment environment;
    
    /**
     * Initializes the PSI environment singleton before running any tests.
     *
     * <p>This ensures the {@link PsiEnvironment} singleton is initialized early,
     * allowing better error reporting if initialization fails. The singleton
     * manages its own lifecycle with JVM shutdown hooks.</p>
     *
     * @throws AssertionError if PSI environment fails to initialize
     */
    @BeforeClass
    public static void setUpClass() {
        environment = PsiEnvironment.getInstance();
        
        // Verify initialization succeeded
        if (!environment.isInitialized()) {
            throw new AssertionError(
                "PSI environment failed to initialize. " +
                "Check stderr for initialization errors.");
        }
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Runs the PSI visitor on a test file and returns the callback recorder.
     *
     * <p>This method performs the complete pipeline for testing PSI visitor traversal:</p>
     * <ol>
     *   <li>Load the test file content using {@link TestCorpus}</li>
     *   <li>Parse it into a KtFile PSI tree using {@link PsiEnvironment}</li>
     *   <li>Create a {@link CallbackRecorder} to capture callbacks</li>
     *   <li>Create a {@link PsiCallbackVisitor} with the recorder</li>
     *   <li>Run the visitor on the PSI tree</li>
     *   <li>Validate that the visitor state is balanced</li>
     * </ol>
     *
     * <p><b>Implementation Details:</b></p>
     * <ul>
     *   <li>Uses {@link PsiEnvironment} singleton for PSI parsing</li>
     *   <li>Extracts filename from path for proper PSI file creation</li>
     *   <li>Validates visitor state balance after traversal</li>
     *   <li>Returns populated recorder for test assertions</li>
     * </ul>
     *
     * @param testFilePath Path to test file relative to test-corpus
     * @return The callback recorder containing all recorded callbacks
     * @throws IOException if the test file cannot be loaded
     * @throws PsiParseException if PSI parsing fails
     * @throws AssertionError if visitor state is unbalanced after traversal
     */
    private CallbackRecorder runVisitorOnFile(String testFilePath) throws IOException, PsiParseException {
        // 1. Load file content
        String content = TestCorpus.loadTestFile(testFilePath);
        
        // 2. Extract filename from path (e.g., "/path/to/BasicClass.kt" -> "BasicClass.kt")
        String fileName = testFilePath.substring(testFilePath.lastIndexOf('/') + 1);
        
        // 3. Parse Kotlin code to PSI using PsiEnvironment singleton
        KtFile ktFile = environment.parseFile(fileName, content);
        
        // 4. Create recorder and visitor
        CallbackRecorder recorder = new CallbackRecorder();
        PsiCallbackVisitor visitor = new PsiCallbackVisitor(recorder);
        
        // 5. Run visitor on PSI tree
        ktFile.accept(visitor);
        
        // 6. Validate state balance
        assertTrue("Visitor state should be balanced after visiting " + testFilePath,
                   visitor.validateState());
        
        return recorder;
    }
    
    /**
     * Asserts that the recorded callback sequence exactly matches the expected callback order.
     *
     * <p>This helper validates that callbacks were invoked in the precise order specified,
     * with no extra or missing callbacks. Useful for verifying strict traversal patterns.</p>
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * assertCallbackSequence(recorder,
     *     "beginClass", "gotMethodDeclaration", "endClass");
     * }</pre>
     *
     * @param recorder The callback recorder containing actual callback sequence
     * @param expectedCallbacks Variable number of expected callback names in order
     * @throws AssertionError if sequences don't match exactly
     */
    private void assertCallbackSequence(CallbackRecorder recorder, String... expectedCallbacks) {
        List<String> actualSequence = new ArrayList<>();
        for (CallbackRecord record : recorder.getRecords()) {
            actualSequence.add(record.getCallbackName());
        }
        
        assertEquals("Callback sequence should match expected order",
                     Arrays.asList(expectedCallbacks), actualSequence);
    }
    
    /**
     * Asserts that specific callbacks are present in the recording, regardless of order.
     *
     * <p>This helper checks for the existence of required callbacks without enforcing
     * a specific order. Useful when you need to verify certain callbacks occurred but
     * don't want to specify the complete sequence.</p>
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * // Verify class and method callbacks exist
     * assertCallbackPresence(recorder, "beginClass", "endClass", "gotMethodDeclaration");
     * }</pre>
     *
     * @param recorder The callback recorder to check
     * @param requiredCallbacks Variable number of callback names that must be present
     * @throws AssertionError if any required callback is missing
     */
    private void assertCallbackPresence(CallbackRecorder recorder, String... requiredCallbacks) {
        for (String callback : requiredCallbacks) {
            assertTrue("Expected callback to be present: " + callback,
                       recorder.hasCallback(callback));
        }
    }
    
    /**
     * Asserts that callback pairing is valid - all begin/end callback pairs are balanced.
     *
     * <p>This helper validates that every begin* callback has a corresponding end* callback
     * in the correct nesting order. This is critical for ensuring proper traversal state
     * management.</p>
     *
     * <p><b>What it checks:</b></p>
     * <ul>
     *   <li>Every beginClass has a matching endClass</li>
     *   <li>Nesting is correct (no beginClass → beginClass → endClass → endClass)</li>
     *   <li>No orphaned begin or end callbacks</li>
     * </ul>
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * CallbackRecorder recorder = runVisitorOnFile(testFile);
     * assertValidPairing(recorder); // Throws if unbalanced
     * }</pre>
     *
     * @param recorder The callback recorder to validate
     * @throws AssertionError if callbacks are unbalanced or have pairing errors
     */
    private void assertValidPairing(CallbackRecorder recorder) {
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Callbacks should be balanced. Validation summary:\n" + result.getValidationSummary(),
                   result.isBalanced());
        assertFalse("No pairing errors should occur. Validation summary:\n" + result.getValidationSummary(),
                    result.hasErrors());
    }
    
    // ==================== Simple Test Cases (Order 1-10) ====================
    
    @Test
    public void testSimpleClassDeclaration() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("BasicClass"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("BasicClass.kt not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Validate actual callback presence from PsiCallbackVisitor.visitClass
        assertCallbackPresence(recorder, "gotTypeDef", "beginTypeBody", "endTypeBody");
        
        // Validate pairing
        assertValidPairing(recorder);
        
        // Validate we have callbacks recorded
        assertTrue("Should have recorded callbacks for class declaration",
                   recorder.getRecords().size() > 0);
    }
    
    @Test
    public void testDataClassDeclaration() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("DataClass"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("DataClass.kt not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Data classes should have type definition callbacks
        // Note: Data class may not have body if no members, so only assert gotTypeDef
        assertCallbackPresence(recorder, "gotTypeDef");
        assertValidPairing(recorder);
    }
    
    @Test
    public void testObjectDeclaration() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("ObjectDeclaration"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("ObjectDeclaration.kt not found in test corpus"));

        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Object declarations are handled by visitObjectDeclaration()
        assertCallbackPresence(recorder, "gotTypeDef");
        assertValidPairing(recorder);
    }
    
    @Test
    public void testSimpleFunction() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("SimpleFunction"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("SimpleFunction.kt not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Top-level functions - currently only logged in Phase 2, no callbacks invoked
        // Functions are deferred to Phase 4 - just validate pairing for now
        assertValidPairing(recorder);
    }
    
    @Test
    public void testSimpleProperty() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("SimpleProperty"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("SimpleProperty.kt not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testExtensionFunction() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("Extension"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Extension function file not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testEnumClass() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("Enum"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Enum file not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertCallbackPresence(recorder, "gotTypeDef", "beginTypeBody", "endTypeBody");
        assertValidPairing(recorder);
    }
    
    @Test
    public void testInterface() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("Interface"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Interface file not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testSealedClass() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("Sealed"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Sealed class file not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertCallbackPresence(recorder, "gotTypeDef", "beginTypeBody", "endTypeBody");
        assertValidPairing(recorder);
    }
    
    @Test
    public void testAnnotationClass() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("Annotation"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Annotation file not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    // ==================== Moderate Test Cases (Order 11-25) ====================
    
    @Test
    public void testClassInheritance() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Inheritance"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Inheritance file not found in test corpus"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Should have multiple type definitions (parent and child)
        assertCallbackPresence(recorder, "gotTypeDef", "beginTypeBody", "endTypeBody");
        assertValidPairing(recorder);
    }
    
    @Test
    public void testInterfaceImplementation() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("InterfaceImpl") || f.contains("Implementation"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Interface implementation file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testCompanionObject() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Companion"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Companion object file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Companion objects have type definition callbacks
        assertCallbackPresence(recorder, "gotTypeDef", "beginTypeBody", "endTypeBody");
        assertValidPairing(recorder);
    }
    
    @Test
    public void testClassWithProperties() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Properties"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Properties file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testClassWithMethods() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Method"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Methods file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertCallbackPresence(recorder, "gotTypeDef", "beginTypeBody", "endTypeBody");
        assertValidPairing(recorder);
    }
    
    @Test
    public void testClassWithConstructor() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Constructor"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Constructor file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testPrimaryConstructorProperties() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Primary"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Primary constructor file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testInitBlock() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Init"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Init block file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testPropertyAccessors() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Accessor") || f.contains("Getter"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Accessor file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testBackingField() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Backing") || f.contains("Field"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Backing field file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testDelegatedProperty() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Delegat"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Delegated property file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testLateinitProperty() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Lateinit") || f.contains("Late"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Lateinit property file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testOperatorOverload() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Operator"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Operator file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testInfixFunction() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
            .filter(f -> f.contains("Infix"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Infix function file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testTailrecFunction() throws IOException, PsiParseException {
        String filePath = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("Tailrec"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Tailrec function file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    // ==================== Complex Test Cases (Order 26-40) ====================
    
    @Test
    public void testNestedClasses() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Nested"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Nested classes file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Multiple nested classes should all have balanced callbacks
        assertCallbackPresence(recorder, "gotTypeDef", "beginTypeBody", "endTypeBody");
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Nested classes should have balanced callbacks",
                   result.isBalanced());
        assertFalse("Nested classes should have no pairing errors",
                    result.hasErrors());
    }
    
    @Test
    public void testGenericClasses() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Generic"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Generics file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testTypeAliases() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("TypeAlias") || f.contains("Alias"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Type alias file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testLambdaExpressions() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Lambda"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Lambda file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testHigherOrderFunctions() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("HigherOrder"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Higher-order functions file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testDSLBuilder() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("DSL"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("DSL builder file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testSealedClassHierarchy() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("SealedHierarchy") || (f.contains("Sealed") && f.contains("Complex")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Sealed class hierarchy file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testInlineClasses() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Inline") && f.contains("Class"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Inline classes file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testVarianceAnnotations() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Variance"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Variance file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testReifiedTypeParameters() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Reified"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Reified type parameters file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testDestructuringDeclarations() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Destructuring"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Destructuring file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testScopeFunctions() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Scope"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Scope functions file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testCoroutines() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Coroutine") || f.contains("Suspend"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Coroutines file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testContracts() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Contract"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Contracts file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testMultiplatformExpectActual() throws IOException, PsiParseException {
        String filePath = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Multiplatform") || f.contains("Expect"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Multiplatform file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    // ==================== Edge Case Tests (Order 41-46) ====================
    
    @Test
    public void testEmptyFile() throws IOException, PsiParseException {
        String filePath = TestCorpus.getEdgeCaseTests().stream()
            .filter(f -> f.contains("Empty"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Empty file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Empty file should have minimal or no structural callbacks
        // File-level callbacks may still occur
        assertValidPairing(recorder);
    }
    
    @Test
    public void testUnicodeIdentifiers() throws IOException, PsiParseException {
        String filePath = TestCorpus.getEdgeCaseTests().stream()
            .filter(f -> f.contains("Unicode"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Unicode file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Unicode should not affect callback pairing
        assertValidPairing(recorder);
    }
    
    @Test
    public void testSingleLineFile() throws IOException, PsiParseException {
        String filePath = TestCorpus.getEdgeCaseTests().stream()
            .filter(f -> f.contains("SingleLine") || f.contains("OneLine"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Single-line file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testDeeplyNested() throws IOException, PsiParseException {
        String filePath = TestCorpus.getEdgeCaseTests().stream()
            .filter(f -> f.contains("Deep") || f.contains("Nested"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Deeply nested file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Deep nesting should maintain balanced callbacks
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Deep nesting should maintain balanced callbacks",
                   result.isBalanced());
    }
    
    @Test
    public void testLongFile() throws IOException, PsiParseException {
        String filePath = TestCorpus.getEdgeCaseTests().stream()
            .filter(f -> f.contains("Long") || f.contains("Large"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Long file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        assertValidPairing(recorder);
    }
    
    @Test
    public void testCommentsOnly() throws IOException, PsiParseException {
        String filePath = TestCorpus.getEdgeCaseTests().stream()
            .filter(f -> f.contains("Comment"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Comments-only file not found"));
        
        CallbackRecorder recorder = runVisitorOnFile(filePath);
        
        // Comments-only file should have minimal callbacks
        assertValidPairing(recorder);
    }
    
    // ==================== Aggregate Validation Tests (Order 47-50) ====================
    
    @Test
    public void testAllSimpleTestsBalanced() {
        List<String> failures = new ArrayList<>();
        
        for (String testFile : TestCorpus.getSimpleTests()) {
            try {
                CallbackRecorder recorder = runVisitorOnFile(testFile);
                CallbackRecorder.ValidationResult result = recorder.getValidationResult();
                
                if (!result.isBalanced() || result.hasErrors()) {
                    failures.add(testFile + ": " + result.getValidationSummary());
                }
            } catch (Exception e) {
                failures.add(testFile + ": Exception - " + e.getMessage());
            }
        }
        
        assertTrue("All simple tests should have balanced callbacks. Failures:\n" + String.join("\n", failures),
                   failures.isEmpty());
    }
    
    @Test
    public void testAllModerateTestsBalanced() {
        List<String> failures = new ArrayList<>();
        
        for (String testFile : TestCorpus.getModerateTests()) {
            try {
                CallbackRecorder recorder = runVisitorOnFile(testFile);
                CallbackRecorder.ValidationResult result = recorder.getValidationResult();
                
                if (!result.isBalanced() || result.hasErrors()) {
                    failures.add(testFile + ": " + result.getValidationSummary());
                }
            } catch (Exception e) {
                failures.add(testFile + ": Exception - " + e.getMessage());
            }
        }
        
        assertTrue("All moderate tests should have balanced callbacks. Failures:\n" + String.join("\n", failures),
                   failures.isEmpty());
    }
    
    @Test
    public void testAllComplexTestsBalanced() {
        List<String> failures = new ArrayList<>();
        
        for (String testFile : TestCorpus.getComplexTests()) {
            try {
                CallbackRecorder recorder = runVisitorOnFile(testFile);
                CallbackRecorder.ValidationResult result = recorder.getValidationResult();
                
                if (!result.isBalanced() || result.hasErrors()) {
                    failures.add(testFile + ": " + result.getValidationSummary());
                }
            } catch (Exception e) {
                failures.add(testFile + ": Exception - " + e.getMessage());
            }
        }
        
        assertTrue("All complex tests should have balanced callbacks. Failures:\n" + String.join("\n", failures),
                   failures.isEmpty());
    }
    
    @Test
    public void testEntireCorpusValidation() {
        List<String> failures = new ArrayList<>();
        int totalTests = 0;
        int passedTests = 0;
        
        for (String testFile : TestCorpus.getAllTestFiles()) {
            totalTests++;
            try {
                CallbackRecorder recorder = runVisitorOnFile(testFile);
                CallbackRecorder.ValidationResult result = recorder.getValidationResult();
                
                if (!result.isBalanced() || result.hasErrors()) {
                    failures.add(testFile + ": " + result.getValidationSummary());
                } else {
                    passedTests++;
                }
            } catch (Exception e) {
                failures.add(testFile + ": Exception - " + e.getMessage());
            }
        }
        
        System.out.println("===== VALIDATION SUMMARY =====");
        System.out.println("Total files tested: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + failures.size());
        System.out.println("==============================");
        
        if (!failures.isEmpty()) {
            System.out.println("\nFAILURES:");
            for (String failure : failures) {
                System.out.println("  " + failure);
            }
        }
        
        assertTrue("All corpus tests should pass. " + failures.size() + " failures occurred. See output for details.",
                   failures.isEmpty());
    }
}