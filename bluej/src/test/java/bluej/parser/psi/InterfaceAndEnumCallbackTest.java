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
 * Tests for Phase 3 Milestone 3.3: Interface and Enum Support Validation.
 * 
 * <p>Validates that interfaces and enums are correctly handled by the existing
 * {@link PsiCallbackVisitor#visitClass(org.jetbrains.kotlin.psi.KtClass)} implementation.</p>
 * 
 * <p><b>Key Insight:</b> Interfaces and enums in Kotlin are already implemented via visitClass()
 * because they are KtClass instances with specific type flags:</p>
 * <ul>
 *   <li>Interfaces: {@code KtClass.isInterface() == true} → {@code TYPEDEF_INTERFACE}</li>
 *   <li>Enums: {@code KtClass.isEnum() == true} → {@code TYPEDEF_ENUM}</li>
 * </ul>
 * 
 * <p><b>Scope:</b> This test class validates that both interfaces and enums produce the
 * correct callback sequences and type classifications. Out of scope: interface method
 * signatures, enum entry initialization, and member declarations (Phase 4).</p>
 * 
 * @see PsiCallbackVisitor
 * @see CallbackRecorder
 */
public class InterfaceAndEnumCallbackTest {
    
    private PsiEnvironment env;
    
    /**
     * Setup test environment before each test.
     */
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    // ==================== INTERFACE TESTS ====================
    
    /**
     * Test 1: Simple interface declaration invokes correct callbacks.
     * 
     * <p>Validates that a minimal interface declaration produces the expected
     * callback sequence identical to class declarations.</p>
     */
    @Test
    public void testSimpleInterface_invokesCorrectSequence() throws PsiParseException {
        String kotlinCode = "interface MyInterface { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have standard callback sequence
        assertTrue("Should have gotDeclBegin callback", 
                  recorder.hasCallback("gotDeclBegin"));
        assertTrue("Should have gotTypeDef callback", 
                  recorder.hasCallback("gotTypeDef"));
        assertTrue("Should have gotTypeDefName callback", 
                  recorder.hasCallback("gotTypeDefName"));
        assertTrue("Should have beginTypeBody callback", 
                  recorder.hasCallback("beginTypeBody"));
        assertTrue("Should have endTypeBody callback", 
                  recorder.hasCallback("endTypeBody"));
        assertTrue("Should have gotTypeDefEnd callback", 
                  recorder.hasCallback("gotTypeDefEnd"));
        
        // Validate pairing
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Callback pairing should be balanced: " + result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no validation errors: " + result.getValidationSummary(), 
                   result.hasErrors());
    }
    
    /**
     * Test 2: Interface has TYPEDEF_INTERFACE type.
     * 
     * <p>Validates that the gotTypeDef callback receives the correct tdType
     * parameter for interface declarations.</p>
     */
    @Test
    public void testInterfaceType_isCorrect() throws PsiParseException {
        String kotlinCode = "interface MyInterface { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> typeDefCallbacks = recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have exactly 1 gotTypeDef callback", 1, typeDefCallbacks.size());
        
        Map<String, Object> params = typeDefCallbacks.get(0).getParameters();
        int tdType = (int) params.get("tdType");
        
        assertEquals("Type definition should be LITERAL_interface", 
                    JavaTokenTypes.LITERAL_interface, tdType);
    }
    
    /**
     * Test 3: Interface name extracted correctly.
     * 
     * <p>Validates that the gotTypeDefName callback extracts the interface name.</p>
     */
    @Test
    public void testInterfaceName_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "interface Clickable { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> nameCallbacks = recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have exactly 1 gotTypeDefName callback", 1, nameCallbacks.size());
        
        Map<String, Object> params = nameCallbacks.get(0).getParameters();
        LocatableToken nameToken = (LocatableToken) params.get("nameToken");
        
        assertNotNull("nameToken should not be null", nameToken);
        assertEquals("Interface name should be 'Clickable'", "Clickable", nameToken.getText());
        assertEquals("Token type should be IDENT", JavaTokenTypes.IDENT, nameToken.getType());
    }
    
    /**
     * Test 4: Interface extending another interface.
     *
     * <p>Validates that interfaces extending other interfaces are handled correctly.
     * In Kotlin PSI, interface supertypes without constructor calls are treated
     * as implements rather than extends, even for interface-to-interface inheritance.</p>
     */
    @Test
    public void testInterfaceExtends_invokesCorrectCallback() throws PsiParseException {
        String kotlinCode = "interface Child : Parent { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // In Kotlin PSI, interface extends another interface is treated as implements
        // because there's no constructor call syntax
        assertTrue("Should have beginTypeDefImplements callback",
                  recorder.hasCallback("beginTypeDefImplements"));
        assertTrue("Should have endTypeDefImplements callback",
                  recorder.hasCallback("endTypeDefImplements"));
    }
    
    /**
     * Test 5: Interface with modifiers.
     * 
     * <p>Validates that interface modifiers produce gotModifier callbacks.</p>
     */
    @Test
    public void testInterfaceWithModifiers_extractsModifiers() throws PsiParseException {
        String kotlinCode = "public interface PublicInterface { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have modifier callback
        assertTrue("Should have gotModifier callback", 
                  recorder.hasCallback("gotModifier"));
        
        List<CallbackRecord> modifierCallbacks = recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback", 
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertEquals("Token type should be LITERAL_public", 
                    JavaTokenTypes.LITERAL_public, token.getType());
        assertEquals("Token text should be 'public'", "public", token.getText());
    }
    
    /**
     * Test 6: Interface with multiple super-interfaces.
     *
     * <p>Validates that interfaces extending multiple interfaces are handled correctly.
     * In Kotlin PSI, multiple interface supertypes are treated as implements.</p>
     */
    @Test
    public void testInterfaceMultipleSupers_usesImplements() throws PsiParseException {
        String kotlinCode = "interface Multi : Interface1, Interface2 { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // In Kotlin PSI, multiple interface supertypes are treated as implements
        assertTrue("Should have beginTypeDefImplements callback",
                  recorder.hasCallback("beginTypeDefImplements"));
        
        // Validate overall structure
        assertTrue("Callbacks should be balanced",
                  recorder.validatePairing());
    }
    
    // ==================== ENUM TESTS ====================
    
    /**
     * Test 7: Simple enum declaration invokes correct callbacks.
     * 
     * <p>Validates that a minimal enum class declaration produces the expected
     * callback sequence.</p>
     */
    @Test
    public void testSimpleEnum_invokesCorrectSequence() throws PsiParseException {
        String kotlinCode = "enum class Color { RED, GREEN, BLUE }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have standard callback sequence
        assertTrue("Should have gotDeclBegin callback", 
                  recorder.hasCallback("gotDeclBegin"));
        assertTrue("Should have gotTypeDef callback", 
                  recorder.hasCallback("gotTypeDef"));
        assertTrue("Should have gotTypeDefName callback", 
                  recorder.hasCallback("gotTypeDefName"));
        assertTrue("Should have beginTypeBody callback", 
                  recorder.hasCallback("beginTypeBody"));
        assertTrue("Should have endTypeBody callback", 
                  recorder.hasCallback("endTypeBody"));
        assertTrue("Should have gotTypeDefEnd callback", 
                  recorder.hasCallback("gotTypeDefEnd"));
        
        // Validate pairing
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Callback pairing should be balanced: " + result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no validation errors: " + result.getValidationSummary(), 
                   result.hasErrors());
    }
    
    /**
     * Test 8: Enum has TYPEDEF_ENUM type.
     * 
     * <p>Validates that the gotTypeDef callback receives the correct tdType
     * parameter for enum declarations.</p>
     */
    @Test
    public void testEnumType_isCorrect() throws PsiParseException {
        String kotlinCode = "enum class Status { ACTIVE, INACTIVE }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> typeDefCallbacks = recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have exactly 1 gotTypeDef callback", 1, typeDefCallbacks.size());
        
        Map<String, Object> params = typeDefCallbacks.get(0).getParameters();
        int tdType = (int) params.get("tdType");
        
        assertEquals("Type definition should be LITERAL_enum", 
                    JavaTokenTypes.LITERAL_enum, tdType);
    }
    
    /**
     * Test 9: Enum name extracted correctly.
     * 
     * <p>Validates that the gotTypeDefName callback extracts the enum name.</p>
     */
    @Test
    public void testEnumName_extractedCorrectly() throws PsiParseException {
        String kotlinCode = "enum class Direction { NORTH, SOUTH, EAST, WEST }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> nameCallbacks = recorder.getCallbacksByName("gotTypeDefName");
        assertEquals("Should have exactly 1 gotTypeDefName callback", 1, nameCallbacks.size());
        
        Map<String, Object> params = nameCallbacks.get(0).getParameters();
        LocatableToken nameToken = (LocatableToken) params.get("nameToken");
        
        assertNotNull("nameToken should not be null", nameToken);
        assertEquals("Enum name should be 'Direction'", "Direction", nameToken.getText());
        assertEquals("Token type should be IDENT", JavaTokenTypes.IDENT, nameToken.getType());
    }
    
    /**
     * Test 10: Enum with interface implementation.
     * 
     * <p>Validates that enums implementing interfaces invoke the
     * beginTypeDefImplements callback.</p>
     */
    @Test
    public void testEnumImplementsInterface_invokesCorrectCallback() throws PsiParseException {
        String kotlinCode = "enum class Color : Printable { RED, GREEN, BLUE }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Enums can implement interfaces
        assertTrue("Should have beginTypeDefImplements callback", 
                  recorder.hasCallback("beginTypeDefImplements"));
        assertTrue("Should have endTypeDefImplements callback", 
                  recorder.hasCallback("endTypeDefImplements"));
        
        // Should NOT have extends
        assertFalse("Should not have beginTypeDefExtends callback", 
                   recorder.hasCallback("beginTypeDefExtends"));
    }
    
    /**
     * Test 11: Enum with modifiers.
     * 
     * <p>Validates that enum modifiers produce gotModifier callbacks.</p>
     */
    @Test
    public void testEnumWithModifiers_extractsModifiers() throws PsiParseException {
        String kotlinCode = "internal enum class Status { ACTIVE, INACTIVE }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have modifier callback
        assertTrue("Should have gotModifier callback", 
                  recorder.hasCallback("gotModifier"));
        
        List<CallbackRecord> modifierCallbacks = recorder.getCallbacksByName("gotModifier");
        assertEquals("Should have exactly 1 gotModifier callback", 
                    1, modifierCallbacks.size());
        
        Map<String, Object> params = modifierCallbacks.get(0).getParameters();
        LocatableToken token = (LocatableToken) params.get("token");
        
        assertEquals("Token type should be LITERAL_internal", 
                    JavaTokenTypes.LITERAL_internal, token.getType());
        assertEquals("Token text should be 'internal'", "internal", token.getText());
    }
    
    /**
     * Test 12: Enum callback sequence includes all expected elements.
     * 
     * <p>Validates that the complete callback sequence for an enum matches
     * the expected pattern.</p>
     */
    @Test
    public void testEnumCallbackSequence_isComplete() throws PsiParseException {
        String kotlinCode = "enum class Priority { LOW, MEDIUM, HIGH }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Verify key callbacks are present in correct order
        assertTrue("Should start with gotDeclBegin", 
                  sequence.get(0).equals("gotDeclBegin"));
        assertTrue("Should have modifiersConsumed", 
                  sequence.contains("modifiersConsumed"));
        assertTrue("Should have gotTypeDef", 
                  sequence.contains("gotTypeDef"));
        assertTrue("Should have gotTypeDefName", 
                  sequence.contains("gotTypeDefName"));
        assertTrue("Should have beginTypeBody", 
                  sequence.contains("beginTypeBody"));
        assertTrue("Should have endTypeBody", 
                  sequence.contains("endTypeBody"));
        assertTrue("Should end with gotTypeDefEnd", 
                  sequence.get(sequence.size() - 1).equals("gotTypeDefEnd"));
    }
    
    // ==================== EDGE CASES ====================
    
    /**
     * Test 13: Empty interface with no methods.
     * 
     * <p>Validates that empty interfaces produce complete callback sequences.</p>
     */
    @Test
    public void testEmptyInterface_hasCompleteStructure() throws PsiParseException {
        String kotlinCode = "interface Empty { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Even empty interface should have complete structure
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Empty interface callbacks should be balanced: " + result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no validation errors: " + result.getValidationSummary(), 
                   result.hasErrors());
        
        // Should have begin/end body even though empty
        assertTrue("Should have beginTypeBody", 
                  recorder.hasCallback("beginTypeBody"));
        assertTrue("Should have endTypeBody", 
                  recorder.hasCallback("endTypeBody"));
    }
    
    /**
     * Test 14: Empty enum with no entries.
     * 
     * <p>Validates that empty enums produce complete callback sequences.
     * Note: While unusual, Kotlin allows empty enums.</p>
     */
    @Test
    public void testEmptyEnum_hasCompleteStructure() throws PsiParseException {
        String kotlinCode = "enum class Empty { }";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Even empty enum should have complete structure
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Empty enum callbacks should be balanced: " + result.getValidationSummary(), 
                  result.isBalanced());
        assertFalse("Should have no validation errors: " + result.getValidationSummary(), 
                   result.hasErrors());
        
        // Should still be classified as enum
        List<CallbackRecord> typeDefCallbacks = recorder.getCallbacksByName("gotTypeDef");
        int tdType = (int) typeDefCallbacks.get(0).getParameters().get("tdType");
        assertEquals("Empty enum should have LITERAL_enum type", 
                    JavaTokenTypes.LITERAL_enum, tdType);
    }
    
    /**
     * Test 15: Interface and enum in same file.
     * 
     * <p>Validates that multiple type declarations (interface and enum) in the
     * same file are handled correctly with separate callback sequences.</p>
     */
    @Test
    public void testInterfaceAndEnum_inSameFile() throws PsiParseException {
        String kotlinCode = 
            "interface Printable { }\n" +
            "enum class Color { RED, GREEN, BLUE }\n";
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have TWO complete sequences
        List<CallbackRecord> declBeginCallbacks = 
            recorder.getCallbacksByName("gotDeclBegin");
        assertEquals("Should have 2 gotDeclBegin callbacks", 2, declBeginCallbacks.size());
        
        List<CallbackRecord> declEndCallbacks = 
            recorder.getCallbacksByName("gotTypeDefEnd");
        assertEquals("Should have 2 gotTypeDefEnd callbacks", 2, declEndCallbacks.size());
        
        // Verify both type classifications
        List<CallbackRecord> typeDefCallbacks = 
            recorder.getCallbacksByName("gotTypeDef");
        assertEquals("Should have 2 gotTypeDef callbacks", 2, typeDefCallbacks.size());
        
        int tdType1 = (int) typeDefCallbacks.get(0).getParameters().get("tdType");
        int tdType2 = (int) typeDefCallbacks.get(1).getParameters().get("tdType");
        
        // One should be interface, one should be enum
        assertTrue("Should have both interface and enum types",
                  (tdType1 == JavaTokenTypes.LITERAL_interface && tdType2 == JavaTokenTypes.LITERAL_enum) ||
                  (tdType1 == JavaTokenTypes.LITERAL_enum && tdType2 == JavaTokenTypes.LITERAL_interface));
        
        // Validate overall pairing
        assertTrue("Multi-type file should have balanced callbacks", 
                  recorder.validatePairing());
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