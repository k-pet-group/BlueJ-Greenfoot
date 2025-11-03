package bluej.parser.psi;

/**
 * States a ValidationContext can be in.
 * 
 * <p>Defines the lifecycle of a {@link ValidationContext} through its state transitions:
 * <pre>
 * INITIATED (created by context initiator)
 *     ↓
 * REFINED (refiner applied)
 *     ↓
 * CLOSED (closer applied, then popped from stack)
 * </pre>
 * 
 * <h2>State Transitions</h2>
 * <ul>
 *   <li><b>INITIATED</b> → <b>REFINED</b>: When a refiner callback is received</li>
 *   <li><b>REFINED</b> → <b>CLOSED</b>: When the corresponding closer callback is received</li>
 * </ul>
 * 
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li>Only INITIATED contexts can be refined</li>
 *   <li>Only REFINED contexts can be closed</li>
 *   <li>CLOSED is a transitional state - context is immediately popped from stack</li>
 * </ul>
 * 
 * @see ValidationContext
 */
public enum ContextState {
    /** Context created, waiting for refiner or closer */
    INITIATED,
    
    /** Refiner applied, waiting for specific closer */
    REFINED,
    
    /** Properly closed (transitional - immediately popped) */
    CLOSED
}