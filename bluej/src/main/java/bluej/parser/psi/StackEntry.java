package bluej.parser.psi;

import java.util.Objects;

/**
 * Sealed interface representing entries on the validation stack.
 * 
 * <p>This interface defines the complete set of types that can appear on the
 * validation context stack. The sealed nature provides compile-time exhaustiveness
 * checking in switch expressions and prevents unauthorized implementations.
 * 
 * <h2>Permitted Types</h2>
 * <ul>
 *   <li>{@link PendingBegin} - Awaiting matching end callback (simple pairs)</li>
 *   <li>{@link ValidationContext} - Active state transition context</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * StackEntry entry = contextStack.peek();
 * switch (entry) {
 *     case PendingBegin pending -> handlePendingBegin(pending);
 *     case ValidationContext ctx -> handleValidationContext(ctx);
 *     // Compiler enforces exhaustiveness - no default needed
 * }
 * }</pre>
 * 
 * @since Java 17 (sealed interfaces)
 * @see PendingBegin
 * @see ValidationContext
 */
public sealed interface StackEntry 
    permits PendingBegin, ValidationContext {
    
    /**
     * Returns the callback type associated with this stack entry.
     * 
     * @return the callback type string (e.g., "beginMethodBody", "gotDeclBegin")
     */
    String callbackType();
    
    /**
     * Returns the position index of this entry in the callback sequence.
     * 
     * @return zero-based index in the callback array
     */
    int index();
}