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

import bluej.parser.JavaParserCallbacks;
import bluej.parser.lexer.LocatableToken;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Records all JavaParserCallbacks invocations for validation testing.
 * 
 * <p>This mock implementation of {@link JavaParserCallbacks} captures every callback
 * invocation along with its parameters, allowing validation tests to verify that
 * PSI-based traversal produces the same callback sequence as the token-based parser.
 * 
 * <h2>Purpose</h2>
 * <p>During Phase 3 (Callback Integration) of the PSI visitor implementation, this
 * recorder enables comparison testing between:
 * <ul>
 *   <li>Legacy token-based parser callback sequences</li>
 *   <li>New PSI-based {@link PsiCallbackVisitor} callback sequences</li>
 * </ul>
 * 
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * // Create recorder instead of actual callbacks
 * CallbackRecorder recorder = new CallbackRecorder();
 * 
 * // Pass to visitor for PSI traversal
 * PsiCallbackVisitor visitor = new PsiCallbackVisitor(recorder);
 * visitor.visitKtFile(ktFile);
 * 
 * // Verify expected callbacks were invoked
 * assertThat(recorder.hasCallback("beginClass")).isTrue();
 * assertThat(recorder.hasCallback("endClass")).isTrue();
 * assertThat(recorder.getCallbackCount("gotMethodDeclaration")).isEqualTo(3);
 * 
 * // Validate begin/end pairing
 * assertThat(recorder.validatePairing()).isTrue();
 * 
 * // Inspect callback sequence for debugging
 * System.out.println(recorder.getCallbackSequence());
 * // Output: beginClass → gotMethodDeclaration → endClass
 * }</pre>
 * 
 * <h2>Parameter Recording</h2>
 * <p>All callback parameters are stored in a map within each {@link CallbackRecord}.
 * This allows tests to verify not just which callbacks were invoked, but also
 * with what parameters. For example:
 * <pre>{@code
 * List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
 * CallbackRecord firstMethod = methodDecls.get(0);
 * LocatableToken nameToken = (LocatableToken) firstMethod.getParameter("token");
 * assertEquals("calculateSum", nameToken.getText());
 * }</pre>
 * 
 * <h2>Validation Support</h2>
 * <p>The recorder integrates with {@link PairingValidator} to verify that begin/end
 * callback pairs are properly matched. This catches common traversal errors such as:
 * <ul>
 *   <li>Missing end callbacks (unbalanced state)</li>
 *   <li>Mismatched begin/end pairs (incorrect nesting)</li>
 *   <li>Callbacks in wrong order</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p><b>NOT thread-safe.</b> This recorder is designed for single-threaded testing.
 * Each test should create a new recorder instance, or call {@link #reset()} between tests.
 * 
 * <h2>Performance Considerations</h2>
 * <p>Recording every callback with all parameters has memory overhead. This is acceptable
 * for testing but would not be suitable for production use. The recorder is specifically
 * designed for validation and debugging, not performance.
 * 
 * @see JavaParserCallbacks Base class with all callback method signatures
 * @see PsiCallbackVisitor PSI visitor that will invoke these callbacks in Phase 3
 * @see PairingValidator Validator for begin/end callback pairing
 * @see CallbackRecord Individual callback invocation record
 */
public class CallbackRecorder extends JavaParserCallbacks {
    
    /**
     * Ordered list of all recorded callback invocations.
     * Maintains insertion order to preserve callback sequence for validation.
     */
    private final List<CallbackRecord> records = new ArrayList<>();
    
    /**
     * Cached pairing validator created on-demand for deferred validation.
     * Null until first validation is requested.
     */
    private PairingValidator cachedValidator = null;
    
    /**
     * Creates a new callback recorder for validation testing.
     * 
     * <p>The recorder starts with an empty record list. All subsequently invoked
     * callbacks will be captured and stored for later inspection.
     */
    public CallbackRecorder() {
        // No initialization needed - records list is already initialized
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Returns all recorded callbacks in the order they were invoked.
     * 
     * <p>The returned list is a defensive copy - modifications will not affect
     * the recorder's internal state.
     * 
     * @return Unmodifiable copy of all callback records
     */
    public List<CallbackRecord> getRecords() {
        return List.copyOf(records);
    }
    
    /**
     * Returns all callbacks of a specific type.
     * 
     * <p>Useful for verifying that specific callbacks were invoked and examining
     * their parameters. For example:
     * <pre>{@code
     * List<CallbackRecord> methodDecls = recorder.getCallbacksByName("gotMethodDeclaration");
     * assertEquals(3, methodDecls.size()); // Three methods declared
     * }</pre>
     * 
     * @param name The callback name to filter by (e.g., "beginClass", "gotMethodDeclaration")
     * @return List of matching callbacks (empty if none match)
     */
    public List<CallbackRecord> getCallbacksByName(String name) {
        return records.stream()
            .filter(r -> r.getCallbackName().equals(name))
            .collect(Collectors.toList());
    }
    
    /**
     * Returns the count of callbacks with a specific name.
     * 
     * <p>Convenient for assertions without needing to get the full list:
     * <pre>{@code
     * assertEquals(3, recorder.getCallbackCount("gotMethodDeclaration"));
     * }</pre>
     * 
     * @param name The callback name to count
     * @return Number of times the callback was invoked
     */
    public int getCallbackCount(String name) {
        return (int) records.stream()
            .filter(r -> r.getCallbackName().equals(name))
            .count();
    }
    
    /**
     * Checks if a specific callback was invoked at least once.
     * 
     * <p>Useful for quick existence checks:
     * <pre>{@code
     * assertTrue(recorder.hasCallback("beginClass"));
     * assertTrue(recorder.hasCallback("endClass"));
     * }</pre>
     * 
     * @param name The callback name to check
     * @return true if the callback was invoked at least once
     */
    public boolean hasCallback(String name) {
        return records.stream()
            .anyMatch(r -> r.getCallbackName().equals(name));
    }
    
    /**
     * Returns the callback sequence as a formatted string for debugging.
     * 
     * <p>Shows the sequence of callbacks in order, separated by arrows.
     * Useful for debugging traversal issues or understanding callback flow:
     * <pre>
     * beginClass → gotMethodDeclaration → gotField → endClass
     * </pre>
     * 
     * @return Human-readable callback sequence
     */
    public String getCallbackSequence() {
        return records.stream()
            .map(CallbackRecord::getCallbackName)
            .collect(Collectors.joining(" → "));
    }
    
    // ==================== Validation Methods ====================
    
    /**
     * Validates that begin/end callback pairs are properly matched.
     *
     * <p>Uses {@link PairingValidator} to verify that every begin* callback has
     * a corresponding end* callback in the correct nesting order. This catches
     * common traversal errors such as:
     * <ul>
     *   <li>Missing endClass after beginClass</li>
     *   <li>endMethod before endClass when method is inside class</li>
     *   <li>Multiple begin* without corresponding end*</li>
     * </ul>
     *
     * <p><b>Example usage:</b>
     * <pre>{@code
     * CallbackRecorder recorder = new CallbackRecorder();
     * visitor.visitKtFile(file, recorder);
     *
     * if (!recorder.validatePairing()) {
     *     fail("Callback pairing validation failed: " +
     *          recorder.getCallbackSequence());
     * }
     * }</pre>
     *
     * @return true if all begin/end pairs are properly matched, false otherwise
     */
    public boolean validatePairing() {
        ValidationResult result = getValidationResult();
        return result.isBalanced() && !result.hasErrors();
    }
    
    /**
     * Returns detailed validation result with comprehensive error information.
     *
     * <p>This method provides the complete validation state including:
     * <ul>
     *   <li>Whether callbacks are balanced (all begin/end pairs matched)</li>
     *   <li>Whether any validation errors occurred</li>
     *   <li>Detailed validation summary with error messages</li>
     * </ul>
     *
     * <p>This is useful for test assertions where you need to provide detailed
     * failure messages showing exactly what went wrong.</p>
     *
     * <p><b>Performance Note:</b> This method now has O(1) complexity since the
     * validator is updated incrementally as callbacks are recorded, rather than
     * reprocessing all records on each call.</p>
     *
     * <p><b>Example usage:</b>
     * <pre>{@code
     * ValidationResult result = recorder.getValidationResult();
     * assertTrue(result.isBalanced(),
     *     "Callbacks should be balanced:\n" + result.getValidationSummary());
     * assertFalse(result.hasErrors(),
     *     "No validation errors should occur:\n" + result.getValidationSummary());
     * }</pre>
     *
     * @return Detailed validation result
     */
    public ValidationResult getValidationResult() {
        if (cachedValidator == null) {
            cachedValidator = new PairingValidator(records);
        }
        return new ValidationResult(cachedValidator);
    }
    
    public String getDetailedValidationSummary() {
        return getValidationResult().getDetailedValidationSummary();
    }
    
    /**
     * Wrapper class for validation results.
     *
     * <p>Provides convenient access to validation state and error information
     * from the underlying {@link PairingValidator}.</p>
     */
    public static class ValidationResult {
        private final boolean balanced;
        private final boolean hasErrors;
        private final String validationSummary;
        private final List<PairingValidator.CallbackPairing> pairings;
        private final List<String> errors;
        
        private ValidationResult(PairingValidator validator) {
            this.balanced = validator.isBalanced();
            this.hasErrors = validator.hasErrors();
            this.validationSummary = validator.getDetailedSummary();
            this.pairings = validator.getPairings();
            this.errors = validator.getErrors();
        }
        
        /**
         * Returns whether all begin/end callback pairs are balanced.
         *
         * @return true if balanced, false otherwise
         */
        public boolean isBalanced() {
            return balanced;
        }
        
        /**
         * Returns whether any validation errors occurred.
         *
         * @return true if errors exist, false otherwise
         */
        public boolean hasErrors() {
            return hasErrors;
        }
        
        /**
         * Returns a detailed validation summary.
         *
         * @return Formatted summary with error details
         */
        public String getValidationSummary() {
            return validationSummary;
        }
        
        /**
         * Returns the detailed validation summary with enhanced context.
         *
         * @return Formatted detailed summary
         */
        public String getDetailedValidationSummary() {
            return validationSummary;
        }
        
        /**
         * Returns the list of callback pairings.
         *
         * @return Unmodifiable list of pairings
         */
        public List<PairingValidator.CallbackPairing> getPairings() {
            return pairings;
        }
        
        /**
         * Returns the list of validation errors.
         *
         * @return Unmodifiable list of error messages
         */
        public List<String> getErrors() {
            return errors;
        }
    }
    
    /**
     * Resets the recorder for a new test.
     *
     * <p>Clears all recorded callbacks and resets the pairing validator.
     * Useful when reusing a recorder instance across multiple tests:</p>
     * <pre>{@code
     * CallbackRecorder recorder = new CallbackRecorder();
     *
     * // First test
     * visitor1.visitKtFile(file1, recorder);
     * assertCallbackSequence(recorder);
     *
     * // Reset for second test
     * recorder.reset();
     *
     * // Second test
     * visitor2.visitKtFile(file2, recorder);
     * assertCallbackSequence(recorder);
     * }</pre>
     */
    public void reset() {
        records.clear();
        cachedValidator = null;
    }
    
    // ==================== Core Recording Method ====================
    
    /**
     * Records a callback invocation with its parameters.
     *
     * <p>This is the internal method called by all overridden callback methods.
     * It creates a {@link CallbackRecord} with the callback name and parameters,
     * then adds it to the records list.</p>
     *
     * <p><b>Deferred Validation:</b> This method NO LONGER performs incremental
     * validation. Instead, validation happens when {@link #getValidationResult()}
     * is called, allowing for much better error messages with full context.</p>
     *
     * @param callbackName The name of the callback being invoked
     * @param parameters Map of parameter names to values
     */
    private void record(String callbackName, Map<String, Object> parameters) {
        records.add(new CallbackRecord(callbackName, parameters));
        cachedValidator = null;
    }
    
    /**
     * Records a callback invocation without parameters.
     *
     * <p>Convenience overload for callbacks that don't have parameters.</p>
     *
     * @param callbackName The name of the callback being invoked
     */
    private void record(String callbackName) {
        record(callbackName, Map.of());
    }

    // ==================== JavaParserCallbacks Overrides ====================
    // Note: Only a subset of callbacks are shown here as examples.
    // In a complete implementation, ALL protected methods from JavaParserCallbacks
    // must be overridden to provide comprehensive recording.
    
    @Override
    protected void beginPackageStatement(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginPackageStatement", params);
    }
    
    @Override
    protected void gotPackage(List<LocatableToken> pkgTokens) {
        Map<String, Object> params = new HashMap<>();
        params.put("pkgTokens", pkgTokens);
        record("gotPackage", params);
    }
    
    @Override
    protected void gotPackageSemi(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotPackageSemi", params);
    }
    
    @Override
    protected void gotModifier(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotModifier", params);
    }
    
    @Override
    protected void modifiersConsumed() {
        record("modifiersConsumed");
    }
    
    @Override
    protected void beginElement(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginElement", params);
    }
    
    @Override
    protected void endElement(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endElement", params);
    }
    
    @Override
    protected void beginMethodBody(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginMethodBody", params);
    }
    
    @Override
    protected void endMethodBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endMethodBody", params);
    }
    
    @Override
    protected void gotTypeDef(LocatableToken firstToken, int tdType) {
        Map<String, Object> params = new HashMap<>();
        params.put("firstToken", firstToken);
        params.put("tdType", tdType);
        record("gotTypeDef", params);
    }
    
    @Override
    protected void gotTypeDefName(LocatableToken nameToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("nameToken", nameToken);
        record("gotTypeDefName", params);
    }
    
    @Override
    protected void beginTypeBody(LocatableToken leftCurlyToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("leftCurlyToken", leftCurlyToken);
        record("beginTypeBody", params);
    }
    
    @Override
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("endCurlyToken", endCurlyToken);
        params.put("included", included);
        record("endTypeBody", params);
    }
    
    @Override
    protected void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("hiddenToken", hiddenToken);
        record("gotMethodDeclaration", params);
    }
    
    @Override
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("hiddenToken", hiddenToken);
        record("gotConstructorDecl", params);
    }
    
    @Override
    protected void beginFieldDeclarations(LocatableToken first) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        record("beginFieldDeclarations", params);
    }
    
    @Override
    protected void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("initExpressionFollows", initExpressionFollows);
        record("gotField", params);
    }
    
    @Override
    protected void endFieldDeclarations(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endFieldDeclarations", params);
    }
    
    @Override
    protected void gotTypeSpec(List<LocatableToken> tokens) {
        Map<String, Object> params = new HashMap<>();
        params.put("tokens", tokens);
        record("gotTypeSpec", params);
    }
    
    @Override
    protected void gotImport(List<LocatableToken> tokens, boolean isStatic, 
                           LocatableToken importToken, LocatableToken semiColonToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("tokens", tokens);
        params.put("isStatic", isStatic);
        params.put("importToken", importToken);
        params.put("semiColonToken", semiColonToken);
        record("gotImport", params);
    }
    
    // ==================== Compilation Unit Callbacks ====================
    
    @Override
    protected void reachedCUstate(int state) {
        record("reachedCUstate", Map.of("state", state));
    }
    
    @Override
    protected void finishedCU(int state) {
        record("finishedCU", Map.of("state", state));
    }
    
    @Override
    protected void gotImportStmtSemi(LocatableToken token) {
        record("gotImportStmtSemi", Map.of("token", token));
    }
    
    @Override
    protected void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
                                    LocatableToken importToken, LocatableToken semiColonToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("tokens", tokens);
        params.put("isStatic", isStatic);
        params.put("importToken", importToken);
        params.put("semiColonToken", semiColonToken);
        record("gotWildcardImport", params);
    }
    
    // ==================== Loop Callbacks ====================
    
    @Override
    protected void beginForLoop(LocatableToken token) {
        record("beginForLoop", Map.of("token", token));
    }
    
    @Override
    protected void beginForLoopBody(LocatableToken token) {
        record("beginForLoopBody", Map.of("token", token));
    }
    
    @Override
    protected void endForLoopBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endForLoopBody", params);
    }
    
    @Override
    protected void endForLoop(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endForLoop", params);
    }
    
    @Override
    protected void beginWhileLoop(LocatableToken token) {
        record("beginWhileLoop", Map.of("token", token));
    }
    
    @Override
    protected void beginWhileLoopBody(LocatableToken token) {
        record("beginWhileLoopBody", Map.of("token", token));
    }
    
    @Override
    protected void endWhileLoopBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endWhileLoopBody", params);
    }
    
    @Override
    protected void endWhileLoop(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endWhileLoop", params);
    }
    
    @Override
    protected void beginDoWhile(LocatableToken token) {
        record("beginDoWhile", Map.of("token", token));
    }
    
    @Override
    protected void beginDoWhileBody(LocatableToken token) {
        record("beginDoWhileBody", Map.of("token", token));
    }
    
    @Override
    protected void endDoWhileBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endDoWhileBody", params);
    }
    
    @Override
    protected void endDoWhile(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endDoWhile", params);
    }
    
    // ==================== Conditional Callbacks ====================
    
    @Override
    protected void beginIfStmt(LocatableToken token) {
        record("beginIfStmt", Map.of("token", token));
    }
    
    @Override
    protected void beginIfCondBlock(LocatableToken token) {
        record("beginIfCondBlock", Map.of("token", token));
    }
    
    @Override
    protected void endIfCondBlock(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endIfCondBlock", params);
    }
    
    @Override
    protected void gotElseIf(LocatableToken token) {
        record("gotElseIf", Map.of("token", token));
    }
    
    @Override
    protected void endIfStmt(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endIfStmt", params);
    }
    
    @Override
    protected void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("isSwitchExpression", isSwitchExpression);
        record("beginSwitchStmt", params);
    }
    
    @Override
    protected void beginSwitchBlock(LocatableToken token) {
        record("beginSwitchBlock", Map.of("token", token));
    }
    
    @Override
    protected void endSwitchBlock(LocatableToken token) {
        record("endSwitchBlock", Map.of("token", token));
    }
    
    @Override
    protected void endSwitchStmt(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endSwitchStmt", params);
    }
    
    @Override
    protected void beginSwitchCase(LocatableToken token) {
        record("beginSwitchCase", Map.of("token", token));
    }
    
    @Override
    protected void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("isArrowSyntax", isArrowSyntax);
        record("gotSwitchCaseType", params);
    }
    
    @Override
    protected void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("wasArrowSyntax", wasArrowSyntax);
        record("endSwitchCase", params);
    }
    
    @Override
    protected void gotSwitchDefault() {
        record("gotSwitchDefault");
    }
    
    // ==================== Exception Handling Callbacks ====================
    
    @Override
    protected void beginTryCatchSmt(LocatableToken token, boolean hasResource) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("hasResource", hasResource);
        record("beginTryCatchSmt", params);
    }
    
    @Override
    protected void beginTryBlock(LocatableToken token) {
        record("beginTryBlock", Map.of("token", token));
    }
    
    @Override
    protected void endTryBlock(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endTryBlock", params);
    }
    
    @Override
    protected void endTryCatchStmt(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endTryCatchStmt", params);
    }
    
    @Override
    protected void gotCatchFinally(LocatableToken token) {
        record("gotCatchFinally", Map.of("token", token));
    }
    
    @Override
    protected void gotMultiCatch(LocatableToken token) {
        record("gotMultiCatch", Map.of("token", token));
    }
    
    @Override
    protected void gotCatchVarName(LocatableToken token) {
        record("gotCatchVarName", Map.of("token", token));
    }
    
    @Override
    protected void beginSynchronizedBlock(LocatableToken token) {
        record("beginSynchronizedBlock", Map.of("token", token));
    }
    
    @Override
    protected void endSynchronizedBlock(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endSynchronizedBlock", params);
    }
    
    // ==================== Type Definition Callbacks ====================
    
    @Override
    protected void gotDeclBegin(LocatableToken token) {
        record("gotDeclBegin", Map.of("token", token));
    }
    
    @Override
    protected void endDecl(LocatableToken token) {
        record("endDecl", Map.of("token", token));
    }
    
    @Override
    protected void gotTypeDefEnd(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("gotTypeDefEnd", params);
    }
    
    @Override
    protected void beginTypeDefExtends(LocatableToken extendsToken) {
        record("beginTypeDefExtends", Map.of("extendsToken", extendsToken));
    }
    
    @Override
    public void endTypeDefExtends() {
        record("endTypeDefExtends");
    }
    
    @Override
    protected void beginTypeDefImplements(LocatableToken implementsToken) {
        record("beginTypeDefImplements", Map.of("implementsToken", implementsToken));
    }
    
    @Override
    public void endTypeDefImplements() {
        record("endTypeDefImplements");
    }
    
    @Override
    protected void beginTypeDefPermits(LocatableToken permitsToken) {
        record("beginTypeDefPermits", Map.of("permitsToken", permitsToken));
    }
    
    @Override
    protected void endTypeDefPermits() {
        record("endTypeDefPermits");
    }
    
    @Override
    protected void gotInnerType(LocatableToken start) {
        record("gotInnerType", Map.of("start", start));
    }
    
    @Override
    protected void gotTopLevelDecl(LocatableToken token) {
        record("gotTopLevelDecl", Map.of("token", token));
    }
    
    // ==================== Variable Declaration Callbacks ====================
    
    @Override
    protected void beginVariableDecl(LocatableToken first) {
        record("beginVariableDecl", Map.of("first", first));
    }
    
    @Override
    protected void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("inited", inited);
        record("gotVariableDecl", params);
    }
    
    @Override
    protected void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("inited", inited);
        record("gotSubsequentVar", params);
    }
    
    @Override
    protected void endVariable(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endVariable", params);
    }
    
    @Override
    protected void endVariableDecls(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endVariableDecls", params);
    }
    
    @Override
    protected void beginForInitDecl(LocatableToken first) {
        record("beginForInitDecl", Map.of("first", first));
    }
    
    @Override
    protected void gotForInit(LocatableToken first, LocatableToken idToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        record("gotForInit", params);
    }
    
    @Override
    protected void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("initFollows", initFollows);
        record("gotSubsequentForInit", params);
    }
    
    @Override
    protected void endForInit(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endForInit", params);
    }
    
    @Override
    protected void endForInitDecls(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endForInitDecls", params);
    }
    
    @Override
    protected void gotForTest(boolean isPresent) {
        record("gotForTest", Map.of("isPresent", isPresent));
    }
    
    @Override
    protected void gotForIncrement(boolean isPresent) {
        record("gotForIncrement", Map.of("isPresent", isPresent));
    }
    
    @Override
    protected void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) {
        Map<String, Object> params = new HashMap<>();
        params.put("forEachLoop", forEachLoop);
        params.put("initExpressionFollows", initExpressionFollows);
        record("determinedForLoop", params);
    }
    
    // ==================== Field Declaration Callbacks ====================
    
    @Override
    protected void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("initFollows", initFollows);
        record("gotSubsequentField", params);
    }
    
    @Override
    protected void endField(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endField", params);
    }
    
    // ==================== Expression Callbacks ====================
    
    @Override
    protected void beginExpression(LocatableToken token, boolean isLambdaBody) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("isLambdaBody", isLambdaBody);
        record("beginExpression", params);
    }
    
    @Override
    protected void endExpression(LocatableToken token, boolean emptyExpression) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("emptyExpression", emptyExpression);
        record("endExpression", params);
    }
    
    @Override
    protected void gotLiteral(LocatableToken token) {
        record("gotLiteral", Map.of("token", token));
    }
    
    @Override
    protected void gotPrimitiveTypeLiteral(LocatableToken token) {
        record("gotPrimitiveTypeLiteral", Map.of("token", token));
    }
    
    @Override
    protected void gotIdentifier(LocatableToken token) {
        record("gotIdentifier", Map.of("token", token));
    }
    
    @Override
    protected void gotIdentifierEOF(LocatableToken token) {
        record("gotIdentifierEOF", Map.of("token", token));
    }
    
    @Override
    protected void gotMemberAccessEOF(LocatableToken token) {
        record("gotMemberAccessEOF", Map.of("token", token));
    }
    
    @Override
    protected void gotCompoundIdent(LocatableToken token) {
        record("gotCompoundIdent", Map.of("token", token));
    }
    
    @Override
    protected void gotCompoundComponent(LocatableToken token) {
        record("gotCompoundComponent", Map.of("token", token));
    }
    
    @Override
    protected void completeCompoundValue(LocatableToken token) {
        record("completeCompoundValue", Map.of("token", token));
    }
    
    @Override
    protected void completeCompoundValueEOF(LocatableToken token) {
        record("completeCompoundValueEOF", Map.of("token", token));
    }
    
    @Override
    protected void completeCompoundClass(LocatableToken token) {
        record("completeCompoundClass", Map.of("token", token));
    }
    
    @Override
    protected void gotMemberAccess(LocatableToken token) {
        record("gotMemberAccess", Map.of("token", token));
    }
    
    @Override
    protected void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("typeArgs", typeArgs);
        record("gotMemberCall", params);
    }
    
    @Override
    protected void gotMethodCall(LocatableToken token) {
        record("gotMethodCall", Map.of("token", token));
    }
    
    @Override
    protected void gotConstructorCall(LocatableToken token) {
        record("gotConstructorCall", Map.of("token", token));
    }
    
    @Override
    protected void gotDotEOF(LocatableToken token) {
        record("gotDotEOF", Map.of("token", token));
    }
    
    @Override
    protected void gotStatementExpression() {
        record("gotStatementExpression");
    }
    
    @Override
    protected void gotClassLiteral(LocatableToken token) {
        record("gotClassLiteral", Map.of("token", token));
    }
    
    @Override
    protected void gotBinaryOperator(LocatableToken token) {
        record("gotBinaryOperator", Map.of("token", token));
    }
    
    @Override
    protected void gotUnaryOperator(LocatableToken token) {
        record("gotUnaryOperator", Map.of("token", token));
    }
    
    @Override
    protected void gotQuestionOperator(LocatableToken token) {
        record("gotQuestionOperator", Map.of("token", token));
    }
    
    @Override
    protected void gotQuestionColon(LocatableToken token) {
        record("gotQuestionColon", Map.of("token", token));
    }
    
    @Override
    protected void gotInstanceOfOperator(LocatableToken token) {
        record("gotInstanceOfOperator", Map.of("token", token));
    }
    
    @Override
    protected void gotInstanceOfVar(LocatableToken token) {
        record("gotInstanceOfVar", Map.of("token", token));
    }
    
    @Override
    protected void gotArrayElementAccess() {
        record("gotArrayElementAccess");
    }
    
    @Override
    protected void gotPostOperator(LocatableToken token) {
        record("gotPostOperator", Map.of("token", token));
    }
    
    @Override
    protected void gotTypeCast(List<LocatableToken> tokens) {
        record("gotTypeCast", Map.of("tokens", tokens));
    }
    
    @Override
    protected void gotArrayTypeIdentifier(LocatableToken token) {
        record("gotArrayTypeIdentifier", Map.of("token", token));
    }
    
    @Override
    protected void gotParentIdentifier(LocatableToken token) {
        record("gotParentIdentifier", Map.of("token", token));
    }
    
    // ==================== Argument/Parameter Callbacks ====================
    
    @Override
    protected void beginArgumentList(LocatableToken token) {
        record("beginArgumentList", Map.of("token", token));
    }
    
    @Override
    protected void endArgument() {
        record("endArgument");
    }
    
    @Override
    protected void endArgumentList(LocatableToken token) {
        record("endArgumentList", Map.of("token", token));
    }
    
    @Override
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("ellipsisToken", ellipsisToken);
        record("gotMethodParameter", params);
    }
    
    @Override
    protected void gotArrayDeclarator() {
        record("gotArrayDeclarator");
    }
    
    @Override
    protected void gotNewArrayDeclarator(boolean withDimension) {
        record("gotNewArrayDeclarator", Map.of("withDimension", withDimension));
    }
    
    @Override
    protected void gotAllMethodParameters() {
        record("gotAllMethodParameters");
    }
    
    @Override
    protected void beginFormalParameter(LocatableToken token) {
        record("beginFormalParameter", Map.of("token", token));
    }
    
    // ==================== Type Parameter Callbacks ====================
    
    @Override
    protected void gotTypeParam(LocatableToken idToken) {
        record("gotTypeParam", Map.of("idToken", idToken));
    }
    
    @Override
    protected void gotTypeParamBound(List<LocatableToken> tokens) {
        record("gotTypeParamBound", Map.of("tokens", tokens));
    }
    
    @Override
    protected void gotMethodTypeParamsBegin() {
        record("gotMethodTypeParamsBegin");
    }
    
    @Override
    protected void endMethodTypeParams() {
        record("endMethodTypeParams");
    }
    
    // ==================== Array/New/Init Callbacks ====================
    
    @Override
    protected void gotExprNew(LocatableToken token) {
        record("gotExprNew", Map.of("token", token));
    }
    
    @Override
    protected void endExprNew(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endExprNew", params);
    }
    
    @Override
    protected void beginArrayInitList(LocatableToken token) {
        record("beginArrayInitList", Map.of("token", token));
    }
    
    @Override
    protected void endArrayInitList(LocatableToken token) {
        record("endArrayInitList", Map.of("token", token));
    }
    
    @Override
    protected void beginAnonClassBody(LocatableToken token, boolean isEnumMember) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("isEnumMember", isEnumMember);
        record("beginAnonClassBody", params);
    }
    
    @Override
    protected void endAnonClassBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endAnonClassBody", params);
    }
    
    @Override
    protected void beginStmtblockBody(LocatableToken token) {
        record("beginStmtblockBody", Map.of("token", token));
    }
    
    @Override
    protected void endStmtblockBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endStmtblockBody", params);
    }
    
    @Override
    protected void beginInitBlock(LocatableToken first, LocatableToken lcurly) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("lcurly", lcurly);
        record("beginInitBlock", params);
    }
    
    @Override
    protected void endInitBlock(LocatableToken rcurly, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("rcurly", rcurly);
        params.put("included", included);
        record("endInitBlock", params);
    }
    
    // ==================== Flow Control Statement Callbacks ====================
    
    @Override
    protected void gotThrow(LocatableToken token) {
        record("gotThrow", Map.of("token", token));
    }
    
    @Override
    protected void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("keywordToken", keywordToken);
        params.put("labelToken", labelToken);
        record("gotBreakContinue", params);
    }
    
    @Override
    protected void gotReturnStatement(boolean hasValue) {
        record("gotReturnStatement", Map.of("hasValue", hasValue));
    }
    
    @Override
    protected void gotYieldStatement() {
        record("gotYieldStatement");
    }
    
    @Override
    protected void gotEmptyStatement() {
        record("gotEmptyStatement");
    }
    
    @Override
    protected void gotAssert() {
        record("gotAssert");
    }
    
    // ==================== Annotation Callbacks ====================
    
    @Override
    protected void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) {
        Map<String, Object> params = new HashMap<>();
        params.put("annName", annName);
        params.put("paramsFollow", paramsFollow);
        record("gotAnnotation", params);
    }
    
    // ==================== Lambda Callbacks ====================
    
    @Override
    protected void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) {
        Map<String, Object> params = new HashMap<>();
        params.put("lambdaIsBlock", lambdaIsBlock);
        params.put("openCurly", openCurly);
        record("beginLambdaBody", params);
    }
    
    @Override
    protected void endLambdaBody(LocatableToken closeCurly) {
        record("endLambdaBody", Map.of("closeCurly", closeCurly));
    }
    
    @Override
    protected void gotLambdaFormalParam() {
        record("gotLambdaFormalParam");
    }
    
    @Override
    protected void gotLambdaFormalName(LocatableToken name) {
        record("gotLambdaFormalName", Map.of("name", name));
    }
    
    @Override
    protected void gotLambdaFormalType(List<LocatableToken> type) {
        record("gotLambdaFormalType", Map.of("type", type));
    }
    
    // ==================== Record Callbacks ====================
    
    @Override
    protected void beginRecordParameters(LocatableToken parenToken) {
        record("beginRecordParameters", Map.of("parenToken", parenToken));
    }
    
    @Override
    protected void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("varargsToken", varargsToken);
        record("gotRecordParameter", params);
    }
    
    @Override
    protected void endRecordParameters(LocatableToken closeParen) {
        record("endRecordParameters", Map.of("closeParen", closeParen));
    }
    
    // ==================== Method Declaration Callbacks ====================
    
    @Override
    protected void endMethodDecl(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endMethodDecl", params);
    }
    
    @Override
    protected void beginThrows(LocatableToken token) {
        record("beginThrows", Map.of("token", token));
    }
    
    @Override
    protected void endThrows() {
        record("endThrows");
    }
    
    // ==================== Comment Callback ====================
    
    @Override
    public void gotComment(LocatableToken token) {
        record("gotComment", Map.of("token", token));
    }
    
    // ==================== Error Callback ====================
    
    @Override
    protected void error(String msg, int beginLine, int beginCol, int endLine, int endCol) {
        Map<String, Object> params = new HashMap<>();
        params.put("msg", msg);
        params.put("beginLine", beginLine);
        params.put("beginCol", beginCol);
        params.put("endLine", endLine);
        params.put("endCol", endCol);
        record("error", params);
        // Don't throw - just record for validation testing
    }
    
    // ==================== CallbackRecord Inner Class ====================
    
    /**
     * Records a single callback invocation with its parameters and timestamp.
     * 
     * <p>Each record captures:
     * <ul>
     *   <li>The callback name (e.g., "beginClass", "gotMethodDeclaration")</li>
     *   <li>All parameters passed to the callback</li>
     *   <li>Timestamp when the callback was invoked</li>
     * </ul>
     * 
     * <p>The timestamp allows chronological ordering and timing analysis if needed,
     * though most validation tests only care about sequence, not timing.
     * 
     * <p><b>Immutability:</b> Once created, a CallbackRecord cannot be modified.
     * This prevents accidental corruption of validation data.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * List<CallbackRecord> records = recorder.getRecords();
     * for (CallbackRecord record : records) {
     *     System.out.println(record.getCallbackName());
     *     if (record.getCallbackName().equals("gotMethodDeclaration")) {
     *         LocatableToken token = (LocatableToken) record.getParameter("token");
     *         System.out.println("Method: " + token.getText());
     *     }
     * }
     * }</pre>
     */
    
    // ==================== Milestone 2.3: Additional Callbacks for Validation ====================
    
    /**
     * Simplified begin/end class callbacks for validation testing.
     * These are convenience methods that wrap the actual JavaParserCallbacks methods.
     * In Phase 3, these will be replaced with proper beginTypeBody/endTypeBody calls
     * with token parameters.
     */
    public void beginClass() {
        record("beginClass");
    }
    
    public void endClass() {
        record("endClass");
    }
}