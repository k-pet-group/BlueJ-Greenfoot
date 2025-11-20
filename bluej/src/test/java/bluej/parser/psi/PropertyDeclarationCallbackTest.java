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
import bluej.parser.psi.visitor.BaseVisitor;
import bluej.parser.psi.visitor.FileVisitor;
import org.jetbrains.kotlin.psi.KtFile;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for Phase 4 Milestone 4.3: Property declaration callback sequence.
 * 
 * <p>Validates that {@link bluej.parser.psi.visitor.FileVisitor#visitProperty(org.jetbrains.kotlin.psi.KtProperty)}
 * invokes the complete callback sequence for property declarations:</p>
 * <ol>
 *   <li>{@code beginFieldDeclarations(token)} - Begin field declarations</li>
 *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
 *   <li>{@code gotTypeSpec(tokens)} - Property type</li>
 *   <li>{@code gotField(first, idToken, initExpressionFollows)} - Field declaration</li>
 *   <li>{@code endField(token, true)} - End field</li>
 *   <li>{@code endFieldDeclarations(token, true)} - End field declarations</li>
 * </ol>
 * 
 * <h2>Test Coverage</h2>
 * <p>This test suite covers ~20 scenarios:</p>
 * <ul>
 *   <li><b>Basic Properties (Tests 1-8):</b> val/var properties, explicit types,
 *       type inference, initializers, multiple properties</li>
 *   <li><b>Custom Accessors (Tests 9-12):</b> Custom getters/setters,
 *       combined accessors, accessor modifiers</li>
 *   <li><b>Extension Properties (Tests 13-15):</b> Extension properties on various types</li>
 *   <li><b>Edge Cases (Tests 16-20):</b> lateinit, const, delegation, backing fields</li>
 * </ul>
 * 
 * @see bluej.parser.psi.visitor.FileVisitor
 * @see CallbackRecorder
 * @see MethodDeclarationCallbackTest Similar test pattern for M4.1
 * @see ConstructorDeclarationCallbackTest Similar test pattern for M4.2
 */
public class PropertyDeclarationCallbackTest extends BasePsiTest {

    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    // ==================== BASIC PROPERTIES ====================
    
    /**
     * Test 1: Simple val property with explicit type.
     */
    @Test
    public void testValProperty_explicitType() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val name: String = "test"
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify field declaration sequence
        assertTrue("Should have beginFieldDeclarations", 
                  recorder.hasCallback("beginFieldDeclarations"));
        assertTrue("Should have gotField", 
                  recorder.hasCallback("gotField"));
        assertTrue("Should have endField", 
                  recorder.hasCallback("endField"));
        assertTrue("Should have endFieldDeclarations", 
                  recorder.hasCallback("endFieldDeclarations"));
        
        // Verify property name
        List<CallbackRecord> fields = recorder.getCallbacksByName("gotField");
        assertEquals("Should have exactly 1 gotField", 1, fields.size());
        
        Map<String, Object> params = fields.get(0).getParameters();
        LocatableToken idToken = (LocatableToken) params.get("idToken");
        assertNotNull("Property name token should not be null", idToken);
        assertEquals("Property name should be 'name'", "name", idToken.getText());
        
        // Verify has initializer flag
        Boolean initExprFollows = (Boolean) params.get("initExpressionFollows");
        assertTrue("Property should have initializer", initExprFollows);
        
        // Verify type spec
        List<CallbackRecord> typeSpecs = recorder.getCallbacksByName("gotTypeSpec");
        assertTrue("Should have at least 1 gotTypeSpec for property type", typeSpecs.size() >= 1);
        
        // Validate pairing with detailed error reporting
        recorder.assertBalanced();
    }
    
    /**
     * Test 2: Simple var property with explicit type.
     */
    @Test
    public void testVarProperty_explicitType() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                var count: Int = 0
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        List<CallbackRecord> fields = recorder.getCallbacksByName("gotField");
        Map<String, Object> params = fields.get(0).getParameters();
        LocatableToken idToken = (LocatableToken) params.get("idToken");
        assertEquals("Property name should be 'count'", "count", idToken.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 3: Property with type inference.
     */
    @Test
    public void testProperty_typeInference() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val inferred = "inferred type"
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Type spec should be present (marked as "inferred")
        List<CallbackRecord> typeSpecs = recorder.getCallbacksByName("gotTypeSpec");
        assertTrue("Should have gotTypeSpec for inferred type", typeSpecs.size() >= 1);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 4: Property without initializer.
     */
    @Test
    public void testProperty_noInitializer() throws PsiParseException {
        String kotlinCode = """
            class TestClass(name: String) {
                val stored: String
                init {
                    stored = name
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify field without initializer
        List<CallbackRecord> fields = recorder.getCallbacksByName("gotField");
        assertTrue("Should have at least 1 property", fields.size() >= 1);
        
        Map<String, Object> params = fields.get(0).getParameters();
        Boolean initExprFollows = (Boolean) params.get("initExpressionFollows");
        assertFalse("Property should not have initializer expression", initExprFollows);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 5: Property with complex type.
     */
    @Test
    public void testProperty_complexType() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val items: List<String> = listOf()
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field and type spec
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        assertTrue("Should have gotTypeSpec", recorder.hasCallback("gotTypeSpec"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 6: Property with nullable type.
     */
    @Test
    public void testProperty_nullableType() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                var nullable: String? = null
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 7: Multiple properties in same class.
     */
    @Test
    public void testProperties_multiple() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val first: String = "first"
                var second: Int = 0
                val third: Boolean = true
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 3 field declarations
        List<CallbackRecord> fields = recorder.getCallbacksByName("gotField");
        assertEquals("Should have 3 gotField callbacks", 3, fields.size());
        
        // Verify property names
        LocatableToken field1 = (LocatableToken) fields.get(0).getParameters().get("idToken");
        LocatableToken field2 = (LocatableToken) fields.get(1).getParameters().get("idToken");
        LocatableToken field3 = (LocatableToken) fields.get(2).getParameters().get("idToken");
        
        assertEquals("First property", "first", field1.getText());
        assertEquals("Second property", "second", field2.getText());
        assertEquals("Third property", "third", field3.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 8: Property in companion object.
     */
    @Test
    public void testProperty_inCompanionObject() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                companion object {
                    val CONSTANT = 100
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        List<CallbackRecord> fields = recorder.getCallbacksByName("gotField");
        Map<String, Object> params = fields.get(0).getParameters();
        LocatableToken idToken = (LocatableToken) params.get("idToken");
        assertEquals("Property should be 'CONSTANT'", "CONSTANT", idToken.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== MODIFIERS ====================
    
    /**
     * Test 9: Private property.
     */
    @Test
    public void testProperty_privateModifier() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                private val secret: String = "hidden"
            }
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
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 10: Lateinit property.
     */
    @Test
    public void testProperty_lateinit() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                lateinit var deferred: String
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Should have lateinit modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasLateinit = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "lateinit".equals(token.getText());
            });
        assertTrue("Should have lateinit modifier", hasLateinit);
        
        // Property should not have initializer
        List<CallbackRecord> fields = recorder.getCallbacksByName("gotField");
        Map<String, Object> params = fields.get(0).getParameters();
        Boolean initExprFollows = (Boolean) params.get("initExpressionFollows");
        assertFalse("Lateinit property should not have initializer", initExprFollows);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 11: Const property.
     */
    @Test
    public void testProperty_const() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                companion object {
                    const val MAX_SIZE = 100
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Should have const modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasConst = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "const".equals(token.getText());
            });
        assertTrue("Should have const modifier", hasConst);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 12: Override property.
     */
    @Test
    public void testProperty_override() throws PsiParseException {
        String kotlinCode = """
            open class Base {
                open val value: Int = 0
            }
            class Child : Base() {
                override val value: Int = 42
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have override modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasOverride = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "override".equals(token.getText());
            });
        assertTrue("Should have override modifier", hasOverride);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== TOP-LEVEL PROPERTIES ====================
    
    /**
     * Test 13: Top-level property.
     */
    @Test
    public void testProperty_topLevel() throws PsiParseException {
        String kotlinCode = """
            val topLevel: String = "global"
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        List<CallbackRecord> fields = recorder.getCallbacksByName("gotField");
        Map<String, Object> params = fields.get(0).getParameters();
        LocatableToken idToken = (LocatableToken) params.get("idToken");
        assertEquals("Property should be 'topLevel'", "topLevel", idToken.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 14: Top-level var property.
     */
    @Test
    public void testProperty_topLevelVar() throws PsiParseException {
        String kotlinCode = """
            var mutable: Int = 0
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== EXTENSION PROPERTIES ====================
    
    /**
     * Test 15: Simple extension property.
     */
    @Test
    public void testExtensionProperty_simple() throws PsiParseException {
        String kotlinCode = """
            val String.firstChar: Char
                get() = this[0]
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        List<CallbackRecord> fields = recorder.getCallbacksByName("gotField");
        Map<String, Object> params = fields.get(0).getParameters();
        LocatableToken idToken = (LocatableToken) params.get("idToken");
        assertEquals("Property should be 'firstChar'", "firstChar", idToken.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 16: Extension property on generic type.
     */
    @Test
    public void testExtensionProperty_onGenericType() throws PsiParseException {
        String kotlinCode = """
            val <T> List<T>.second: T?
                get() = if (this.size > 1) this[1] else null
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 17: Extension var property with setter.
     */
    @Test
    public void testExtensionProperty_var() throws PsiParseException {
        String kotlinCode = """
            var StringBuilder.lastChar: Char
                get() = this[this.length - 1]
                set(value) { this.setCharAt(this.length - 1, value) }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== CUSTOM ACCESSORS ====================
    
    /**
     * Test 18: Property with custom getter.
     * Note: Custom accessors are deferred to Phase 6 for body traversal.
     */
    @Test
    public void testProperty_customGetter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val computed: String
                    get() = "computed"
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Note: Custom getter body traversal deferred to Phase 6
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 19: Property with custom setter.
     */
    @Test
    public void testProperty_customSetter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                var value: String = ""
                    set(v) { field = v.lowercase() }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 20: Property with both custom getter and setter.
     */
    @Test
    public void testProperty_customGetterAndSetter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                var value: String = ""
                    get() = field.uppercase()
                    set(v) { field = v.lowercase() }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== DELEGATED PROPERTIES ====================
    
    /**
     * Test 21: Property with lazy delegation.
     * Note: Delegation expression parsing deferred to Phase 6.
     */
    @Test
    public void testProperty_lazyDelegation() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val lazy: String by lazy { "computed" }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Note: Delegation expression parsing deferred to Phase 6
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== PROPERTY PARAMETERS ====================
    
    /**
     * Test 22: Primary constructor property parameter.
     * Note: Property parameters are handled by constructor visitor.
     */
    @Test
    public void testProperty_primaryConstructorParameter() throws PsiParseException {
        String kotlinCode = """
            class Person(val name: String, var age: Int)
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Constructor parameters are processed by visitPrimaryConstructor
        // Properties aspect is implicit in ClassInfo
        assertTrue("Should have gotConstructorDecl", recorder.hasCallback("gotConstructorDecl"));
        
        // Should have parameters
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 2 constructor parameters", 2, params.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== EDGE CASES ====================
    
    /**
     * Test 23: Property with backing field reference.
     */
    @Test
    public void testProperty_backingFieldReference() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                var value: String = ""
                    get() = field
                    set(v) { field = v }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have field declaration
        assertTrue("Should have gotField", recorder.hasCallback("gotField"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 24: Property callback sequence order.
     */
    @Test
    public void testProperty_callbackSequenceOrder() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val property: String = "value"
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .toList();
        
        // Find key callback positions
        int beginFieldDecls = sequence.indexOf("beginFieldDeclarations");
        int gotTypeSpec = sequence.indexOf("gotTypeSpec");
        int gotField = sequence.indexOf("gotField");
        int endField = sequence.indexOf("endField");
        int endFieldDecls = sequence.indexOf("endFieldDeclarations");
        
        // Verify all found
        assertTrue("Should have beginFieldDeclarations", beginFieldDecls >= 0);
        assertTrue("Should have gotTypeSpec", gotTypeSpec >= 0);
        assertTrue("Should have gotField", gotField >= 0);
        assertTrue("Should have endField", endField >= 0);
        assertTrue("Should have endFieldDeclarations", endFieldDecls >= 0);
        
        // Verify order
        assertTrue("beginFieldDeclarations should come before gotTypeSpec",
                  beginFieldDecls < gotTypeSpec);
        assertTrue("gotTypeSpec should come before gotField",
                  gotTypeSpec < gotField);
        assertTrue("gotField should come before endField",
                  gotField < endField);
        assertTrue("endField should come before endFieldDeclarations",
                  endField < endFieldDecls);
    }
    
    /**
     * Test 25: Property within class callback sequence.
     */
    @Test
    public void testProperty_nestedInClassSequence() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                val property: String = "value"
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .toList();
        
        // Property sequence should be within class body markers
        int classBeginBody = sequence.indexOf("beginTypeBody");
        int classEndBody = sequence.lastIndexOf("endTypeBody");
        int propertyField = sequence.indexOf("gotField");
        
        assertTrue("Class should have beginTypeBody", classBeginBody >= 0);
        assertTrue("Class should have endTypeBody", classEndBody >= 0);
        assertTrue("Should have property field", propertyField >= 0);
        assertTrue("Property should be between class body markers",
                  classBeginBody < propertyField && propertyField < classEndBody);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
}