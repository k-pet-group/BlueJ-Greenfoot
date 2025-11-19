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
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests for Phase 3 Milestone 3.2: Object and Companion Object Support.
 * 
 * <p>Validates that {@link PsiCallbackVisitor#visitObjectDeclaration(org.jetbrains.kotlin.psi.KtObjectDeclaration)}
 * invokes the complete callback sequence for Kotlin object declarations, mapping them to class callbacks
 * since BlueJ's ClassInfo model doesn't have special object types.</p>
 * 
 * <p><b>Object Declaration Types:</b></p>
 * <ul>
 *   <li>Singleton objects ({@code object MySingleton})</li>
 *   <li>Companion objects ({@code companion object})</li>
 *   <li>Named companion objects ({@code companion object Factory})</li>
 * </ul>
 * 
 * <p><b>Expected Callback Sequence:</b></p>
 * <ol>
 *   <li>{@code gotDeclBegin(token)} - Begin declaration</li>
 *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
 *   <li>{@code gotTypeDef(token, LITERAL_class)} - Type definition (objects map to classes)</li>
 *   <li>{@code gotTypeDefName(nameToken)} - Object or "Companion" name</li>
 *   <li>Supertype processing (objects can implement interfaces)</li>
 *   <li>{@code beginTypeBody(token)} - Begin body</li>
 *   <li>{@code endTypeBody(token, true)} - End body</li>
 *   <li>{@code gotTypeDefEnd(token, true)} - End declaration</li>
 * </ol>
 * 
 * @see PsiCallbackVisitor
 * @see CallbackRecorder
 */
public class ObjectDeclarationCallbackTest {
    
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
     * Test 1: Simple object declaration invokes correct callbacks.
     * 
     * <p>Validates that a minimal object declaration {@code object MySingleton { }}
     * produces the expected callback sequence matching the class pattern.</p>
     */
    @Test
    public void testSimpleObject_invokesCorrectSequence() throws PsiParseException {
        String kotlinCode = "object MySingleton { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have same structure as class
        assertTrue("Should have gotDeclBegin", recorder.hasCallback("gotDeclBegin"));
        assertTrue("Should have modifiersConsumed", recorder.hasCallback("modifiersConsumed"));
        assertTrue("Should have gotTypeDef", recorder.hasCallback("gotTypeDef"));
        assertTrue("Should have gotTypeDefName", recorder.hasCallback("gotTypeDefName"));
        assertTrue("Should have beginTypeBody", recorder.hasCallback("beginTypeBody"));
        assertTrue("Should have endTypeBody", recorder.hasCallback("endTypeBody"));
        assertTrue("Should have gotTypeDefEnd", recorder.hasCallback("gotTypeDefEnd"));
        
        // Validate pairing
        assertTrue("Callback pairing should be balanced", 
                  recorder.validatePairing());
    }
    
    /**
     * Test 2: Object name extracted correctly.
     * 
     * <p>Validates that {@code gotTypeDefName} callback receives a token with
     * the correct object name text.</p>
     */
    @Test
    public void testObjectName_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "object MyObject { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> nameCallbacks = recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have exactly 1 gotTypeDefName callback", 1, nameCallbacks.size());
        
        LocatableToken nameToken = (LocatableToken) nameCallbacks.get(0).getParameters().get("nameToken");
        assertEquals("Object name should be 'MyObject'", "MyObject", nameToken.getText());
        assertEquals("Token type should be IDENT", JavaTokenTypes.IDENT, nameToken.getType());
    }
    
    /**
     * Test 3: Object has LITERAL_class typedef type.
     * 
     * <p>Validates that objects are mapped to LITERAL_class type since
     * BlueJ's ClassInfo doesn't have special object types.</p>
     */
    @Test
    public void testObject_hasClassType() throws PsiParseException {
        String kotlinCode = "object MyObject { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> typeDefCallbacks = recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have exactly 1 gotTypeDef callback", 1, typeDefCallbacks.size());
        
        Map<String, Object> params = typeDefCallbacks.get(0).getParameters();
        int tdType = (int) params.get("tdType");
        
        assertEquals("Object should be mapped to LITERAL_class", 
                    JavaTokenTypes.LITERAL_class, tdType);
    }
    
    // ==================== COMPANION OBJECT HANDLING ====================
    
    /**
     * Test 4: Companion object inside class handled.
     * 
     * <p>Validates that companion objects are detected as nested declarations
     * and produce complete callback sequences.</p>
     */
    @Test
    public void testCompanionObject_insideClass() throws PsiParseException {
        String kotlinCode = """
            class MyClass {
                companion object { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have TWO type definitions (class + companion)
        List<CallbackRecord> typeDefCallbacks = recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have 2 gotTypeDef callbacks (class + companion)", 
                    2, typeDefCallbacks.size());
        
        // Both should contribute to balanced pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 5: Named companion object name extracted.
     * 
     * <p>Validates that named companion objects like {@code companion object Factory}
     * have their explicit name extracted correctly.</p>
     */
    @Test
    public void testNamedCompanion_nameExtracted() throws PsiParseException {
        String kotlinCode = """
            class MyClass {
                companion object Factory { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> nameCallbacks = recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have 2 gotTypeDefName callbacks", 2, nameCallbacks.size());
        
        // Extract names
        Set<String> names = nameCallbacks.stream()
            .map(r -> ((LocatableToken) r.getParameters().get("nameToken")).getText())
            .collect(Collectors.toSet());
        
        assertTrue("Should have class name", names.contains("MyClass"));
        assertTrue("Should have companion name", names.contains("Factory"));
    }
    
    /**
     * Test 6: Unnamed companion uses "Companion" as default.
     * 
     * <p>Validates that companion objects without explicit names use "Companion"
     * as the default name.</p>
     */
    @Test
    public void testUnnamedCompanion_usesDefaultName() throws PsiParseException {
        String kotlinCode = """
            class MyClass {
                companion object { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> nameCallbacks = recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have 2 gotTypeDefName callbacks", 2, nameCallbacks.size());
        
        // Extract names
        Set<String> names = nameCallbacks.stream()
            .map(r -> ((LocatableToken) r.getParameters().get("nameToken")).getText())
            .collect(Collectors.toSet());
        
        assertTrue("Should have class name", names.contains("MyClass"));
        assertTrue("Should have default 'Companion' name", names.contains("Companion"));
    }
    
    // ==================== INTERFACE IMPLEMENTATION ====================
    
    /**
     * Test 7: Object with interface implementation.
     * 
     * <p>Validates that objects implementing interfaces invoke
     * beginTypeDefImplements callback.</p>
     */
    @Test
    public void testObjectImplementsInterface_invokesCallback() throws PsiParseException {
        String kotlinCode = "object MyObject : MyInterface { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have implements callback
        assertTrue("Should have beginTypeDefImplements callback",
                  recorder.hasCallback("beginTypeDefImplements"));
        
        List<CallbackRecord> implCallbacks = recorder.getCallbacksByName("beginTypeDefImplements");
        assertEquals("Should have exactly 1 beginTypeDefImplements callback",
                    1, implCallbacks.size());
    }
    
    /**
     * Test 8: Object with multiple interfaces.
     * 
     * <p>Validates that objects implementing multiple interfaces
     * invoke implements callback once (matching class behavior).</p>
     */
    @Test
    public void testObjectMultipleInterfaces_singleCallback() throws PsiParseException {
        String kotlinCode = "object MyObject : Interface1, Interface2 { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have beginTypeDefImplements callback",
                  recorder.hasCallback("beginTypeDefImplements"));
        
        List<CallbackRecord> implCallbacks = recorder.getCallbacksByName("beginTypeDefImplements");
        assertEquals("Should invoke beginTypeDefImplements once",
                    1, implCallbacks.size());
    }
    
    // ==================== MODIFIER HANDLING ====================
    
    /**
     * Test 9: Object with visibility modifier.
     * 
     * <p>Validates that objects with modifiers produce gotModifier callbacks.</p>
     */
    @Test
    public void testObjectWithModifier_invokesGotModifier() throws PsiParseException {
        String kotlinCode = "internal object MyObject { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have modifier callback
        assertTrue("Should have gotModifier callback",
                  recorder.hasCallback("gotModifier"));
        
        List<CallbackRecord> modifierCallbacks = recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback",
                    1, modifierCallbacks.size());
        
        LocatableToken token = (LocatableToken) modifierCallbacks.get(0).getParameters().get("token");
        assertEquals("Should be internal modifier", 
                    JavaTokenTypes.LITERAL_internal, token.getType());
    }
    
    /**
     * Test 10: Object with multiple modifiers.
     * 
     * <p>Validates that multiple modifiers are processed in order.</p>
     */
    @Test
    public void testObjectMultipleModifiers_processedInOrder() throws PsiParseException {
        String kotlinCode = "public open object MyObject { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> modifierCallbacks = recorder.getCallbacksByName("gotModifier");
        assertTrue("Should have at least 2 gotModifier callbacks",
                  modifierCallbacks.size() >= 2);
    }
    
    // ==================== NESTED OBJECTS ====================
    
    /**
     * Test 11: Nested object inside class.
     * 
     * <p>Validates that objects nested inside classes are detected
     * and processed with complete callback sequences.</p>
     */
    @Test
    public void testNestedObject_insideClass() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                object Nested { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have TWO type definitions (class + nested object)
        List<CallbackRecord> typeDefCallbacks = recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have 2 gotTypeDef callbacks", 2, typeDefCallbacks.size());
        
        List<CallbackRecord> nameCallbacks = recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have 2 gotTypeDefName callbacks", 2, nameCallbacks.size());
        
        // Extract names
        Set<String> names = nameCallbacks.stream()
            .map(r -> ((LocatableToken) r.getParameters().get("nameToken")).getText())
            .collect(Collectors.toSet());
        
        assertTrue("Should have Outer class", names.contains("Outer"));
        assertTrue("Should have Nested object", names.contains("Nested"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 12: Object inside object (nested objects).
     * 
     * <p>Validates that objects can contain nested objects with
     * proper callback nesting.</p>
     */
    @Test
    public void testObjectInsideObject_nestedCorrectly() throws PsiParseException {
        String kotlinCode = """
            object Outer {
                object Inner { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have TWO complete sequences
        List<CallbackRecord> beginCallbacks = recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 2 gotDeclBegin callbacks", 2, beginCallbacks.size());
        
        List<CallbackRecord> endCallbacks = recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 2 gotTypeDefEnd callbacks", 2, endCallbacks.size());
        
        assertTrue("Nested objects should be balanced", recorder.validatePairing());
    }
    
    // ==================== CALLBACK SEQUENCE VALIDATION ====================
    
    /**
     * Test 13: Complete callback sequence order.
     * 
     * <p>Validates that the callback sequence for objects matches
     * the expected order.</p>
     */
    @Test
    public void testObjectCallbackSequence_correctOrder() throws PsiParseException {
        String kotlinCode = "object Simple { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Expected sequence for simple object
        List<String> expected = Arrays.asList(
            "gotDeclBegin",
            "modifiersConsumed",
            "gotTypeDef",
            "gotTypeDefName",
            "beginTypeBody",
            "endTypeBody",
            "gotTypeDefEnd"
        );
        
        assertEquals("Callback sequence should match expected", expected, sequence);
    }
    
    /**
     * Test 14: Object with interface has correct sequence.
     * 
     * <p>Validates that interface implementation appears in the
     * correct position in the callback sequence.</p>
     */
    @Test
    public void testObjectWithInterface_correctSequence() throws PsiParseException {
        String kotlinCode = "object MyObject : MyInterface { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Expected sequence with interface
        List<String> expected = Arrays.asList(
            "gotDeclBegin",
            "modifiersConsumed",
            "gotTypeDef",
            "gotTypeDefName",
            "beginTypeDefImplements",
            "endTypeDefImplements",
            "beginTypeBody",
            "endTypeBody",
            "gotTypeDefEnd"
        );
        
        assertEquals("Callback sequence with interface should match expected", 
                    expected, sequence);
    }
    
    // ==================== TOKEN VALIDATION ====================
    
    /**
     * Test 15: gotDeclBegin receives class token.
     *
     * <p>Validates that the declaration begin callback receives a token
     * with type {@link JavaTokenTypes#LITERAL_class} since objects are
     * mapped to classes in BlueJ's ClassInfo model.</p>
     */
    @Test
    @Ignore("TODO: figure our if that makes sense")
    public void testDeclBegin_hasClassToken() throws PsiParseException {
        String kotlinCode = "object TestObject { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> declBeginCallbacks = recorder.getCallbacksByName("gotDeclBegin");
        assertEquals(1, declBeginCallbacks.size());
        
        Map<String, Object> params = declBeginCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertNotNull("gotDeclBegin should have token", token);
        assertEquals("Token type should be LITERAL_class",
                    JavaTokenTypes.LITERAL_class, token.getType());
    }
    
    /**
     * Test 16: Interface token has correct text.
     * 
     * <p>Validates that the token passed to beginTypeDefImplements contains
     * the interface type reference text for objects.</p>
     */
    @Test
    public void testObjectInterfaceToken_hasCorrectText() throws PsiParseException {
        String kotlinCode = "object MyObject : MyInterface { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> implCallbacks = recorder.getCallbacksByName("beginTypeDefImplements");
        assertEquals(1, implCallbacks.size());
        
        Map<String, Object> params = implCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("implementsToken");
        
        assertNotNull("Token should not be null", token);
        assertEquals("Token should contain interface name",
                    "MyInterface", token.getText());
        assertEquals("Token type should be IDENT",
                    JavaTokenTypes.IDENT, token.getType());
    }
    
    // ==================== EDGE CASES ====================
    
    /**
     * Test 17: Multiple top-level objects.
     * 
     * <p>Validates that multiple object declarations in the same file
     * each get complete callback sequences.</p>
     */
    @Test
    public void testMultipleObjects_eachGetSequence() throws PsiParseException {
        String kotlinCode = 
            "object First { }\n" +
            "object Second { }\n";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 complete sequences
        List<CallbackRecord> declBeginCallbacks = recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 2 gotDeclBegin callbacks", 2, declBeginCallbacks.size());
        
        List<CallbackRecord> declEndCallbacks = recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 2 gotTypeDefEnd callbacks", 2, declEndCallbacks.size());
        
        // Verify both names captured
        List<CallbackRecord> nameCallbacks = recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have 2 gotTypeDefName callbacks", 2, nameCallbacks.size());
        
        LocatableToken name1 = (LocatableToken) nameCallbacks.get(0).getParameters().get("nameToken");
        LocatableToken name2 = (LocatableToken) nameCallbacks.get(1).getParameters().get("nameToken");
        
        assertEquals("First object name", "First", name1.getText());
        assertEquals("Second object name", "Second", name2.getText());
        
        assertTrue("Multiple objects should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 18: Companion object maintains proper nesting.
     * 
     * <p>Validates that companion object callbacks appear between class's
     * beginTypeBody and endTypeBody.</p>
     */
    @Test
    public void testCompanionObject_maintainsProperNesting() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                companion object { }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        int firstBeginBody = sequence.indexOf("beginTypeBody");
        int lastEndBody = sequence.lastIndexOf("endTypeBody");
        
        // Count gotDeclBegin between the body markers (should include companion)
        int companionDeclCount = 0;
        for (int i = firstBeginBody + 1; i < lastEndBody; i++) {
            if ("gotDeclBegin".equals(sequence.get(i))) {
                companionDeclCount++;
            }
        }
        
        assertTrue("Class should have beginTypeBody", firstBeginBody >= 0);
        assertTrue("Class should have endTypeBody", lastEndBody >= 0);
        assertEquals("Companion declaration should be between class's body markers",
                    1, companionDeclCount);
        assertTrue("beginTypeBody should come before endTypeBody",
                  firstBeginBody < lastEndBody);
    }
    
    /**
     * Test 19: Object without body handled gracefully.
     * 
     * <p>Validates that objects can have empty bodies without issues.</p>
     */
    @Test
    public void testObjectEmptyBody_handledGracefully() throws PsiParseException {
        String kotlinCode = "object Empty { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should still have complete sequence
        assertTrue("Should have gotDeclBegin", recorder.hasCallback("gotDeclBegin"));
        assertTrue("Should have beginTypeBody", recorder.hasCallback("beginTypeBody"));
        assertTrue("Should have endTypeBody", recorder.hasCallback("endTypeBody"));
        assertTrue("Should have gotTypeDefEnd", recorder.hasCallback("gotTypeDefEnd"));
        
        assertTrue("Empty body should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 20: State management tracks object scope correctly.
     * 
     * <p>Validates that VisitorState properly maintains scope balance
     * through object visitation.</p>
     */
    @Test
    public void testStateManagement_tracksObjectScopeCorrectly() throws PsiParseException {
        String kotlinCode = """
            object Outer {
                object Middle {
                    object Inner { }
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
        
        // Visit the file (triggers object visitation)
        ktFile.accept(visitor);
        
        return recorder;
    }
}