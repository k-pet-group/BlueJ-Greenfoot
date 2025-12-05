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

import bluej.parser.JavaParserCallbacksBase;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Records all JavaParserCallbacks invocations for validation testing.
 * 
 * <p>This mock implementation of {@link JavaParserCallbacksBase} captures every callback
 * invocation along with its parameters, allowing validation tests to verify that
 * PSI-based traversal produces the same callback sequence as the token-based parser.
 * 
 * <h2>Purpose</h2>
 * <p>During Phase 3 (Callback Integration) of the PSI visitor implementation, this
 * recorder enables comparison testing between:
 * <ul>
 *   <li>Legacy token-based parser callback sequences</li>
 *   <li>New PSI-based {@link bluej.parser.psi.visitor.BaseVisitor} callback sequences</li>
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
 * @see JavaParserCallbacksBase Base class with all callback method signatures
 * @see bluej.parser.psi.visitor.BaseVisitor PSI visitor that will invoke these callbacks in Phase 3
 * @see PairingValidator Validator for begin/end callback pairing
 * @see CallbackRecord Individual callback invocation record
 */
public class CallbackRecorder implements JavaParserCallbacksAdapter {
    
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
        if (!(result.isBalanced() && !result.hasErrors())) {
            System.out.println(result.getDetailedValidationSummary());
        }
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
     * Asserts that callbacks are properly balanced, throwing AssertionError with detailed context if not.
     *
     * <p>This method provides much better error messages than a simple {@code assertTrue(validatePairing())},
     * showing exactly which callbacks are unmatched and why.</p>
     *
     * <p><b>Example usage:</b>
     * <pre>{@code
     * CallbackRecorder recorder = parseAndVisit(kotlinCode);
     * recorder.assertBalanced(); // Throws with detailed error if not balanced
     * }</pre>
     *
     * @throws AssertionError if callbacks are not balanced, with detailed validation summary
     */
    public void assertBalanced() {
        ValidationResult result = getValidationResult();
        if (!result.isBalanced() || result.hasErrors()) {
            throw new AssertionError(
                "Callback pairing validation failed:\n" +
                "===========================================\n" +
                result.getValidationSummary() + "\n" +
                "===========================================\n" +
                "Callback Sequence:\n" +
                getCallbackSequence()
            );
        }
    }

    @Override
    public JavaTokenFilter getTokenStream() {
        return null;
    }

    @Override
    public void skipToToken(LocatableToken targetToken, boolean included) {
        return;
    }

    @Override
    public LocatableToken getLastToken() {
        return null;
    }

    @Override
    public void setLastToken(LocatableToken lastToken) {

    }

    @Override
    public void setEmitRangeStart(int line, int column) {

    }

    @Override
    public void clearEmitRangeStart() {

    }

    @Override
    public void setEmitRangeEnd(int line, int column) {

    }

    @Override
    public void clearEmitRangeEnd() {

    }

    @Override
    public boolean isInEmitRange(int line, int column) {
        return true;
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
     * <p><b>Stacktrace Capture:</b> This method captures the execution stacktrace
     * at the point where the callback is invoked during parsing. This stacktrace
     * shows the actual parsing code path, which is critical for debugging - it
     * reveals where the callback was recorded, not where it was validated.</p>
     *
     * <p><b>Deferred Validation:</b> This method NO LONGER performs incremental
     * validation. Instead, validation happens when {@link #getValidationResult()}
     * is called, allowing for much better error messages with full context.</p>
     *
     * @param callbackName The name of the callback being invoked
     * @param parameters Map of parameter names to values
     */
    private void record(String callbackName, Map<String, Object> parameters) {
        // Capture stacktrace at parsing time (NOT at validation time)
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        records.add(new CallbackRecord(callbackName, parameters, stackTrace));
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
    public void beginPackageStatement(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginPackageStatement", params);
    }
    
    @Override
    public void gotPackage(List<LocatableToken> pkgTokens) {
        Map<String, Object> params = new HashMap<>();
        params.put("pkgTokens", pkgTokens);
        record("gotPackage", params);
    }
    
    @Override
    public void gotPackageSemi(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotPackageSemi", params);
    }
    
    @Override
    public void gotModifier(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotModifier", params);
    }
    
    @Override
    public void modifiersConsumed() {
        record("modifiersConsumed");
    }
    
    @Override
    public void beginElement(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginElement", params);
    }
    
    @Override
    public void endElement(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endElement", params);
    }
    
    @Override
    public void beginMethodBody(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginMethodBody", params);
    }
    
    @Override
    public void endMethodBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endMethodBody", params);
    }
    
    @Override
    public void gotTypeDef(LocatableToken firstToken, int tdType) {
        Map<String, Object> params = new HashMap<>();
        params.put("firstToken", firstToken);
        params.put("tdType", tdType);
        record("gotTypeDef", params);
    }
    
    @Override
    public void gotTypeDefName(LocatableToken nameToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("nameToken", nameToken);
        record("gotTypeDefName", params);
    }
    
    @Override
    public void beginTypeBody(LocatableToken leftCurlyToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("leftCurlyToken", leftCurlyToken);
        record("beginTypeBody", params);
    }
    
    @Override
    public void endTypeBody(LocatableToken endCurlyToken, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("endCurlyToken", endCurlyToken);
        params.put("included", included);
        record("endTypeBody", params);
    }
    
    @Override
    public void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("hiddenToken", hiddenToken);
        record("gotMethodDeclaration", params);
    }
    
    @Override
    public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("hiddenToken", hiddenToken);
        record("gotConstructorDecl", params);
    }

    @Override
    public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken, String name) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("hiddenToken", hiddenToken);
        params.put("name", name);
        record("gotConstructorDecl", params);
    }
    
    @Override
    public void beginFieldDeclarations(LocatableToken first) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        record("beginFieldDeclarations", params);
    }
    
    @Override
    public void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("initExpressionFollows", initExpressionFollows);
        record("gotField", params);
    }
    
    @Override
    public void endFieldDeclarations(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endFieldDeclarations", params);
    }
    
    @Override
    public void gotTypeSpec(List<LocatableToken> tokens) {
        Map<String, Object> params = new HashMap<>();
        params.put("tokens", tokens);
        record("gotTypeSpec", params);
    }
    
    @Override
    public void gotImport(List<LocatableToken> tokens, boolean isStatic,
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
    public void reachedCUstate(int state) {
        Map<String, Object> params = new HashMap<>();
        params.put("state", state);
        record("reachedCUstate", params);
    }
    
    @Override
    public void finishedCU(int state) {
        Map<String, Object> params = new HashMap<>();
        params.put("state", state);
        record("finishedCU", params);
    }
    
    @Override
    public void gotImportStmtSemi(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotImportStmtSemi", params);
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
        endElement(token, true);
    }
    
    @Override
    public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
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
    public void beginForLoop(LocatableToken token) {
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
        // beginElement(token);

        // TODO2: `EditorParser` actually overrides it not emit the `beginElement` which makes it even more funky

        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginForLoop", params);
    }
    
    @Override
    public void beginForLoopBody(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginForLoopBody", params);
    }
    
    @Override
    public void endForLoopBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endForLoopBody", params);
    }
    
    @Override
    public void endForLoop(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endForLoop", params);
    }
    
    @Override
    public void beginWhileLoop(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginWhileLoop", params);
    }
    
    @Override
    public void beginWhileLoopBody(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginWhileLoopBody", params);
    }
    
    @Override
    public void endWhileLoopBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endWhileLoopBody", params);
    }
    
    @Override
    public void endWhileLoop(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endWhileLoop", params);
    }
    
    @Override
    public void beginDoWhile(LocatableToken token) {
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
        beginElement(token);

        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginDoWhile", params);
    }
    
    @Override
    public void beginDoWhileBody(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginDoWhileBody", params);
    }
    
    @Override
    public void endDoWhileBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endDoWhileBody", params);
    }
    
    @Override
    public void endDoWhile(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endDoWhile", params);
    }
    
    // ==================== Conditional Callbacks ====================
    
    @Override
    public void beginIfStmt(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginIfStmt", params);
    }
    
    @Override
    public void beginIfCondBlock(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginIfCondBlock", params);
    }
    
    @Override
    public void endIfCondBlock(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endIfCondBlock", params);
    }
    
    @Override
    public void gotElseIf(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotElseIf", params);
    }
    
    @Override
    public void endIfStmt(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endIfStmt", params);
    }
    
    @Override
    public void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("isSwitchExpression", isSwitchExpression);
        record("beginSwitchStmt", params);
    }
    
    @Override
    public void beginSwitchBlock(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginSwitchBlock", params);
    }
    
    @Override
    public void endSwitchBlock(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("endSwitchBlock", params);
    }
    
    @Override
    public void endSwitchStmt(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endSwitchStmt", params);
    }
    
    @Override
    public void beginSwitchCase(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginSwitchCase", params);
    }
    
    @Override
    public void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("isArrowSyntax", isArrowSyntax);
        record("gotSwitchCaseType", params);
    }
    
    @Override
    public void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("wasArrowSyntax", wasArrowSyntax);
        record("endSwitchCase", params);
    }
    
    @Override
    public void gotSwitchDefault() {
        record("gotSwitchDefault");
    }
    
    // ==================== Exception Handling Callbacks ====================
    
    @Override
    public void beginTryCatchStmt(LocatableToken token, boolean hasResource) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("hasResource", hasResource);
        record("beginTryCatchStmt", params);
    }
    
    @Override
    public void beginTryBlock(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginTryBlock", params);
    }
    
    @Override
    public void endTryBlock(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endTryBlock", params);
    }
    
    @Override
    public void endTryCatchStmt(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endTryCatchStmt", params);
    }
    
    @Override
    public void gotCatchFinally(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotCatchFinally", params);
    }
    
    @Override
    public void gotMultiCatch(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotMultiCatch", params);
    }
    
    @Override
    public void gotCatchVarName(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotCatchVarName", params);
    }
    
    @Override
    public void beginSynchronizedBlock(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginSynchronizedBlock", params);
    }
    
    @Override
    public void endSynchronizedBlock(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endSynchronizedBlock", params);
    }
    
    // ==================== Type Definition Callbacks ====================
    
    @Override
    public void gotDeclBegin(LocatableToken token) {
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
//        beginElement(token);

        // TODO2: `EditorParser` actually overrides it not emit the `beginElement` which makes it even more funky

        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotDeclBegin", params);
    }
    
    @Override
    public void endDecl(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("endDecl", params);
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
        // endElement(token, included);
        // TODO2: `EditorParser` actually overrides it not emit the `endElement` which makes it even more funky
    }
    
    @Override
    public void gotTypeDefEnd(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("gotTypeDefEnd", params);
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
        // endElement(token, included);
        // TODO2: `EditorParser` actually overrides it not emit the `endElement` which makes it even more funky
    }
    
    @Override
    public void beginTypeDefExtends(LocatableToken extendsToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("extendsToken", extendsToken);
        record("beginTypeDefExtends", params);
    }
    
    @Override
    public void endTypeDefExtends() {
        record("endTypeDefExtends");
    }
    
    @Override
    public void beginTypeDefImplements(LocatableToken implementsToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("implementsToken", implementsToken);
        record("beginTypeDefImplements", params);
    }
    
    @Override
    public void endTypeDefImplements() {
        record("endTypeDefImplements");
    }
    
    @Override
    public void beginTypeDefPermits(LocatableToken permitsToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("permitsToken", permitsToken);
        record("beginTypeDefPermits", params);
    }
    
    @Override
    public void endTypeDefPermits() {
        record("endTypeDefPermits");
    }
    
    @Override
    public void gotInnerType(LocatableToken start) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", start);
        record("gotInnerType", params);
    }
    
    @Override
    public void gotTopLevelDecl(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotTopLevelDecl", params);
    }
    
    // ==================== Variable Declaration Callbacks ====================
    
    @Override
    public void beginVariableDecl(LocatableToken first) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        record("beginVariableDecl", params);
    }
    
    @Override
    public void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("inited", inited);
        record("gotVariableDecl", params);
    }
    
    @Override
    public void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("inited", inited);
        record("gotSubsequentVar", params);
    }
    
    @Override
    public void endVariable(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endVariable", params);
    }
    
    @Override
    public void endVariableDecls(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endVariableDecls", params);
    }
    
    @Override
    public void beginForInitDecl(LocatableToken first) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        record("beginForInitDecl", params);
    }
    
    @Override
    public void gotForInit(LocatableToken first, LocatableToken idToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        record("gotForInit", params);
    }
    
    @Override
    public void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("initFollows", initFollows);
        record("gotSubsequentForInit", params);
    }
    
    @Override
    public void endForInit(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endForInit", params);
    }
    
//    @Override
//    public void endForInitDecl(LocatableToken token, boolean included) {
//        Map<String, Object> params = new HashMap<>();
//        params.put("token", token);
//        params.put("included", included);
//        record("endForInitDecl", params);
//    }
    
    @Override
    public void endForInitDecls(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endForInitDecls", params);
    }
    
    @Override
    public void gotForTest(boolean isPresent) {
        Map<String, Object> params = new HashMap<>();
        params.put("isPresent", isPresent);
        record("gotForTest", params);
    }
    
    @Override
    public void gotForIncrement(boolean isPresent) {
        Map<String, Object> params = new HashMap<>();
        params.put("isPresent", isPresent);
        record("gotForIncrement", params);
    }
    
    @Override
    public void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) {
        Map<String, Object> params = new HashMap<>();
        params.put("forEachLoop", forEachLoop);
        params.put("initExpressionFollows", initExpressionFollows);
        record("determinedForLoop", params);
    }
    
    // ==================== Field Declaration Callbacks ====================
    
    @Override
    public void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("initFollows", initFollows);
        record("gotSubsequentField", params);
    }
    
    @Override
    public void endField(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endField", params);
    }
    
    // ==================== Expression Callbacks ====================
    
    @Override
    public void beginExpression(LocatableToken token, boolean isLambdaBody) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("isLambdaBody", isLambdaBody);
        record("beginExpression", params);
    }
    
    @Override
    public void endExpression(LocatableToken token, boolean emptyExpression) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("emptyExpression", emptyExpression);
        record("endExpression", params);
    }
    
    @Override
    public void gotLiteral(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotLiteral", params);
    }
    
    @Override
    public void gotPrimitiveTypeLiteral(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotPrimitiveTypeLiteral", params);
    }
    
    @Override
    public void gotIdentifier(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotIdentifier", params);
    }
    
    @Override
    public void gotIdentifierEOF(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotIdentifierEOF", params);
    }
    
    @Override
    public void gotMemberAccessEOF(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotMemberAccessEOF", params);
    }
    
    @Override
    public void gotCompoundIdent(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotCompoundIdent", params);
    }
    
    @Override
    public void gotCompoundComponent(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotCompoundComponent", params);
    }
    
    @Override
    public void completeCompoundValue(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("completeCompoundValue", params);
    }
    
    @Override
    public void completeCompoundValueEOF(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("completeCompoundValueEOF", params);
    }
    
    @Override
    public void completeCompoundClass(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("completeCompoundClass", params);
    }
    
    @Override
    public void gotMemberAccess(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotMemberAccess", params);
    }
    
    @Override
    public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("typeArgs", typeArgs);
        record("gotMemberCall", params);
    }
    
    @Override
    public void gotMethodCall(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotMethodCall", params);
    }
    
    @Override
    public void gotConstructorCall(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotConstructorCall", params);
    }
    
    @Override
    public void gotDotEOF(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotDotEOF", params);
    }
    
    @Override
    public void gotStatementExpression() {
        record("gotStatementExpression");
    }
    
    @Override
    public void gotClassLiteral(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotClassLiteral", params);
    }
    
    @Override
    public void gotBinaryOperator(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotBinaryOperator", params);
    }
    
    @Override
    public void gotUnaryOperator(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotUnaryOperator", params);
    }
    
    @Override
    public void gotQuestionOperator(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotQuestionOperator", params);
    }
    
    @Override
    public void gotQuestionColon(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotQuestionColon", params);
    }
    
    @Override
    public void gotInstanceOfOperator(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotInstanceOfOperator", params);
    }
    
    @Override
    public void gotInstanceOfVar(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotInstanceOfVar", params);
    }
    
    @Override
    public void gotArrayElementAccess() {
        record("gotArrayElementAccess");
    }
    
    @Override
    public void gotPostOperator(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotPostOperator", params);
    }
    
    @Override
    public void gotTypeCast(List<LocatableToken> tokens) {
        Map<String, Object> params = new HashMap<>();
        params.put("tokens", tokens);
        record("gotTypeCast", params);
    }
    
    @Override
    public void gotArrayTypeIdentifier(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotArrayTypeIdentifier", params);
    }
    
    @Override
    public void gotParentIdentifier(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotParentIdentifier", params);
    }
    
    // ==================== Argument/Parameter Callbacks ====================
    
    @Override
    public void beginArgumentList(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginArgumentList", params);
    }
    
    @Override
    public void endArgument() {
        record("endArgument");
    }
    
    @Override
    public void endArgumentList(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("endArgumentList", params);
    }
    
    @Override
    public void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("ellipsisToken", ellipsisToken);
        record("gotMethodParameter", params);
    }
    
    @Override
    public void gotArrayDeclarator() {
        record("gotArrayDeclarator");
    }
    
    @Override
    public void gotNewArrayDeclarator(boolean withDimension) {
        Map<String, Object> params = new HashMap<>();
        params.put("withDimension", withDimension);
        record("gotNewArrayDeclarator", params);
    }
    
    @Override
    public void gotAllMethodParameters() {
        record("gotAllMethodParameters");
    }
    
    @Override
    public void beginFormalParameter(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginFormalParameter", params);
    }
    
    // ==================== Type Parameter Callbacks ====================
    
    @Override
    public void gotTypeParam(LocatableToken idToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("idToken", idToken);
        record("gotTypeParam", params);
    }
    
    @Override
    public void gotTypeParamBound(List<LocatableToken> tokens) {
        Map<String, Object> params = new HashMap<>();
        params.put("tokens", tokens);
        record("gotTypeParamBound", params);
    }
    
    @Override
    public void gotMethodTypeParamsBegin() {
        record("gotMethodTypeParamsBegin");
    }
    
    @Override
    public void endMethodTypeParams() {
        record("endMethodTypeParams");
    }
    
    // ==================== Array/New/Init Callbacks ====================
    
    @Override
    public void gotExprNew(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotExprNew", params);
    }
    
    @Override
    public void endExprNew(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endExprNew", params);
    }
    
    @Override
    public void beginArrayInitList(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginArrayInitList", params);
    }
    
    @Override
    public void endArrayInitList(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("endArrayInitList", params);
    }
    
    @Override
    public void beginAnonClassBody(LocatableToken token, boolean isEnumMember) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("isEnumMember", isEnumMember);
        record("beginAnonClassBody", params);
    }
    
    @Override
    public void endAnonClassBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endAnonClassBody", params);
    }
    
    @Override
    public void beginStmtblockBody(LocatableToken token) {
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
        beginElement(token);

        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginStmtblockBody", params);
    }
    
    @Override
    public void endStmtblockBody(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endStmtblockBody", params);
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
        endElement(token, included);
    }
    
    @Override
    public void beginInitBlock(LocatableToken first, LocatableToken lcurly) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("lcurly", lcurly);
        record("beginInitBlock", params);
    }
    
    @Override
    public void endInitBlock(LocatableToken rcurly, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("rcurly", rcurly);
        params.put("included", included);
        record("endInitBlock", params);
    }
    
    // ==================== Flow Control Statement Callbacks ====================
    
    @Override
    public void gotThrow(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotThrow", params);
    }
    
    @Override
    public void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("keywordToken", keywordToken);
        params.put("labelToken", labelToken);
        record("gotBreakContinue", params);
    }
    
    @Override
    public void gotReturnStatement(boolean hasValue) {
        Map<String, Object> params = new HashMap<>();
        params.put("hasValue", hasValue);
        record("gotReturnStatement", params);
    }
    
    @Override
    public void gotYieldStatement() {
        record("gotYieldStatement");
    }
    
    @Override
    public void gotEmptyStatement() {
        record("gotEmptyStatement");
    }
    
    @Override
    public void gotAssert() {
        record("gotAssert");
    }
    
    // ==================== Annotation Callbacks ====================
    
    @Override
    public void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) {
        Map<String, Object> params = new HashMap<>();
        params.put("annName", annName);
        params.put("paramsFollow", paramsFollow);
        record("gotAnnotation", params);
    }
    
    // ==================== Lambda Callbacks ====================
    
    @Override
    public void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) {
        Map<String, Object> params = new HashMap<>();
        params.put("lambdaIsBlock", lambdaIsBlock);
        params.put("openCurly", openCurly);
        record("beginLambdaBody", params);
    }
    
    @Override
    public void endLambdaBody(LocatableToken closeCurly) {
        Map<String, Object> params = new HashMap<>();
        params.put("closeCurly", closeCurly);
        record("endLambdaBody", params);
    }
    
    @Override
    public void gotLambdaFormalParam() {
        record("gotLambdaFormalParam");
    }
    
    @Override
    public void gotLambdaFormalName(LocatableToken name) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        record("gotLambdaFormalName", params);
    }
    
    @Override
    public void gotLambdaFormalType(List<LocatableToken> type) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        record("gotLambdaFormalType", params);
    }
    
    // ==================== Record Callbacks ====================
    
    @Override
    public void beginRecordParameters(LocatableToken parenToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("parenToken", parenToken);
        record("beginRecordParameters", params);
    }
    
    @Override
    public void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {
        Map<String, Object> params = new HashMap<>();
        params.put("first", first);
        params.put("idToken", idToken);
        params.put("varargsToken", varargsToken);
        record("gotRecordParameter", params);
    }
    
    @Override
    public void endRecordParameters(LocatableToken closeParen) {
        Map<String, Object> params = new HashMap<>();
        params.put("closeParen", closeParen);
        record("endRecordParameters", params);
    }
    
    // ==================== Method Declaration Callbacks ====================
    
    @Override
    public void endMethodDecl(LocatableToken token, boolean included) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        params.put("included", included);
        record("endMethodDecl", params);
        // `JavaParserCallbacksBase` calls it and we need to track it
        // TODO: how to not do this manually here?
        // endElement(token, included);
        // TODO2: `EditorParser` actually overrides it not emit the `endElement` which makes it even more funky
    }
    
    @Override
    public void beginThrows(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("beginThrows", params);
    }
    
    @Override
    public void endThrows() {
        record("endThrows");
    }
    
    // ==================== Comment Callback ====================
    
    @Override
    public void gotComment(LocatableToken token) {
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        record("gotComment", params);
    }
    
    // ==================== Error Callback ====================
    
    @Override
    public void error(String msg, int beginLine, int beginCol, int endLine, int endCol) {
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