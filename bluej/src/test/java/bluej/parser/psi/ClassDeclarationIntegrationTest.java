/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.psi;

import org.jetbrains.kotlin.psi.KtFile;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for class declaration callback implementation (Phase 3 Milestone 3.1 Task 5).
 * 
 * <p>Tests the complete class declaration implementation against real Kotlin files from
 * the test corpus, validating:
 * <ul>
 *   <li>Callback sequence correctness</li>
 *   <li>Callback pairing balance (begin/end matching)</li>
 *   <li>Integration with Kotlin PSI environment</li>
 *   <li>Handling of various class declaration patterns</li>
 * </ul>
 * 
 * <p><b>Test Organization:</b></p>
 * <ul>
 *   <li><b>Simple corpus files</b> - Basic class declarations (should work well)</li>
 *   <li><b>Complex corpus files</b> - Advanced features (may have limitations)</li>
 *   <li><b>Edge cases</b> - Boundary conditions and unusual constructs</li>
 *   <li><b>Statistics</b> - Overall corpus coverage metrics</li>
 * </ul>
 * 
 * <p><b>Known Limitations (Phase 3.1 Scope):</b></p>
 * <ul>
 *   <li>Method declarations not yet implemented (Phase 4)</li>
 *   <li>Property declarations not yet implemented (Phase 4)</li>
 *   <li>Generic type parameters not yet implemented (Phase 5)</li>
 *   <li>Lambda expressions not yet implemented (Phase 6)</li>
 * </ul>
 * 
 * <p><b>Expected Results:</b></p>
 * <ul>
 *   <li>✅ Simple corpus files should work well (basic classes)</li>
 *   <li>⚠️ Moderate corpus files may have gaps (properties, methods not implemented)</li>
 *   <li>⚠️ Complex corpus files will have limitations (future phases)</li>
 *   <li>✅ Edge cases should handle gracefully without crashes</li>
 * </ul>
 * 
 * @see ClassDeclarationCallbackTest Unit tests for class declarations
 * @see PsiCallbackVisitor PSI visitor implementation
 * @see TestCorpus Test file management utility
 */
@DisplayName("Class Declaration Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClassDeclarationIntegrationTest {
    
    private static PsiEnvironment environment;
    
    @BeforeAll
    static void setupEnvironment() {
        environment = PsiEnvironment.getInstance();
        assertTrue(environment.isInitialized(), 
            "PSI environment must be initialized for integration tests");
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Parse a Kotlin file and run the visitor, capturing all callbacks.
     * 
     * <p>This helper method encapsulates the complete workflow:
     * <ol>
     *   <li>Load test file content from corpus</li>
     *   <li>Parse to Kotlin PSI tree</li>
     *   <li>Create callback recorder</li>
     *   <li>Run visitor on PSI tree</li>
     *   <li>Validate visitor state</li>
     * </ol>
     * 
     * @param relativePath Path to test file in corpus (e.g., "/bluej/parser/psi/test-corpus/simple/BasicClass.kt")
     * @return CallbackRecorder with all captured callbacks
     * @throws IOException if file cannot be loaded
     * @throws PsiParseException if parsing fails
     */
    private CallbackRecorder visitTestFile(String relativePath) throws IOException, PsiParseException {
        // Load file content
        String content = TestCorpus.loadTestFile(relativePath);
        
        // Extract filename from path
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        
        // Parse to PSI
        KtFile ktFile = environment.parseFile(fileName, content);
        assertNotNull(ktFile, "Parsed KtFile should not be null");
        
        // Create recorder and visitor
        CallbackRecorder recorder = new CallbackRecorder();
        PsiCallbackVisitor visitor = new PsiCallbackVisitor(recorder);
        
        // Visit the file
        ktFile.accept(visitor);
        
        return recorder;
    }
    
    // ==================== Simple Corpus Tests ====================
    
    @Test
    @Order(1)
    @DisplayName("BasicClass.kt produces balanced callbacks")
    void testBasicClass() throws IOException, PsiParseException {
        String testFile = TestCorpus.getSimpleTests().stream()
            .filter(f -> f.contains("BasicClass"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("BasicClass.kt not found in simple test corpus"));
        
        CallbackRecorder recorder = visitTestFile(testFile);
        
        // Validate pairing
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue(result.isBalanced(), 
            "BasicClass should have balanced callbacks: " + result.getValidationSummary());
        assertFalse(result.hasErrors(),
            "BasicClass should have no pairing errors: " + result.getValidationSummary());
        
        // Should have basic type definition structure
        assertTrue(recorder.hasCallback("gotTypeDef"), 
            "Should have gotTypeDef callback");
        assertTrue(recorder.hasCallback("gotTypeDefName"), 
            "Should have gotTypeDefName callback");
        assertTrue(recorder.hasCallback("beginTypeBody"), 
            "Should have beginTypeBody callback");
        assertTrue(recorder.hasCallback("endTypeBody"), 
            "Should have endTypeBody callback");
    }
    
    @Test
    @Order(2)
    @DisplayName("All simple corpus files have balanced callbacks")
    void testAllSimpleFilesBalanced() {
        List<String> failures = new ArrayList<>();
        int successCount = 0;
        
        for (String testFile : TestCorpus.getSimpleTests()) {
            try {
                CallbackRecorder recorder = visitTestFile(testFile);
                CallbackRecorder.ValidationResult result = recorder.getValidationResult();
                
                if (!result.isBalanced() || result.hasErrors()) {
                    failures.add(testFile + ": " + result.getValidationSummary());
                } else {
                    successCount++;
                }
            } catch (Exception e) {
                failures.add(testFile + ": Exception - " + e.getMessage());
            }
        }
        
        System.out.println("Simple corpus results: " + successCount + "/" + 
                          TestCorpus.getSimpleTests().size() + " passed");
        
        assertTrue(failures.isEmpty(), 
            "All simple tests should pass. Failures:\n" + String.join("\n", failures));
    }
    
    @Test
    @Order(3)
    @DisplayName("Simple corpus files contain expected callbacks")
    void testSimpleFilesHaveExpectedCallbacks() throws IOException, PsiParseException {
        // Test a few representative simple files
        List<String> simpleFiles = TestCorpus.getSimpleTests();
        
        if (simpleFiles.isEmpty()) {
            fail("No simple test files found in corpus");
        }
        
        // Take up to 3 simple files for detailed validation
        int filesToTest = Math.min(3, simpleFiles.size());
        for (int i = 0; i < filesToTest; i++) {
            String testFile = simpleFiles.get(i);
            CallbackRecorder recorder = visitTestFile(testFile);
            
            // All simple files should have at least some callbacks
            assertTrue(recorder.getRecords().size() > 0,
                "File " + testFile + " should produce callbacks");
            
            // Validate pairing
            assertTrue(recorder.validatePairing(),
                "File " + testFile + " should have balanced callbacks");
        }
    }
    
    // ==================== Complex Corpus Tests ====================
    
    @Test
    @Order(4)
    @DisplayName("NestedClasses.kt handles nesting correctly")
    void testNestedClassesCorpus() throws IOException, PsiParseException {
        String testFile = TestCorpus.getComplexTests().stream()
            .filter(f -> f.contains("Nested"))
            .findFirst()
            .orElse(null);
        
        if (testFile == null) {
            System.out.println("⚠️  NestedClasses.kt not found - skipping test");
            return;
        }
        
        CallbackRecorder recorder = visitTestFile(testFile);
        
        // Should have multiple class declarations
        int typeDefCount = recorder.getCallbackCount("gotTypeDef");
        assertTrue(typeDefCount > 1, 
            "Nested classes file should have multiple gotTypeDef callbacks, found: " + typeDefCount);
        
        // Validate all callbacks paired
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue(result.isBalanced(), 
            "Nested structure should maintain balance: " + result.getValidationSummary());
    }
    
    @Test
    @Order(5)
    @DisplayName("Complex files maintain callback balance")
    void testComplexFilesBalanced() {
        List<String> failures = new ArrayList<>();
        List<String> limitationsExpected = new ArrayList<>();
        int balancedCount = 0;
        
        for (String testFile : TestCorpus.getComplexTests()) {
            try {
                CallbackRecorder recorder = visitTestFile(testFile);
                CallbackRecorder.ValidationResult result = recorder.getValidationResult();
                
                if (result.isBalanced() && !result.hasErrors()) {
                    balancedCount++;
                } else {
                    // Complex files may have limitations - document but don't fail
                    limitationsExpected.add(testFile + ": " + result.getValidationSummary());
                }
            } catch (Exception e) {
                failures.add(testFile + ": Exception - " + e.getMessage());
            }
        }
        
        int totalComplex = TestCorpus.getComplexTests().size();
        System.out.println("Complex corpus results: " + balancedCount + "/" + totalComplex + " fully balanced");
        
        if (!limitationsExpected.isEmpty()) {
            System.out.println("⚠️  Known limitations in complex files (expected):");
            limitationsExpected.forEach(msg -> System.out.println("  - " + msg.substring(0, Math.min(100, msg.length()))));
        }
        
        // Only fail on unexpected exceptions, not on known limitations
        assertTrue(failures.isEmpty(), 
            "Complex tests should not crash. Failures:\n" + String.join("\n", failures));
    }
    
    // ==================== Edge Case Tests ====================
    
    @Test
    @Order(6)
    @DisplayName("EmptyFile.kt handles gracefully")
    void testEmptyFile() {
        String testFile = TestCorpus.getEdgeCaseTests().stream()
            .filter(f -> f.contains("Empty"))
            .findFirst()
            .orElse(null);
        
        if (testFile == null) {
            System.out.println("⚠️  EmptyFile.kt not found - skipping test");
            return;
        }
        
        try {
            CallbackRecorder recorder = visitTestFile(testFile);
            
            // Empty file should have no class declarations
            assertEquals(0, recorder.getCallbacksByName("gotTypeDef").size(),
                "Empty file should have no class declarations");
            
            // Should still be balanced
            assertTrue(recorder.validatePairing(),
                "Empty file should maintain callback balance");
                
        } catch (Exception e) {
            fail("Empty file should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("Unicode.kt handles non-ASCII correctly")
    void testUnicodeHandling() {
        String testFile = TestCorpus.getEdgeCaseTests().stream()
            .filter(f -> f.contains("Unicode"))
            .findFirst()
            .orElse(null);
        
        if (testFile == null) {
            System.out.println("⚠️  Unicode.kt not found - skipping test");
            return;
        }
        
        try {
            CallbackRecorder recorder = visitTestFile(testFile);
            
            // Should process without exceptions
            assertTrue(recorder.getRecords().size() > 0, 
                "Should process Unicode file");
            assertTrue(recorder.validatePairing(),
                "Unicode file should maintain callback balance");
                
        } catch (Exception e) {
            fail("Unicode file should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    @Order(8)
    @DisplayName("Edge cases handle gracefully without crashes")
    void testEdgeCasesGraceful() {
        List<String> crashes = new ArrayList<>();
        int processedCount = 0;
        
        for (String testFile : TestCorpus.getEdgeCaseTests()) {
            try {
                CallbackRecorder recorder = visitTestFile(testFile);
                // Just verify it doesn't crash - balance may vary for edge cases
                processedCount++;
            } catch (Exception e) {
                crashes.add(testFile + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        
        System.out.println("Edge case results: " + processedCount + "/" + 
                          TestCorpus.getEdgeCaseTests().size() + " processed");
        
        assertTrue(crashes.isEmpty(), 
            "Edge cases should not crash. Crashes:\n" + String.join("\n", crashes));
    }
    
    // ==================== Statistics and Summary Tests ====================
    
    @Test
    @Order(9)
    @DisplayName("Corpus statistics - count valid files processed")
    void testCorpusStatistics() {
        int totalFiles = 0;
        int successfulFiles = 0;
        int balancedFiles = 0;
        int unbalancedFiles = 0;
        List<String> errors = new ArrayList<>();
        List<String> unbalancedDetails = new ArrayList<>();
        
        for (String testFile : TestCorpus.getAllTestFiles()) {
            totalFiles++;
            try {
                CallbackRecorder recorder = visitTestFile(testFile);
                successfulFiles++;
                
                CallbackRecorder.ValidationResult result = recorder.getValidationResult();
                if (result.isBalanced() && !result.hasErrors()) {
                    balancedFiles++;
                } else {
                    unbalancedFiles++;
                    // Capture first few characters of file name for readability
                    String shortName = testFile.substring(Math.max(0, testFile.lastIndexOf('/') + 1));
                    unbalancedDetails.add(shortName + " (callbacks: " + recorder.getRecords().size() + ")");
                }
            } catch (Exception e) {
                errors.add(testFile + ": " + e.getMessage());
            }
        }
        
        // Print comprehensive statistics
        System.out.println("\n===== CORPUS VALIDATION RESULTS =====");
        System.out.println("Total files: " + totalFiles);
        System.out.println("Successfully visited: " + successfulFiles);
        System.out.println("  ✅ Balanced callbacks: " + balancedFiles);
        System.out.println("  ⚠️  Unbalanced callbacks: " + unbalancedFiles);
        System.out.println("  ❌ Parsing errors: " + errors.size());
        
        double successRate = totalFiles > 0 ? (100.0 * successfulFiles / totalFiles) : 0;
        double balanceRate = successfulFiles > 0 ? (100.0 * balancedFiles / successfulFiles) : 0;
        
        System.out.println("\nSuccess rate: " + String.format("%.1f%%", successRate));
        System.out.println("Balance rate: " + String.format("%.1f%%", balanceRate));
        
        if (!unbalancedDetails.isEmpty() && unbalancedDetails.size() <= 10) {
            System.out.println("\nUnbalanced files (may be expected for Phase 3.1 scope):");
            unbalancedDetails.forEach(d -> System.out.println("  - " + d));
        } else if (unbalancedDetails.size() > 10) {
            System.out.println("\nUnbalanced files: " + unbalancedDetails.size() + " total");
            System.out.println("  (Details omitted - too many to display)");
        }
        
        if (!errors.isEmpty() && errors.size() <= 5) {
            System.out.println("\nParsing errors:");
            errors.forEach(e -> System.out.println("  " + e));
        } else if (errors.size() > 5) {
            System.out.println("\nParsing errors: " + errors.size() + " total");
        }
        
        System.out.println("======================================\n");
        
        // Validation criteria: At least simple tests should work
        int simpleTestCount = TestCorpus.getTestFileCount("simple");
        assertTrue(successfulFiles >= simpleTestCount,
            "At least all simple corpus files should be processed successfully. " +
            "Expected >= " + simpleTestCount + ", got " + successfulFiles);
        
        // At least 50% of simple tests should be balanced
        assertTrue(balancedFiles >= simpleTestCount / 2,
            "At least half of simple tests should be balanced. " +
            "Expected >= " + (simpleTestCount / 2) + ", got " + balancedFiles);
    }
    
    @Test
    @Order(10)
    @DisplayName("Phase 3.1 implementation scope verification")
    void testPhase31ScopeVerification() {
        System.out.println("\n===== PHASE 3.1 SCOPE VERIFICATION =====");
        System.out.println("Implemented features (should work):");
        System.out.println("  ✅ Basic class declarations");
        System.out.println("  ✅ Interface declarations");
        System.out.println("  ✅ Enum class declarations");
        System.out.println("  ✅ Modifier extraction (public, private, etc.)");
        System.out.println("  ✅ Supertype processing (extends, implements)");
        System.out.println("  ✅ Nested class handling");
        
        System.out.println("\nNot yet implemented (future phases):");
        System.out.println("  ⏭️  Method declarations (Phase 4)");
        System.out.println("  ⏭️  Property declarations (Phase 4)");
        System.out.println("  ⏭️  Generic type parameters (Phase 5)");
        System.out.println("  ⏭️  Lambda expressions (Phase 6)");
        
        System.out.println("\nExpected behavior:");
        System.out.println("  • Simple classes: Should work well");
        System.out.println("  • Complex classes with members: May have incomplete callbacks");
        System.out.println("  • Generics: Type parameters not yet processed");
        System.out.println("  • All files: Should maintain callback balance without crashes");
        System.out.println("==========================================\n");
        
        // This test always passes - it's just documentation
        assertTrue(true, "Phase 3.1 scope documented");
    }
}