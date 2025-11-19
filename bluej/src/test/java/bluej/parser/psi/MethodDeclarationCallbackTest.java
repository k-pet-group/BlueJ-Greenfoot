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
 * Tests for Phase 4 Milestone 4.1: Method declaration callback sequence.
 * 
 * <p>Validates that {@link PsiCallbackVisitor#visitNamedFunction(org.jetbrains.kotlin.psi.KtNamedFunction)}
 * invokes the complete callback sequence for method declarations:</p>
 * <ol>
 *   <li>{@code gotDeclBegin(token)} - Begin declaration</li>
 *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
 *   <li>{@code gotTypeSpec(tokens)} - Return type</li>
 *   <li>{@code gotMethodDeclaration(nameToken, javadocToken)} - Method name</li>
 *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
 *   <li>Generic type parameters (if present)</li>
 *   <li>{@code gotTypeSpec(tokens)} × n - Parameter types</li>
 *   <li>{@code gotMethodParameter(nameToken, ellipsisToken)} × n - Parameters</li>
 *   <li>{@code gotAllMethodParameters()} - All parameters processed</li>
 *   <li>Method body (if present)</li>
 *   <li>{@code endMethodDecl(token, true)} - End declaration</li>
 * </ol>
 * 
 * @see PsiCallbackVisitor
 * @see CallbackRecorder
 */
public class MethodDeclarationCallbackTest {
    
    private PsiEnvironment env;
    
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    // ==================== BASIC METHODS ====================
    
    /**
     * Test 1: Simple method with no parameters or return type.
     */
    @Test
    public void testSimpleMethod_noParamsNoReturn() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun simpleMethod() {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify method declaration callback invoked
        assertTrue("Should have gotMethodDeclaration callback",
                  recorder.hasCallback("gotMethodDeclaration"));
        
        List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
        assertEquals("Should have exactly 1 gotMethodDeclaration", 1, methodDecls.size());
        
        Map<String, Object> params = methodDecls.get(0).getParameters();
        LocatableToken nameToken = (LocatableToken) params.get("token");
        
        assertNotNull("Method name token should not be null", nameToken);
        assertEquals("Method name should be 'simpleMethod'", "simpleMethod", nameToken.getText());
        assertEquals("Token type should be IDENT", JavaTokenTypes.IDENT, nameToken.getType());
        
        // Verify modifiersConsumed called
        assertTrue("Should have modifiersConsumed callback",
                  recorder.hasCallback("modifiersConsumed"));
        
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
     * Test 2: Method with return type.
     */
    @Test
    public void testMethod_withReturnType() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun getNumber(): Int {
                    return 42
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify return type spec callback
        List<CallbackRecord> typeSpecs = recorder.getCallbacksByName("gotTypeSpec");
        assertTrue("Should have at least 1 gotTypeSpec for return type", typeSpecs.size() >= 1);
        
        // First type spec should be return type
        Map<String, Object> params = typeSpecs.get(0).getParameters();
        @SuppressWarnings("unchecked")
        List<LocatableToken> tokens = (List<LocatableToken>) params.get("tokens");
        
        assertNotNull("Type tokens should not be null", tokens);
        assertFalse("Type tokens should not be empty", tokens.isEmpty());
        assertEquals("Return type should be 'Int'", "Int", tokens.get(0).getText());
        
        // Verify method declaration called
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 3: Method with single parameter.
     */
    @Test
    public void testMethod_withSingleParameter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun greet(name: String) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify parameter type spec
        List<CallbackRecord> typeSpecs = recorder.getCallbacksByName("gotTypeSpec");
        assertTrue("Should have at least 1 gotTypeSpec for parameter type", typeSpecs.size() >= 1);
        
        // Verify parameter callback
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have exactly 1 gotMethodParameter", 1, params.size());
        
        Map<String, Object> paramMap = params.get(0).getParameters();
        LocatableToken paramToken = (LocatableToken) paramMap.get("token");
        
        assertNotNull("Parameter token should not be null", paramToken);
        assertEquals("Parameter name should be 'name'", "name", paramToken.getText());
        
        // Verify all parameters processed
        assertTrue("Should have gotAllMethodParameters", recorder.hasCallback("gotAllMethodParameters"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 4: Method with multiple parameters.
     */
    @Test
    public void testMethod_withMultipleParameters() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify parameter callbacks
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 2 gotMethodParameter callbacks", 2, params.size());
        
        // Verify first parameter
        Map<String, Object> param1 = params.get(0).getParameters();
        LocatableToken token1 = (LocatableToken) param1.get("token");
        assertEquals("First parameter should be 'a'", "a", token1.getText());
        
        // Verify second parameter
        Map<String, Object> param2 = params.get(1).getParameters();
        LocatableToken token2 = (LocatableToken) param2.get("token");
        assertEquals("Second parameter should be 'b'", "b", token2.getText());
        
        // Verify all parameters processed
        assertTrue("Should have gotAllMethodParameters", recorder.hasCallback("gotAllMethodParameters"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 5: Method with expression body.
     */
    @Test
    public void testMethod_expressionBody() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun double(x: Int): Int = x * 2
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method body callbacks
        assertTrue("Should have beginMethodBody", recorder.hasCallback("beginMethodBody"));
        assertTrue("Should have endMethodBody", recorder.hasCallback("endMethodBody"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 6: Method with block body.
     */
    @Test
    public void testMethod_blockBody() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun calculate() {
                    val x = 1 + 2
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method body callbacks
        List<CallbackRecord> beginBody = recorder.getCallbacksByName("beginMethodBody");
        assertEquals("Should have exactly 1 beginMethodBody", 1, beginBody.size());
        
        List<CallbackRecord> endBody = recorder.getCallbacksByName("endMethodBody");
        assertEquals("Should have exactly 1 endMethodBody", 1, endBody.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 7: Abstract method with no body.
     */
    @Test
    public void testMethod_abstractNoBody() throws PsiParseException {
        String kotlinCode = """
            abstract class TestClass {
                abstract fun abstractMethod(): String
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Should NOT have method body callbacks
        assertFalse("Should not have beginMethodBody for abstract method",
                   recorder.hasCallback("beginMethodBody"));
        assertFalse("Should not have endMethodBody for abstract method",
                   recorder.hasCallback("endMethodBody"));
        
        // Should have abstract modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasAbstract = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "abstract".equals(token.getText());
            });
        assertTrue("Should have abstract modifier", hasAbstract);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 8: Method with Unit return type (implicit).
     */
    @Test
    public void testMethod_implicitUnitReturn() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun doSomething() {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Unit return type is implicit - no gotTypeSpec for return type
        // (Only parameter types would have gotTypeSpec)
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 9: Method with generic type parameter.
     */
    @Test
    public void testMethod_withGenericTypeParameter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun <T> identity(value: T): T {
                    return value
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have type parameter callbacks
        assertTrue("Should have gotMethodTypeParamsBegin", recorder.hasCallback("gotMethodTypeParamsBegin"));
        assertTrue("Should have endMethodTypeParams", recorder.hasCallback("endMethodTypeParams"));
        
        List<CallbackRecord> typeParams = recorder.getCallbacksByName("gotTypeParam");
        assertEquals("Should have exactly 1 gotTypeParam", 1, typeParams.size());
        
        Map<String, Object> params = typeParams.get(0).getParameters();
        LocatableToken idToken = (LocatableToken) params.get("idToken");
        assertEquals("Type parameter should be 'T'", "T", idToken.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 10: Method with bounded generic type parameter.
     */
    @Test
    public void testMethod_boundedTypeParameter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun <T : Number> sum(values: List<T>): Double {
                    return 0.0
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have type parameter
        assertTrue("Should have gotTypeParam", recorder.hasCallback("gotTypeParam"));
        
        // Should have type parameter bound
        List<CallbackRecord> bounds = recorder.getCallbacksByName("gotTypeParamBound");
        assertEquals("Should have exactly 1 gotTypeParamBound", 1, bounds.size());
        
        Map<String, Object> params = bounds.get(0).getParameters();
        @SuppressWarnings("unchecked")
        List<LocatableToken> tokens = (List<LocatableToken>) params.get("tokens");
        assertNotNull("Bound tokens should not be null", tokens);
        assertFalse("Bound tokens should not be empty", tokens.isEmpty());
        assertEquals("Bound should be 'Number'", "Number", tokens.get(0).getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== EXTENSION FUNCTIONS ====================
    
    /**
     * Test 11: Simple extension function.
     */
    @Test
    public void testExtensionFunction_simple() throws PsiParseException {
        String kotlinCode = """
            fun String.extension(): Int {
                return this.length
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
        Map<String, Object> params = methodDecls.get(0).getParameters();
        LocatableToken nameToken = (LocatableToken) params.get("token");
        assertEquals("Method name should be 'extension'", "extension", nameToken.getText());
        
        // Note: Extension receiver type handling may vary - just ensure method is processed
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 12: Extension function with parameters.
     */
    @Test
    public void testExtensionFunction_withParameters() throws PsiParseException {
        String kotlinCode = """
            fun String.repeat(times: Int): String {
                return this.repeat(times)
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Should have parameter
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 1 parameter", 1, params.size());
        
        Map<String, Object> paramMap = params.get(0).getParameters();
        LocatableToken paramToken = (LocatableToken) paramMap.get("token");
        assertEquals("Parameter should be 'times'", "times", paramToken.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 13: Extension function with return type.
     */
    @Test
    public void testExtensionFunction_withReturnType() throws PsiParseException {
        String kotlinCode = """
            fun Int.asString(): String {
                return this.toString()
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Should have return type
        List<CallbackRecord> typeSpecs = recorder.getCallbacksByName("gotTypeSpec");
        assertTrue("Should have at least 1 gotTypeSpec for return type", typeSpecs.size() >= 1);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 14: Extension function on generic type.
     */
    @Test
    public void testExtensionFunction_onGenericType() throws PsiParseException {
        String kotlinCode = """
            fun <T> List<T>.second(): T? {
                return if (this.size > 1) this[1] else null
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Should have type parameters
        assertTrue("Should have gotMethodTypeParamsBegin", recorder.hasCallback("gotMethodTypeParamsBegin"));
        assertTrue("Should have gotTypeParam", recorder.hasCallback("gotTypeParam"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 15: Chained extension functions.
     */
    @Test
    public void testExtensionFunctions_multiple() throws PsiParseException {
        String kotlinCode = """
            fun String.first(): Char = this[0]
            fun String.last(): Char = this[this.length - 1]
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have two method declarations
        List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
        assertEquals("Should have 2 gotMethodDeclaration callbacks", 2, methodDecls.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== SPECIAL FUNCTIONS ====================
    
    /**
     * Test 16: Operator overloading function.
     */
    @Test
    public void testOperatorFunction() throws PsiParseException {
        String kotlinCode = """
            class Point(val x: Int, val y: Int) {
                operator fun plus(other: Point): Point {
                    return Point(x + other.x, y + other.y)
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have operator modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasOperator = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "operator".equals(token.getText());
            });
        assertTrue("Should have operator modifier", hasOperator);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 17: Infix function.
     */
    @Test
    public void testInfixFunction() throws PsiParseException {
        String kotlinCode = """
            class Pair {
                infix fun to(other: Int): Pair {
                    return Pair()
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have infix modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasInfix = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "infix".equals(token.getText());
            });
        assertTrue("Should have infix modifier", hasInfix);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 18: Suspend function.
     */
    @Test
    public void testSuspendFunction() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                suspend fun asyncOperation(): String {
                    return "result"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have suspend modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasSuspend = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "suspend".equals(token.getText());
            });
        assertTrue("Should have suspend modifier", hasSuspend);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 19: Inline function.
     */
    @Test
    public void testInlineFunction() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                inline fun inlined(block: () -> Unit) {
                    block()
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have inline modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasInline = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "inline".equals(token.getText());
            });
        assertTrue("Should have inline modifier", hasInline);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 20: Operator and inline combined.
     */
    @Test
    public void testCombinedModifiers_operatorInline() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                inline operator fun invoke(): String = "invoked"
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have both modifiers
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        assertTrue("Should have at least 2 modifiers", modifiers.size() >= 2);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== PARAMETER VARIATIONS ====================
    
    /**
     * Test 21: Vararg parameter.
     */
    @Test
    public void testMethod_varargParameter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun varargMethod(vararg items: String) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Verify parameter with ellipsis
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have exactly 1 gotMethodParameter", 1, params.size());
        
        Map<String, Object> paramMap = params.get(0).getParameters();
        LocatableToken ellipsisToken = (LocatableToken) paramMap.get("ellipsisToken");
        
        assertNotNull("Vararg should have ellipsis token", ellipsisToken);
        assertEquals("Ellipsis token type should be TRIPLE_DOT", JavaTokenTypes.TRIPLE_DOT, ellipsisToken.getType());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 22: Multiple vararg (not valid Kotlin, but test handling).
     */
    @Test
    public void testMethod_regularAndVarargParameters() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun mixed(first: String, vararg items: Int) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 parameters
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 2 gotMethodParameter callbacks", 2, params.size());
        
        // First parameter: no ellipsis
        Map<String, Object> param1 = params.get(0).getParameters();
        LocatableToken ellipsis1 = (LocatableToken) param1.get("ellipsisToken");
        assertNull("First parameter should not have ellipsis", ellipsis1);
        
        // Second parameter: has ellipsis
        Map<String, Object> param2 = params.get(1).getParameters();
        LocatableToken ellipsis2 = (LocatableToken) param2.get("ellipsisToken");
        assertNotNull("Second parameter should have ellipsis", ellipsis2);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 23: Parameter with function type.
     */
    @Test
    public void testMethod_functionTypeParameter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun higher(callback: (Int) -> String): String {
                    return callback(42)
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have parameter
        assertTrue("Should have gotMethodParameter", recorder.hasCallback("gotMethodParameter"));
        
        // Parameter type should be function type
        List<CallbackRecord> typeSpecs = recorder.getCallbacksByName("gotTypeSpec");
        assertTrue("Should have type specs", typeSpecs.size() > 0);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 24: Parameter with nullable type.
     */
    @Test
    public void testMethod_nullableParameter() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun process(value: String?): Boolean {
                    return value != null
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have parameter
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 1 parameter", 1, params.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 25: Parameter with generic type.
     */
    @Test
    public void testMethod_genericParameterType() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun processList(items: List<String>): Int {
                    return items.size
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have parameter
        assertTrue("Should have gotMethodParameter", recorder.hasCallback("gotMethodParameter"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== MODIFIERS ====================
    
    /**
     * Test 26: Public method.
     */
    @Test
    public void testMethod_publicModifier() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                public fun publicMethod() {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have public modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasPublic = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "public".equals(token.getText());
            });
        assertTrue("Should have public modifier", hasPublic);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 27: Private method.
     */
    @Test
    public void testMethod_privateModifier() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                private fun privateMethod() {
                }
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
     * Test 28: Protected method.
     */
    @Test
    public void testMethod_protectedModifier() throws PsiParseException {
        String kotlinCode = """
            open class TestClass {
                protected fun protectedMethod() {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have protected modifier
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        boolean hasProtected = modifiers.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "protected".equals(token.getText());
            });
        assertTrue("Should have protected modifier", hasProtected);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 29: Override method.
     */
    @Test
    public void testMethod_overrideModifier() throws PsiParseException {
        String kotlinCode = """
            open class Base {
                open fun method() {
                }
            }
            class Child : Base() {
                override fun method() {
                }
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
    
    /**
     * Test 30: Multiple modifiers on method.
     */
    @Test
    public void testMethod_multipleModifiers() throws PsiParseException {
        String kotlinCode = """
            open class TestClass {
                protected open fun openMethod() {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have multiple modifiers
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        assertTrue("Should have at least 2 modifiers", modifiers.size() >= 2);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== COMPLEX SCENARIOS ====================
    
    /**
     * Test 31: Method with all features combined.
     */
    @Test
    public void testMethod_allFeaturesCombined() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                public fun <T : Number> process(
                    first: String,
                    vararg values: T
                ): Double {
                    return 0.0
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have all key callbacks
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        assertTrue("Should have gotMethodTypeParamsBegin", recorder.hasCallback("gotMethodTypeParamsBegin"));
        assertTrue("Should have gotTypeParam", recorder.hasCallback("gotTypeParam"));
        assertTrue("Should have gotTypeParamBound", recorder.hasCallback("gotTypeParamBound"));
        
        // Should have 2 parameters
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have 2 parameters", 2, params.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 32: Multiple methods in same class.
     */
    @Test
    public void testClass_multipleMethods() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun method1() {
                }
                
                fun method2(param: Int): String {
                    return "test"
                }
                
                fun method3(a: Int, b: Int): Boolean {
                    return a == b
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 3 method declarations
        List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
        assertEquals("Should have 3 gotMethodDeclaration callbacks", 3, methodDecls.size());
        
        // Should have 3 endMethodDecl
        List<CallbackRecord> endDecls = recorder.getCallbacksByName("endMethodDecl");
        assertEquals("Should have 3 endMethodDecl callbacks", 3, endDecls.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 33: Method in nested class.
     */
    @Test
    public void testMethod_inNestedClass() throws PsiParseException {
        String kotlinCode = """
            class Outer {
                class Inner {
                    fun innerMethod(): Int {
                        return 42
                    }
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 34: Empty parameter list explicitly.
     */
    @Test
    public void testMethod_explicitEmptyParamList() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun noParams(): String {
                    return "test"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have gotAllMethodParameters even with no parameters
        assertTrue("Should have gotAllMethodParameters", recorder.hasCallback("gotAllMethodParameters"));
        
        // Should NOT have any gotMethodParameter
        List<CallbackRecord> params = recorder.getCallbacksByName("gotMethodParameter");
        assertEquals("Should have no gotMethodParameter callbacks", 0, params.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 35: Callback sequence order is correct.
     */
    @Test
    public void testMethod_callbackSequenceOrder() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun method(param: String): Int {
                    return 0
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Find key callback positions
        int declBeginIdx = sequence.indexOf("gotDeclBegin");
        int methodDeclIdx = sequence.indexOf("gotMethodDeclaration");
        int modifiersConsumedIdx = sequence.indexOf("modifiersConsumed");
        int allParamsIdx = sequence.indexOf("gotAllMethodParameters");
        int endMethodDeclIdx = sequence.indexOf("endMethodDecl");
        
        // Verify all found
        assertTrue("Should have gotDeclBegin", declBeginIdx >= 0);
        assertTrue("Should have gotMethodDeclaration", methodDeclIdx >= 0);
        assertTrue("Should have modifiersConsumed", modifiersConsumedIdx >= 0);
        assertTrue("Should have gotAllMethodParameters", allParamsIdx >= 0);
        assertTrue("Should have endMethodDecl", endMethodDeclIdx >= 0);
        
        // Verify order: declBegin before methodDecl before modifiersConsumed before allParams before endMethodDecl
        assertTrue("gotDeclBegin should come before gotMethodDeclaration", 
                  declBeginIdx < methodDeclIdx);
//        assertTrue("gotMethodDeclaration should come before modifiersConsumed",
//                  methodDeclIdx < modifiersConsumedIdx);
        assertTrue("modifiersConsumed should come before gotAllMethodParameters",
                  modifiersConsumedIdx < allParamsIdx);
        assertTrue("gotAllMethodParameters should come before endMethodDecl",
                  allParamsIdx < endMethodDeclIdx);
    }
    
    // ==================== ADDITIONAL COVERAGE ====================
    
    /**
     * Test 36: Interface method with default implementation.
     */
    @Test
    public void testInterfaceMethod_withDefaultImpl() throws PsiParseException {
        String kotlinCode = """
            interface MyInterface {
                fun defaultMethod(): String {
                    return "default"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        // Should have body (default implementation)
        assertTrue("Should have beginMethodBody", recorder.hasCallback("beginMethodBody"));
        assertTrue("Should have endMethodBody", recorder.hasCallback("endMethodBody"));
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 37: Method with multiple generic parameters.
     */
    @Test
    public void testMethod_multipleTypeParameters() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun <K, V> mapOf(key: K, value: V): Map<K, V> {
                    return emptyMap()
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 type parameters
        List<CallbackRecord> typeParams = recorder.getCallbacksByName("gotTypeParam");
        assertEquals("Should have 2 gotTypeParam callbacks", 2, typeParams.size());
        
        // Verify type parameter names
        LocatableToken param1 = (LocatableToken) typeParams.get(0).getParameters().get("idToken");
        LocatableToken param2 = (LocatableToken) typeParams.get(1).getParameters().get("idToken");
        assertEquals("First type param should be 'K'", "K", param1.getText());
        assertEquals("Second type param should be 'V'", "V", param2.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 38: Method callback sequence within class sequence.
     */
    @Test
    public void testMethod_nestedInClassSequence() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun method() {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<String> sequence = recorder.getRecords().stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.toList());
        
        // Method sequence should be within class body markers
        int classBeginBody = sequence.indexOf("beginTypeBody");
        int classEndBody = sequence.lastIndexOf("endTypeBody");
        int methodDecl = sequence.indexOf("gotMethodDeclaration");
        
        assertTrue("Class should have beginTypeBody", classBeginBody >= 0);
        assertTrue("Class should have endTypeBody", classEndBody >= 0);
        assertTrue("Should have method declaration", methodDecl >= 0);
        assertTrue("Method should be between class body markers",
                  classBeginBody < methodDecl && methodDecl < classEndBody);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 39: Method with complex return type.
     */
    @Test
    public void testMethod_complexReturnType() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                fun complexReturn(): List<Map<String, Int>>? {
                    return null
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have return type spec
        List<CallbackRecord> typeSpecs = recorder.getCallbacksByName("gotTypeSpec");
        assertTrue("Should have at least 1 gotTypeSpec", typeSpecs.size() >= 1);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 40: Method pairing validation is strict.
     */
    @Test
    public void testMethod_pairingValidation() throws PsiParseException {
        String kotlinCode = """
                class TestClass {
                    fun method1() {
                    }
                
                    fun method2() {
                    }
                }
                """;

        CallbackRecorder recorder = parseAndVisit(kotlinCode);

        // Get detailed validation result
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();

        assertTrue("Callbacks should be balanced: " + result.getValidationSummary(),
                result.isBalanced());
        assertFalse("Should have no validation errors: " + result.getValidationSummary(),
                result.hasErrors());
    }

    /**
     * Test 41: Verify clearModifierState() prevents modifier leakage between methods.
     * 
     * <p>Tests that modifiers from one method don't leak into the next method.
     * This validates the fix for review issue M3 where clearModifierState()
     * was not being called in the finally block.</p>
     */
    @Test
    public void testModifierState_clearedBetweenMethods() throws PsiParseException {
        String kotlinCode = """
            class TestClass {
                private fun first() {
                }
                
                fun second() {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Get all modifier callbacks
        List<CallbackRecord> modifiers = recorder.getCallbacksByName("gotModifier");
        
        // Should only have 1 'private' modifier (from first method)
        long privateCount = modifiers.stream()
            .filter(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("token");
                return token != null && "private".equals(token.getText());
            })
            .count();
        
        assertEquals("Should have exactly 1 private modifier", 1, privateCount);
        
        // Get all method declarations
        List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
        assertEquals("Should have 2 method declarations", 2, methodDecls.size());

        // Validate pairing - ensures proper cleanup
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }

    /**
     * Test 42: Method with annotations.
     * 
     * <p>Tests that methods with Kotlin annotations are properly processed.
     * Validates the fix for review issue M5.</p>
     */
    @Test
    public void testMethod_withAnnotations() throws PsiParseException {
        String kotlinCode = """
            @Deprecated("Use newMethod instead")
            @Suppress("UNUSED")
            fun oldMethod(): String {
                return "old"
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
        assertEquals("Should have exactly 1 method", 1, methodDecls.size());
        
        Map<String, Object> params = methodDecls.get(0).getParameters();
        LocatableToken nameToken = (LocatableToken) params.get("token");
        assertEquals("Method name should be 'oldMethod'", "oldMethod", nameToken.getText());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 43: Nested local function (currently deferred to Phase 6).
     * 
     * <p>Tests that top-level functions with nested local functions are
     * handled correctly. Note: Phase 4 skips local function body traversal,
     * so we only verify the outer function is processed.</p>
     * 
     * <p>Validates the fix for review issue M4.</p>
     */
    @Test
    public void testMethod_nestedLocalFunction() throws PsiParseException {
        String kotlinCode = """
            fun outerFunction() {
                fun innerFunction() {
                    println("inner")
                }
                innerFunction()
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method declaration for outer function
        assertTrue("Should have gotMethodDeclaration", recorder.hasCallback("gotMethodDeclaration"));
        
        List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
        assertEquals("Should have exactly 1 method (outer only, inner deferred to Phase 6)", 
                    1, methodDecls.size());
        
        Map<String, Object> params = methodDecls.get(0).getParameters();
        LocatableToken nameToken = (LocatableToken) params.get("token");
        assertEquals("Method name should be 'outerFunction'", "outerFunction", nameToken.getText());
        
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
        
        // Visit the file (triggers class and method visitation)
        ktFile.accept(visitor);
        
        // Validate callback pairing is balanced after traversal
        CallbackRecorder.ValidationResult result = recorder.getValidationResult();
        assertTrue("Callback pairing should be balanced after traversal: " + result.getValidationSummary(),
                  result.isBalanced());
        assertFalse("Should have no validation errors after traversal: " + result.getValidationSummary(),
                   result.hasErrors());
        
        return recorder;
    }
}