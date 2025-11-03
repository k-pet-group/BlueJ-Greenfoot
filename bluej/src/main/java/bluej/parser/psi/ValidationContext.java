package bluej.parser.psi;

import java.util.Objects;

/**
 * Record representing an active validation context for state-based callbacks.
 * 
 * <p>Used for state transition patterns like:
 * <ul>
 *   <li>gotDeclBegin → gotMethodDeclaration → endMethodDecl</li>
 *   <li>gotDeclBegin → gotTypeDef → gotTypeDefEnd</li>
 * </ul>
 * 
 * <h2>State Machine</h2>
 * <pre>
 * INITIATED (created by context initiator)
 *     ↓
 * REFINED (refiner applied)
 *     ↓
 * CLOSED (closer applied, then popped from stack)
 * </pre>
 * 
 * <h2>Immutability Pattern</h2>
 * <p>Since records are immutable, state transitions create new instances:
 * <pre>{@code
 * ValidationContext initiated = new ValidationContext(
 *     "gotDeclBegin", 5, null, -1, ContextState.INITIATED
 * );
 * 
 * // Refining creates new instance:
 * ValidationContext refined = initiated.refine("gotMethodDeclaration", 8);
 * }</pre>
 * 
 * @param initiator the context initiator callback (e.g., "gotDeclBegin")
 * @param initiatorIndex position of initiator in callback sequence
 * @param refiner the refiner callback, or null if not yet refined
 * @param refinerIndex position of refiner, or -1 if not refined
 * @param state current state (INITIATED, REFINED, or CLOSED)
 * 
 * @see StackEntry
 * @see ContextState
 */
public record ValidationContext(
    String initiator,
    int initiatorIndex,
    String refiner,
    int refinerIndex,
    ContextState state
) implements StackEntry {
    
    /**
     * Compact constructor with validation.
     */
    public ValidationContext {
        Objects.requireNonNull(initiator, "initiator must not be null");
        Objects.requireNonNull(state, "state must not be null");
        
        if (initiator.isEmpty()) {
            throw new IllegalArgumentException("initiator must not be empty");
        }
        
        if (initiatorIndex < 0) {
            throw new IllegalArgumentException("initiatorIndex must be non-negative");
        }
        
        // State invariants
        if (state == ContextState.REFINED || state == ContextState.CLOSED) {
            Objects.requireNonNull(refiner, "refiner required for REFINED/CLOSED state");
            if (refinerIndex < 0) {
                throw new IllegalArgumentException(
                    "refinerIndex must be non-negative for REFINED/CLOSED state"
                );
            }
        }
        
        if (state == ContextState.INITIATED && refiner != null) {
            throw new IllegalArgumentException("refiner must be null for INITIATED state");
        }
    }
    
    @Override
    public String callbackType() {
        return initiator;
    }
    
    @Override
    public int index() {
        return initiatorIndex;
    }
    
    /**
     * Creates new ValidationContext with this context refined.
     * 
     * @param refinerType the refiner callback type
     * @param refinerIdx the refiner position
     * @return new ValidationContext in REFINED state
     * @throws IllegalStateException if not in INITIATED state
     */
    public ValidationContext refine(String refinerType, int refinerIdx) {
        if (state != ContextState.INITIATED) {
            throw new IllegalStateException("Cannot refine context in state: " + state);
        }
        
        return new ValidationContext(
            initiator, initiatorIndex, refinerType, refinerIdx, ContextState.REFINED
        );
    }
    
    /**
     * Creates new ValidationContext with this context closed.
     * 
     * @return new ValidationContext in CLOSED state
     * @throws IllegalStateException if not in REFINED state
     */
    public ValidationContext close() {
        if (state != ContextState.REFINED) {
            throw new IllegalStateException("Cannot close context in state: " + state);
        }
        
        return new ValidationContext(
            initiator, initiatorIndex, refiner, refinerIndex, ContextState.CLOSED
        );
    }
}