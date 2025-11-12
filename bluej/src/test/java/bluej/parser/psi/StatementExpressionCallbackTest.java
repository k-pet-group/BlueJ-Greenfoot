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

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for Phase 6: Statement and Expression Parsing.
 * 
 * <p>Validates that {@link PsiCallbackVisitor} correctly processes all statement
 * and expression types introduced in Phase 6:</p>
 * <ul>
 *   <li><b>Phase 6.1:</b> Block expressions, if/else, return, variable declarations</li>
 *   <li><b>Phase 6.2:</b> Loops (for, while, do-while, break, continue)</li>
 *   <li><b>Phase 6.3:</b> When expressions (Kotlin's switch)</li>
 *   <li><b>Phase 6.4:</b> Exception handling (try-catch-finally, throw)</li>
 *   <li><b>Phase 6.5:</b> Primary expressions (literals, operators, calls, member access)</li>
 *   <li><b>Phase 6.6:</b> Advanced expressions (lambdas, type operations, null safety)</li>
 * </ul>
 * 
 * @see PsiCallbackVisitor
 * @see CallbackRecorder
 */
//@Ignore("Statement parsing disabled for now")
public class StatementExpressionCallbackTest {
    
    private PsiEnvironment env;
    
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    // ==================== PHASE 6.1: CONTROL FLOW ====================
    
    /**
     * Test 1: Block expression with multiple statements.
     */
    @Test
    public void testBlockExpression_multipleStatements() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                val x = 1
                val y = 2
                println(x + y)
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have block body callbacks
        assertTrue("Should have beginStmtblockBody", 
                  recorder.hasCallback("beginStmtblockBody"));
        assertTrue("Should have endStmtblockBody", 
                  recorder.hasCallback("endStmtblockBody"));
        
        // Should have statement wrapping (beginElement/endElement per statement)
        List<CallbackRecord> beginElements = recorder.getCallbacksByName("beginElement");
        assertTrue("Should have beginElement for statements", beginElements.size() >= 3);
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 2: If expression with then and else branches.
     */
    @Test
    public void testIfExpression_withElse() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int): String {
                if (x > 0) {
                    return "positive"
                } else {
                    return "non-positive"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have if statement callbacks
        assertTrue("Should have beginIfStmt", recorder.hasCallback("beginIfStmt"));
        assertTrue("Should have endIfStmt", recorder.hasCallback("endIfStmt"));
        
        // Should have condition blocks
        List<CallbackRecord> condBlocks = recorder.getCallbacksByName("beginIfCondBlock");
        assertEquals("Should have 2 condition blocks (then + else)", 2, condBlocks.size());
        
        // Validate pairing
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 3: If expression without else.
     */
    @Test
    public void testIfExpression_noElse() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int) {
                if (x > 0) {
                    println("positive")
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have beginIfStmt", recorder.hasCallback("beginIfStmt"));
        
        // Should have only 1 condition block (then branch)
        List<CallbackRecord> condBlocks = recorder.getCallbacksByName("beginIfCondBlock");
        assertEquals("Should have 1 condition block (then only)", 1, condBlocks.size());
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 4: Else-if chain.
     */
    @Test
    public void testIfExpression_elseIfChain() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int): String {
                if (x > 0) {
                    return "positive"
                } else if (x < 0) {
                    return "negative"
                } else {
                    return "zero"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have gotElseIf callback
        assertTrue("Should have gotElseIf", recorder.hasCallback("gotElseIf"));
        
        // Multiple beginIfStmt for nested structure
        List<CallbackRecord> ifStmts = recorder.getCallbacksByName("beginIfStmt");
        assertTrue("Should have multiple beginIfStmt for else-if", ifStmts.size() >= 2);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 5: Return statement with value.
     */
    @Test
    public void testReturnStatement_withValue() throws PsiParseException {
        String kotlinCode = """
            fun test(): Int {
                return 42
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have return statement callback
        List<CallbackRecord> returns = recorder.getCallbacksByName("gotReturnStatement");
        assertEquals("Should have 1 return statement", 1, returns.size());
        
        Map<String, Object> params = returns.get(0).getParameters();
        Boolean hasValue = (Boolean) params.get("hasValue");
        assertTrue("Return should have value", hasValue);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 6: Return statement without value (Unit return).
     */
    @Test
    public void testReturnStatement_noValue() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                return
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> returns = recorder.getCallbacksByName("gotReturnStatement");
        assertEquals("Should have 1 return statement", 1, returns.size());
        
        Map<String, Object> params = returns.get(0).getParameters();
        Boolean hasValue = (Boolean) params.get("hasValue");
        assertFalse("Return should not have value", hasValue);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== PHASE 6.2: LOOPS ====================
    
    /**
     * Test 7: For loop with range.
     */
    @Test
    public void testForLoop_withRange() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                for (i in 1..10) {
                    println(i)
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have for loop callbacks
        assertTrue("Should have beginForLoop", recorder.hasCallback("beginForLoop"));
        assertTrue("Should have endForLoop", recorder.hasCallback("endForLoop"));
        
        // Should have loop variable declaration
        assertTrue("Should have beginForInitDecl", recorder.hasCallback("beginForInitDecl"));
        assertTrue("Should have gotForInit", recorder.hasCallback("gotForInit"));
        
        // Should have test expression
        assertTrue("Should have gotForTest", recorder.hasCallback("gotForTest"));
        
        // Should have loop body
        assertTrue("Should have beginForLoopBody", recorder.hasCallback("beginForLoopBody"));
        assertTrue("Should have endForLoopBody", recorder.hasCallback("endForLoopBody"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 8: While loop.
     */
    @Test
    public void testWhileLoop() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                var x = 10
                while (x > 0) {
                    x--
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have beginWhileLoop", recorder.hasCallback("beginWhileLoop"));
        assertTrue("Should have endWhileLoop", recorder.hasCallback("endWhileLoop"));
        assertTrue("Should have beginWhileLoopBody", recorder.hasCallback("beginWhileLoopBody"));
        assertTrue("Should have endWhileLoopBody", recorder.hasCallback("endWhileLoopBody"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 9: Do-while loop.
     */
    @Test
    public void testDoWhileLoop() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                var x = 10
                do {
                    x--
                } while (x > 0)
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have beginDoWhile", recorder.hasCallback("beginDoWhile"));
        assertTrue("Should have endDoWhile", recorder.hasCallback("endDoWhile"));
        assertTrue("Should have beginDoWhileBody", recorder.hasCallback("beginDoWhileBody"));
        assertTrue("Should have endDoWhileBody", recorder.hasCallback("endDoWhileBody"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 10: Break statement.
     */
    @Test
    public void testBreakStatement() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                while (true) {
                    break
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have break callback
        List<CallbackRecord> breaks = recorder.getCallbacksByName("gotBreakContinue");
        assertTrue("Should have at least 1 break/continue", breaks.size() >= 1);
        
        Map<String, Object> params = breaks.get(0).getParameters();
        LocatableToken keywordToken = (LocatableToken) params.get("keywordToken");
        assertEquals("Should be break keyword", "break", keywordToken.getText());
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 11: Continue statement.
     */
    @Test
    public void testContinueStatement() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                for (i in 1..10) {
                    if (i % 2 == 0) continue
                    println(i)
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> continues = recorder.getCallbacksByName("gotBreakContinue");
        assertTrue("Should have continue statement", continues.size() >= 1);
        
        // Find the continue (not break)
        boolean foundContinue = continues.stream()
            .anyMatch(r -> {
                LocatableToken token = (LocatableToken) r.getParameters().get("keywordToken");
                return token != null && "continue".equals(token.getText());
            });
        assertTrue("Should have continue keyword", foundContinue);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 12: Labeled break.
     */
    @Test
    public void testBreak_withLabel() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                outer@ for (i in 1..10) {
                    for (j in 1..10) {
                        if (i * j > 50) break@outer
                    }
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        List<CallbackRecord> breaks = recorder.getCallbacksByName("gotBreakContinue");
        assertTrue("Should have break statement", breaks.size() >= 1);
        
        // Check if any break has a label
        boolean hasLabel = breaks.stream()
            .anyMatch(r -> r.getParameters().get("labelToken") != null);
        assertTrue("Should have labeled break", hasLabel);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== PHASE 6.3: WHEN EXPRESSIONS ====================
    
    /**
     * Test 13: Simple when expression.
     */
    @Test
    public void testWhenExpression_simple() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int): String {
                when (x) {
                    1 -> return "one"
                    2 -> return "two"
                    else -> return "other"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have switch (when) callbacks
        assertTrue("Should have beginSwitchStmt", recorder.hasCallback("beginSwitchStmt"));
        assertTrue("Should have endSwitchStmt", recorder.hasCallback("endSwitchStmt"));
        assertTrue("Should have beginSwitchBlock", recorder.hasCallback("beginSwitchBlock"));
        assertTrue("Should have endSwitchBlock", recorder.hasCallback("endSwitchBlock"));
        
        // Should have case callbacks
        List<CallbackRecord> cases = recorder.getCallbacksByName("beginSwitchCase");
        assertTrue("Should have switch cases", cases.size() >= 2);
        
        // Should have default (else) clause
        assertTrue("Should have gotSwitchDefault", recorder.hasCallback("gotSwitchDefault"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 14: When expression with multiple conditions per entry.
     */
    @Test
    public void testWhenExpression_multipleConditions() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int): String {
                when (x) {
                    1, 2, 3 -> return "small"
                    4, 5 -> return "medium"
                    else -> return "large"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have beginSwitchStmt", recorder.hasCallback("beginSwitchStmt"));
        
        // Should have multiple cases
        List<CallbackRecord> cases = recorder.getCallbacksByName("beginSwitchCase");
        assertTrue("Should have multiple cases", cases.size() >= 2);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 15: When expression without subject.
     */
    @Test
    public void testWhenExpression_noSubject() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int, y: Int): String {
                when {
                    x > y -> return "x greater"
                    x < y -> return "y greater"
                    else -> return "equal"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have beginSwitchStmt", recorder.hasCallback("beginSwitchStmt"));
        assertTrue("Should have switch cases", recorder.hasCallback("beginSwitchCase"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== PHASE 6.4: EXCEPTION HANDLING ====================
    
    /**
     * Test 16: Try-catch block.
     */
    @Test
    public void testTryCatch() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                try {
                    riskyOperation()
                } catch (e: Exception) {
                    handleError(e)
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have try-catch callbacks
        assertTrue("Should have beginTryCatchSmt", recorder.hasCallback("beginTryCatchSmt"));
        assertTrue("Should have endTryCatchStmt", recorder.hasCallback("endTryCatchStmt"));
        assertTrue("Should have beginTryBlock", recorder.hasCallback("beginTryBlock"));
        assertTrue("Should have endTryBlock", recorder.hasCallback("endTryBlock"));
        
        // Should have catch clause
        List<CallbackRecord> catchClauses = recorder.getCallbacksByName("gotCatchFinally");
        assertTrue("Should have at least 1 catch clause", catchClauses.size() >= 1);
        
        // Should have catch variable
        assertTrue("Should have gotCatchVarName", recorder.hasCallback("gotCatchVarName"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 17: Try-catch-finally block.
     */
    @Test
    public void testTryCatchFinally() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                try {
                    riskyOperation()
                } catch (e: Exception) {
                    handleError(e)
                } finally {
                    cleanup()
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 gotCatchFinally callbacks (catch + finally)
        List<CallbackRecord> catchFinally = recorder.getCallbacksByName("gotCatchFinally");
        assertEquals("Should have 2 gotCatchFinally (catch + finally)", 2, catchFinally.size());
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 18: Multiple catch clauses.
     */
    @Test
    public void testTryCatch_multipleCatches() throws PsiParseException {
        String kotlinCode = """
            import java.io.IOException
            import java.lang.IllegalArgumentException
            
            fun test() {
                try {
                    riskyOperation()
                } catch (e: IOException) {
                    handleIO(e)
                } catch (e: IllegalArgumentException) {
                    handleArg(e)
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have multiple catch clauses
        List<CallbackRecord> catches = recorder.getCallbacksByName("gotCatchFinally");
        assertTrue("Should have multiple catch clauses", catches.size() >= 2);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 19: Throw statement.
     */
    @Test
    public void testThrowStatement() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int) {
                if (x < 0) {
                    throw IllegalArgumentException("Negative value")
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have throw callback
        assertTrue("Should have gotThrow", recorder.hasCallback("gotThrow"));
        
        List<CallbackRecord> throwCallbacks = recorder.getCallbacksByName("gotThrow");
        assertEquals("Should have 1 throw statement", 1, throwCallbacks.size());
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== PHASE 6.5: PRIMARY EXPRESSIONS ====================
    
    /**
     * Test 20: Literal expressions.
     */
    @Test
    public void testLiterals() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                val a = 42
                val b = 3.14
                val c = true
                val d = "hello"
                val e = 'x'
                val f = null
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have multiple literal callbacks
        List<CallbackRecord> literals = recorder.getCallbacksByName("gotLiteral");
        assertTrue("Should have multiple literals", literals.size() >= 6);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 21: Binary operators.
     */
    @Test
    public void testBinaryOperators() throws PsiParseException {
        String kotlinCode = """
            fun test(a: Int, b: Int): Int {
                val sum = a + b
                val diff = a - b
                val prod = a * b
                val quot = a / b
                val comp = a > b
                val eq = a == b
                return sum
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have binary operator callbacks
        List<CallbackRecord> operators = recorder.getCallbacksByName("gotBinaryOperator");
        assertTrue("Should have multiple binary operators", operators.size() >= 6);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 22: Unary operators.
     */
    @Test
    public void testUnaryOperators() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int, flag: Boolean): Int {
                val neg = -x
                val pos = +x
                val not = !flag
                var y = x
                y++
                return y
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have unary operator callbacks
        List<CallbackRecord> unary = recorder.getCallbacksByName("gotUnaryOperator");
        List<CallbackRecord> post = recorder.getCallbacksByName("gotPostOperator");
        
        assertTrue("Should have unary operators", (unary.size() + post.size()) >= 3);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 23: Method call expression.
     */
    @Test
    public void testMethodCall() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                println("Hello")
                val result = calculate(1, 2, 3)
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method call callbacks
        List<CallbackRecord> calls = recorder.getCallbacksByName("gotMethodCall");
        assertTrue("Should have method calls", calls.size() >= 2);
        
        // Should have argument lists
        assertTrue("Should have beginArgumentList", recorder.hasCallback("beginArgumentList"));
        assertTrue("Should have endArgumentList", recorder.hasCallback("endArgumentList"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 24: Member access (dot operator).
     */
    @Test
    public void testMemberAccess() throws PsiParseException {
        String kotlinCode = """
            fun test(obj: MyClass) {
                val x = obj.property
                obj.method()
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have member access callbacks
        List<CallbackRecord> access = recorder.getCallbacksByName("gotMemberAccess");
        assertTrue("Should have member access", access.size() >= 2);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 25: Array access expression.
     */
    @Test
    public void testArrayAccess() throws PsiParseException {
        String kotlinCode = """
            fun test(arr: IntArray) {
                val x = arr[0]
                arr[1] = 42
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have array access callbacks
        List<CallbackRecord> arrayAccess = recorder.getCallbacksByName("gotArrayElementAccess");
        assertTrue("Should have array access", arrayAccess.size() >= 2);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== PHASE 6.6: ADVANCED EXPRESSIONS ====================
    
    /**
     * Test 26: Lambda expression.
     */
    @Test
    public void testLambdaExpression() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                val lambda = { x: Int -> x * 2 }
                val result = lambda(5)
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have lambda callbacks
        assertTrue("Should have beginLambdaBody", recorder.hasCallback("beginLambdaBody"));
        assertTrue("Should have endLambdaBody", recorder.hasCallback("endLambdaBody"));
        
        // Should have lambda parameter
        assertTrue("Should have gotLambdaFormalParam", recorder.hasCallback("gotLambdaFormalParam"));
        assertTrue("Should have gotLambdaFormalName", recorder.hasCallback("gotLambdaFormalName"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 27: Lambda with multiple parameters.
     */
    @Test
    public void testLambda_multipleParameters() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                val add = { x: Int, y: Int -> x + y }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have 2 lambda parameters
        List<CallbackRecord> params = recorder.getCallbacksByName("gotLambdaFormalParam");
        assertEquals("Should have 2 lambda parameters", 2, params.size());
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 28: Type cast (as operator).
     */
    @Test
    public void testTypeCast() throws PsiParseException {
        String kotlinCode = """
            fun test(obj: Any): String {
                return obj as String
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have type cast callback
        assertTrue("Should have gotTypeCast", recorder.hasCallback("gotTypeCast"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 29: Type check (is operator).
     */
    @Test
    public void testTypeCheck() throws PsiParseException {
        String kotlinCode = """
            fun test(obj: Any): Boolean {
                return obj is String
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have instanceof callback
        assertTrue("Should have gotInstanceOfOperator", recorder.hasCallback("gotInstanceOfOperator"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 30: Safe cast (as? operator).
     */
    @Test
    public void testSafeCast() throws PsiParseException {
        String kotlinCode = """
            fun test(obj: Any): String? {
                return obj as? String
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have type cast callback
        assertTrue("Should have gotTypeCast", recorder.hasCallback("gotTypeCast"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== INTEGRATION: COMPLEX NESTED SCENARIOS ====================
    
    /**
     * Test 31: Nested if-else with expressions.
     */
    @Test
    public void testComplexNesting_ifElseWithExpressions() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int, y: Int): String {
                if (x > 0) {
                    if (y > 0) {
                        return "both positive"
                    } else {
                        return "x positive, y negative"
                    }
                } else {
                    return "x negative"
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have multiple nested if statements
        List<CallbackRecord> ifStmts = recorder.getCallbacksByName("beginIfStmt");
        assertTrue("Should have nested if statements", ifStmts.size() >= 2);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 32: Loop with try-catch inside.
     */
    @Test
    public void testComplexNesting_loopWithTryCatch() throws PsiParseException {
        String kotlinCode = """
            fun test(items: List<String>) {
                for (item in items) {
                    try {
                        process(item)
                    } catch (e: Exception) {
                        log(e)
                    }
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have for loop and try-catch
        assertTrue("Should have beginForLoop", recorder.hasCallback("beginForLoop"));
        assertTrue("Should have beginTryCatchSmt", recorder.hasCallback("beginTryCatchSmt"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 33: When expression with complex branches.
     */
    @Test
    public void testComplexNesting_whenWithComplexBranches() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int): Int {
                return when (x) {
                    in 1..10 -> {
                        val temp = x * 2
                        temp + 1
                    }
                    in 11..20 -> x / 2
                    else -> 0
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have beginSwitchStmt", recorder.hasCallback("beginSwitchStmt"));
        assertTrue("Should have switch cases", recorder.hasCallback("beginSwitchCase"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 34: Method call with lambda argument.
     */
    @Test
    public void testComplexExpression_methodCallWithLambda() throws PsiParseException {
        String kotlinCode = """
            fun test(list: List<Int>): List<Int> {
                return list.map { it * 2 }.filter { it > 10 }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have method calls and lambdas
        assertTrue("Should have gotMethodCall", recorder.hasCallback("gotMethodCall"));
        assertTrue("Should have beginLambdaBody", recorder.hasCallback("beginLambdaBody"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 35: Chained member access and calls.
     */
    @Test
    public void testComplexExpression_chainedAccess() throws PsiParseException {
        String kotlinCode = """
            fun test(obj: MyClass): String {
                return obj.property.toString().uppercase()
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have multiple member access calls
        List<CallbackRecord> access = recorder.getCallbacksByName("gotMemberAccess");
        assertTrue("Should have chained member access", access.size() >= 2);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    // ==================== EDGE CASES ====================
    
    /**
     * Test 36: Empty blocks.
     */
    @Test
    public void testEdgeCase_emptyBlocks() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                if (true) {
                }
                while (false) {
                }
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have if statement", recorder.hasCallback("beginIfStmt"));
        assertTrue("Should have while loop", recorder.hasCallback("beginWhileLoop"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 37: Single-line if without braces.
     */
    @Test
    public void testEdgeCase_singleLineIf() throws PsiParseException {
        String kotlinCode = """
            fun test(x: Int) {
                if (x > 0) println("positive")
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        assertTrue("Should have if statement", recorder.hasCallback("beginIfStmt"));
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 38: Expression as statement.
     */
    @Test
    public void testEdgeCase_expressionStatement() throws PsiParseException {
        String kotlinCode = """
            fun test() {
                42
                "hello"
                true
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Expressions as statements should be wrapped with beginElement/endElement
        List<CallbackRecord> elements = recorder.getCallbacksByName("beginElement");
        assertTrue("Should have statement elements", elements.size() >= 3);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 39: Deeply nested expressions.
     */
    @Test
    public void testEdgeCase_deeplyNestedExpressions() throws PsiParseException {
        String kotlinCode = """
            fun test(): Int {
                return ((1 + 2) * (3 + 4)) / ((5 - 6) + (7 * 8))
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // Should have multiple binary operators
        List<CallbackRecord> operators = recorder.getCallbacksByName("gotBinaryOperator");
        assertTrue("Should have deeply nested operators", operators.size() >= 6);
        
        assertTrue("Callbacks should be balanced", recorder.validatePairing());
    }
    
    /**
     * Test 40: String template (treated as literal in Phase 6).
     */
    @Test
    public void testEdgeCase_stringTemplate() throws PsiParseException {
        String kotlinCode = """
            fun test(name: String) {
                println("Hello, ${'$'}name!")
            }
            """;
        
        CallbackRecorder recorder = parseAndVisit(kotlinCode);
        
        // String templates are treated as literals in Phase 6
        assertTrue("Should have literal callback", recorder.hasCallback("gotLiteral"));
        
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
        
        // Visit the file (triggers traversal)
        ktFile.accept(visitor);
        
        // Validate state is balanced after traversal
        assertTrue("Visitor state should be balanced after traversal", 
                  visitor.validateState());
        
        return recorder;
    }
}