package bluej.parser.psi;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defines a state transition rule for context-based validation.
 * 
 * <p>State transition rules define how a context initiator can be refined by
 * specific refiners and then closed by corresponding closers. This enables
 * validation of complex callback patterns that go beyond simple begin/end pairs.
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>CONTEXT_INITIATOR creates ValidationContext (INITIATED)</li>
 *   <li>One of validRefiners refines context (REFINED)</li>
 *   <li>Corresponding closer closes context (CLOSED)</li>
 * </ol>
 * 
 * <h2>Example: Method Declaration Rule</h2>
 * <pre>{@code
 * StateTransitionRule declRule = new StateTransitionRule(
 *     "gotDeclBegin",
 *     List.of("gotMethodDeclaration", "gotTypeDef"),
 *     Map.of(
 *         "gotMethodDeclaration", "endMethodDecl",
 *         "gotTypeDef", "gotTypeDefEnd"
 *     )
 * );
 * 
 * // Valid sequences:
 * // gotDeclBegin → gotMethodDeclaration → endMethodDecl
 * // gotDeclBegin → gotTypeDef → gotTypeDefEnd
 * }</pre>
 * 
 * <h2>Validation</h2>
 * <p>The constructor validates that every refiner has a corresponding closer
 * in the refinerToCloser map. This prevents configuration errors at initialization
 * time rather than at runtime.
 * 
 * <h2>Immutability</h2>
 * <p>All collections are defensively copied to ensure immutability. This class
 * is thread-safe for read operations.
 * 
 * @see CallbackRole
 * @see ValidationContext
 * @see PairingValidator
 */
public class StateTransitionRule {
    
    /**
     * The callback that initiates this context (e.g., "gotDeclBegin").
     */
    public final String contextInitiator;
    
    /**
     * List of valid refiners for this context (e.g., ["gotMethodDeclaration", "gotTypeDef"]).
     * Immutable copy.
     */
    public final List<String> validRefiners;
    
    /**
     * Map from refiner to its corresponding closer (e.g., "gotMethodDeclaration" → "endMethodDecl").
     * Immutable copy.
     */
    public final Map<String, String> refinerToCloser;
    
    /**
     * Constructs a state transition rule with validation.
     * 
     * @param contextInitiator the callback that starts this context (not null)
     * @param validRefiners list of callbacks that can refine this context (not null)
     * @param refinerToCloser map from refiner to closer callback (not null)
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if any refiner lacks a corresponding closer
     */
    public StateTransitionRule(
        String contextInitiator,
        List<String> validRefiners,
        Map<String, String> refinerToCloser
    ) {
        this.contextInitiator = Objects.requireNonNull(contextInitiator, "contextInitiator must not be null");
        this.validRefiners = List.copyOf(Objects.requireNonNull(validRefiners, "validRefiners must not be null"));
        this.refinerToCloser = Map.copyOf(Objects.requireNonNull(refinerToCloser, "refinerToCloser must not be null"));
        
        // Validation: all refiners must have closers
        for (String refiner : this.validRefiners) {
            if (!this.refinerToCloser.containsKey(refiner)) {
                throw new IllegalArgumentException(
                    "Refiner '" + refiner + "' has no corresponding closer in refinerToCloser map"
                );
            }
        }
    }
    
    /**
     * Returns the closer callback for the given refiner.
     * 
     * @param refiner the refiner callback name
     * @return the corresponding closer callback, or null if refiner not found
     */
    public String getCloserFor(String refiner) {
        return refinerToCloser.get(refiner);
    }
    
    /**
     * Checks if the given callback is a valid refiner for this rule.
     * 
     * @param callback the callback to check
     * @return true if callback is in validRefiners, false otherwise
     */
    public boolean isValidRefiner(String callback) {
        return validRefiners.contains(callback);
    }
    
    @Override
    public String toString() {
        return String.format("StateTransitionRule[initiator=%s, refiners=%s]", 
            contextInitiator, validRefiners);
    }
}