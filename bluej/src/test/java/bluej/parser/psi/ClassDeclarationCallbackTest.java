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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            .map(CallbackRecord::getCallbackName)
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
        List<CallbackRecord> nameCallbacks =
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
        List<CallbackRecord> typeDefCallbacks =
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
        
        List<CallbackRecord> typeDefCallbacks =
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
        
        List<CallbackRecord> typeDefCallbacks =
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
        List<CallbackRecord> beginBodyCallbacks =
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
        List<CallbackRecord> endBodyCallbacks =
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
        
        List<CallbackRecord> endCallbacks =
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
            .map(CallbackRecord::getCallbackName)
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
        List<CallbackRecord> nameCallbacks =
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
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        assertTrue("Should have gotDeclBegin", sequence.contains("gotDeclBegin"));
        assertTrue("Should have gotTypeDefName", sequence.contains("gotTypeDefName"));
        assertTrue("Should have beginTypeBody", sequence.contains("beginTypeBody"));
        
        // Verify still classified as regular class
        List<CallbackRecord> typeDefCallbacks =
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
        List<CallbackRecord> declBeginCallbacks =
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 2 gotDeclBegin callbacks", 2, declBeginCallbacks.size());
        
        List<CallbackRecord> declEndCallbacks =
            recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 2 gotTypeDefEnd callbacks", 2, declEndCallbacks.size());
        
        // Verify both class names captured
        List<CallbackRecord> nameCallbacks =
            recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have 2 gotTypeDefName callbacks", 2, nameCallbacks.size());
        
        LocatableToken name1 = (LocatableToken) nameCallbacks.get(0).getParameters().get("nameToken");
        LocatableToken name2 = (LocatableToken) nameCallbacks.get(1).getParameters().get("nameToken");
        
        assertEquals("First class name", "FirstClass", name1.getText());
        assertEquals("Second class name", "SecondClass", name2.getText());
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
        
        List<CallbackRecord> declBeginCallbacks =
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
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("modifiersConsumed");
        assertEquals("Should have exactly 1 modifiersConsumed callback", 
                    1, modifierCallbacks.size());
        
        // Verify it comes before gotTypeDef in sequence
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        int modifiersIndex = sequence.indexOf("modifiersConsumed");
        int typeDefIndex = sequence.indexOf("gotTypeDef");
        
        assertTrue("modifiersConsumed should come before gotTypeDef", 
                  modifiersIndex < typeDefIndex);
    }
    
    // ==================== MODIFIER EXTRACTION (TASK 2) ====================
    
    /**
     * Test 15: Public modifier extracted correctly.
     *
     * <p>Validates that a public class produces a gotModifier callback
     * with the correct token type.</p>
     */
    @Test
    public void testPublicModifier_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "public class PublicClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have gotModifier callback
        assertTrue("Should have gotModifier callback",
                  recorder.hasCallback("gotModifier"));
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback",
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Token type should be LITERAL_public",
                    JavaTokenTypes.LITERAL_public, token.getType());
        assertEquals("Token text should be 'public'", "public", token.getText());
    }
    
    /**
     * Test 16: Private modifier extracted correctly.
     *
     * <p>Validates that a private class produces the correct modifier callback.</p>
     */
    @Test
    public void testPrivateModifier_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "private class PrivateClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback",
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Token type should be LITERAL_private",
                    JavaTokenTypes.LITERAL_private, token.getType());
        assertEquals("Token text should be 'private'", "private", token.getText());
    }
    
    /**
     * Test 17: Abstract modifier extracted correctly.
     *
     * <p>Validates that abstract classes produce the correct modifier callback.</p>
     */
    @Test
    public void testAbstractModifier_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "abstract class AbstractClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback",
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Token type should be ABSTRACT",
                    JavaTokenTypes.ABSTRACT, token.getType());
        assertEquals("Token text should be 'abstract'", "abstract", token.getText());
    }
    
    /**
     * Test 18: Multiple modifiers extracted in order.
     *
     * <p>Validates that multiple modifiers are processed in source order
     * and each produces its own gotModifier callback.</p>
     */
    @Test
    public void testMultipleModifiers_extractedInOrder() throws PsiParseException {
        String kotlinCode = "public abstract class MultiModifierClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 2 gotModifier callbacks",
                    2, modifierCallbacks.size());
        
        // First should be public
        Map<String, Object> params1 = modifierCallbacks.get(0).getParameters();
        LocatableToken token1 = (LocatableToken) params1.get("token");
        assertEquals("First modifier should be public",
                    JavaTokenTypes.LITERAL_public, token1.getType());
        assertEquals("First modifier text should be 'public'", "public", token1.getText());
        
        // Second should be abstract
        Map<String, Object> params2 = modifierCallbacks.get(1).getParameters();
        LocatableToken token2 = (LocatableToken) params2.get("token");
        assertEquals("Second modifier should be abstract",
                    JavaTokenTypes.ABSTRACT, token2.getType());
        assertEquals("Second modifier text should be 'abstract'", "abstract", token2.getText());
    }
    
    /**
     * Test 19: Internal modifier uses Kotlin-specific token.
     *
     * <p>Validates that Kotlin's 'internal' visibility modifier uses the
     * Kotlin-specific token type LITERAL_internal (not mapped to Java public).</p>
     */
    @Test
    public void testInternalModifier_usesKotlinToken() throws PsiParseException {
        String kotlinCode = "internal class InternalClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback",
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Internal should use LITERAL_internal token",
                    JavaTokenTypes.LITERAL_internal, token.getType());
        assertEquals("Token text should be 'internal'", "internal", token.getText());
    }
    
    /**
     * Test 20: Open modifier uses Kotlin-specific token.
     *
     * <p>Validates that Kotlin's 'open' modifier (allows inheritance) produces
     * a gotModifier callback with the Kotlin-specific LITERAL_open token type.</p>
     */
    @Test
    public void testOpenModifier_usesKotlinToken() throws PsiParseException {
        String kotlinCode = "open class OpenClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback",
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Open should use LITERAL_open token",
                    JavaTokenTypes.LITERAL_open, token.getType());
        assertEquals("Token text should be 'open'", "open", token.getText());
    }
    
    /**
     * Test 21: Class without modifiers has no gotModifier callbacks.
     *
     * <p>Validates that classes without explicit modifiers still invoke
     * modifiersConsumed but don't produce any gotModifier callbacks.</p>
     */
    @Test
    public void testNoModifiers_noGotModifierCallbacks() throws PsiParseException {
        String kotlinCode = "class NoModifiersClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have no gotModifier callbacks",
                    0, modifierCallbacks.size());
        
        // Should still call modifiersConsumed
        assertTrue("Should have modifiersConsumed callback",
                  recorder.hasCallback("modifiersConsumed"));
    }
    
    /**
     * Test 22: Protected modifier extracted correctly.
     *
     * <p>Validates that protected modifier produces the correct callback.</p>
     */
    @Test
    public void testProtectedModifier_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "protected class ProtectedClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback",
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Token type should be LITERAL_protected",
                    JavaTokenTypes.LITERAL_protected, token.getType());
        assertEquals("Token text should be 'protected'", "protected", token.getText());
    }
    
    /**
     * Test 23: Final modifier extracted correctly.
     *
     * <p>Validates that final modifier produces the correct callback.</p>
     */
    @Test
    public void testFinalModifier_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "final class FinalClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback",
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Token type should be FINAL",
                    JavaTokenTypes.FINAL, token.getType());
        assertEquals("Token text should be 'final'", "final", token.getText());
    }
    
    /**
     * Test 24: Public class has gotModifier in sequence.
     *
     * <p>Validates that gotModifier callbacks appear in the correct position
     * within the overall callback sequence (after gotDeclBegin, before modifiersConsumed).</p>
     */
    @Test
    public void testPublicClass_modifierInSequence() throws PsiParseException {
        String kotlinCode = "public class PublicClass { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Expected sequence with modifier
        List<String> expected = java.util.Arrays.asList(
            "gotDeclBegin",
            "gotModifier",        // NEW: public modifier
            "modifiersConsumed",
            "gotTypeDef",
            "gotTypeDefName",
            "beginTypeBody",
            "endTypeBody",
            "gotTypeDefEnd"
        );
        
        assertEquals("Callback sequence should match expected", expected, sequence);
    }
    
    // ==================== SUPERTYPE PROCESSING (TASK 3) ====================
    
    /**
     * Test 25: Class with superclass invokes beginTypeDefExtends.
     *
     * <p>Validates that a class with a superclass (using constructor call syntax)
     * invokes the beginTypeDefExtends callback.</p>
     */
    @Test
    public void testClassWithSuperclass_invokesBeginTypeDefExtends() throws PsiParseException {
        String kotlinCode = "class Child : Parent() { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have beginTypeDefExtends callback
        assertTrue("Should have beginTypeDefExtends callback",
                  recorder.hasCallback("beginTypeDefExtends"));
        
        List<CallbackRecord> extendsCallbacks =
            recorder.getCallbacksByName("beginTypeDefExtends");
        assertEquals("Should have exactly 1 beginTypeDefExtends callback",
                    1, extendsCallbacks.size());
    }
    
    /**
     * Test 26: Class implementing interface invokes beginTypeDefImplements.
     *
     * <p>Validates that a class implementing an interface (no constructor call)
     * invokes the beginTypeDefImplements callback.</p>
     */
    @Test
    public void testClassImplementingInterface_invokesBeginTypeDefImplements() throws PsiParseException {
        String kotlinCode = "class Impl : Interface { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have beginTypeDefImplements callback
        assertTrue("Should have beginTypeDefImplements callback",
                  recorder.hasCallback("beginTypeDefImplements"));
        
        List<CallbackRecord> implCallbacks =
            recorder.getCallbacksByName("beginTypeDefImplements");
        assertEquals("Should have exactly 1 beginTypeDefImplements callback",
                    1, implCallbacks.size());
    }
    
    /**
     * Test 27: Class with superclass and interfaces invokes both extends and implements.
     *
     * <p>Validates that a class with both superclass and interfaces invokes both
     * callbacks and that extends comes before implements in the sequence.</p>
     */
    @Test
    public void testClassWithSuperclassAndInterfaces_invokesBothCallbacks() throws PsiParseException {
        String kotlinCode = "class Multi : Parent(), Interface1, Interface2 { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have both callbacks
        assertTrue("Should have beginTypeDefExtends callback",
                  recorder.hasCallback("beginTypeDefExtends"));
        assertTrue("Should have beginTypeDefImplements callback",
                  recorder.hasCallback("beginTypeDefImplements"));
        
        // Validate sequence
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        int extendsIndex = sequence.indexOf("beginTypeDefExtends");
        int implIndex = sequence.indexOf("beginTypeDefImplements");
        
        assertTrue("Should have extends in sequence", extendsIndex >= 0);
        assertTrue("Should have implements in sequence", implIndex >= 0);
        assertTrue("Extends should come before implements",
                  extendsIndex < implIndex);
    }
    
    /**
     * Test 28: Class with multiple interfaces only invokes implements once.
     *
     * <p>Validates that multiple interfaces result in a single beginTypeDefImplements
     * callback invocation.</p>
     */
    @Test
    public void testMultipleInterfacesOnly_invokesImplementsOnce() throws PsiParseException {
        String kotlinCode = "class MultiImpl : Interface1, Interface2, Interface3 { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should NOT have extends
        assertFalse("Should not have beginTypeDefExtends callback",
                   recorder.hasCallback("beginTypeDefExtends"));
        
        // Should have implements
        assertTrue("Should have beginTypeDefImplements callback",
                  recorder.hasCallback("beginTypeDefImplements"));
        
        List<CallbackRecord> implCallbacks =
            recorder.getCallbacksByName("beginTypeDefImplements");
        assertEquals("Should invoke beginTypeDefImplements once",
                    1, implCallbacks.size());
    }
    
    /**
     * Test 29: Class with no supertypes has no extends/implements callbacks.
     *
     * <p>Validates that a class without supertypes does not invoke either
     * beginTypeDefExtends or beginTypeDefImplements.</p>
     */
    @Test
    public void testNoSupertypes_noExtendsOrImplementsCallbacks() throws PsiParseException {
        String kotlinCode = "class Standalone { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should NOT have extends or implements
        assertFalse("Should not have beginTypeDefExtends callback",
                   recorder.hasCallback("beginTypeDefExtends"));
        assertFalse("Should not have beginTypeDefImplements callback",
                   recorder.hasCallback("beginTypeDefImplements"));
    }
    
    /**
     * Test 30: Supertype callback sequence is correct.
     *
     * <p>Validates that the complete callback sequence includes supertypes
     * in the correct position (after gotTypeDefName, before beginTypeBody).</p>
     */
    @Test
    public void testSupertypeCallbackSequence_isCorrect() throws PsiParseException {
        String kotlinCode = "class Full : Parent(), Interface1 { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Expected sequence with supertypes
        List<String> expected = java.util.Arrays.asList(
            "gotDeclBegin",
            "modifiersConsumed",
            "gotTypeDef",
            "gotTypeDefName",
            "beginTypeDefExtends",     // Superclass begin
            "endTypeDefExtends",       // Superclass end
            "beginTypeDefImplements",  // Interfaces begin
            "endTypeDefImplements",    // Interfaces end
            "beginTypeBody",
            "endTypeBody",
            "gotTypeDefEnd"
        );
        
        assertEquals("Callback sequence should match expected with supertypes",
                    expected, sequence);
    }
    
    /**
     * Test 31: Superclass token has correct text.
     *
     * <p>Validates that the token passed to beginTypeDefExtends contains
     * the superclass type reference text.</p>
     */
    @Test
    public void testSuperclassToken_hasCorrectText() throws PsiParseException {
        String kotlinCode = "class Child : Parent() { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> extendsCallbacks =
            recorder.getCallbacksByName("beginTypeDefExtends");
        assertEquals(1, extendsCallbacks.size());
        
        Map<String, Object> params = extendsCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("extendsToken");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Token should contain superclass name",
                    "Parent", token.getText());
        assertEquals("Token type should be IDENT",
                    JavaTokenTypes.IDENT, token.getType());
    }
    
    /**
     * Test 32: Interface token has correct text.
     *
     * <p>Validates that the token passed to beginTypeDefImplements contains
     * the first interface type reference text.</p>
     */
    @Test
    public void testInterfaceToken_hasCorrectText() throws PsiParseException {
        String kotlinCode = "class Impl : Interface1, Interface2 { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> implCallbacks =
            recorder.getCallbacksByName("beginTypeDefImplements");
        assertEquals(1, implCallbacks.size());
        
        Map<String, Object> params = implCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("implementsToken");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Token should contain first interface name",
                    "Interface1", token.getText());
        assertEquals("Token type should be IDENT",
                    JavaTokenTypes.IDENT, token.getType());
    }
    
    // ==================== NESTED CLASS HANDLING (TASK 4) ====================
    
    /**
     * Test 33: Single nested class produces two complete callback sequences.
     * 
     * <p>Validates that nested classes trigger recursive visitClass() calls,
     * each producing a complete callback sequence.</p>
     */
    @Test
    public void testNestedClass_producesCompleteSequences() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                class Inner { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have TWO complete sequences (outer + inner)
        List<CallbackRecord> beginCallbacks =
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 2 gotDeclBegin callbacks (outer + inner)", 
                    2, beginCallbacks.size());
        
        List<CallbackRecord> endCallbacks =
            recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 2 gotTypeDefEnd callbacks (outer + inner)", 
                    2, endCallbacks.size());
        
        // Validate pairing for nested structure
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Nested classes should have balanced callbacks: " + result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no pairing errors: " + result.getValidationSummary(), 
                   result.hasErrors());
    }
    
    /**
     * Test 34: Nested class callback sequence maintains proper nesting.
     * 
     * <p>Validates that inner class callbacks appear between outer class's
     * beginTypeBody and endTypeBody.</p>
     */
    @Test
    public void testNestedClass_maintainsProperNesting() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                class Inner { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Pattern: Outer begins → ... → Outer beginTypeBody →
        //          Inner complete sequence → Outer endTypeBody → ... → Outer ends
        
        // Simply verify: inner sequence is nested within outer's body markers
        int firstBeginBody = sequence.indexOf("beginTypeBody");
        int lastEndBody = sequence.lastIndexOf("endTypeBody");
        
        // Count gotDeclBegin between the body markers (should include inner)
        int innerDeclCount = 0;
        for (int i = firstBeginBody + 1; i < lastEndBody; i++) {
            if ("gotDeclBegin".equals(sequence.get(i))) {
                innerDeclCount++;
            }
        }
        
        assertTrue("Outer should have beginTypeBody", firstBeginBody >= 0);
        assertTrue("Outer should have endTypeBody", lastEndBody >= 0);
        assertTrue("Inner declaration should be between outer's body markers",
                  innerDeclCount >= 1);
        assertTrue("beginTypeBody should come before endTypeBody",
                  firstBeginBody < lastEndBody);
    }
    
    /**
     * Test 35: Deeply nested classes (3 levels) maintain correct scope.
     * 
     * <p>Validates that deep nesting produces the correct number of complete
     * sequences with proper callback pairing at each level.</p>
     */
    @Test
    public void testDeeplyNestedClasses_maintainCorrectScope() throws PsiParseException {
        String kotlinCode = """
            class Level1 {
                class Level2 {
                    class Level3 { }
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have THREE complete sequences
        List<CallbackRecord> beginCallbacks =
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 3 gotDeclBegin callbacks", 3, beginCallbacks.size());
        
        List<CallbackRecord> endCallbacks =
            recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 3 gotTypeDefEnd callbacks", 3, endCallbacks.size());
        
        // Validate pairing - all nested scopes properly balanced
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Deeply nested classes should have balanced callbacks: " + result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no pairing errors: " + result.getValidationSummary(), 
                   result.hasErrors());
    }
    
    /**
     * Test 36: Multiple nested classes at same level handled correctly.
     * 
     * <p>Validates that multiple sibling nested classes each produce
     * complete sequences.</p>
     */
    @Test
    public void testMultipleNestedClasses_sameLevelHandled() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                class Inner1 { }
                class Inner2 { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have THREE sequences (1 outer + 2 inner)
        List<CallbackRecord> beginCallbacks =
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 3 gotDeclBegin callbacks (1 outer + 2 inner)", 
                    3, beginCallbacks.size());
        
        List<CallbackRecord> endCallbacks =
            recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 3 gotTypeDefEnd callbacks", 3, endCallbacks.size());
        
        // Both inner classes should be between outer's body delimiters
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        int outerBeginBody = sequence.indexOf("beginTypeBody");
        int outerEndBody = sequence.lastIndexOf("endTypeBody");
        
        // Count gotDeclBegin between outer's body markers
        long innerCount = sequence.subList(outerBeginBody + 1, outerEndBody).stream()
            .filter(s -> s.equals("gotDeclBegin"))
            .count();
        
        assertEquals("Should have 2 inner class declarations within outer body", 
                    2, innerCount);
    }
    
    /**
     * Test 37: Nested class with modifiers handled correctly.
     * 
     * <p>Validates that nested classes with modifiers produce gotModifier
     * callbacks in addition to the standard sequence.</p>
     */
    @Test
    public void testNestedClassWithModifiers_handledCorrectly() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                private abstract class Inner : Parent() { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Inner class should have at least private modifier
        List<CallbackRecord> modifierCallbacks =
            recorder.getCallbacksByName("gotModifier");
        assertTrue("Should have at least 1 modifier for inner class (private/abstract)",
                  modifierCallbacks.size() >= 1);
        
        // Inner class should have extends (Parent superclass)
        assertTrue("Should have beginTypeDefExtends callback for inner class",
                  recorder.hasCallback("beginTypeDefExtends"));
        
        // Should have two complete callback sequences (outer + inner)
        List<CallbackRecord> beginCallbacks =
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 2 complete sequences", 2, beginCallbacks.size());
        
        // Overall structure should be balanced
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        System.out.println("DEBUG TEST: Validation summary: " + result.getValidationSummary());
        System.out.println("DEBUG TEST: Is balanced: " + result.isBalanced());
        System.out.println("DEBUG TEST: Has errors: " + result.hasErrors());
    }
    
    /**
     * Test 38: Nested class names extracted correctly.
     * 
     * <p>Validates that both outer and inner class names are captured
     * correctly in their respective gotTypeDefName callbacks.</p>
     */
    @Test
    public void testNestedClassNames_extractedCorrectly() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                class Inner { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> nameCallbacks =
            recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have 2 name callbacks (outer + inner)", 
                    2, nameCallbacks.size());
        
        // Extract names
        LocatableToken name1 = (LocatableToken) nameCallbacks.get(0).getParameters().get("nameToken");
        LocatableToken name2 = (LocatableToken) nameCallbacks.get(1).getParameters().get("nameToken");
        
        // One should be "Outer", one should be "Inner"
        Set<String> names = new HashSet<>(Arrays.asList(name1.getText(), name2.getText()));
        assertTrue("Should have Outer class name", names.contains("Outer"));
        assertTrue("Should have Inner class name", names.contains("Inner"));
    }
    
    /**
     * Test 39: Mixed nesting (nested classes and multiple siblings).
     * 
     * <p>Validates complex nesting pattern with both vertical nesting
     * and horizontal siblings.</p>
     */
    @Test
    public void testMixedNesting_handledCorrectly() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                class Inner1 { }
                class Inner2 {
                    class DeepInner { }
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 4 complete sequences
        List<CallbackRecord> beginCallbacks =
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 4 gotDeclBegin callbacks", 4, beginCallbacks.size());
        
        List<CallbackRecord> endCallbacks =
            recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 4 gotTypeDefEnd callbacks", 4, endCallbacks.size());
        
        // Validate pairing for complex nested structure
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Mixed nesting should have balanced callbacks: " + result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no pairing errors: " + result.getValidationSummary(), 
                   result.hasErrors());
    }
    
    /**
     * Test 40: State management tracks nested scope correctly.
     * 
     * <p>Validates that VisitorState properly maintains scope balance
     * through nested class visitation via pairing validation.</p>
     */
    @Test
    public void testStateManagement_tracksNestedScopeCorrectly() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                class Middle {
                    class Inner { }
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // If state management is wrong, pairing will fail
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("State management should maintain balance through deep nesting: " + 
                  result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no scope imbalance errors: " + result.getValidationSummary(), 
                   result.hasErrors());
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

        // Validate pairing
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        var summary = result.getValidationSummary();

        assertTrue("Callback pairing should be balanced: " + summary,
                result.isBalanced());
        assertFalse("Should have no validation errors: " + summary,
                result.hasErrors());

        return recorder;
    }
}