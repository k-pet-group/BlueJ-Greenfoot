package bluej.parser.psi;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Validates that begin/end callback pairs are properly matched during PSI traversal.
 *
 * <p>This validator performs <b>deferred validation</b> - it analyzes the complete
 * callback sequence at construction time, providing enhanced error messages with full
 * context including:
 * <ul>
 *   <li>Position indices of both begin and end callbacks</li>
 *   <li>Which begin callback each end attempted to match</li>
 *   <li>Context about callbacks in between pairs</li>
 *   <li>Clear descriptions of what went wrong</li>
 * </ul>
 * 
 * <h3>Architecture</h3>
 * <p>The validator accepts the complete callback sequence via constructor and immediately
 * performs validation. Results are cached and accessed via query methods. This approach
 * provides better error messages compared to incremental validation.</p>
 * 
 * <h3>Usage Pattern</h3>
 * <pre>{@code
 * // After collecting all callbacks
 * PairingValidator validator = new PairingValidator(callbackRecords);
 * 
 * // Query validation results
 * if (!validator.isBalanced()) {
 *     System.err.println(validator.getDetailedSummary());
 * }
 * 
 * // Access pairing information
 * for (CallbackPairing pairing : validator.getPairings()) {
 *     System.out.println(pairing);
 * }
 * }</pre>
 * 
 * <h3>Enhanced Error Messages</h3>
 * <p>Example error output:</p>
 * <pre>
 * Callback pairing mismatch at position 15:
 *   endClass at index 15 attempted to match beginMethod at index 10
 *   Expected: endMethod (to properly close beginMethod)
 *   Got: endClass (4 callbacks between begin and end)
 * </pre>
 * 
 * <h3>Thread Safety</h3>
 * <p><strong>Immutable after construction.</strong> All validation happens in the constructor,
 * making this class thread-safe for read operations.</p>
 * 
 * @see PsiCallbackVisitor
 * @see CallbackRecord
 */
public class PairingValidator {
    
    /**
     * List of callback pairings discovered during validation.
     * Immutable after construction.
     */
    private final List<CallbackPairing> pairings;
    
    /**
     * List of validation errors with enhanced context.
     * Immutable after construction.
     */
    private final List<String> errors;
    
    /**
     * Whether the callback sequence is balanced (all begin/end pairs matched).
     * Immutable after construction.
     */
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
        ValidationState state = validateSequence(callbacks);
        this.pairings = state.pairings;
        this.errors = state.errors;
        this.balanced = state.balanced;
    }
    
    /**
     * Helper class to track pending begin callbacks with their position.
     */
    private static class PendingBegin {
        final String callback;
        final int index;
        PendingBegin(String callback, int index) {
            this.callback = callback;
            this.index = index;
        }
    }
    
    /**
     * Performs validation on the complete callback sequence.
     *
     * <p>This private method analyzes the complete sequence and builds pairing
     * relationships with enhanced error context.</p>
     *
     * @param callbacks The complete sequence of callback records
     * @return Validation state with pairings and errors
     */
    private ValidationState validateSequence(List<CallbackRecord> callbacks) {
        List<CallbackPairing> pairings = new ArrayList<>();
        List<String> sequenceErrors = new ArrayList<>();
        Stack<PendingBegin> beginStack = new Stack<>();
        
        // First pass: validate and build pairings
        for (int i = 0; i < callbacks.size(); i++) {
            CallbackRecord record = callbacks.get(i);
            String callbackName = record.getCallbackName();
            
            // Validate callback name
            if (callbackName == null || callbackName.isEmpty()) {
                sequenceErrors.add(String.format(
                    "Invalid callback at position %d: null or empty callback name", i));
                continue;
            }
            
            if (callbackName.startsWith("begin")) {
                // Push begin callback onto stack
                beginStack.push(new PendingBegin(callbackName, i));
                
            } else if (callbackName.startsWith("end")) {
                // Validate end callback
                if (beginStack.isEmpty()) {
                    sequenceErrors.add(String.format(
                        "Unpaired end callback at position %d:\n" +
                        "  Callback: %s\n" +
                        "  Error: No matching begin callback found (stack is empty)",
                        i, callbackName));
                    continue;
                }
                
                // Pop the most recent begin
                PendingBegin pendingBegin = beginStack.pop();
                String expectedEnd = pendingBegin.callback.replace("begin", "end");
                
                if (expectedEnd.equals(callbackName)) {
                    // Successful match
                    pairings.add(new CallbackPairing(
                        pendingBegin.callback, pendingBegin.index,
                        callbackName, i, true, null));
                } else {
                    // Mismatch - record error and put begin back on stack
                    int callbacksBetween = i - pendingBegin.index - 1;
                    String contextInfo = callbacksBetween > 0
                        ? String.format(" (%d callback%s between begin and end)",
                                       callbacksBetween, callbacksBetween == 1 ? "" : "s")
                        : "";
                    
                    String detailedError = String.format(
                        "Callback pairing mismatch at position %d:\n" +
                        "  %s at index %d attempted to match %s at index %d\n" +
                        "  Expected: %s (to properly close %s)\n" +
                        "  Got: %s%s",
                        i, callbackName, i, pendingBegin.callback, pendingBegin.index,
                        expectedEnd, pendingBegin.callback, callbackName, contextInfo);
                    
                    sequenceErrors.add(detailedError);
                    
                    // Put the begin back on stack - it remains unmatched
                    // Don't create a pairing for mismatches
                    beginStack.push(pendingBegin);
                }
            }
            // Ignore non-begin/end callbacks
        }
        
        // Second pass: report unmatched begins
        while (!beginStack.isEmpty()) {
            PendingBegin unmatched = beginStack.pop();
            String expectedEnd = unmatched.callback.replace("begin", "end");
            
            String detailedError = String.format(
                "Unmatched begin callback:\n" +
                "  %s at index %d was never closed\n" +
                "  Expected: %s should appear after this callback\n" +
                "  Context: This begin callback remains open at end of sequence",
                unmatched.callback, unmatched.index, expectedEnd);
            
            sequenceErrors.add(detailedError);
            pairings.add(new CallbackPairing(
                unmatched.callback, unmatched.index,
                null, -1, false, detailedError));
        }
        
        boolean balanced = sequenceErrors.isEmpty();
        return new ValidationState(pairings, sequenceErrors, balanced);
    }
    
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