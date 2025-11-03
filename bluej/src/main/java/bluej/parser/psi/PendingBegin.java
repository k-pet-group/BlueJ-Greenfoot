package bluej.parser.psi;

import bluej.parser.lexer.LocatableToken;
import java.util.Objects;

/**
 * Record representing a pending begin callback awaiting its matching end.
 * 
 * <p>Used for simple paired blocks like:
 * <ul>
 *   <li>beginMethodBody ↔ endMethodBody</li>
 *   <li>beginForLoop ↔ endForLoop</li>
 *   <li>beginTypeBody ↔ endTypeBody</li>
 * </ul>
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Push onto stack when begin* callback encountered</li>
 *   <li>Pop from stack when matching end* callback found</li>
 *   <li>Error if stack empty or types don't match</li>
 * </ol>
 * 
 * <h2>Immutability</h2>
 * <p>Records are immutable by default, preventing accidental modification
 * of validation state. This ensures thread-safety and predictable behavior.
 * 
 * @param callbackType the begin* callback type (e.g., "beginMethodBody")
 * @param beginToken the token for error location reporting
 * @param index the position in the callback sequence (0-based)
 * 
 * @see StackEntry
 */
public record PendingBegin(
    String callbackType,
    LocatableToken beginToken,
    int index
) implements StackEntry {
    
    /**
     * Compact constructor with validation.
     * 
     * <p>Ensures:
     * <ul>
     *   <li>callbackType is not null or empty</li>
     *   <li>beginToken is not null</li>
     *   <li>index is non-negative</li>
     * </ul>
     * 
     * @throws NullPointerException if callbackType or beginToken is null
     * @throws IllegalArgumentException if validation fails
     */
    public PendingBegin {
        Objects.requireNonNull(callbackType, "callbackType must not be null");
        Objects.requireNonNull(beginToken, "beginToken must not be null");
        
        if (callbackType.isEmpty()) {
            throw new IllegalArgumentException("callbackType must not be empty");
        }
        
        if (index < 0) {
            throw new IllegalArgumentException("index must be non-negative");
        }
    }
    
    /**
     * Returns the expected end callback type for this begin.
     * 
     * @return the expected end type (e.g., "beginMethodBody" → "endMethodBody")
     */
    public String expectedEndType() {
        return callbackType.replace("begin", "end");
    }
}