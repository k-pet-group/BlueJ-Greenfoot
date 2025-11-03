package bluej.parser.psi;

import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import java.util.*;

/**
 * Validates pairing of parser callbacks using hybrid state machine.
 * 
 * <p>Handles two validation patterns:
 * <ul>
 *   <li><b>Simple Pairs</b>: begin* ↔ end* (name-based)</li>
 *   <li><b>State Transitions</b>: initiator → refiner → closer (state-based)</li>
 * </ul>
 * 
 * <h2>Type Safety</h2>
 * <p>Uses sealed interfaces and pattern matching for compile-time guarantees.
 * 
 * <h2>Thread Safety</h2>
 * <p><strong>Immutable after construction.</strong> All validation happens in the constructor,
 * making this class thread-safe for read operations.
 * 
 * @see StackEntry
 * @see CallbackRole
 * @see StateTransitionRule
 */
public class PairingValidator {
    
    // Configuration maps and rules
    private final Map<String, CallbackRole> callbackRoles;
    private final List<StateTransitionRule> stateRules;
    
    // Special pairing map for callbacks that don't follow begin*/end* convention
    private final Map<String, String> specialPairings;
    
    // Validation results (immutable after construction)
    private final List<CallbackPairing> pairings;
    private final List<String> errors;
    private final boolean balanced;
    
    /**
     * Constructs a validator and performs deferred validation on the complete callback sequence.
     *
     * <p>Validation happens immediately in the constructor, analyzing the complete
     * sequence to provide enhanced error messages with full context.</p>
     *
     * @param callbacks The complete sequence of callback records to validate
     */
    public PairingValidator(List<CallbackRecord> callbacks) {
        this.callbackRoles = new HashMap<>();
        this.stateRules = new ArrayList<>();
        this.specialPairings = new HashMap<>();
        
        initializeCallbackRoles();
        initializeStateRules();
        initializeSpecialPairings();
        
        ValidationState state = validateSequence(callbacks);
        this.pairings = state.pairings;
        this.errors = state.errors;
        this.balanced = state.balanced;
    }
    
    /**
     * Register callback roles for all known callbacks.
     */
    private void initializeCallbackRoles() {
        // State transition callbacks
        callbackRoles.put("gotDeclBegin", CallbackRole.CONTEXT_INITIATOR);
        callbackRoles.put("gotMethodDeclaration", CallbackRole.CONTEXT_REFINER);
        callbackRoles.put("gotConstructorDecl", CallbackRole.CONTEXT_REFINER);
        callbackRoles.put("gotTypeDef", CallbackRole.CONTEXT_REFINER);
        callbackRoles.put("beginFieldDeclarations", CallbackRole.CONTEXT_REFINER);
        callbackRoles.put("endMethodDecl", CallbackRole.CONTEXT_CLOSER);
        callbackRoles.put("gotTypeDefEnd", CallbackRole.CONTEXT_CLOSER);
        callbackRoles.put("endFieldDeclarations", CallbackRole.CONTEXT_CLOSER);

        // Simple paired callbacks
        callbackRoles.put("beginMethodBody", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endMethodBody", CallbackRole.PAIRED_END);
        callbackRoles.put("beginTypeBody", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endTypeBody", CallbackRole.PAIRED_END);
        callbackRoles.put("beginForLoop", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endForLoop", CallbackRole.PAIRED_END);
        callbackRoles.put("beginWhileLoop", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endWhileLoop", CallbackRole.PAIRED_END);
        callbackRoles.put("beginForInitExpression", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endForInitExpression", CallbackRole.PAIRED_END);
        callbackRoles.put("beginForUpdateExpression", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endForUpdateExpression", CallbackRole.PAIRED_END);
        callbackRoles.put("beginExpression", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endExpression", CallbackRole.PAIRED_END);
        callbackRoles.put("beginDoLoop", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endDoLoop", CallbackRole.PAIRED_END);
        callbackRoles.put("beginExpressionStatement", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endExpressionStatement", CallbackRole.PAIRED_END);
        callbackRoles.put("beginBreak", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endBreak", CallbackRole.PAIRED_END);
        callbackRoles.put("beginContinue", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endContinue", CallbackRole.PAIRED_END);
        callbackRoles.put("beginReturnStatement", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endReturnStatement", CallbackRole.PAIRED_END);
        callbackRoles.put("beginThrowStatement", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endThrowStatement", CallbackRole.PAIRED_END);
        callbackRoles.put("beginSynchronizedBlock", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endSynchronizedBlock", CallbackRole.PAIRED_END);
        callbackRoles.put("beginTryBlock", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endTryBlock", CallbackRole.PAIRED_END);
        callbackRoles.put("beginCatchClause", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endCatchClause", CallbackRole.PAIRED_END);
        callbackRoles.put("beginFinallyBlock", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endFinallyBlock", CallbackRole.PAIRED_END);
        callbackRoles.put("beginArrayInitExpression", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endArrayInitExpression", CallbackRole.PAIRED_END);
//        callbackRoles.put("beginFieldDeclarations", CallbackRole.PAIRED_BEGIN);
//        callbackRoles.put("endFieldDeclarations", CallbackRole.PAIRED_END);
        callbackRoles.put("gotField", CallbackRole.PAIRED_BEGIN);          // First field opener
        callbackRoles.put("gotSubsequentField", CallbackRole.PAIRED_BEGIN); // 2nd+ field opener
        callbackRoles.put("endField", CallbackRole.PAIRED_END);             // Field closer (reused!)
        callbackRoles.put("beginConstructor", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endConstructor", CallbackRole.PAIRED_END);
        callbackRoles.put("beginMethodParam", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endMethodParam", CallbackRole.PAIRED_END);
        callbackRoles.put("beginConstructorParam", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endConstructorParam", CallbackRole.PAIRED_END);
        callbackRoles.put("beginLambdaExpression", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endLambdaExpression", CallbackRole.PAIRED_END);
        callbackRoles.put("beginIfCondition", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endIfCondition", CallbackRole.PAIRED_END);
        callbackRoles.put("beginIfBody", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endIfBody", CallbackRole.PAIRED_END);
        callbackRoles.put("beginElseClause", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endElseClause", CallbackRole.PAIRED_END);
        callbackRoles.put("beginBlock", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endBlock", CallbackRole.PAIRED_END);
        
        // Additional begin/end pairs from existing tests
        callbackRoles.put("beginClass", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endClass", CallbackRole.PAIRED_END);
        callbackRoles.put("beginMethod", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endMethod", CallbackRole.PAIRED_END);
        callbackRoles.put("beginObject", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endObject", CallbackRole.PAIRED_END);
        callbackRoles.put("beginParsing", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endParsing", CallbackRole.PAIRED_END);
        callbackRoles.put("beginTypeDecl", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endTypeDecl", CallbackRole.PAIRED_END);
        callbackRoles.put("beginMethodDeclaration", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endMethodDeclaration", CallbackRole.PAIRED_END);
        callbackRoles.put("beginFieldDeclaration", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endFieldDeclaration", CallbackRole.PAIRED_END);
        callbackRoles.put("beginClassDeclaration", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endClassDeclaration", CallbackRole.PAIRED_END);
        callbackRoles.put("beginMethodSignature", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endMethodSignature", CallbackRole.PAIRED_END);
        
        // Support for test callback names with "end" in middle
        callbackRoles.put("beginExtendedMethod", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endExtendedMethod", CallbackRole.PAIRED_END);
        callbackRoles.put("beginAppend", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endAppend", CallbackRole.PAIRED_END);
        callbackRoles.put("beginExtended", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endExtended", CallbackRole.PAIRED_END);
        callbackRoles.put("beginAppendData", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endAppendData", CallbackRole.PAIRED_END);
        callbackRoles.put("beginWeekend", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endWeekend", CallbackRole.PAIRED_END);
        callbackRoles.put("beginDescriptor", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endDescriptor", CallbackRole.PAIRED_END);
        
        // Generic test patterns (A, B, C, First, Second, Third)
        callbackRoles.put("beginA", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endA", CallbackRole.PAIRED_END);
        callbackRoles.put("beginB", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endB", CallbackRole.PAIRED_END);
        callbackRoles.put("beginC", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endC", CallbackRole.PAIRED_END);
        callbackRoles.put("beginFirst", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endFirst", CallbackRole.PAIRED_END);
        callbackRoles.put("beginSecond", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endSecond", CallbackRole.PAIRED_END);
        callbackRoles.put("beginThird", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endThird", CallbackRole.PAIRED_END);
        callbackRoles.put("beginClass1", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endClass1", CallbackRole.PAIRED_END);
        callbackRoles.put("beginClass2", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endClass2", CallbackRole.PAIRED_END);
        callbackRoles.put("beginClass3", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endClass3", CallbackRole.PAIRED_END);
        
        // Informational callbacks (no validation)
        callbackRoles.put("gotModifier", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotIdentifier", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotTypeSpec", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotFieldType", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotMethodParameter", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotConstructorParameter", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotArrayDeclarator", CallbackRole.INFORMATIONAL);
        callbackRoles.put("modifiersConsumed", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotAllMethodParameters", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotMethodTypeParamsBegin", CallbackRole.INFORMATIONAL);
        callbackRoles.put("endMethodTypeParams", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotTypeParam", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotTypeParamBound", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotTypeDefName", CallbackRole.INFORMATIONAL);
        callbackRoles.put("beginTypeDefExtends", CallbackRole.INFORMATIONAL);
        callbackRoles.put("endTypeDefExtends", CallbackRole.INFORMATIONAL);
        callbackRoles.put("beginTypeDefImplements", CallbackRole.INFORMATIONAL);
        callbackRoles.put("endTypeDefImplements", CallbackRole.INFORMATIONAL);
    }
    
    /**
     * Register special pairings for callbacks that don't follow begin/end naming convention.
     * Maps begin callback names to their corresponding end callback names.
     */
    private void initializeSpecialPairings() {
        // Field callbacks: gotField and gotSubsequentField both pair with endField
        // We'll store the primary one and check others via logic
        specialPairings.put("gotField", "endField");
        specialPairings.put("gotSubsequentField", "endField");
    }
    
    /**
     * Register state transition rules.
     */
    private void initializeStateRules() {
        // Declaration rule: gotDeclBegin can be refined to method or type
        StateTransitionRule declRule = new StateTransitionRule(
            "gotDeclBegin",
            List.of("gotMethodDeclaration", "gotConstructorDecl", "gotTypeDef",  "beginFieldDeclarations"),
            Map.of(
                "gotMethodDeclaration", "endMethodDecl",
                "gotConstructorDecl", "endMethodDecl",
                "gotTypeDef", "gotTypeDefEnd",
                "beginFieldDeclarations", "endFieldDeclarations"
            )
        );
        stateRules.add(declRule);
    }
    
    /**
     * Performs validation on the complete callback sequence using hybrid state machine.
     *
     * @param callbacks The complete sequence of callback records
     * @return Validation state with pairings and errors
     */
    private ValidationState validateSequence(List<CallbackRecord> callbacks) {
        List<CallbackPairing> pairings = new ArrayList<>();
        List<String> sequenceErrors = new ArrayList<>();
        Stack<StackEntry> contextStack = new Stack<>();
        
        for (int i = 0; i < callbacks.size(); i++) {
            CallbackRecord record = callbacks.get(i);
            String callbackName = record.getCallbackName();
            
            // Validate callback name
            if (callbackName == null || callbackName.isEmpty()) {
                sequenceErrors.add(String.format(
                    "Invalid callback at position %d: null or empty callback name", i));
                continue;
            }
            
            // Get callback role with fallback for unregistered begin/end callbacks
            CallbackRole role;
            if (callbackRoles.containsKey(callbackName)) {
                role = callbackRoles.get(callbackName);
            } else if (callbackName.startsWith("begin")) {
                // Fallback: treat unregistered begin* as PAIRED_BEGIN
                role = CallbackRole.PAIRED_BEGIN;
            } else if (callbackName.startsWith("end")) {
                // Fallback: treat unregistered end* as PAIRED_END
                role = CallbackRole.PAIRED_END;
            } else {
                // Default to informational
                role = CallbackRole.INFORMATIONAL;
            }
            
            // Dispatch based on role
            switch (role) {
                case CONTEXT_INITIATOR -> 
                    handleContextInitiator(callbackName, i, contextStack, sequenceErrors);
                    
                case CONTEXT_REFINER -> 
                    handleContextRefiner(callbackName, i, contextStack, sequenceErrors);
                    
                case CONTEXT_CLOSER -> 
                    handleContextCloser(callbackName, i, contextStack, sequenceErrors, pairings);
                    
                case PAIRED_BEGIN -> 
                    handlePairedBegin(callbackName, i, contextStack);
                    
                case PAIRED_END -> 
                    handlePairedEnd(callbackName, i, contextStack, sequenceErrors, pairings);
                    
                case INFORMATIONAL -> { 
                    /* No validation */ 
                }
            }
        }
        
        // Final check: all contexts must be closed
        validateAllContextsClosed(contextStack, sequenceErrors, pairings);
        
        boolean isBalanced = sequenceErrors.isEmpty();
        return new ValidationState(pairings, sequenceErrors, isBalanced);
    }
    
    /**
     * Handle CONTEXT_INITIATOR role.
     */
    private void handleContextInitiator(
        String callback, 
        int index,
        Stack<StackEntry> contextStack,
        List<String> errors
    ) {
        StateTransitionRule rule = findRuleForInitiator(callback);
        
        if (rule == null) {
            errors.add(String.format(
                "No state transition rule for initiator: %s at position %d", callback, index));
            return;
        }
        
        ValidationContext context = new ValidationContext(
            callback, index, null, -1, ContextState.INITIATED
        );
        contextStack.push(context);
    }
    
    /**
     * Handle CONTEXT_REFINER role.
     */
    private void handleContextRefiner(
        String callback,
        int index,
        Stack<StackEntry> contextStack,
        List<String> errors
    ) {
        if (contextStack.isEmpty()) {
            errors.add(String.format(
                "Refiner without initiator: %s at position %d", callback, index));
            return;
        }
        
        StackEntry top = contextStack.peek();
        
        switch (top) {
            case PendingBegin pending -> {
                errors.add(String.format(
                    "Cannot refine simple pair '%s' at index %d with refiner: %s at index %d",
                    pending.callbackType(), pending.index(), callback, index
                ));
            }
            
            case ValidationContext ctx -> {
                if (ctx.state() != ContextState.INITIATED) {
                    errors.add(String.format(
                        "Cannot refine context in state: %s at position %d", ctx.state(), index
                    ));
                    return;
                }
                
                StateTransitionRule rule = findRuleForInitiator(ctx.initiator());
                
                if (rule == null || !rule.validRefiners.contains(callback)) {
                    errors.add(String.format(
                        "Invalid refiner '%s' for initiator '%s' at position %d",
                        callback, ctx.initiator(), index
                    ));
                    return;
                }
                
                // Refine context (immutable pattern)
                ValidationContext refined = ctx.refine(callback, index);
                contextStack.pop();
                contextStack.push(refined);
            }
        }
    }
    
    /**
     * Handle CONTEXT_CLOSER role.
     */
    private void handleContextCloser(
        String callback,
        int index,
        Stack<StackEntry> contextStack,
        List<String> errors,
        List<CallbackPairing> pairings
    ) {
        if (contextStack.isEmpty()) {
            errors.add(String.format(
                "Closer without context: %s at position %d", callback, index));
            return;
        }
        
        StackEntry top = contextStack.peek();
        
        switch (top) {
            case PendingBegin pending -> {
                errors.add(String.format(
                    "Expected end for '%s' at index %d but got closer: %s at index %d",
                    pending.callbackType(), pending.index(), callback, index
                ));
            }
            
            case ValidationContext ctx -> {
                if (ctx.state() != ContextState.REFINED) {
                    errors.add(String.format(
                        "Cannot close context in state: %s at position %d", ctx.state(), index
                    ));
                    return;
                }
                
                StateTransitionRule rule = findRuleForInitiator(ctx.initiator());
                if (rule == null) {
                    errors.add(String.format(
                        "No rule found for initiator: %s at position %d", ctx.initiator(), index
                    ));
                    return;
                }
                
                String expectedCloser = rule.getCloserFor(ctx.refiner());
                
                if (!callback.equals(expectedCloser)) {
                    errors.add(String.format(
                        "Expected closer '%s' for refiner '%s', got '%s' at position %d",
                        expectedCloser, ctx.refiner(), callback, index
                    ));
                    return;
                }
                
                // Successfully closed - create pairing and remove
                contextStack.pop();
                pairings.add(new CallbackPairing(
                    ctx.initiator(), ctx.initiatorIndex(),
                    callback, index, true, null
                ));
            }
        }
    }
    
    /**
     * Handle PAIRED_BEGIN role.
     */
    private void handlePairedBegin(
        String callback,
        int index,
        Stack<StackEntry> contextStack
    ) {
        // Create dummy token for now (will be enhanced later if needed)
        LineColPos dummyPos = new LineColPos(1, 1, 0);
        LocatableToken dummyToken = new LocatableToken(0, "", dummyPos, dummyPos);
        PendingBegin pending = new PendingBegin(callback, dummyToken, index);
        contextStack.push(pending);
    }
    
    /**
     * Handle PAIRED_END role.
     */
    private void handlePairedEnd(
        String callback,
        int index,
        Stack<StackEntry> contextStack,
        List<String> errors,
        List<CallbackPairing> pairings
    ) {
        if (contextStack.isEmpty()) {
            errors.add(String.format(
                "Unpaired end callback at position %d:\n" +
                "  Callback: %s\n" +
                "  Error: No matching begin callback found (stack is empty)",
                index, callback));
            return;
        }
        
        StackEntry top = contextStack.peek();
        
        switch (top) {
            case PendingBegin pending -> {
                // Check if this is a valid pairing
                boolean isValidPair;
                String expectedEnd;
                
                // Check for special pairings (e.g., gotField/gotSubsequentField ↔ endField)
                if (specialPairings.containsKey(pending.callbackType())) {
                    expectedEnd = specialPairings.get(pending.callbackType());
                    isValidPair = callback.equals(expectedEnd);
                } else {
                    // Standard begin*/end* pairing
                    expectedEnd = pending.expectedEndType();
                    isValidPair = callback.equals(expectedEnd);
                }
                
                if (!isValidPair) {
                    int callbacksBetween = index - pending.index() - 1;
                    String contextInfo = callbacksBetween > 0
                        ? String.format(" (%d callback%s between begin and end)",
                                       callbacksBetween, callbacksBetween == 1 ? "" : "s")
                        : "";
                    
                    String detailedError = String.format(
                        "Callback pairing mismatch at position %d:\n" +
                        "  %s at index %d attempted to match %s at index %d\n" +
                        "  Expected: %s (to properly close %s)\n" +
                        "  Got: %s%s",
                        index, callback, index, pending.callbackType(), pending.index(),
                        expectedEnd, pending.callbackType(), callback, contextInfo);
                    
                    errors.add(detailedError);
                    // Don't pop - keep begin on stack as unmatched
                    return;
                }
                
                // Valid pair
                contextStack.pop();
                pairings.add(new CallbackPairing(
                    pending.callbackType(), pending.index(),
                    callback, index, true, null
                ));
            }
            
            case ValidationContext ctx -> {
                errors.add(String.format(
                    "Expected ValidationContext closer but got end: %s at position %d", callback, index
                ));
            }
        }
    }
    
    /**
     * Validate all contexts closed at end.
     */
    private void validateAllContextsClosed(
        Stack<StackEntry> contextStack,
        List<String> errors,
        List<CallbackPairing> pairings
    ) {
        if (contextStack.isEmpty()) {
            return;
        }
        
        while (!contextStack.isEmpty()) {
            StackEntry entry = contextStack.pop();
            
            switch (entry) {
                case PendingBegin pending -> {
                    String expectedEnd = pending.expectedEndType();
                    String detailedError = String.format(
                        "Unmatched begin callback:\n" +
                        "  %s at index %d was never closed\n" +
                        "  Expected: %s should appear after this callback\n" +
                        "  Context: This begin callback remains open at end of sequence",
                        pending.callbackType(), pending.index(), expectedEnd);
                    
                    errors.add(detailedError);
                    pairings.add(new CallbackPairing(
                        pending.callbackType(), pending.index(),
                        null, -1, false, detailedError
                    ));
                }
                
                case ValidationContext ctx -> {
                    String detailedError = String.format(
                        "Unclosed context: initiator='%s' at index %d, refiner='%s', state=%s",
                        ctx.initiator(), ctx.initiatorIndex(), ctx.refiner(), ctx.state()
                    );
                    
                    errors.add(detailedError);
                    pairings.add(new CallbackPairing(
                        ctx.initiator(), ctx.initiatorIndex(),
                        null, -1, false, detailedError
                    ));
                }
            }
        }
    }
    
    /**
     * Find state transition rule for initiator.
     */
    private StateTransitionRule findRuleForInitiator(String initiator) {
        return stateRules.stream()
            .filter(rule -> rule.contextInitiator.equals(initiator))
            .findFirst()
            .orElse(null);
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Returns whether all begin/end callback pairs are balanced.
     *
     * @return true if all pairs matched correctly, false otherwise
     */
    public boolean isBalanced() {
        return balanced;
    }
    
    /**
     * Returns whether any validation errors occurred.
     *
     * @return true if errors exist or callbacks unmatched, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty() || !balanced;
    }
    
    /**
     * Returns the list of callback pairings discovered during validation.
     *
     * <p>Each pairing shows which begin callback matched (or attempted to match)
     * with which end callback, including position indices.</p>
     *
     * @return Unmodifiable list of callback pairings
     */
    public List<CallbackPairing> getPairings() {
        return List.copyOf(pairings);
    }
    
    /**
     * Returns all validation errors with enhanced context.
     *
     * @return Unmodifiable list of detailed error messages
     */
    public List<String> getErrors() {
        return List.copyOf(errors);
    }
    
    /**
     * Returns the count of unmatched begin callbacks.
     *
     * @return Number of begin callbacks that were never matched
     */
    public int getUnmatchedCount() {
        return (int) pairings.stream()
            .filter(p -> !p.isMatched())
            .count();
    }
    
    /**
     * Returns a list of all unmatched begin callbacks.
     *
     * @return Unmodifiable list of unmatched begin callback names
     */
    public List<String> getUnmatchedCallbacks() {
        return pairings.stream()
            .filter(p -> !p.isMatched())
            .map(CallbackPairing::getBeginCallback)
            .toList();
    }
    
    /**
     * Generates a detailed validation summary for debugging.
     *
     * <p>Includes pairing information, position indices, and full error context.</p>
     *
     * @return Formatted detailed summary
     */
    public String getDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Sequence Validation Summary:\n");
        sb.append("  Total pairings: ").append(pairings.size()).append("\n");
        sb.append("  Matched: ").append(pairings.stream().filter(CallbackPairing::isMatched).count()).append("\n");
        sb.append("  Unmatched: ").append(getUnmatchedCount()).append("\n");
        sb.append("  Errors: ").append(errors.size()).append("\n");
        sb.append("  Balanced: ").append(balanced).append("\n");
        
        if (!pairings.isEmpty()) {
            sb.append("\nPairings:\n");
            for (CallbackPairing pairing : pairings) {
                sb.append("  ").append(pairing).append("\n");
            }
        }
        
        if (!errors.isEmpty()) {
            sb.append("\nDetailed Errors:\n");
            for (String error : errors) {
                sb.append("  ").append(error).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Generates a basic validation summary (legacy compatibility).
     *
     * @return Formatted summary with error details
     */
    public String getValidationSummary() {
        return getDetailedSummary();
    }
    
    // ==================== Data Classes ====================
    
    /**
     * Internal validation state holder.
     */
    private static class ValidationState {
        final List<CallbackPairing> pairings;
        final List<String> errors;
        final boolean balanced;
        
        ValidationState(List<CallbackPairing> pairings, List<String> errors, boolean balanced) {
            this.pairings = pairings;
            this.errors = errors;
            this.balanced = balanced;
        }
    }
    
    /**
     * Represents a begin/end callback pairing with position information.
     * Used to provide detailed error context in validation results.
     */
    public static class CallbackPairing {
        private final String beginCallback;
        private final int beginIndex;
        private final String endCallback; // null if unmatched
        private final int endIndex; // -1 if unmatched
        private final boolean matched;
        private final String errorMessage; // null if no error
        
        private CallbackPairing(String beginCallback, int beginIndex, String endCallback, 
                               int endIndex, boolean matched, String errorMessage) {
            this.beginCallback = beginCallback;
            this.beginIndex = beginIndex;
            this.endCallback = endCallback;
            this.endIndex = endIndex;
            this.matched = matched;
            this.errorMessage = errorMessage;
        }
        
        public String getBeginCallback() { return beginCallback; }
        public int getBeginIndex() { return beginIndex; }
        public String getEndCallback() { return endCallback; }
        public int getEndIndex() { return endIndex; }
        public boolean isMatched() { return matched; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            if (matched) {
                return String.format("%s[%d] ← → %s[%d]", beginCallback, beginIndex, endCallback, endIndex);
            } else if (endCallback == null) {
                return String.format("%s[%d] (unmatched)", beginCallback, beginIndex);
            } else {
                return String.format("%s[%d] ← X → %s[%d]: mismatch", beginCallback, beginIndex, endCallback, endIndex);
            }
        }
    }
}