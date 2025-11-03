package bluej.parser.psi;

/**
 * Defines the role a callback plays in validation.
 * 
 * <p>Two validation strategies:
 * <ol>
 *   <li><b>Simple paired blocks</b>: PAIRED_BEGIN ↔ PAIRED_END</li>
 *   <li><b>State transitions</b>: CONTEXT_INITIATOR → CONTEXT_REFINER → CONTEXT_CLOSER</li>
 * </ol>
 * 
 * <h2>Role Categories</h2>
 * 
 * <h3>State-Based Validation</h3>
 * <ul>
 *   <li><b>CONTEXT_INITIATOR</b>: Creates new validation context (e.g., gotDeclBegin)</li>
 *   <li><b>CONTEXT_REFINER</b>: Refines existing context (e.g., gotMethodDeclaration)</li>
 *   <li><b>CONTEXT_CLOSER</b>: Closes refined context (e.g., endMethodDecl)</li>
 * </ul>
 * 
 * <h3>Name-Based Validation</h3>
 * <ul>
 *   <li><b>PAIRED_BEGIN</b>: Simple begin* callback (e.g., beginMethodBody)</li>
 *   <li><b>PAIRED_END</b>: Simple end* callback (e.g., endMethodBody)</li>
 * </ul>
 * 
 * <h3>No Validation</h3>
 * <ul>
 *   <li><b>INFORMATIONAL</b>: Callbacks that don't require pairing validation</li>
 * </ul>
 * 
 * <h2>Examples</h2>
 * <pre>
 * State transition pattern:
 *   gotDeclBegin (CONTEXT_INITIATOR)
 *   → gotMethodDeclaration (CONTEXT_REFINER)
 *   → beginMethodBody (PAIRED_BEGIN)
 *   → endMethodBody (PAIRED_END)
 *   → endMethodDecl (CONTEXT_CLOSER)
 * 
 * Simple pair pattern:
 *   beginForLoop (PAIRED_BEGIN)
 *   → endForLoop (PAIRED_END)
 * 
 * Informational:
 *   gotModifier (INFORMATIONAL)
 *   gotIdentifier (INFORMATIONAL)
 * </pre>
 * 
 * @see PairingValidator
 * @see StackEntry
 * @see StateTransitionRule
 */
public enum CallbackRole {
    /** Starts new context (e.g., gotDeclBegin) */
    CONTEXT_INITIATOR,
    
    /** Refines existing context (e.g., gotMethodDeclaration) */
    CONTEXT_REFINER,
    
    /** Closes refined context (e.g., endMethodDecl) */
    CONTEXT_CLOSER,
    
    /** Simple begin* callback (e.g., beginMethodBody) */
    PAIRED_BEGIN,
    
    /** Simple end* callback (e.g., endMethodBody) */
    PAIRED_END,
    
    /** No validation impact (e.g., gotModifier) */
    INFORMATIONAL
}