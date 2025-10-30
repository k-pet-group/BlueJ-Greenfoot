/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.psi;

import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * State management infrastructure for PSI visitor traversal.
 * 
 * <p>This class provides essential state tracking for the {@link PsiCallbackVisitor}
 * during its traversal of the Kotlin PSI tree. It maintains context information that
 * allows the visitor to understand:</p>
 * <ul>
 *   <li>The current scope hierarchy (file → class → nested class → method)</li>
 *   <li>Accumulated modifiers for the declaration being processed</li>
 *   <li>Nesting depth for proper context management</li>
 *   <li>Type of containing elements (class, object, etc.)</li>
 * </ul>
 * 
 * <h2>Core Responsibilities</h2>
 * 
 * <h3>1. Scope Stack Management</h3>
 * <p>The visitor maintains a stack of PSI elements representing the current traversal
 * context. This enables answering questions like "What class am I currently inside?"
 * or "How deeply nested is this declaration?"</p>
 * 
 * <h3>2. Modifier Tracking</h3>
 * <p>As the visitor encounters modifier lists (public, private, static, etc.), it
 * accumulates them for the current declaration. When the declaration is complete,
 * the modifiers are cleared for the next declaration.</p>
 * 
 * <h3>3. Nesting Level Tracking</h3>
 * <p>The stack size directly corresponds to nesting depth, where 0 = top level,
 * 1 = inside first class, 2 = inside nested class, etc.</p>
 * 
 * <h3>4. Context Queries</h3>
 * <p>Provides methods to query the current context without exposing internal
 * implementation details. For example, checking if currently inside a class or
 * retrieving the containing class element.</p>
 * 
 * <h2>Critical Usage Pattern</h2>
 * <p><b>IMPORTANT:</b> The push/pop operations MUST be used in a try-finally pattern
 * to ensure proper cleanup even if exceptions occur during traversal:</p>
 * 
 * <pre>{@code
 * // In PsiCallbackVisitor.visitClass():
 * @Override
 * public void visitClass(KtClass ktClass) {
 *     state.pushScope(ktClass);
 *     try {
 *         // Process class: extract modifiers, supertypes, members
 *         // Visit child elements
 *         super.visitClass(ktClass);
 *     } finally {
 *         state.popScope();  // ALWAYS called, even on exception
 *     }
 * }
 * }</pre>
 * 
 * <p>Failing to use try-finally will result in stack imbalance, causing incorrect
 * context information for subsequent declarations.</p>
 * 
 * <h2>Phase 2 Usage</h2>
 * <p>In Phase 2 (Foundation), this class supports traversal validation by:</p>
 * <ul>
 *   <li>Tracking scope entry/exit for logging purposes</li>
 *   <li>Validating stack balance in tests (empty after complete traversal)</li>
 *   <li>Providing nesting depth for traversal verification</li>
 *   <li>Accumulating modifiers for future callback parameter creation</li>
 * </ul>
 * 
 * <h2>Phase 3 Migration</h2>
 * <p>In Phase 3 (Callback Integration), this class will additionally:</p>
 * <ul>
 *   <li>Provide scope context to callback methods</li>
 *   <li>Supply modifier sets for gotMethodDeclaration(), gotFieldDeclaration(), etc.</li>
 *   <li>Enable proper nesting in callback invocations</li>
 *   <li>Support pairing validation (matching begin/end calls)</li>
 * </ul>
 * 
 * <h2>Implementation Details</h2>
 * 
 * <h3>Scope Stack</h3>
 * <p>Uses {@link Stack} to maintain LIFO ordering of scopes. Each element pushed
 * represents entering a new scope (class, object, function), and each pop represents
 * exiting that scope.</p>
 * 
 * <h3>Modifier Set</h3>
 * <p>Uses {@link HashSet} to accumulate unique modifiers. Modifiers are added as
 * strings (e.g., "public", "static", "final") and can be queried individually or
 * as a complete set.</p>
 * 
 * <h3>Performance Characteristics</h3>
 * <ul>
 *   <li>pushScope(): O(1) - Stack push operation</li>
 *   <li>popScope(): O(1) - Stack pop operation</li>
 *   <li>getCurrentScope(): O(1) - Stack peek operation</li>
 *   <li>addModifier(): O(1) - HashSet add operation</li>
 *   <li>hasModifier(): O(1) - HashSet lookup operation</li>
 *   <li>isInsideClass(): O(n) - Linear search through stack (n = stack depth)</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p><b>NOT THREAD-SAFE.</b> This class is designed for single-threaded use within
 * a single visitor instance. Each parsing operation should create a new visitor
 * with its own VisitorState instance. Do not share VisitorState across threads or
 * parsing operations.</p>
 * 
 * <h2>Validation Support</h2>
 * <p>For testing and debugging, this class provides methods to validate proper
 * usage:</p>
 * <ul>
 *   <li>{@link #isStackBalanced()} - Verify all pushed scopes were popped</li>
 *   <li>{@link #getStackSize()} - Check current nesting depth</li>
 * </ul>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create visitor state
 * VisitorState state = new VisitorState();
 * 
 * // Enter class scope
 * state.pushScope(classElement);
 * try {
 *     // Add modifiers encountered
 *     state.addModifier("public");
 *     state.addModifier("final");
 *     
 *     // Check context
 *     if (state.isInsideClass()) {
 *         System.out.println("Currently inside a class");
 *         System.out.println("Nesting depth: " + state.getNestingDepth());
 *     }
 *     
 *     // Get modifiers for callback
 *     Set<String> modifiers = state.getModifiers();
 *     
 *     // Process class members...
 *     
 *     // Clear modifiers for next declaration
 *     state.clearModifiers();
 * } finally {
 *     state.popScope();
 * }
 * 
 * // Verify balanced after traversal
 * assert state.isStackBalanced() : "Scope stack not balanced!";
 * }</pre>
 * 
 * @see PsiCallbackVisitor Main visitor that uses this state management
 * @see <a href="file:///docs/planning/visitor-foundation/implementation-strategy.md">Implementation Strategy</a>
 */
@OnThread(Tag.Any)
public class VisitorState {
    
    /**
     * Maximum allowed nesting depth to prevent stack overflow.
     * Protects against pathological or malicious deeply nested structures.
     */
    private static final int MAX_NESTING_DEPTH = 100;
    
    /**
     * Stack of PSI elements representing the current scope hierarchy.
     *
     * <p>Elements are pushed when entering a scope (class, object, etc.) and
     * popped when exiting. The top of the stack represents the most immediate
     * enclosing scope.</p>
     *
     * <p><b>Invariant:</b> Stack must be balanced (empty) after complete traversal.</p>
     */
    private final Stack<PsiElement> scopeStack = new Stack<>();
    
    /**
     * Set of modifiers accumulated for the current declaration.
     * 
     * <p>Modifiers are added as the visitor encounters modifier lists and
     * cleared after processing each declaration. Examples: "public", "private",
     * "static", "final", "abstract", "override".</p>
     * 
     * <p><b>Usage Pattern:</b> Add modifiers → process declaration → clear modifiers</p>
     */
    private final Set<String> currentModifiers = new HashSet<>();
    
    /**
     * Creates a new visitor state with empty scope stack and modifier set.
     * 
     * <p>Each visitor instance should have its own state instance. Do not share
     * state across multiple parsing operations.</p>
     */
    public VisitorState() {
        // Intentionally empty - fields initialized with defaults
    }
    
    // ==================== Scope Stack Management ====================
    
    /**
     * Pushes a new scope onto the stack, entering a nested context.
     * 
     * <p>This method MUST be followed by {@link #popScope()} in a finally block
     * to ensure proper cleanup. Failing to balance push/pop operations will
     * result in incorrect context information.</p>
     * 
     * <p><b>Usage Pattern:</b></p>
     * <pre>{@code
     * state.pushScope(element);
     * try {
     *     // Process element and children
     * } finally {
     *     state.popScope();
     * }
     * }</pre>
     * 
     * <p><b>Phase 2:</b> Used for traversal validation and nesting tracking.</p>
     * <p><b>Phase 3:</b> Will provide context for callback invocation.</p>
     * 
     * @param element The PSI element representing the new scope (class, object, etc.)
     * @throws NullPointerException if element is null
     */
    public void pushScope(PsiElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Cannot push null scope");
        }
        
        if (scopeStack.size() >= MAX_NESTING_DEPTH) {
            throw new IllegalStateException(
                "Maximum nesting depth exceeded: " + MAX_NESTING_DEPTH +
                ". This may indicate malformed or pathological code.");
        }
        
        scopeStack.push(element);
    }
    
    /**
     * Pops the current scope from the stack, exiting the nested context.
     * 
     * <p>This method MUST be called in a finally block after {@link #pushScope(PsiElement)}
     * to ensure cleanup occurs even if exceptions are thrown during traversal.</p>
     * 
     * <p><b>Safety:</b> Silently handles empty stack (no-op) to avoid exceptions
     * during error recovery. However, in correct usage, the stack should never
     * be empty when popScope() is called.</p>
     * 
     * @see #pushScope(PsiElement) for required usage pattern
     */
    public void popScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }
    
    /**
     * Returns the current scope element without removing it from the stack.
     * 
     * <p>This represents the most immediate enclosing scope. For example, if
     * currently processing a method inside a class, this returns the class element.</p>
     * 
     * @return The current scope element, or null if at top level (no enclosing scope)
     */
    public PsiElement getCurrentScope() {
        return scopeStack.isEmpty() ? null : scopeStack.peek();
    }
    
    /**
     * Returns the current nesting depth.
     * 
     * <p>Nesting depth corresponds to stack size:</p>
     * <ul>
     *   <li>0 = Top level (file scope)</li>
     *   <li>1 = Inside first-level declaration (class, object)</li>
     *   <li>2 = Inside nested declaration (nested class, member function)</li>
     *   <li>3+ = Deeper nesting levels</li>
     * </ul>
     * 
     * <p><b>Usage:</b> Can be used for indentation in logging or for validation
     * of expected nesting patterns.</p>
     * 
     * @return The current nesting depth (0 = top level)
     */
    public int getNestingDepth() {
        return scopeStack.size();
    }
    
    // ==================== Modifier Tracking ====================
    
    /**
     * Adds a modifier to the current declaration context.
     * 
     * <p>Modifiers should be added as they are encountered during traversal.
     * Common modifiers include: "public", "private", "protected", "internal",
     * "static", "final", "abstract", "override", "open", "data", "sealed".</p>
     * 
     * <p><b>Usage Pattern:</b></p>
     * <pre>{@code
     * // When visiting modifier list
     * for (modifier in modifierList) {
     *     state.addModifier(modifier.getText());
     * }
     * 
     * // Process declaration with modifiers
     * Set<String> modifiers = state.getModifiers();
     * 
     * // Clear for next declaration
     * state.clearModifiers();
     * }</pre>
     * 
     * <p><b>Note:</b> Duplicate modifiers are automatically handled by Set semantics.</p>
     * 
     * @param modifier The modifier to add (e.g., "public", "static")
     * @throws NullPointerException if modifier is null
     */
    public void addModifier(String modifier) {
        if (modifier == null) {
            throw new NullPointerException("Cannot add null modifier");
        }
        currentModifiers.add(modifier);
    }
    
    /**
     * Clears all accumulated modifiers.
     * 
     * <p>This method MUST be called after processing each declaration to reset
     * modifier state for the next declaration. Failing to clear modifiers will
     * cause modifiers from previous declarations to leak into subsequent ones.</p>
     * 
     * <p><b>Usage Pattern:</b> Call after invoking callback or completing declaration
     * processing.</p>
     * 
     * @see #addModifier(String) for modifier accumulation pattern
     */
    public void clearModifiers() {
        currentModifiers.clear();
    }
    
    /**
     * Returns an immutable copy of current modifiers.
     * 
     * <p>The returned set is a defensive copy - modifications to it will not
     * affect the internal state. This prevents accidental mutation of modifier
     * state by callers.</p>
     * 
     * <p><b>Usage:</b> Retrieve modifiers when invoking callbacks or for
     * validation purposes.</p>
     * 
     * @return Unmodifiable set of current modifiers (empty if none accumulated)
     */
    public Set<String> getModifiers() {
        return Collections.unmodifiableSet(new HashSet<>(currentModifiers));
    }
    
    /**
     * Checks if the current declaration has a specific modifier.
     * 
     * <p>This is a convenience method for checking individual modifiers without
     * retrieving the entire set.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * if (state.hasModifier("static")) {
     *     // Process static member differently
     * }
     * }</pre>
     * 
     * @param modifier The modifier to check for (e.g., "public", "static")
     * @return true if the modifier is present, false otherwise
     */
    public boolean hasModifier(String modifier) {
        return currentModifiers.contains(modifier);
    }
    
    // ==================== Context Queries ====================
    
    /**
     * Checks if currently inside a class declaration.
     * 
     * <p>This searches the scope stack for any {@link KtClass} element,
     * regardless of nesting depth. Returns true if inside any class (regular,
     * data, enum, sealed, etc.).</p>
     * 
     * <p><b>Performance:</b> O(n) where n is nesting depth. Typically n is small
     * (1-3), so performance impact is negligible.</p>
     * 
     * <p><b>Phase 2:</b> Used for traversal validation and context checks.</p>
     * <p><b>Phase 3:</b> Will enable context-sensitive callback invocation.</p>
     * 
     * @return true if currently inside any class declaration, false otherwise
     */
    public boolean isInsideClass() {
        return scopeStack.stream()
            .anyMatch(e -> e instanceof KtClass);
    }
    
    /**
     * Checks if currently inside an object declaration.
     * 
     * <p>This searches the scope stack for any {@link KtObjectDeclaration} element.
     * Returns true if inside any object (singleton, companion, anonymous).</p>
     * 
     * <p><b>Performance:</b> O(n) where n is nesting depth.</p>
     * 
     * @return true if currently inside any object declaration, false otherwise
     */
    public boolean isInsideObject() {
        return scopeStack.stream()
            .anyMatch(e -> e instanceof KtObjectDeclaration);
    }
    
    /**
     * Returns the containing class element, if any.
     * 
     * <p>Searches the scope stack from most recent to oldest, returning the first
     * {@link KtClass} element found. This represents the most immediate enclosing
     * class.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Inside nested class:
     * class Outer {
     *     class Inner {  // <-- getContainingClass() returns Inner
     *         fun method() { ... }
     *     }
     * }
     * }</pre>
     * 
     * <p><b>Performance:</b> O(n) where n is nesting depth.</p>
     * 
     * @return The containing class element, or null if not inside any class
     */
    public KtClass getContainingClass() {
        // Iterate from top of stack (most recent) to bottom (oldest)
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            PsiElement element = scopeStack.get(i);
            if (element instanceof KtClass) {
                return (KtClass) element;
            }
        }
        return null;
    }
    
    // ==================== Validation Support ====================
    
    /**
     * Checks if the scope stack is balanced (empty).
     * 
     * <p>This is primarily used for testing and validation to ensure all
     * push/pop operations were properly balanced during traversal. After a
     * complete traversal, the stack should be empty.</p>
     * 
     * <p><b>Usage in Tests:</b></p>
     * <pre>{@code
     * ktFile.accept(visitor);
     * assertTrue(visitor.getState().isStackBalanced(),
     *     "Scope stack not balanced after traversal");
     * }</pre>
     * 
     * @return true if scope stack is empty (all scopes properly exited), false otherwise
     */
    public boolean isStackBalanced() {
        return scopeStack.isEmpty();
    }
    
    /**
     * Returns the current scope stack size.
     * 
     * <p>This is primarily for debugging and testing. In normal operation, use
     * {@link #getNestingDepth()} instead, which has a clearer semantic meaning.</p>
     * 
     * <p><b>Note:</b> Stack size equals nesting depth.</p>
     * 
     * @return The number of elements currently on the scope stack
     * @see #getNestingDepth() for semantic equivalent
     */
    public int getStackSize() {
        return scopeStack.size();
    }
}