package bluej.parser.psi;

import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import org.jetbrains.kotlin.com.intellij.psi.JavaTokenType;

import java.util.*;
import java.util.stream.Collectors;

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
    private final Map<String, Set<String>> specialPairings;
    
    // Callback sequence storage for visualization
    private final List<CallbackRecord> callbacks;
    
    // Validation results (immutable after construction)
    private final List<CallbackPairing> pairings;
    private final Map<Integer, String> errors;
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
        // Store defensive copy of callbacks for visualization
        this.callbacks = new ArrayList<>(callbacks);
        
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
        callbackRoles.put("beginInitBlock", CallbackRole.CONTEXT_REFINER);
        callbackRoles.put("endInitBlock", CallbackRole.CONTEXT_CLOSER);
        callbackRoles.put("beginVariableDecl", CallbackRole.CONTEXT_REFINER);
        callbackRoles.put("endVariableDecls", CallbackRole.CONTEXT_CLOSER);

        // Simple paired callbacks
        callbackRoles.put("beginElement", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endElement", CallbackRole.PAIRED_END);
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
        callbackRoles.put("beginArgumentList", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endArgumentList", CallbackRole.PAIRED_END);
//        callbackRoles.put("beginArgument", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endArgument", CallbackRole.INFORMATIONAL);


        callbackRoles.put("gotField", CallbackRole.PAIRED_BEGIN);          // First field opener
        callbackRoles.put("gotSubsequentField", CallbackRole.PAIRED_BEGIN); // 2nd+ field opener
        callbackRoles.put("endField", CallbackRole.PAIRED_END);             // Field closer (reused!)

        callbackRoles.put("gotVariableDecl", CallbackRole.PAIRED_BEGIN);  // First field opener
        callbackRoles.put("gotSubsequentVar", CallbackRole.PAIRED_BEGIN); // 2nd+ field opener
        callbackRoles.put("endVariable", CallbackRole.PAIRED_END);        // Field closer (reused!)

        callbackRoles.put("beginFormalParameter", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("gotMethodParameter", CallbackRole.PAIRED_END);
        callbackRoles.put("gotRecordParameter", CallbackRole.PAIRED_END);

        callbackRoles.put("beginPackageStatement", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("gotPackageSemi", CallbackRole.PAIRED_END);



        //        callbackRoles.put("gotAllMethodParameters", CallbackRole.PAIRED_END);

        callbackRoles.put("gotExprNew", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endExprNew", CallbackRole.PAIRED_END);

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
        callbackRoles.put("beginRecordParameters", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endRecordParameters", CallbackRole.PAIRED_END);
        
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
//        callbackRoles.put("gotMethodParameter", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotAllMethodParameters", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotConstructorParameter", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotArrayDeclarator", CallbackRole.INFORMATIONAL);
        callbackRoles.put("modifiersConsumed", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotMethodTypeParamsBegin", CallbackRole.INFORMATIONAL);
        callbackRoles.put("endMethodTypeParams", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotTypeParam", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotTypeParamBound", CallbackRole.INFORMATIONAL);
        callbackRoles.put("gotTypeDefName", CallbackRole.INFORMATIONAL);
        callbackRoles.put("beginTypeDefExtends", CallbackRole.INFORMATIONAL);
        callbackRoles.put("endTypeDefExtends", CallbackRole.INFORMATIONAL);
        callbackRoles.put("beginTypeDefImplements", CallbackRole.INFORMATIONAL);
        callbackRoles.put("endTypeDefImplements", CallbackRole.INFORMATIONAL);
//        callbackRoles.put("endForInitDecls", CallbackRole.INFORMATIONAL);  // Marker for end of all for-init declarations
        callbackRoles.put("beginForInitDecl", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endForInitDecls", CallbackRole.PAIRED_END);  // Marker for end of all for-init declarations
        callbackRoles.put("gotForInit", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endForInit", CallbackRole.PAIRED_END);
//        callbackRoles.put("endForInitDecl", CallbackRole.PAIRED_END);
        callbackRoles.put("beginTryCatchStmt", CallbackRole.PAIRED_BEGIN);
        callbackRoles.put("endTryCatchStmt", CallbackRole.PAIRED_END);
    }
    
    /**
     * Register special pairings for callbacks that don't follow begin/end naming convention.
     * Maps begin callback names to their corresponding end callback names.
     */
    private void initializeSpecialPairings() {
        // Field callbacks: gotField and gotSubsequentField both pair with endField
        // We'll store the primary one and check others via logic
        specialPairings.put("gotField", Set.of("endField"));
        specialPairings.put("gotSubsequentField", Set.of("endField"));
        specialPairings.put("gotExprNew", Set.of("endExprNew"));
        // Variables also have specific pairing
        specialPairings.put("gotVariableDecl", Set.of("endVariable"));
        specialPairings.put("gotSubsequentVar", Set.of("endVariable"));
        specialPairings.put("beginFormalParameter", Set.of("gotMethodParameter", "gotRecordParameter"));
        specialPairings.put("beginForInitDecl", Set.of("endForInitDecls"));
        specialPairings.put("gotForInit", Set.of("endForInit"));
        specialPairings.put("beginPackageStatement", Set.of("gotPackageSemi"));
    }
    
    /**
     * Register state transition rules.
     */
    private void initializeStateRules() {
        // Declaration rule: gotDeclBegin can be refined to method or type
        StateTransitionRule declRule = new StateTransitionRule(
            "gotDeclBegin",
            List.of("gotMethodDeclaration", "gotConstructorDecl", "gotTypeDef",  "beginFieldDeclarations", "beginInitBlock", "beginVariableDecl"),
            Map.of(
                "gotMethodDeclaration", "endMethodDecl",
                "gotConstructorDecl", "endMethodDecl",
                "gotTypeDef", "gotTypeDefEnd",
                "beginFieldDeclarations", "endFieldDeclarations",
                "beginInitBlock", "endInitBlock",
                "beginVariableDecl","endVariableDecls"
//                "gotForInit", "endForInit"
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
        Map<Integer, String> sequenceErrors = new HashMap<>();
        Stack<StackEntry> contextStack = new Stack<>();
        
        for (int i = 0; i < callbacks.size(); i++) {
            CallbackRecord record = callbacks.get(i);
            String callbackName = record.getCallbackName();
            
            // Validate callback name
            if (callbackName == null || callbackName.isEmpty()) {
                sequenceErrors.put(i, String.format(
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
                    handleContextInitiator(record, i, contextStack, sequenceErrors);
                    
                case CONTEXT_REFINER -> 
                    handleContextRefiner(record, i, contextStack, sequenceErrors);
                    
                case CONTEXT_CLOSER -> 
                    handleContextCloser(record, i, contextStack, sequenceErrors, pairings);
                    
                case PAIRED_BEGIN -> 
                    handlePairedBegin(record, i, contextStack);
                    
                case PAIRED_END -> 
                    handlePairedEnd(record, i, contextStack, sequenceErrors, pairings);
                    
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
        CallbackRecord callback,
        int index,
        Stack<StackEntry> contextStack,
        Map<Integer, String> errors
    ) {
        String callbackName = callback.getCallbackName();
        StateTransitionRule rule = findRuleForInitiator(callbackName);
        
        if (rule == null) {
            errors.put(index, String.format(
                "No state transition rule for initiator: %s at position %d", callback, index));
            return;
        }
        
        // Get stacktrace from CallbackRecord (captured during parsing)
        StackTraceElement[] stackTrace = callbacks.get(index).getCallStackTrace();
        ValidationContext context = new ValidationContext(
            callback, index, null, -1, ContextState.INITIATED, stackTrace
        );
        contextStack.push(context);
    }
    
    /**
     * Handle CONTEXT_REFINER role.
     */
    private void handleContextRefiner(
        CallbackRecord callback,
        int index,
        Stack<StackEntry> contextStack,
        Map<Integer, String> errors
    ) {
        if (contextStack.isEmpty()) {
            errors.put(index, String.format(
                "Refiner without initiator: %s at position %d", callback, index));
            return;
        }
        
        StackEntry top = contextStack.peek();
        
        switch (top) {
            case PendingBegin pending -> {
                errors.put(index, String.format(
                    "Cannot refine simple pair '%s' at index %d with refiner: %s at index %d",
                    pending.callbackType(), pending.index(), callback, index
                ));
            }
            
            case ValidationContext ctx -> {
                if (ctx.state() != ContextState.INITIATED) {
                    errors.put(index, String.format(
                        "Cannot refine context in state: %s at position %d", ctx.state(), index
                    ));
                    return;
                }

                CallbackRecord initiator = ctx.initiator();
                
                StateTransitionRule rule = findRuleForInitiator(initiator.getCallbackName());
                
                if (rule == null || !rule.validRefiners.contains(callback.getCallbackName())) {
                    errors.put(index, String.format(
                        "Invalid refiner '%s' for initiator '%s' at position %d",
                        callback, initiator, index
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
        CallbackRecord callback,
        int index,
        Stack<StackEntry> contextStack,
        Map<Integer, String> errors,
        List<CallbackPairing> pairings
    ) {
        if (contextStack.isEmpty()) {
            errors.put(index, String.format(
                "Closer without context: %s at position %d", callback, index));
            return;
        }
        
        StackEntry top = contextStack.peek();
        
        switch (top) {
            case PendingBegin pending -> {
                errors.put(index, String.format(
                    "Expected end for '%s' at index %d but got closer: %s at index %d",
                    pending.callbackType(), pending.index(), callback, index
                ));
            }
            
            case ValidationContext ctx -> {
                if (ctx.state() != ContextState.REFINED) {
                    errors.put(index, String.format(
                        "Cannot close context in state: %s at position %d", ctx.state(), index
                    ));
                    return;
                }

                CallbackRecord initiator = ctx.initiator();

                StateTransitionRule rule = findRuleForInitiator(initiator.getCallbackName());

                if (rule == null) {
                    errors.put(index, String.format(
                        "No rule found for initiator: %s at position %d", initiator, index
                    ));
                    return;
                }

                CallbackRecord refiner = ctx.refiner();

                String expectedCloser = rule.getCloserFor(refiner.getCallbackName());
                
                if (!callback.getCallbackName().equals(expectedCloser)) {
                    errors.put(index, String.format(
                        "Expected closer '%s' for refiner '%s', got '%s' at position %d",
                        expectedCloser, refiner, callback, index
                    ));
                    return;
                }
                
                // Successfully closed - create pairing and remove
                contextStack.pop();
                pairings.add(new CallbackPairing(
                    initiator, ctx.initiatorIndex(),
                    callback, index, true, null
                ));
            }
        }
    }
    
    /**
     * Handle PAIRED_BEGIN role.
     */
    private void handlePairedBegin(
        CallbackRecord callback,
        int index,
        Stack<StackEntry> contextStack
    ) {
        // Get stacktrace from CallbackRecord (captured during parsing)
        StackTraceElement[] stackTrace = callbacks.get(index).getCallStackTrace();
        PendingBegin pending = new PendingBegin(callback, index, stackTrace);
        contextStack.push(pending);
    }
    
    /**
     * Handle PAIRED_END role.
     */
    private void handlePairedEnd(
        CallbackRecord callback,
        int index,
        Stack<StackEntry> contextStack,
        Map<Integer, String> errors,
        List<CallbackPairing> pairings
    ) {
        var isOptionallyOpened = callback.getCallbackName().equals("gotAllMethodParameters");

        if (contextStack.isEmpty() && !isOptionallyOpened) {
            errors.put(index, String.format(
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
                    var potentialEnds = specialPairings.get(pending.callbackType());

                    if (potentialEnds.contains(callback.getCallbackName())) {
                        expectedEnd = callback.getCallbackName();
                        isValidPair = true;
                    }
                    else {
                        expectedEnd = "Set(" + potentialEnds.stream().collect(Collectors.joining(", ")) + ")";
                        isValidPair = false;
                    }
                } else {
                    // Standard begin*/end* pairing
                    expectedEnd = pending.expectedEndType();
                    isValidPair = callback.getCallbackName().equals(expectedEnd);
                }
                
                if (!isValidPair && !isOptionallyOpened) {
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
                        index, callback.getCallbackName(), index, pending.callbackType(), pending.index(),
                        expectedEnd, pending.callbackType(), callback.getCallbackName(), contextInfo);
                    
                    errors.put(index, detailedError);
                    // Don't pop - keep begin on stack as unmatched
                    return;
                }
                
                // Valid pair
                contextStack.pop();
                pairings.add(new CallbackPairing(
                    pending.callback(), pending.index(),
                    callback, index, true, null
                ));
            }
            
            case ValidationContext ctx -> {
                if (!isOptionallyOpened) {
                    errors.put(index, String.format(
                            "Expected ValidationContext closer but got end: %s at position %d", callback.getCallbackName(), index
                    ));
                }
                else {
                    contextStack.pop();
                }
            }
        }
    }
    
    /**
     * Validate all contexts closed at end.
     */
    private void validateAllContextsClosed(
        Stack<StackEntry> contextStack,
        Map<Integer, String> errors,
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
                    
                    errors.put(pending.index(), detailedError);
                    pairings.add(new CallbackPairing(
                        pending.callback(), pending.index(),
                        null, -1, false, detailedError
                    ));
                }
                
                case ValidationContext ctx -> {
                    String detailedError = String.format(
                        "Unclosed context: initiator='%s' at index %d, refiner='%s', state=%s",
                        ctx.initiator(), ctx.initiatorIndex(), ctx.refiner(), ctx.state()
                    );
                    
                    errors.put(ctx.initiatorIndex(), detailedError);
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
    
    // ==================== Nested Visualization Helper Methods ====================
    
    /**
     * Determines the role of a callback with fallback for unregistered callbacks.
     * Extracted from validation logic for reuse in visualization.
     */
    private CallbackRole determineRole(String callbackName) {
        // Handle null or empty callback names
        if (callbackName == null || callbackName.isEmpty()) {
            return CallbackRole.INFORMATIONAL;
        }
        
        if (callbackRoles.containsKey(callbackName)) {
            return callbackRoles.get(callbackName);
        } else if (callbackName.startsWith("begin")) {
            return CallbackRole.PAIRED_BEGIN;
        } else if (callbackName.startsWith("end")) {
            return CallbackRole.PAIRED_END;
        } else {
            return CallbackRole.INFORMATIONAL;
        }
    }
    
    /**
     * Determines if a callback opens a new scope (increases depth).
     */
    private boolean isOpener(String callbackName) {
        CallbackRole role = determineRole(callbackName);
        return role == CallbackRole.CONTEXT_INITIATOR || role == CallbackRole.PAIRED_BEGIN;
    }
    
    /**
     * Determines if a callback closes a scope (decreases depth).
     */
    private boolean isCloser(String callbackName) {
        CallbackRole role = determineRole(callbackName);
        return role == CallbackRole.CONTEXT_CLOSER || role == CallbackRole.PAIRED_END;
    }
    
    /**
     * Finds error message for a specific callback at given index.
     * Checks both the errors list and unmatched pairings.
     */
    private String findErrorForCallback(int index) {
        String callbackName = callbacks.get(index).getCallbackName();
        
//        // Check errors list for mentions of this callback at this index
//        // Handle null callback names safely
//        for (var entry : errors.keySet()) {
//
//            if (callbackName != null && error.contains(callbackName) && error.contains("" + index)) {
//                return error;
//            } else if (callbackName == null && error.contains("null") && error.contains("" + index)) {
//                return error;
//            }
//        }

        if (errors.containsKey(index)) {
            return errors.get(index);
        }
        
        // Check if this is an unmatched begin in pairings
        for (CallbackPairing pairing : pairings) {
            if (pairing.getBeginIndex() == index && !pairing.isMatched()) {
                CallbackRecord begin = pairing.getBeginCallback();
                String beginCallbackName = begin.getCallbackName();
                String expectedEnd = beginCallbackName.replace("begin", "end");
                // Check special pairings
                if (specialPairings.containsKey(pairing.getBeginCallback())) {
                    var potentialEnds = specialPairings.get(pairing.getBeginCallback());

                    if (potentialEnds.size() == 1) {
                        expectedEnd = potentialEnds.iterator().next();
                    }
                    else {
                        expectedEnd = "Set(" + potentialEnds.stream().collect(Collectors.joining(", ")) + ")";
                    }
                }

                return "UNPAIRED: expected " + expectedEnd;
            }
        }

        return null;
    }
    
    /**
     * Determines the symbol character for a callback based on its role and error status.
     *
     * @param record The callback record
     * @param pairedMap Map of begin indices to their matched ends (for paired detection)
     * @param errorMap Map of indices to error messages
     * @return Symbol character: +, >, *, !, or -
     */
    private String determineSymbol(CallbackRecord record,
                                   Map<Integer, CallbackRecord> pairedMap,
                                   Map<Integer, String> errorMap) {
        int index = callbacks.indexOf(record);
        String callbackName = record.getCallbackName();
        
        // Error takes precedence
        if (errorMap.containsKey(index)) {
            return "!";
        }
        
        CallbackRole role = determineRole(callbackName);
        return switch (role) {
            case CONTEXT_INITIATOR, PAIRED_BEGIN -> "+";
            case CONTEXT_REFINER -> ">";
            case CONTEXT_CLOSER, PAIRED_END -> "-";
            case INFORMATIONAL -> "*";
        };
    }
    
    /**
     * Formats a single callback line with indentation, symbol, and details.
     */
    private String formatCallbackLine(int depth, String symbol,
                                      CallbackRecord record, String errorDetails) {
        StringBuilder line = new StringBuilder();
        
        // Add indentation (2 spaces per level)
        line.append("  ".repeat(Math.max(0, depth)));
        
        // Add symbol
        line.append(symbol).append(" ");
        
        // Add callback name and index (handle null callback name)
        int index = callbacks.indexOf(record);
        String callbackName = record.getCallbackName();
        line.append(callbackName != null ? callbackName : "<null>").append("[").append(index).append("]");
        
        // Add token information if available
//        Object token = record.getParameter("token");
//        if (token != null) {
//            line.append(" (token = ").append(formatToken(token)).append(")");
//        }

        line.append(" (");

        for (var parameter : record.getParameters().entrySet()) {
            line
                .append(parameter.getKey())
                .append(" = ");

            switch (parameter.getValue()) {
                case LocatableToken token -> line.append(formatToken(token));
                case JavaTokenType tokenType -> line.append(tokenType.toString());
                case Object other -> line.append(other.toString());
                case null -> line.append("null");
            }

            line.append(", ");
        }

        line.append(")");
        
        // Add error details if present
        if (errorDetails != null && !errorDetails.isEmpty()) {
            var indent = "  ".repeat(Math.max(0, depth));

            line.append("\n");
            line.append(indent);

            var indentedError = errorDetails.lines()
                .flatMap (errorLine -> List.of(indent, "  |", errorLine, "\n").stream())
                .skip(2)  // don't indent and prefix the first line
                .collect(Collectors.joining(""));

            // and drop the last newline (yes, ugly)
            line.append("  ERROR: ").append(indentedError.substring(0, indentedError.length() - 1));
        }
        
        return line.toString();
    }
    
    /**
     * Formats token information for display.
     * Handles LocatableToken and generic objects.
     */
    private String formatToken(Object token) {
        if (token == null) {
            return "null";
        }
        
        if (token instanceof LocatableToken lt) {
            return String.format("LocatableToken(line=%d, col=%d, text='%s')",
                lt.getLine(), lt.getColumn(),
                (lt.getText() != null ? lt.getText() : "")).replaceAll("\n", "\\\\n");
        }
        
        return token.toString();
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
        return List.copyOf(errors.values());
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
            .map(CallbackRecord::getCallbackName)
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
            for (String error : errors.values()) {
                sb.append("  ").append(error).append("\n");
            }
        }

        sb.append(getNestedSummary());
        
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
    
    /**
     * Generates a nested visualization of the callback sequence.
     * 
     * <p>This method provides a hierarchical tree-like view of callbacks, showing
     * nesting relationships through indentation. Each callback is prefixed with a
     * symbol indicating its role:
     * <ul>
     *   <li>{@code +} - Opens new scope (CONTEXT_INITIATOR, PAIRED_BEGIN)</li>
     *   <li>{@code >} - Refines current scope (CONTEXT_REFINER)</li>
     *   <li>{@code *} - Informational, no validation (INFORMATIONAL)</li>
     *   <li>{@code !} - Error or unpaired callback</li>
     *   <li>{@code -} - Closes scope (CONTEXT_CLOSER, PAIRED_END)</li>
     * </ul>
     * 
     * <p>Example output:
     * <pre>
     * Nested Callback Sequence:
     * 
     * + gotDeclBegin[0] (token = LocatableToken(...))
     * &gt; gotMethodDecl[1]
     *   + beginMethodBody[2]
     *     * gotIdentifier[3]
     *   - endMethodBody[5]
     * - endMethodDecl[6]
     * </pre>
     * 
     * <p><strong>Complexity:</strong> O(n) where n is the number of callbacks,
     * as we traverse the list once building visualization data.
     * 
     * <p><strong>Thread Safety:</strong> Safe to call concurrently as it only
     * reads immutable state.
     * 
     * @return Formatted nested visualization showing callback hierarchy
     */
    public String getNestedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Nested Callback Sequence:\n\n");
        
        // Handle empty callbacks
        if (callbacks.isEmpty()) {
            sb.append("(no callbacks)\n");
            return sb.toString();
        }
        
        // Build lookup maps for quick access during traversal
        Map<Integer, CallbackRecord> pairedMap = new HashMap<>();
//        Map<Integer, String> errorMap = new HashMap<>();
        
//        // Populate error map
//        for (int i = 0; i < callbacks.size(); i++) {
//            String error = findErrorForCallback(i);
//            if (error != null) {
//                errorMap.put(i, error);
//            }
//        }
        
        // Track current nesting depth
        int currentDepth = 0;
        
        // Maximum depth safety check
        final int MAX_DEPTH = 100;
        
        // Traverse callbacks and build nested visualization
        for (int i = 0; i < callbacks.size(); i++) {
            CallbackRecord record = callbacks.get(i);
            String callbackName = record.getCallbackName();
            
            // Determine symbol for this callback
            String symbol = determineSymbol(record, pairedMap, this.errors);
            
            // Get error details if present
            String errorDetails = this.errors.get(i);
            
            // Adjust depth BEFORE printing for closers
            // This ensures the closing brace appears at the same level as the opening
            if (isCloser(callbackName)) {
                currentDepth = Math.max(0, currentDepth - 1);
            }
            
            // Safety check for excessive nesting
            if (currentDepth > MAX_DEPTH) {
                sb.append("  ".repeat(MAX_DEPTH));
                sb.append("! WARNING: Maximum nesting depth exceeded at callback ")
                  .append(i).append("\n");
                break;
            }
            
            // Format and append line
            String line = formatCallbackLine(currentDepth, symbol, record, errorDetails);
            sb.append(line).append("\n");
            
            // Adjust depth AFTER printing for openers
            // This increases depth for subsequent callbacks inside this scope
            if (isOpener(callbackName)) {
                currentDepth++;
            }
        }
        
        return sb.toString();
    }
    
    // ==================== Data Classes ====================
    
    /**
     * Internal validation state holder.
     */
    private static class ValidationState {
        final List<CallbackPairing> pairings;
        final Map<Integer, String> errors;
        final boolean balanced;
        
        ValidationState(List<CallbackPairing> pairings, Map<Integer, String> errors, boolean balanced) {
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
        private final CallbackRecord beginCallback;
        private final int beginIndex;
        private final CallbackRecord endCallback; // null if unmatched
        private final int endIndex; // -1 if unmatched
        private final boolean matched;
        private final String errorMessage; // null if no error
        
        private CallbackPairing(CallbackRecord beginCallback, int beginIndex, CallbackRecord endCallback,
                               int endIndex, boolean matched, String errorMessage) {
            this.beginCallback = beginCallback;
            this.beginIndex = beginIndex;
            this.endCallback = endCallback;
            this.endIndex = endIndex;
            this.matched = matched;
            this.errorMessage = errorMessage;
        }
        
        public CallbackRecord getBeginCallback() { return beginCallback; }
        public int getBeginIndex() { return beginIndex; }
        public CallbackRecord getEndCallback() { return endCallback; }
        public int getEndIndex() { return endIndex; }
        public boolean isMatched() { return matched; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            if (matched) {
                return String.format("%s[%d] ← → %s[%d]", beginCallback.getCallbackName(), beginIndex, endCallback.getCallbackName(), endIndex);
            } else if (endCallback == null) {
                return String.format("%s[%d] (unmatched)", beginCallback.getCallbackName(), beginIndex);
            } else {
                return String.format("%s[%d] ← X → %s[%d]: mismatch", beginCallback.getCallbackName(), beginIndex, endCallback.getCallbackName(), endIndex);
            }
        }
    }
}