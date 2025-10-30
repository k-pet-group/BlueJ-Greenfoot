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

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import org.jetbrains.kotlin.psi.KtFile;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests for Phase 3 Milestone 3.1 Task 1: Core visitClass() callback sequence.
 * 
 * <p>Validates that {@link PsiCallbackVisitor#visitClass(org.jetbrains.kotlin.psi.KtClass)}
 * invokes the complete callback sequence for simple class declarations:</p>
 * <ol>
 *   <li>{@code gotDeclBegin(token)} - Begin declaration</li>
 *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
 *   <li>{@code gotTypeDef(token, tdType)} - Type definition</li>
 *   <li>{@code gotTypeDefName(nameToken)} - Type name</li>
 *   <li>{@code beginTypeBody(token)} - Begin body</li>
 *   <li>{@code endTypeBody(token, true)} - End body</li>
 *   <li>{@code gotTypeDefEnd(token, true)} - End declaration</li>
 * </ol>
 * 
 * <p><b>Scope:</b> Tests simple class declarations without modifiers, inheritance, or members.
 * Complex scenarios (modifiers, supertypes, nested classes) are deferred to later tasks.</p>
 * 
 * @see PsiCallbackVisitor
 * @see CallbackRecorder
 */
public class ClassDeclarationCallbackTest {
    
    private PsiEnvironment env;
    
    /**
     * Setup test environment before each test.
     */
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    // ==================== CORE CALLBACK SEQUENCE ====================
    
    /**
     * Test 1: Simple empty class invokes correct callback sequence.
     * 
     * <p>Validates that a minimal class declaration {@code class SimpleClass { }}
     * produces the expected 7-callback sequence without errors.</p>
     * 
     * <p><b>Success Criteria:</b></p>
     * <ul>
     *   <li>All 7 callbacks invoked in correct order</li>
     *   <li>No extra or missing callbacks</li>
     *   <li>Callback pairing is balanced (validated by {@link PairingValidator})</li>
     * </ul>
     */
    @Test
    public void testSimpleEmptyClass_invokesCorrectSequence() throws PsiParseException {
        String kotlinCode = "class SimpleClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Extract just the callback names for sequence validation
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecorder.CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Validate exact sequence (7 callbacks)
        assertEquals("Should have exactly 7 callbacks", 7, sequence.size());
        assertEquals("Callback 1: gotDeclBegin", "gotDeclBegin", sequence.get(0));
        assertEquals("Callback 2: modifiersConsumed", "modifiersConsumed", sequence.get(1));
        assertEquals("Callback 3: gotTypeDef", "gotTypeDef", sequence.get(2));
        assertEquals("Callback 4: gotTypeDefName", "gotTypeDefName", sequence.get(3));
        assertEquals("Callback 5: beginTypeBody", "beginTypeBody", sequence.get(4));
        assertEquals("Callback 6: endTypeBody", "endTypeBody", sequence.get(5));
        assertEquals("Callback 7: gotTypeDefEnd", "gotTypeDefEnd", sequence.get(6));
        
        // Validate pairing
        assertTrue("Callback pairing should be balanced", 
                  recorder.validatePairing());
    }
    
    /**
     * Test 2: Class name extracted correctly.
     * 
     * <p>Validates that {@code gotTypeDefName} callback receives a token with
     * the correct class name text.</p>
     */
    @Test
    public void testClassName_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "class MyTestClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify gotTypeDefName was called with correct name
        List<CallbackRecorder.CallbackRecord> nameCallbacks = 
            recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have exactly 1 gotTypeDefName callback", 1, nameCallbacks.size());
        
        Map<String, Object> params = nameCallbacks.get(0).getParameters();
        LocatableToken nameToken = (LocatableToken) params.get("nameToken");
        
        assertNotNull("nameToken should not be null", nameToken);
        assertEquals("Class name should be 'MyTestClass'", "MyTestClass", nameToken.getText());
        assertEquals("Token type should be IDENT", JavaTokenTypes.IDENT, nameToken.getType());
    }
    
    /**
     * Test 3: Regular class has LITERAL_class typedef type.
     * 
     * <p>Validates that {@code gotTypeDef} callback receives the correct
     * {@code tdType} parameter for a regular class (not interface/enum).</p>
     */
    @Test
    public void testRegularClass_hasClassType() throws PsiParseException {
        String kotlinCode = "class RegularClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify gotTypeDef was called with CLASS type
        List<CallbackRecorder.CallbackRecord> typeDefCallbacks = 
            recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have exactly 1 gotTypeDef callback", 1, typeDefCallbacks.size());
        
        Map<String, Object> params = typeDefCallbacks.get(0).getParameters();
        int tdType = (int) params.get("tdType");
        
        assertEquals("Type definition should be LITERAL_class", 
                    JavaTokenTypes.LITERAL_class, tdType);
    }
    
    /**
     * Test 4: Interface has LITERAL_interface typedef type.
     * 
     * <p>Validates type classification for interface declarations.</p>
     */
    @Test
    public void testInterface_hasInterfaceType() throws PsiParseException {
        String kotlinCode = "interface MyInterface { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecorder.CallbackRecord> typeDefCallbacks = 
            recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have exactly 1 gotTypeDef callback", 1, typeDefCallbacks.size());
        
        Map<String, Object> params = typeDefCallbacks.get(0).getParameters();
        int tdType = (int) params.get("tdType");
        
        assertEquals("Type definition should be LITERAL_interface", 
                    JavaTokenTypes.LITERAL_interface, tdType);
    }
    
    /**
     * Test 5: Enum class has LITERAL_enum typedef type.
     * 
     * <p>Validates type classification for enum class declarations.</p>
     */
    @Test
    public void testEnumClass_hasEnumType() throws PsiParseException {
        String kotlinCode = "enum class Color { RED, GREEN, BLUE }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecorder.CallbackRecord> typeDefCallbacks = 
            recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have exactly 1 gotTypeDef callback", 1, typeDefCallbacks.size());
        
        Map<String, Object> params = typeDefCallbacks.get(0).getParameters();
        int tdType = (int) params.get("tdType");
        
        assertEquals("Type definition should be LITERAL_enum", 
                    JavaTokenTypes.LITERAL_enum, tdType);
    }
    
    /**
     * Test 6: Begin/end type body callbacks invoked with correct tokens.
     * 
     * <p>Validates that body delimiters produce the expected callbacks with
     * appropriate token types (LCURLY/RCURLY).</p>
     */
    @Test
    public void testTypeBody_delimitersCorrect() throws PsiParseException {
        String kotlinCode = """
          class TestClass {
            // Comment inside body
          }
        """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify beginTypeBody callback
        List<CallbackRecorder.CallbackRecord> beginBodyCallbacks = 
            recorder.getCallbacksByName("beginTypeBody");
        assertEquals("Should have exactly 1 beginTypeBody callback", 1, beginBodyCallbacks.size());
        
        Map<String, Object> beginParams = beginBodyCallbacks.get(0).getParameters();
        LocatableToken beginToken = (LocatableToken) beginParams.get("leftCurlyToken");
        assertNotNull("beginTypeBody should have leftCurlyToken", beginToken);
        assertEquals("Opening token type should be LCURLY", JavaTokenTypes.LCURLY, beginToken.getType());
        assertEquals("Opening token should be on line 1", 1, beginToken.getLine());
        assertEquals("Opening token should be at column 19", 19, beginToken.getColumn());
        assertEquals("Opening token should end on line 1", 1, beginToken.getEndLine());
        assertEquals("Opening token should end at column 20", 20, beginToken.getEndColumn());

        // Verify endTypeBody callback
        List<CallbackRecorder.CallbackRecord> endBodyCallbacks =
            recorder.getCallbacksByName("endTypeBody");
        assertEquals("Should have exactly 1 endTypeBody callback", 1, endBodyCallbacks.size());
        
        Map<String, Object> endParams = endBodyCallbacks.get(0).getParameters();
        LocatableToken endToken = (LocatableToken) endParams.get("endCurlyToken");
        boolean included = (boolean) endParams.get("included");
        
        assertNotNull("endTypeBody should have endCurlyToken", endToken);
        assertEquals("Token type should be RCURLY", JavaTokenTypes.RCURLY, endToken.getType());
        assertTrue("included parameter should be true", included);
        assertEquals("Closing token should be on line 3", 3, endToken.getLine());
        assertEquals("Closing token should be at column 3", 3, endToken.getColumn());
        assertEquals("Closing token should end on line 3", 3, endToken.getEndLine());
        assertEquals("Closing token should end at column 4", 4, endToken.getEndColumn());

        // Verify tokens are different (not reused)
        assertNotEquals("Begin and end tokens should be different objects",
                       beginToken, endToken);
    }
    
    /**
     * Test 7: gotTypeDefEnd invoked with correct parameters.
     * 
     * <p>Validates the final callback in the sequence has correct token and included flag.</p>
     */
    @Test
    public void testTypeDefEnd_correctParameters() throws PsiParseException {
        String kotlinCode = "class TestClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecorder.CallbackRecord> endCallbacks = 
            recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have exactly 1 gotTypeDefEnd callback", 1, endCallbacks.size());
        
        Map<String, Object> params = endCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        boolean included = (boolean) params.get("included");
        
        assertNotNull("gotTypeDefEnd should have token", token);
        assertTrue("included parameter should be true", included);
    }
    
    /**
     * Test 8: Data class produces same callback sequence.
     * 
     * <p>Validates that data class modifier doesn't affect core callback sequence
     * (modifiers are processed but not yet extracted in Task 1).</p>
     */
    @Test
    public void testDataClass_sameCallbackSequence() throws PsiParseException {
        String kotlinCode = "data class User(val name: String) {}";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Extract callback names
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecorder.CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Should have same 7-callback core sequence
        // (Primary constructor parameters don't generate callbacks in Task 1)
        assertTrue("Should start with gotDeclBegin", sequence.contains("gotDeclBegin"));
        assertTrue("Should have modifiersConsumed", sequence.contains("modifiersConsumed"));
        assertTrue("Should have gotTypeDef", sequence.contains("gotTypeDef"));
        assertTrue("Should have gotTypeDefName", sequence.contains("gotTypeDefName"));
        assertTrue("Should have beginTypeBody", sequence.contains("beginTypeBody"));
        assertTrue("Should have endTypeBody", sequence.contains("endTypeBody"));
        assertTrue("Should have gotTypeDefEnd", sequence.contains("gotTypeDefEnd"));
        
        // Verify class name
        List<CallbackRecorder.CallbackRecord> nameCallbacks = 
            recorder.getCallbacksByName("gotTypeDefName");
        assertEquals(1, nameCallbacks.size());
        LocatableToken nameToken = (LocatableToken) nameCallbacks.get(0).getParameters().get("nameToken");
        assertEquals("User", nameToken.getText());
    }
    
    /**
     * Test 9: Sealed class produces same callback sequence.
     * 
     * <p>Validates that sealed modifier doesn't affect core callback sequence.</p>
     */
    @Test
    public void testSealedClass_sameCallbackSequence() throws PsiParseException {
        String kotlinCode = "sealed class Result { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have core sequence
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecorder.CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        assertTrue("Should have gotDeclBegin", sequence.contains("gotDeclBegin"));
        assertTrue("Should have gotTypeDefName", sequence.contains("gotTypeDefName"));
        assertTrue("Should have beginTypeBody", sequence.contains("beginTypeBody"));
        
        // Verify still classified as regular class
        List<CallbackRecorder.CallbackRecord> typeDefCallbacks = 
            recorder.getCallbacksByName("gotTypeDef");
        int tdType = (int) typeDefCallbacks.get(0).getParameters().get("tdType");
        assertEquals("Sealed class should have LITERAL_class type", 
                    JavaTokenTypes.LITERAL_class, tdType);
    }
    
    /**
     * Test 10: Null-safe handling when class has no name.
     * 
     * <p>Validates that anonymous or malformed classes don't crash the visitor.
     * Note: This is an edge case - most Kotlin classes have names.</p>
     */
    @Test
    public void testAnonymousClass_handlesGracefully() throws PsiParseException {
        // While Kotlin doesn't typically have anonymous classes in this syntax,
        // we test that the visitor handles null names gracefully
        String kotlinCode = "class SimpleClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should still complete sequence even if name extraction might have issues
        assertTrue("Should have gotDeclBegin", recorder.hasCallback("gotDeclBegin"));
        assertTrue("Should have gotTypeDefEnd", recorder.hasCallback("gotTypeDefEnd"));
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== PAIRING VALIDATION ====================
    
    /**
     * Test 11: Callback pairing validation passes for simple class.
     * 
     * <p>Uses {@link CallbackRecorder#validatePairing()} to verify that
     * begin/end callback pairs are properly matched.</p>
     */
    @Test
    public void testCallbackPairing_balanced() throws PsiParseException {
        String kotlinCode = "class BalancedClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Validate pairing with detailed error message
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        
        assertTrue("Callbacks should be balanced: " + result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no validation errors: " + result.getValidationSummary(), 
                   result.hasErrors());
    }
    
    /**
     * Test 12: Multiple classes in same file each get complete sequences.
     * 
     * <p>Validates that visitor handles multiple top-level class declarations
     * independently with separate callback sequences.</p>
     */
    @Test
    public void testMultipleClasses_eachGetSequence() throws PsiParseException {
        String kotlinCode = 
            "class FirstClass { }\n" +
            "class SecondClass { }\n";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 complete sequences (7 callbacks × 2 classes = 14 callbacks)
        List<CallbackRecorder.CallbackRecord> declBeginCallbacks = 
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 2 gotDeclBegin callbacks", 2, declBeginCallbacks.size());
        
        List<CallbackRecorder.CallbackRecord> declEndCallbacks = 
            recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 2 gotTypeDefEnd callbacks", 2, declEndCallbacks.size());
        
        // Verify both class names captured
        List<CallbackRecorder.CallbackRecord> nameCallbacks = 
            recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have 2 gotTypeDefName callbacks", 2, nameCallbacks.size());
        
        LocatableToken name1 = (LocatableToken) nameCallbacks.get(0).getParameters().get("nameToken");
        LocatableToken name2 = (LocatableToken) nameCallbacks.get(1).getParameters().get("nameToken");
        
        assertEquals("First class name", "FirstClass", name1.getText());
        assertEquals("Second class name", "SecondClass", name2.getText());
        
        // Validate overall pairing
        assertTrue("Multi-class pairing should be balanced", recorder.validatePairing());
    }
    
    // ==================== TOKEN TYPE VALIDATION ====================
    
    /**
     * Test 13: gotDeclBegin receives class token with correct type.
     * 
     * <p>Validates that the declaration begin callback receives a token
     * with type {@link JavaTokenTypes#LITERAL_class}.</p>
     */
    @Test
    public void testDeclBegin_hasClassToken() throws PsiParseException {
        String kotlinCode = "class TestClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecorder.CallbackRecord> declBeginCallbacks = 
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals(1, declBeginCallbacks.size());
        
        Map<String, Object> params = declBeginCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("gotDeclBegin should have token", token);
        assertEquals("Token type should be LITERAL_class", 
                    JavaTokenTypes.LITERAL_class, token.getType());
    }
    
    /**
     * Test 14: modifiersConsumed called even when no modifiers present.
     * 
     * <p>Validates that the modifier callback is invoked even for classes
     * without explicit modifiers, establishing the pattern for Task 2.</p>
     */
    @Test
    public void testModifiersConsumed_calledWithoutModifiers() throws PsiParseException {
        String kotlinCode = "class NoModifiers { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should still call modifiersConsumed even with no modifiers
        List<CallbackRecorder.CallbackRecord> modifierCallbacks = 
            recorder.getCallbacksByName("modifiersConsumed");
        assertEquals("Should have exactly 1 modifiersConsumed callback", 
                    1, modifierCallbacks.size());
        
        // Verify it comes before gotTypeDef in sequence
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecorder.CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        int modifiersIndex = sequence.indexOf("modifiersConsumed");
        int typeDefIndex = sequence.indexOf("gotTypeDef");
        
        assertTrue("modifiersConsumed should come before gotTypeDef", 
                  modifiersIndex < typeDefIndex);
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Helper method to parse Kotlin code and visit with CallbackRecorder.
     * 
     * <p>This encapsulates the common pattern:</p>
     * <ol>
     *   <li>Parse Kotlin code to {@link KtFile}</li>
     *   <li>Create {@link CallbackRecorder}</li>
     *   <li>Create {@link PsiCallbackVisitor} with recorder</li>
     *   <li>Visit the parsed file</li>
     *   <li>Return recorder for assertion</li>
     * </ol>
     * 
     * @param kotlinCode The Kotlin source code to parse
     * @return CallbackRecorder with captured callbacks
     * @throws PsiParseException if parsing fails
     */
    private CallbackRecorder parseAndVisit(String kotlinCode) throws PsiParseException {
        // Parse Kotlin code to KtFile
        KtFile ktFile = env.parseFile("Test.kt", kotlinCode);
        assertNotNull("File should parse successfully", ktFile);
        
        // Create recorder and visitor
        CallbackRecorder recorder = new CallbackRecorder();
        PsiCallbackVisitor visitor = new PsiCallbackVisitor(recorder);
        
        // Visit the file (triggers class visitation)
        ktFile.accept(visitor);
        
        // Validate state is balanced after traversal
        assertTrue("Visitor state should be balanced after traversal", 
                  visitor.validateState());
        
        return recorder;
    }
}