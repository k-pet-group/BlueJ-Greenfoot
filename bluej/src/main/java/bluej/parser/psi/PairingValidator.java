package bluej.parser.psi;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Validates that begin/end callback pairs are properly matched during PSI traversal.
 * 
 * <p>This validator uses a LIFO (Last-In-First-Out) stack to ensure that callback
 * pairs follow proper nesting rules: the most recent "begin" callback must be matched
 * by the corresponding "end" callback before any earlier "begin" callbacks can be closed.
 * 
 * <h3>Phase 2 Context</h3>
 * <p>This is infrastructure-only code created during Phase 2 (Milestone 2.2: Base Visitor Structure).
 * It will be integrated into {@link PsiCallbackVisitor} during Phase 3 when actual callback
 * invocation is implemented. For now, it provides standalone validation capabilities that can
 * be tested independently.
 * 
 * <h3>Usage Pattern</h3>
 * <p>The typical usage pattern during Phase 3 traversal will be:
 * <ol>
 *   <li>Call {@link #recordBegin(String)} when entering a structural element (class, method, etc.)</li>
 *   <li>Process the element's children</li>
 *   <li>Call {@link #recordEnd(String)} when exiting the element</li>
 *   <li>Check {@link #isBalanced()} and {@link #hasErrors()} after traversal completes</li>
 * </ol>
 * 
 * <h3>Thread Safety</h3>
 * <p><strong>NOT thread-safe.</strong> This validator is designed for single-threaded use during
 * a single PSI traversal. Each traversal should use its own validator instance, or call
 * {@link #reset()} between traversals.
 * 
 * <h3>Example Usage</h3>
 * <pre>
 * PairingValidator validator = new PairingValidator();
 * 
 * // Correct nesting:
 * validator.recordBegin("beginClass");
 *   validator.recordBegin("beginMethod");
 *   validator.recordEnd("endMethod");  // OK: matches beginMethod
 * validator.recordEnd("endClass");      // OK: matches beginClass
 * 
 * assert validator.isBalanced();  // true
 * assert !validator.hasErrors();  // true
 * 
 * // Incorrect nesting (mismatched):
 * validator.reset();
 * validator.recordBegin("beginClass");
 * validator.recordEnd("endMethod");  // ERROR: expected endClass
 * 
 * assert !validator.isBalanced();  // false
 * assert validator.hasErrors();    // true
 * 
 * // Incorrect nesting (unpaired end):
 * validator.reset();
 * validator.recordEnd("endClass");  // ERROR: no matching begin
 * 
 * assert validator.hasErrors();  // true
 * </pre>
 * 
 * @see PsiCallbackVisitor
 * @see VisitorState
 */
public class PairingValidator {
    
    /**
     * Stack tracking begin callbacks in LIFO order.
     * The top of the stack represents the most recent unmatched begin callback.
     */
    private final Stack<String> pairingStack = new Stack<>();
    
    /**
     * List of validation errors encountered during traversal.
     * Includes errors for null/empty callbacks, unpaired ends, and mismatched pairs.
     */
    private final List<String> errors = new ArrayList<>();
    
    /**
     * Records a begin callback for later matching with its corresponding end callback.
     * 
     * <p>The callback type should follow the naming convention "beginX" where X identifies
     * the structural element (e.g., "beginClass", "beginMethod", "beginField").
     * 
     * @param callbackType the type of begin callback being recorded (e.g., "beginClass")
     * @throws NullPointerException if callbackType is null (recorded as validation error)
     */
    public void recordBegin(String callbackType) {
        if (callbackType == null || callbackType.isEmpty()) {
            errors.add("Invalid begin callback: null or empty type");
            return;
        }
        pairingStack.push(callbackType);
    }
    
    /**
     * Records an end callback and validates that it matches the most recent begin callback.
     * 
     * <p>The callback type should follow the naming convention "endX" where X identifies
     * the structural element (e.g., "endClass", "endMethod", "endField"). The validator
     * converts "end" to "begin" to determine the expected matching begin callback.
     * 
     * <p>Validation fails if:
     * <ul>
     *   <li>The callback type is null or empty</li>
     *   <li>No begin callback has been recorded (unpaired end)</li>
     *   <li>The end callback doesn't match the most recent begin callback (mismatched pair)</li>
     * </ul>
     * 
     * @param callbackType the type of end callback being recorded (e.g., "endClass")
     * @return {@code true} if the pairing is valid, {@code false} if validation fails
     */
    public boolean recordEnd(String callbackType) {
        if (callbackType == null || callbackType.isEmpty()) {
            errors.add("Invalid end callback: null or empty type");
            return false;
        }
        
        if (pairingStack.isEmpty()) {
            errors.add("Unpaired end callback: " + callbackType + " (no matching begin)");
            return false;
        }
        
        String expectedBegin = pairingStack.pop();
        
        // Convert "endX" to "beginX" by replacing only the "end" prefix
        String actualBegin;
        if (callbackType.startsWith("end")) {
            actualBegin = "begin" + callbackType.substring(3);
        } else {
            errors.add("Invalid end callback: must start with 'end', got '" + callbackType + "'");
            return false;
        }
        
        if (!expectedBegin.equals(actualBegin)) {
            // Improved error message showing expected end callback
            String expectedEnd = expectedBegin.replace("begin", "end");
            errors.add("Mismatched callback pair: expected " + expectedEnd +
                       " (to match " + expectedBegin + "), got " + callbackType);
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if all begin/end callback pairs are properly matched.
     * 
     * <p>A balanced state means that every begin callback recorded has been matched
     * with its corresponding end callback. This should be {@code true} at the end
     * of a successful traversal.
     * 
     * @return {@code true} if the pairing stack is empty (all pairs matched),
     *         {@code false} if there are unmatched begin callbacks
     */
    public boolean isBalanced() {
        return pairingStack.isEmpty();
    }
    
    /**
     * Returns the count of unmatched begin callbacks.
     * 
     * <p>A non-zero count indicates that some begin callbacks were never matched
     * with their corresponding end callbacks, which typically indicates an incomplete
     * or aborted traversal.
     * 
     * @return the number of begin callbacks that have not been matched with end callbacks
     */
    public int getUnmatchedCount() {
        return pairingStack.size();
    }
    
    /**
     * Returns a list of all unmatched begin callbacks in the order they were recorded.
     * 
     * <p>The returned list is a defensive copy; modifications to it will not affect
     * the validator's internal state.
     * 
     * @return a new list containing the unmatched begin callback types
     */
    public List<String> getUnmatchedCallbacks() {
        return new ArrayList<>(pairingStack);
    }
    
    /**
     * Returns all validation errors encountered during the current traversal.
     * 
     * <p>Errors include:
     * <ul>
     *   <li>Null or empty callback types</li>
     *   <li>Unpaired end callbacks (no matching begin)</li>
     *   <li>Mismatched callback pairs (end doesn't match most recent begin)</li>
     * </ul>
     * 
     * <p>The returned list is a defensive copy; modifications to it will not affect
     * the validator's internal state.
     * 
     * @return a new list containing all validation error messages
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Checks if any validation errors occurred during the current traversal.
     * 
     * <p>This returns {@code true} if either:
     * <ul>
     *   <li>Any explicit validation errors were recorded (null callbacks, unpaired ends, mismatches)</li>
     *   <li>There are unmatched begin callbacks (incomplete traversal)</li>
     * </ul>
     * 
     * @return {@code true} if validation errors exist or callbacks are unmatched,
     *         {@code false} if no errors occurred and all pairs are matched
     */
    public boolean hasErrors() {
        return !errors.isEmpty() || !pairingStack.isEmpty();
    }
    
    /**
     * Resets the validator state for a new traversal.
     * 
     * <p>This clears both the pairing stack and the error list, allowing the same
     * validator instance to be reused for multiple traversals. This is more efficient
     * than creating new validator instances.
     * 
     * <h3>Usage Example</h3>
     * <pre>
     * PairingValidator validator = new PairingValidator();
     * 
     * // First traversal
     * validator.recordBegin("beginClass");
     * validator.recordEnd("endClass");
     * assert validator.isBalanced();
     * 
     * // Reset for second traversal
     * validator.reset();
     * 
     * // Second traversal
     * validator.recordBegin("beginMethod");
     * validator.recordEnd("endMethod");
     * assert validator.isBalanced();
     * </pre>
     */
    public void reset() {
        pairingStack.clear();
        errors.clear();
    }
    
    /**
     * Generates a validation summary for debugging purposes.
     * 
     * <p>The summary includes:
     * <ul>
     *   <li>Count of validation errors</li>
     *   <li>Count of unmatched begin callbacks</li>
     *   <li>Detailed list of all errors (if any)</li>
     *   <li>Detailed list of unmatched callbacks (if any)</li>
     * </ul>
     * 
     * <p>This is particularly useful for debugging traversal issues or understanding
     * why validation failed.
     * 
     * <h3>Example Output</h3>
     * <pre>
     * Pairing Validation Summary:
     *   Errors: 1
     *   Unmatched: 1
     * 
     * Errors:
     *   - Mismatched callback pair: expected end for beginClass, got endMethod
     * 
     * Unmatched callbacks:
     *   - beginClass
     * </pre>
     * 
     * @return a formatted string containing the validation summary
     */
    public String getValidationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pairing Validation Summary:\n");
        sb.append("  Errors: ").append(errors.size()).append("\n");
        sb.append("  Unmatched: ").append(pairingStack.size()).append("\n");
        
        if (!errors.isEmpty()) {
            sb.append("\nErrors:\n");
            for (String error : errors) {
                sb.append("  - ").append(error).append("\n");
            }
        }
        
        if (!pairingStack.isEmpty()) {
            sb.append("\nUnmatched callbacks:\n");
            for (String callback : pairingStack) {
                sb.append("  - ").append(callback).append("\n");
            }
        }
        
        return sb.toString();
    }
}