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

import static org.junit.Assert.*;

/**
 * Tests for Phase 4 Milestone 4.2: Constructor declaration callback sequence.
 * 
 * <p>Validates that constructor-related visitor methods invoke the complete callback
 * sequences for Kotlin constructors:</p>
 * <ul>
 *   <li>{@link PsiCallbackVisitor#visitPrimaryConstructor} - Primary constructors in class header</li>
 *   <li>{@link PsiCallbackVisitor#visitSecondaryConstructor} - Secondary constructors in class body</li>
 *   <li>{@link PsiCallbackVisitor#visitAnonymousInitializer} - Init blocks for initialization</li>
 * </ul>
 * 
 * <h2>Test Coverage</h2>
 * <p>This test suite covers ~20 scenarios:</p>
 * <ul>
 *   <li><b>Primary Constructors (Tests 1-8):</b> Empty, with parameters, property parameters,
 *       visibility modifiers, init blocks</li>
 *   <li><b>Secondary Constructors (Tests 9-14):</b> Simple, delegation (this/super),
 *       parameters, bodies, multiple constructors</li>
 *   <li><b>Edge Cases (Tests 15-20):</b> No explicit constructor, vararg, private,
 *       delegation chains, init blocks</li>
 * </ul>
 * 
 * @see PsiCallbackVisitor
 * @see CallbackRecorder
 * @see MethodDeclarationCallbackTest Similar test pattern for M4.1
 */
public class ConstructorDeclarationCallbackTest {
    
    private PsiEnvironment env;
    
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    // ==================== PRIMARY CONSTRUCTORS ====================
    
    /**
     * Test 1: Empty primary constructor (no parameters).
     */
    @Test
    public void testPrimaryConstructor_empty() throws PsiParseException {
        String kotlinCode = """
            class EmptyConstructor()
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify constructor declaration callback invoked
        assertTrue("Should have gotConstructorDecl callback",
                  recorder.hasCallback("gotConstructorDecl"));
        
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertEquals("Should have exactly 1 gotConstructorDecl", 1, constructorDecls.size());
        
        Map<String, Object> params = constructorDecls.get(0).getParameters();
        LocatableToken nameToken = (LocatableToken) params.get("token");
        
        assertNotNull("Constructor name token should not be null", nameToken);
        assertEquals("Constructor name should be class name 'EmptyConstructor'", 
                    "EmptyConstructor", nameToken.getText());
        
        // Verify no parameters
        List<CallbackRecord> paramRecords = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 0 parameters", 0, paramRecords.size());
        
        // Verify gotAllMethodParameters called
        assertTrue("Should have gotAllMethodParameters callback",
                  recorder.hasCallback("gotAllMethodParameters"));
        
        // Verify endMethodDecl called
        assertTrue("Should have endMethodDecl callback",
                  recorder.hasCallback("endMethodDecl"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 2: Primary constructor with simple parameters.
     */
    @Test
    public void testPrimaryConstructor_withParameters() throws PsiParseException {
        String kotlinCode = """
            class Person(name: String, age: Int)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify constructor declaration
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        // Verify parameters
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 2 parameters", 2, params.size());
        
        // Verify first parameter
        Map<String, Object> param1 = params.get(0).getParameters();
        LocatableToken token1 = (LocatableToken) param1.get("token");
        assertEquals("First parameter should be 'name'", "name", token1.getText());
        
        // Verify second parameter
        Map<String, Object> param2 = params.get(1).getParameters();
        LocatableToken token2 = (LocatableToken) param2.get("token");
        assertEquals("Second parameter should be 'age'", "age", token2.getText());
        
        // Verify parameter types
        List<CallbackRecord> typeSpecs = recorder.getCallbacksByName("gotTypeSpec");
        assertTrue("Should have at least 2 type specs for parameters", typeSpecs.size() >= 2);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 3: Primary constructor with property parameters (val/var).
     */
    @Test
    public void testPrimaryConstructor_propertyParameters() throws PsiParseException {
        String kotlinCode = """
            class Person(val name: String, var age: Int)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify constructor declaration
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        // Verify both parameters processed as constructor parameters
        // Note: Property aspect will be handled in Phase 4.3
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 2 constructor parameters", 2, params.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 4: Primary constructor with mixed property and regular parameters.
     */
    @Test
    public void testPrimaryConstructor_mixedParameters() throws PsiParseException {
        String kotlinCode = """
            class MixedParams(val id: Int, temp: String, var count: Int)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify 3 parameters
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 3 parameters", 3, params.size());
        
        // Verify parameter names
        LocatableToken param1 = (LocatableToken) params.get(0).getParameters().get("token");
        LocatableToken param2 = (LocatableToken) params.get(1).getParameters().get("token");
        LocatableToken param3 = (LocatableToken) params.get(2).getParameters().get("token");
        
        assertEquals("First parameter", "id", param1.getText());
        assertEquals("Second parameter", "temp", param2.getText());
        assertEquals("Third parameter", "count", param3.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 5: Primary constructor with visibility modifier.
     */
    @Test
    public void testPrimaryConstructor_privateModifier() throws PsiParseException {
        String kotlinCode = """
            class Private private constructor(param: String)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have private modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasPrivate = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "private".equals(token.getText());
            });
        assertTrue("Should have private modifier", hasPrivate);
        
        // Verify constructor declaration
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 6: Primary constructor with default parameter values.
     * Note: Default values are not processed in Phase 4 (require expression parsing).
     */
    @Test
    public void testPrimaryConstructor_defaultParameterValues() throws PsiParseException {
        String kotlinCode = """
            class DefaultParams(name: String = "default", count: Int = 0)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Constructor should be processed
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        // Parameters should be processed (default values skipped in Phase 4)
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 2 parameters", 2, params.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 7: Primary constructor with annotations.
     */
    @Test
    public void testPrimaryConstructor_withAnnotations() throws PsiParseException {
        String kotlinCode = """
            class Annotated @Deprecated("Old") constructor(param: String)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Constructor should be processed
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 8: Primary constructor with init block.
     */
    @Test
    public void testPrimaryConstructor_withInitBlock() throws PsiParseException {
        String kotlinCode = """
            class WithInit(val name: String) {
                init {
                    println("Initialized")
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have constructor
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        // Should have init block
        assertTrue("Should have beginInitBlock", recorder.hasCallback("beginInitBlock"));
        assertTrue("Should have endInitBlock", recorder.hasCallback("endInitBlock"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== SECONDARY CONSTRUCTORS ====================
    
    /**
     * Test 9: Simple secondary constructor.
     */
    @Test
    public void testSecondaryConstructor_simple() throws PsiParseException {
        String kotlinCode = """
            class MyClass {
                constructor(param: String) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify constructor declaration
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertEquals("Should have exactly 1 constructor", 1, constructorDecls.size());
        
        // Should have parameter
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 1 parameter", 1, params.size());
        
        // Should have body
        assertTrue("Should have beginMethodBody", recorder.hasCallback("beginMethodBody"));
        assertTrue("Should have endMethodBody", recorder.hasCallback("endMethodBody"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 10: Secondary constructor with this() delegation.
     */
    @Test
    public void testSecondaryConstructor_thisDelegation() throws PsiParseException {
        String kotlinCode = """
            class MyClass(val primary: Int) {
                constructor(secondary: String) : this(0) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 constructors (primary + secondary)
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertEquals("Should have 2 constructors", 2, constructorDecls.size());
        
        // Secondary constructor should have body
        List<CallbackRecord> bodyBegins = recorder.getCallbacksByName("beginMethodBody");
        assertEquals("Should have 1 body (secondary only)", 1, bodyBegins.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 11: Secondary constructor with super() delegation.
     */
    @Test
    public void testSecondaryConstructor_superDelegation() throws PsiParseException {
        String kotlinCode = """
            open class Base(param: Int)
            
            class Child : Base {
                constructor(param: String) : super(0) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Child should have secondary constructor
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertTrue("Should have at least 1 constructor", constructorDecls.size() >= 1);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 12: Secondary constructor with multiple parameters.
     */
    @Test
    public void testSecondaryConstructor_multipleParameters() throws PsiParseException {
        String kotlinCode = """
            class MyClass {
                constructor(a: Int, b: String, c: Boolean) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify 3 parameters
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 3 parameters", 3, params.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 13: Secondary constructor with body statements.
     */
    @Test
    public void testSecondaryConstructor_withBody() throws PsiParseException {
        String kotlinCode = """
            class MyClass {
                constructor(param: String) {
                    val x = param.length
                    println(x)
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have body callbacks
        assertTrue("Should have beginMethodBody", recorder.hasCallback("beginMethodBody"));
        assertTrue("Should have endMethodBody", recorder.hasCallback("endMethodBody"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 14: Multiple secondary constructors.
     */
    @Test
    public void testSecondaryConstructors_multiple() throws PsiParseException {
        String kotlinCode = """
            class MultipleConstructors(val primary: Int) {
                constructor(a: String) : this(0) {
                }
                
                constructor(a: String, b: Int) : this(b) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 3 constructors total (1 primary + 2 secondary)
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertEquals("Should have 3 constructors", 3, constructorDecls.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== EDGE CASES ====================
    
    /**
     * Test 15: Class with no explicit constructor (implicit default).
     * Note: Kotlin generates default constructor implicitly - not processed by visitor.
     */
    @Test
    public void testConstructor_implicitDefault() throws PsiParseException {
        String kotlinCode = """
            class NoExplicitConstructor {
                fun method() {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // No explicit constructor - no gotConstructorDecl callback
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertEquals("Should have 0 explicit constructors", 0, constructorDecls.size());
        
        // But should have class and method
        assertTrue("Should have class", recorder.hasCallback("gotTypeDef"));
        assertTrue("Should have method", recorder.hasCallback("gotMethodDeclaration"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 16: Constructor with vararg parameter.
     */
    @Test
    public void testConstructor_varargParameter() throws PsiParseException {
        String kotlinCode = """
            class VarargConstructor(vararg items: String)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify parameter with ellipsis
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 1 parameter", 1, params.size());
        
        Map<String, Object> paramMap = params.get(0).getParameters();
        LocatableToken ellipsisToken = (LocatableToken) paramMap.get("ellipsisToken");
        
        assertNotNull("Vararg should have ellipsis token", ellipsisToken);
        assertEquals("Ellipsis token type should be TRIPLE_DOT", 
                    JavaTokenTypes.TRIPLE_DOT, ellipsisToken.getType());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 17: Constructor with generic type parameters (rare but valid).
     * Note: Constructors don't typically have type parameters in Kotlin.
     */
    @Test
    public void testConstructor_genericParameter() throws PsiParseException {
        String kotlinCode = """
            class GenericParam(param: List<String>)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Constructor should be processed
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        // Parameter with generic type
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 1 parameter", 1, params.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 18: Private constructor (singleton pattern).
     */
    @Test
    public void testConstructor_private() throws PsiParseException {
        String kotlinCode = """
            class Singleton private constructor() {
                companion object {
                    val instance = Singleton()
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have private modifier for constructor
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasPrivate = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "private".equals(token.getText());
            });
        assertTrue("Should have private modifier on constructor", hasPrivate);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 19: Constructor delegation chain (primary → secondary → secondary).
     */
    @Test
    public void testConstructor_delegationChain() throws PsiParseException {
        String kotlinCode = """
            class Chain(val a: Int, val b: String) {
                constructor(a: Int) : this(a, "default") {
                }
                
                constructor() : this(0) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 3 constructors (1 primary + 2 secondary)
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertEquals("Should have 3 constructors", 3, constructorDecls.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 20: Multiple init blocks.
     */
    @Test
    public void testInitBlocks_multiple() throws PsiParseException {
        String kotlinCode = """
            class MultipleInit(val param: String) {
                init {
                    println("First init")
                }
                
                val property = "value"
                
                init {
                    println("Second init")
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 init blocks
        List<CallbackRecord> beginInit = recorder.getCallbacksByName("beginInitBlock");
        assertEquals("Should have 2 beginInitBlock callbacks", 2, beginInit.size());
        
        List<CallbackRecord> endInit = recorder.getCallbacksByName("endInitBlock");
        assertEquals("Should have 2 endInitBlock callbacks", 2, endInit.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 21: Init block without explicit constructor.
     */
    @Test
    public void testInitBlock_noExplicitConstructor() throws PsiParseException {
        String kotlinCode = """
            class InitOnly {
                init {
                    println("Init")
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have init block but no explicit constructor
        assertTrue("Should have beginInitBlock", recorder.hasCallback("beginInitBlock"));
        
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertEquals("Should have 0 explicit constructors", 0, constructorDecls.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 22: Constructor callback sequence order validation.
     */
    @Test
    public void testConstructor_callbackSequenceOrder() throws PsiParseException {
        String kotlinCode = """
            class TestClass(param: String) {
                constructor(a: Int) : this("test") {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .toList();
        
        // Find first constructor callbacks
        int firstConstructorDecl = sequence.indexOf("gotConstructorDecl");
        int firstModifiersConsumed = -1;
        int firstAllParams = -1;
        int firstEndMethodDecl = -1;
        
        // Find positions after first constructor
        for (int i = firstConstructorDecl + 1; i < sequence.size(); i++) {
            if ("modifiersConsumed".equals(sequence.get(i)) && firstModifiersConsumed == -1) {
                firstModifiersConsumed = i;
            }
            if ("gotAllMethodParameters".equals(sequence.get(i)) && firstAllParams == -1) {
                firstAllParams = i;
            }
            if ("endMethodDecl".equals(sequence.get(i)) && firstEndMethodDecl == -1) {
                firstEndMethodDecl = i;
                break;
            }
        }
        
        // Verify order
        assertTrue("Should have gotConstructorDecl", firstConstructorDecl >= 0);
        assertTrue("Should have modifiersConsumed", firstModifiersConsumed >= 0);
        assertTrue("Should have gotAllMethodParameters", firstAllParams >= 0);
        assertTrue("Should have endMethodDecl", firstEndMethodDecl >= 0);
        
        assertTrue("modifiersConsumed should come after gotConstructorDecl",
                  firstConstructorDecl < firstModifiersConsumed);
        assertTrue("gotAllMethodParameters should come after modifiersConsumed",
                  firstModifiersConsumed < firstAllParams);
        assertTrue("endMethodDecl should come after gotAllMethodParameters",
                  firstAllParams < firstEndMethodDecl);
    }
    
    /**
     * Test 23: Primary and secondary constructor in same class.
     */
    @Test
    public void testConstructors_primaryAndSecondary() throws PsiParseException {
        String kotlinCode = """
            class BothConstructors(val name: String) {
                constructor(name: String, id: Int) : this(name) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 constructor declarations
        List<CallbackRecord> constructorDecls = recorder.getCallbacksByName("gotConstructorDecl");
        assertEquals("Should have 2 constructors", 2, constructorDecls.size());
        
        // Primary constructor: name = "BothConstructors"
        Map<String, Object> primary = constructorDecls.get(0).getParameters();
        LocatableToken primaryName = (LocatableToken) primary.get("token");
        assertEquals("Primary constructor uses class name", "BothConstructors", primaryName.getText());
        
        // Secondary constructor: name = "constructor" keyword
        Map<String, Object> secondary = constructorDecls.get(1).getParameters();
        LocatableToken secondaryName = (LocatableToken) secondary.get("token");
        assertEquals("Secondary constructor uses 'constructor' keyword", "constructor", secondaryName.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Helper method to parse Kotlin code and visit with CallbackRecorder.
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
        
        // Visit the file (triggers class and constructor visitation)
        ktFile.accept(visitor);
        
        // Validate state is balanced after traversal
        assertTrue("Visitor state should be balanced after traversal", 
                  visitor.validateState());
        
        return recorder;
    }
}