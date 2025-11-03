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

import bluej.parser.JavaParserCallbacks;
import bluej.parser.PsiCallbackVisitorAdapter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.LineColPos;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import org.jetbrains.annotations.NotNull;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI visitor that traverses Kotlin PSI tree structure for parser integration.
 *
 * <p><b>PHASE 2 IMPLEMENTATION - TRAVERSAL WITH STATE MANAGEMENT</b></p>
 *
 * <p>This visitor is part of Phase 2 (Foundation) of the PSI parser integration.
 * In Phase 2, the visitor focuses on correct PSI tree traversal, state management,
 * and logging. <b>Callback invocation is deferred to Phase 3.</b></p>
 *
 * <h2>Phase 2 Behavior</h2>
 * <p>The visitor logs each declaration it encounters to {@link #traversalLog} for
 * validation purposes and maintains scope context via {@link VisitorState}.
 * This allows verification that:</p>
 * <ul>
 *   <li>All declaration types are visited correctly</li>
 *   <li>Traversal order matches expected depth-first pattern</li>
 *   <li>Nested declarations are handled properly</li>
 *   <li>State management (push/pop) is balanced correctly</li>
 *   <li>State infrastructure is ready for Phase 3 callback integration</li>
 * </ul>
 * 
 * <h2>Visitor Pattern</h2>
 * <p>This class extends {@link KtVisitorVoid} from the Kotlin compiler, which provides
 * the visitor framework for traversing Kotlin PSI elements. The visitor uses a depth-first
 * traversal strategy:</p>
 * <pre>{@code
 * visitKtFile(file)
 *   → visitClass(class1)
 *       → visitNamedFunction(method1)
 *       → visitProperty(field1)
 *   → visitClass(class2)
 *       → visitClass(nestedClass)
 * }</pre>
 * 
 * <h2>Traversal Logging</h2>
 * <p>Each visit method logs an entry to {@link #traversalLog} in the format:</p>
 * <pre>{@code
 * "VISIT: <ElementType>: <ElementName>"
 * 
 * Examples:
 * "VISIT: CLASS: MyClass"
 * "VISIT: FUNCTION: calculateSum"
 * "VISIT: PROPERTY: count"
 * "VISIT: OBJECT: Singleton"
 * }</pre>
 * 
 * <p>This log serves as validation data for Phase 2 tests, ensuring complete
 * traversal before Phase 3 adds callback invocation complexity.</p>
 * 
 * <h2>State Management</h2>
 * <p>The visitor maintains a {@link VisitorState} instance that tracks:</p>
 * <ul>
 *   <li>Current scope context (file, class, nested class) via scope stack</li>
 *   <li>Accumulated modifiers for declarations (prepared for Phase 3)</li>
 *   <li>Nesting level for proper context tracking</li>
 * </ul>
 *
 * <p><b>CRITICAL:</b> Each visit method uses the push/pop pattern with try-finally
 * to ensure balanced state management even when exceptions occur:</p>
 * <pre>{@code
 * state.pushScope(element);
 * try {
 *     // Process element
 *     super.visitXxx(element);
 * } finally {
 *     state.popScope();  // ALWAYS cleanup
 * }
 * }</pre>
 *
 * <p>After traversal completes via {@link #visitKtFile(KtFile)}, the method
 * {@link #validateState()} MUST be called to verify state stack is balanced.</p>
 * 
 * <h2>Declaration Visit Methods</h2>
 * <p>The visitor implements visit methods for Kotlin declaration types:</p>
 * <ul>
 *   <li>{@link #visitClass(KtClass)} - Classes, interfaces, enums</li>
 *   <li>{@link #visitNamedFunction(KtNamedFunction)} - Functions and methods</li>
 *   <li>{@link #visitProperty(KtProperty)} - Properties (val/var)</li>
 *   <li>{@link #visitObjectDeclaration(KtObjectDeclaration)} - Object declarations and companions</li>
 * </ul>
 * 
 * <h2>Usage Example (Phase 2)</h2>
 * <pre>{@code
 * // Parse Kotlin file to PSI
 * KtFile ktFile = psiEnvironment.parseKotlinFile(sourceText);
 *
 * // Create visitor
 * PsiCallbackVisitor visitor = new PsiCallbackVisitor();
 *
 * // Traverse PSI tree
 * ktFile.accept(visitor);
 *
 * // Validate traversal
 * List<String> log = visitor.getTraversalLog();
 * assert log.contains("VISIT: CLASS: MyClass");
 * assert log.contains("VISIT: FUNCTION: myMethod");
 *
 * // Validate state management
 * assert visitor.validateState() : "State stack not balanced!";
 * }</pre>
 * 
 * <h2>Phase 3 Migration Path</h2>
 * <p>When transitioning to Phase 3 (Callback Integration), this visitor will be enhanced to:</p>
 * <ol>
 *   <li>Accept a {@code JavaParserCallbacks} instance in constructor</li>
 *   <li>Invoke appropriate callback methods (beginTypeDecl, gotMethodDeclaration, etc.)</li>
 *   <li>Use {@code TokenFactory} to create token parameters for callbacks</li>
 *   <li>Extract modifiers from declarations to populate state</li>
 *   <li>Call {@link #clearModifierState()} after each declaration processing</li>
 *   <li>Remove or reduce traversal logging (keep for debugging only)</li>
 * </ol>
 * 
 * <h2>Thread Safety</h2>
 * <p>This visitor is NOT thread-safe. Each parsing operation should create a new
 * visitor instance. The {@link #traversalLog} is mutable and not synchronized.</p>
 * 
 * <h2>Design Rationale</h2>
 * <p><b>Why separate Phase 2 (traversal) from Phase 3 (callbacks)?</b></p>
 * <ul>
 *   <li><b>Complexity Management:</b> Validate traversal logic independently before adding
 *       callback invocation complexity</li>
 *   <li><b>Incremental Validation:</b> Prove PSI structure understanding before integrating
 *       with existing parser infrastructure</li>
 *   <li><b>Risk Reduction:</b> Identify traversal issues early without callback interference</li>
 *   <li><b>Clear Milestones:</b> Measurable progress - Phase 2 complete when traversal is proven correct</li>
 * </ul>
 * 
 * @see KtVisitorVoid Base visitor class from Kotlin compiler
 * @see VisitorState State management for scope tracking
 * @see TokenFactory Token creation for callback parameters (Phase 3)
 * @see <a href="file:///docs/planning/visitor-foundation/implementation-strategy.md">Implementation Strategy</a>
 */
@OnThread(Tag.Any)
public class PsiCallbackVisitor extends KtVisitorVoid {
    
    /**
     * Traversal log for Phase 2 validation.
     * 
     * <p>Records each declaration visit in format: "VISIT: &lt;Type&gt;: &lt;Name&gt;"</p>
     * 
     * <p><b>Phase 2 Only:</b> This log is the primary output of Phase 2 visitor.
     * In Phase 3, this will be removed or made optional for debugging.</p>
     */
    private final List<String> traversalLog = new ArrayList<>();
    
    /**
     * Optional callback adapter for invoking parser callbacks.
     * Null in Phase 2 traversal-only mode, set for Phase 3 callback integration.
     * Uses adapter pattern to access protected JavaParserCallbacks methods.
     */
    private final PsiCallbackVisitorAdapter callbacks;
    
    /**
     * Visitor state for scope tracking and modifier management.
     *
     * <p>Tracks scope stack for context queries, accumulates modifiers for
     * declarations, and provides nesting depth information. Each visit method
     * uses push/pop pattern to maintain balanced state.</p>
     *
     * <p><b>Phase 2:</b> State tracking enabled for traversal validation.</p>
     * <p><b>Phase 3:</b> State will provide context for callback invocation.</p>
     *
     * @see VisitorState for detailed state management documentation
     */
    private final VisitorState state = new VisitorState();
    
    /**
     * Creates a new PSI callback visitor for Phase 2 traversal validation.
     *
     * <p><b>Phase 2:</b> No parameters needed - visitor only logs traversal.</p>
     *
     * <p><b>Phase 3:</b> Constructor will accept {@code JavaParserCallbacks} and
     * other dependencies for callback invocation.</p>
     */
    public PsiCallbackVisitor() {
        this.callbacks = null;
    }
    
    /**
     * Creates a new PSI callback visitor with callbacks for validation testing.
     *
     * <p><b>Phase 3:</b> This constructor enables callback integration by accepting
     * any {@link JavaParserCallbacks} instance wrapped in an adapter. The adapter
     * provides public methods to invoke protected callback methods from the
     * bluej.parser.psi package.</p>
     *
     * @param callbacks The JavaParserCallbacks instance (will be wrapped in adapter)
     */
    public PsiCallbackVisitor(JavaParserCallbacks callbacks) {
        this.callbacks = new PsiCallbackVisitorAdapter(callbacks);
    }
    
    /**
     * Returns the traversal log for Phase 2 validation.
     * 
     * <p>The log contains entries for each declaration visited, allowing tests to
     * verify traversal completeness and order.</p>
     * 
     * @return Unmodifiable view of traversal log entries
     */
    public List<String> getTraversalLog() {
        return List.copyOf(traversalLog);
    }
    
    /**
     * Entry point for PSI traversal - visits a Kotlin file.
     *
     * <p>This method is the starting point for traversing a Kotlin PSI tree.
     * It logs the file visit and then explicitly visits all declarations in the file.</p>
     *
     * <p><b>Critical:</b> Unlike some visitor patterns, Kotlin's {@link KtVisitorVoid}
     * does NOT automatically traverse children when calling super. We must explicitly
     * iterate over and visit each declaration.</p>
     *
     * <h3>Phase 2 Behavior</h3>
     * <p>Logs: "VISIT: FILE: &lt;fileName&gt;"</p>
     * <p>Then explicitly visits all top-level declarations (classes, functions, properties).</p>
     *
     * <h3>Phase 3 Migration</h3>
     * <p>Will invoke {@code callbacks.beginParsing()} before traversal and
     * {@code callbacks.endParsing()} after traversal.</p>
     *
     * @param file The Kotlin file PSI element to traverse
     */
    @Override
    public void visitKtFile(@NotNull KtFile file) {
        if (file == null) {
            return; // Gracefully handle null
        }
        
        // Phase 2: Log file visit
        String fileName = file.getName();
        traversalLog.add("VISIT: FILE: " + fileName);
        
        // Explicitly visit all declarations in the file
        // Note: Kotlin PSI visitor requires explicit iteration over children
        for (KtDeclaration declaration : file.getDeclarations()) {
            declaration.accept(this);
        }
    }
    
    /**
     * Visits a class declaration (class, interface, or enum).
     *
     * <p>This method is invoked for all class-like declarations in Kotlin including:</p>
     * <ul>
     *   <li>Regular classes ({@code class MyClass})</li>
     *   <li>Interfaces ({@code interface MyInterface})</li>
     *   <li>Enum classes ({@code enum class MyEnum})</li>
     *   <li>Data classes ({@code data class MyData})</li>
     *   <li>Sealed classes ({@code sealed class MySealed})</li>
     * </ul>
     *
     * <h3>Phase 3 Milestone 3.1 Task 1: Core Callback Sequence</h3>
     * <p>Implements the complete callback sequence for simple class declarations:</p>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin declaration</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed (empty for now)</li>
     *   <li>{@code gotTypeDef(token, tdType)} - Type definition</li>
     *   <li>{@code gotTypeDefName(nameToken)} - Type name</li>
     *   <li>Supertype processing (deferred to later task)</li>
     *   <li>{@code beginTypeBody(token)} - Begin body</li>
     *   <li>Visit members via {@code super.visitClass()}</li>
     *   <li>{@code endTypeBody(token, true)} - End body</li>
     *   <li>{@code gotTypeDefEnd(token, true)} - End declaration</li>
     * </ol>
     *
     * @param ktClass The class declaration PSI element
     */
    @Override
    public void visitClass(@NotNull KtClass ktClass) {
        if (ktClass == null) {
            return;
        }
        
        // Phase 2: Log class visit (retained for debugging)
        String className = ktClass.getName();
        traversalLog.add("VISIT: CLASS: " + (className != null ? className : "<anonymous>"));
        
        // State management with try-finally
        state.pushScope(ktClass);
        try {
            // Skip if no callbacks configured
            if (callbacks == null) {
                super.visitClass(ktClass);
                return;
            }

            // 1. Begin declaration
            int tdType = determineTypeDefType(ktClass);
            LocatableToken classToken = createToken(ktClass.getClassOrInterfaceKeyword(), tdType);

            callbacks.invokeDeclBegin(classToken);
            
            // 2. Process modifiers
            KtModifierList modifierList = ktClass.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }
            callbacks.invokeModifiersConsumed();
            
            // 3. Type definition
            callbacks.invokeTypeDef(classToken, tdType);
            
            // 4. Type name
            if (className != null && ktClass.getNameIdentifier() != null) {
                LocatableToken nameToken = createToken(ktClass.getNameIdentifier(), JavaTokenTypes.IDENT);
                callbacks.invokeTypeDefName(nameToken);
            }
            
            // 5. Process supertypes
            processSuperTypes(ktClass);
            
            // Phase 4.2: Process primary constructor BEFORE type body (class header element)
            KtPrimaryConstructor primaryConstructor = ktClass.getPrimaryConstructor();
            if (primaryConstructor != null) {
                visitPrimaryConstructor(primaryConstructor);
            }

            LocatableToken finalToken;
            
            // 6. Begin type body
            KtClassBody body = ktClass.getBody();
            if (body != null) {
                // Extract separate opening and closing brace elements
                PsiElement lBrace = body.getLBrace();
                PsiElement rBrace = body.getRBrace();
                
                if (lBrace != null && rBrace != null) {
                    // Create separate tokens for opening and closing braces
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    
                    callbacks.invokeBeginTypeBody(lBraceToken);
                    
                    // 7. Visit nested declarations explicitly
                    // Note: Kotlin PSI visitor requires explicit iteration over body declarations
                    // super.visitClass() does NOT automatically recurse into nested classes/objects/members
                    // CRITICAL: Filter to KtClass AND exclude KtEnumEntry (enum constants, not classes)
                    for (KtDeclaration declaration : body.getDeclarations()) {
                        if (declaration instanceof KtClass && !(declaration instanceof KtEnumEntry)) {
                            declaration.accept(this);  // Triggers visitClass() for nested classes
                        } else if (declaration instanceof KtObjectDeclaration) {
                            declaration.accept(this);  // Triggers visitObjectDeclaration() for companion/nested objects
                        } else if (declaration instanceof KtNamedFunction) {
                            declaration.accept(this);  // Phase 4.1: Visit member functions
                        } else if (declaration instanceof KtSecondaryConstructor) {
                            declaration.accept(this);  // Phase 4.2: Visit secondary constructors
                        } else if (declaration instanceof KtAnonymousInitializer) {
                            declaration.accept(this);  // Phase 4.2: Visit init blocks
                        } else if (declaration instanceof KtProperty) {
                            declaration.accept(this);  // Phase 4.3: Visit properties
                        }
                        // Note: Enum entries handled separately
                    }
                    
                    // 8. End type body with separate closing brace token
                    callbacks.invokeEndTypeBody(rBraceToken, true);

                    finalToken = rBraceToken;
                }
                // Note: If braces missing (malformed code), no nested declarations to visit
            }

            var lastChild = ktClass.getLastChild();

            finalToken = createToken(lastChild, JavaTokenTypes.EOF); // TODO: figure out how to get proper token type
            
            // 9. End declaration
            callbacks.invokeTypeDefEnd(finalToken, true);

        } finally {
            state.popScope();  // ALWAYS cleanup
        }
    }
    
    /**
     * Visits a function declaration (method).
     *
     * <p>This method is invoked for all named function declarations including:</p>
     * <ul>
     *   <li>Member functions (methods in classes)</li>
     *   <li>Extension functions ({@code fun String.extension()})</li>
     *   <li>Operator overloading ({@code operator fun plus()})</li>
     *   <li>Infix functions ({@code infix fun to()})</li>
     *   <li>Top-level functions (deferred - Phase 4 future work)</li>
     * </ul>
     *
     * <h3>Phase 4 Milestone 4.1: Method Declaration Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin method declaration</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers (visibility, operator, etc.)</li>
     *   <li>{@code gotTypeSpec(tokens)} - Return type specification</li>
     *   <li>{@code gotMethodDeclaration(nameToken, javadocToken)} - Method name and javadoc</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>Generic type parameters (if present):</li>
     *   <ul>
     *     <li>{@code gotMethodTypeParamsBegin()}</li>
     *     <li>{@code gotTypeParam(idToken)} × n</li>
     *     <li>{@code gotTypeParamBound(tokens)} × n (if bounded)</li>
     *     <li>{@code endMethodTypeParams()}</li>
     *   </ul>
     *   <li>{@code gotTypeSpec(tokens)} × n - Parameter types</li>
     *   <li>{@code gotMethodParameter(nameToken, ellipsisToken)} × n - Parameters</li>
     *   <li>{@code gotAllMethodParameters()} - All parameters processed</li>
     *   <li>Method body (if present):</li>
     *   <ul>
     *     <li>{@code beginMethodBody(token)}</li>
     *     <li>Body traversal (Phase 6 - skipped in Phase 4)</li>
     *     <li>{@code endMethodBody(token, true)}</li>
     *   </ul>
     *   <li>{@code endMethodDecl(token, true)} - End method declaration</li>
     * </ol>
     *
     * <h3>Kotlin-Specific Features</h3>
     * <ul>
     *   <li><b>Extension Functions:</b> Receiver type handled via {@link KtNamedFunction#getReceiverTypeReference()}</li>
     *   <li><b>Operator Overloading:</b> {@code operator} modifier detected and recorded</li>
     *   <li><b>Infix Functions:</b> {@code infix} modifier detected and recorded</li>
     *   <li><b>Suspend Functions:</b> {@code suspend} modifier detected and recorded</li>
     *   <li><b>Expression Bodies:</b> {@code fun method() = expr} vs {@code fun method() { ... }}</li>
     *   <li><b>Vararg Parameters:</b> Detected via {@link KtParameter#hasModifier} with {@code KtTokens.VARARG_KEYWORD}</li>
     * </ul>
     *
     * <h3>Out of Scope (Phase 4)</h3>
     * <ul>
     *   <li>Method body traversal (expressions/statements) - deferred to Phase 6</li>
     *   <li>Local/nested functions - deferred to Phase 6</li>
     *   <li>Default parameter values - requires expression parsing (Phase 6)</li>
     * </ul>
     *
     * @param function The function declaration PSI element
     */
    @Override
    public void visitNamedFunction(@NotNull KtNamedFunction function) {
        if (function == null) {
            return;
        }
        
        // Phase 2: Log function visit (retained for debugging)
        String functionName = function.getName();
        traversalLog.add("VISIT: FUNCTION: " + (functionName != null ? functionName : "<anonymous>"));
        
        // State management with try-finally
        state.pushScope(function);
        try {
            // Skip if no callbacks configured
            if (callbacks == null) {
                // Phase 2 mode: no callback invocation
                return;
            }
            
            // 1. Begin declaration
            LocatableToken declToken = createToken(function.getFunKeyword(), JavaTokenTypes.LITERAL_fun);
            callbacks.invokeDeclBegin(declToken);
            
            // 2. Process modifiers (visibility, operator, infix, suspend, etc.)
            KtModifierList modifierList = function.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
                processMethodSpecificModifiers(function);
            }
            
            // 3. Return type (before method declaration as per callback protocol)
            KtTypeReference returnTypeRef = function.getTypeReference();
            if (returnTypeRef != null) {
                List<LocatableToken> returnTypeTokens = extractTypeTokens(returnTypeRef);
                callbacks.invokeTypeSpec(returnTypeTokens);
            }
            // Note: If no explicit return type, Kotlin infers Unit - we skip gotTypeSpec in this case
            
            // 4. Method declaration (name + javadoc)
            PsiElement nameIdentifier = function.getNameIdentifier();
            if (nameIdentifier != null && functionName != null) {
                LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                
                // TODO Phase 4.4: Extract KDoc comments as javadocToken
                // KDoc format: /** ... */ above function declaration
                // Use KtDeclaration.getDocComment() for extraction
                // Map KDoc structure to LocatableToken (requires KDoc→JavaDoc conversion)
                LocatableToken javadocToken = null;  // Intentionally null until Phase 4.4
                
                callbacks.invokeMethodDeclaration(nameToken, javadocToken);
            }
            
            // 5. Modifiers consumed
            callbacks.invokeModifiersConsumed();
            
            // 6. Generic type parameters (if present)
            KtTypeParameterList typeParams = function.getTypeParameterList();
            if (typeParams != null && !typeParams.getParameters().isEmpty()) {
                processMethodTypeParameters(typeParams);
            }
            
            // 7. Parameters
            processMethodParameters(function);
            
            // 8. Throws clause (rare in Kotlin - skip for Phase 4)
            // Kotlin uses @Throws annotation instead of throws clause
            
            // 9. Method body (mark boundaries but don't traverse - Phase 6)
            processMethodBody(function);
            
            // 10. End declaration
            LocatableToken endToken = createToken(function, JavaTokenTypes.LITERAL_void);
            callbacks.invokeEndMethodDecl(endToken, true);
            
        } finally {
            // Clear modifier state to prevent leakage into next declaration
            clearModifierState();
            state.popScope();  // ALWAYS cleanup
        }
    }
    
    /**
     * Visits a primary constructor declaration.
     *
     * <p>Primary constructors appear in the class header and may define property parameters:</p>
     * <pre>{@code
     * class Person(val name: String, age: Int)
     *               ^-- primary constructor
     * }</pre>
     *
     * <h3>Phase 4 Milestone 4.2: Primary Constructor Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin constructor declaration (uses METHOD_DECL token)</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers (visibility only for constructors)</li>
     *   <li>{@code gotConstructorDecl(nameToken, javadocToken)} - Constructor name (class name)</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>{@code gotTypeSpec(tokens)} × n - Parameter types</li>
     *   <li>{@code gotMethodParameter(nameToken, null)} × n - Parameters (no vararg in constructors)</li>
     *   <li>{@code gotAllMethodParameters()} - All parameters processed</li>
     *   <li>{@code endMethodDecl(token, true)} - End constructor declaration</li>
     * </ol>
     *
     * <h3>Parameter Properties</h3>
     * <p>Parameters with {@code val} or {@code var} modifiers also define properties. In Phase 4.2,
     * we process them as constructor parameters. The property aspects will be handled in Milestone 4.3
     * when property visitor is implemented.</p>
     *
     * <h3>Primary vs Secondary Constructors</h3>
     * <p>Primary constructors have no explicit body - initialization happens via init blocks
     * (processed separately by {@link #visitAnonymousInitializer(KtAnonymousInitializer)}).
     * Secondary constructors have explicit bodies and delegation calls.</p>
     *
     * <h3>Constructor Detection</h3>
     * <p>Primary constructors are accessed via {@code KtClass.getPrimaryConstructor()} rather
     * than being visited directly. This method is called from {@link #visitClass(KtClass)} to
     * handle primary constructor before processing class members.</p>
     *
     * <h3>Out of Scope (Phase 4)</h3>
     * <ul>
     *   <li>Constructor body traversal - deferred to Phase 6</li>
     *   <li>Property parameter processing - deferred to Milestone 4.3</li>
     *   <li>Default parameter values - requires expression parsing (Phase 6)</li>
     * </ul>
     *
     * @param constructor The primary constructor PSI element
     */
    @Override
    public void visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor) {
        if (constructor == null) {
            return;
        }

        // State management with try-finally
        state.pushScope(constructor);
        try {
            // Skip if no callbacks configured
            if (callbacks == null) {
                // Phase 2 mode: no callback invocation
                super.visitPrimaryConstructor(constructor);
                return;
            }

            // 1. Begin declaration
            LocatableToken declToken = createToken(constructor, JavaTokenTypes.LITERAL_void);
            callbacks.invokeDeclBegin(declToken);

            // 2. Process modifiers (visibility modifiers only for constructors)
            KtModifierList modifierList = constructor.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }

            // 3. Constructor declaration (use class name as constructor name)
            // Primary constructor doesn't have its own name - use parent class name
            PsiElement parent = constructor.getParent();
            LocatableToken nameToken;
            if (parent instanceof KtClass) {
                KtClass ktClass = (KtClass) parent;
                PsiElement classNameId = ktClass.getNameIdentifier();
                if (classNameId != null) {
                    nameToken = createToken(classNameId, JavaTokenTypes.IDENT);
                } else {
                    // Fallback: use constructor element itself
                    nameToken = createToken(constructor, JavaTokenTypes.IDENT);
                }
            } else {
                // Fallback: use constructor element itself
                nameToken = createToken(constructor, JavaTokenTypes.IDENT);
            }

            // TODO Phase 4.4: Extract KDoc from parent class for primary constructor
            LocatableToken javadocToken = null;  // Intentionally null until Phase 4.4

            callbacks.invokeConstructorDecl(nameToken, javadocToken);

            // 4. Modifiers consumed
            callbacks.invokeModifiersConsumed();

            // 5. Parameters (including property parameters with val/var)
            processConstructorParameters(constructor);

            // 6. End declaration (primary constructors have no explicit body)
            LocatableToken endToken = createToken(constructor, JavaTokenTypes.LITERAL_void);
            callbacks.invokeEndMethodDecl(endToken, true);

            // Visit children for any nested declarations
            super.visitPrimaryConstructor(constructor);

        } finally {
            // Clear modifier state to prevent leakage (learned from M4.1 review issue M3)
            clearModifierState();
            state.popScope();  // ALWAYS cleanup
        }
    }
    
    /**
     * Visits a secondary constructor declaration.
     *
     * <p>Secondary constructors appear in class body with explicit bodies and delegation:</p>
     * <pre>{@code
     * class Person(val name: String) {
     *     constructor(name: String, id: Int) : this(name) {
     *         // Constructor body
     *     }
     * }
     * }</pre>
     *
     * <h3>Phase 4 Milestone 4.2: Secondary Constructor Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin constructor declaration</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code gotConstructorDecl(nameToken, javadocToken)} - Constructor name ("constructor" keyword)</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>{@code gotTypeSpec(tokens)} × n - Parameter types</li>
     *   <li>{@code gotMethodParameter(nameToken, null)} × n - Parameters</li>
     *   <li>{@code gotAllMethodParameters()} - All parameters processed</li>
     *   <li>{@code beginMethodBody(token)} - Begin body (if present)</li>
     *   <li>Body traversal (Phase 6 - skipped in Phase 4)</li>
     *   <li>{@code endMethodBody(token, true)} - End body</li>
     *   <li>{@code endMethodDecl(token, true)} - End constructor declaration</li>
     * </ol>
     *
     * <h3>Constructor Delegation</h3>
     * <p>Secondary constructors must delegate to either the primary constructor ({@code : this(...)})
     * or superclass constructor ({@code : super(...)}). The delegation call is accessed via
     * {@link KtSecondaryConstructor#getDelegationCall()}. In Phase 4, we note the delegation
     * exists but don't traverse the call expression (deferred to Phase 6).</p>
     *
     * <h3>Out of Scope (Phase 4)</h3>
     * <ul>
     *   <li>Constructor body traversal (expressions/statements) - Phase 6</li>
     *   <li>Delegation call expression parsing - Phase 6</li>
     *   <li>Default parameter values - Phase 6</li>
     * </ul>
     *
     * @param constructor The secondary constructor PSI element
     */
    @Override
    public void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor) {
        if (constructor == null) {
            return;
        }
        
        // State management with try-finally
        state.pushScope(constructor);
        try {
            // Skip if no callbacks configured
            if (callbacks == null) {
                super.visitSecondaryConstructor(constructor);
                return;
            }
            
            // 1. Begin declaration
            LocatableToken declToken = createToken(constructor, JavaTokenTypes.LITERAL_void);
            callbacks.invokeDeclBegin(declToken);
            
            // 2. Process modifiers (visibility modifiers for constructors)
            KtModifierList modifierList = constructor.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }
            
            // 3. Constructor declaration (use "constructor" keyword as name indicator)
            PsiElement constructorKeyword = constructor.getConstructorKeyword();
            LocatableToken nameToken;
            if (constructorKeyword != null) {
                nameToken = createToken(constructorKeyword, JavaTokenTypes.IDENT);
            } else {
                // Fallback: use constructor element itself
                nameToken = createToken(constructor, JavaTokenTypes.IDENT);
            }
            
            // TODO Phase 4.4: Extract KDoc for secondary constructor
            LocatableToken javadocToken = null;  // Intentionally null until Phase 4.4
            
            callbacks.invokeConstructorDecl(nameToken, javadocToken);
            
            // 4. Modifiers consumed
            callbacks.invokeModifiersConsumed();
            
            // 5. Parameters
            processConstructorParameters(constructor);
            
            // 6. Constructor delegation (: this(...) or : super(...))
            // Phase 4: Note delegation exists but skip expression parsing (Phase 6)
            KtConstructorDelegationCall delegation = constructor.getDelegationCall();
            if (delegation != null) {
                // Delegation call present (this() or super())
                // Delegation expression parsing deferred to Phase 6
            }
            
            // 7. Constructor body (if present)
            KtBlockExpression body = constructor.getBodyExpression();
            if (body != null) {
                PsiElement lBrace = body.getLBrace();
                if (lBrace != null) {
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    callbacks.invokeBeginMethodBody(lBraceToken);
                }
                
                // Phase 4: Skip body traversal (expressions/statements - Phase 6)
                
                PsiElement rBrace = body.getRBrace();
                if (rBrace != null) {
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    callbacks.invokeEndMethodBody(rBraceToken, true);
                }
            }
            
            // 8. End declaration
            LocatableToken endToken = createToken(constructor, JavaTokenTypes.LITERAL_void);
            callbacks.invokeEndMethodDecl(endToken, true);
            
            // Visit children for any nested declarations
            super.visitSecondaryConstructor(constructor);
            
        } finally {
            // Clear modifier state to prevent leakage (learned from M4.1 review issue M3)
            clearModifierState();
            state.popScope();  // ALWAYS cleanup
        }
    }
    
    /**
     * Visits an init block (anonymous initializer).
     *
     * <p>Init blocks provide initialization code for primary constructors:</p>
     * <pre>{@code
     * class MyClass(param: String) {
     *     init {
     *         println("Initialized with: $param")
     *     }
     * }
     * }</pre>
     *
     * <h3>Phase 4 Milestone 4.2: Init Block Callback Sequence</h3>
     * <ol>
     *   <li>{@code beginInitBlock(firstToken, lcurlyToken)} - Begin init block</li>
     *   <li>Block body traversal (Phase 6 - skipped in Phase 4)</li>
     *   <li>{@code endInitBlock(rcurlyToken, true)} - End init block</li>
     * </ol>
     *
     * <h3>Init Block Semantics</h3>
     * <p>Init blocks are executed as part of primary constructor initialization, in declaration
     * order interspersed with property initializers. Multiple init blocks are allowed and
     * execute in source order.</p>
     *
     * <h3>Association with Constructors</h3>
     * <p>Init blocks are associated with the primary constructor (implicit or explicit). They
     * execute before secondary constructor bodies when delegation calls are made.</p>
     *
     * <h3>Out of Scope (Phase 4)</h3>
     * <ul>
     *   <li>Init block body traversal (statements) - Phase 6</li>
     *   <li>Property initializer interleaving - Phase 6</li>
     * </ul>
     *
     * @param initializer The init block PSI element (KtAnonymousInitializer is the actual type)
     */
    @Override
    public void visitAnonymousInitializer(@NotNull KtAnonymousInitializer initializer) {
        if (initializer == null) {
            return;
        }
        
        // State management with try-finally
        state.pushScope(initializer);
        try {
            // Skip if no callbacks configured
            if (callbacks == null) {
                super.visitAnonymousInitializer(initializer);
                return;
            }
            
            // 1. Begin init block
            // First token is the "init" keyword, second is the block itself
            PsiElement initKeyword = initializer.getFirstChild();  // "init" keyword
            KtExpression bodyExpr = initializer.getBody();
            
            if (initKeyword != null && bodyExpr instanceof KtBlockExpression) {
                KtBlockExpression body = (KtBlockExpression) bodyExpr;
                LocatableToken firstToken = createToken(initKeyword, JavaTokenTypes.LITERAL_static);
                
                PsiElement lBrace = body.getLBrace();
                if (lBrace != null) {
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    callbacks.invokeBeginInitBlock(firstToken, lBraceToken);
                }
                
                // Phase 4: Skip init block body traversal (statements - Phase 6)
                
                // 2. End init block
                PsiElement rBrace = body.getRBrace();
                if (rBrace != null) {
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    callbacks.invokeEndInitBlock(rBraceToken, true);
                }
            }
            
            // Visit children for any nested declarations (unlikely in init blocks)
            super.visitAnonymousInitializer(initializer);
            
        } finally {
            state.popScope();  // ALWAYS cleanup
        }
    }
    
    /**
     * Visits a property declaration (val or var).
     *
     * <p>This method is invoked for property declarations including:</p>
     * <ul>
     *   <li>Class member properties</li>
     *   <li>Top-level properties</li>
     *   <li>Extension properties</li>
     *   <li>Property parameters (handled by constructor visitor)</li>
     * </ul>
     *
     * <h3>Phase 4 Milestone 4.3: Property Declaration Callback Sequence</h3>
     * <p>Kotlin properties map to Java fields in BlueJ's ClassInfo. The callback sequence follows
     * the field declaration pattern from {@link JavaParserCallbacks}:</p>
     * <ol>
     *   <li>{@code beginFieldDeclarations(token)} - Begin field declarations</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code gotTypeSpec(tokens)} - Property type</li>
     *   <li>{@code gotField(first, idToken, initExpressionFollows)} - Field declaration</li>
     *   <li>{@code endField(token, true)} - End field</li>
     *   <li>{@code endFieldDeclarations(token, true)} - End field declarations</li>
     * </ol>
     *
     * <h3>Kotlin Property Semantics</h3>
     * <p>Kotlin properties map to Java fields with implicit getters/setters:</p>
     * <ul>
     *   <li>{@code val name: String} → field "name" (ClassInfo infers getter)</li>
     *   <li>{@code var count: Int} → field "count" (ClassInfo infers getter + setter)</li>
     * </ul>
     *
     * <p><b>Note:</b> BlueJ's ClassInfo logic infers synthetic getters/setters from field metadata.
     * We don't need to explicitly generate method declarations for them unless there are
     * custom accessors (handled separately).</p>
     *
     * <h3>Custom Accessors</h3>
     * <p>Properties with custom getter/setter bodies are handled by
     * {@link #visitPropertyAccessor(KtPropertyAccessor)} which is called during
     * traversal of property children.</p>
     *
     * <h3>Out of Scope (Phase 4)</h3>
     * <ul>
     *   <li>Property initialization expressions - Phase 6</li>
     *   <li>Delegated properties (by lazy, observable) - Phase 6</li>
     *   <li>Property bodies in custom accessors - Phase 6</li>
     * </ul>
     *
     * @param property The property declaration PSI element
     */
    @Override
    public void visitProperty(@NotNull KtProperty property) {
        if (property == null) {
            return;
        }
        
        // Phase 2: Log property visit (retained for debugging)
        String propertyName = property.getName();
        traversalLog.add("VISIT: PROPERTY: " + (propertyName != null ? propertyName : "<anonymous>"));
        
        // State management with try-finally
        state.pushScope(property);
        try {
            // Skip if no callbacks configured
            if (callbacks == null) {
                super.visitProperty(property);
                return;
            }
            
            // 1. Begin field declarations
            LocatableToken propertyToken = createToken( property.getValOrVarKeyword(), property.isVar() ? JavaTokenTypes.LITERAL_var : JavaTokenTypes.LITERAL_val);
            callbacks.invokeDeclBegin(propertyToken);
            callbacks.invokeBeginFieldDeclarations(propertyToken);
            
            // 2. Process modifiers
            KtModifierList modifierList = property.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }
            
            // 3. Property type
            LocatableToken typeToken = processPropertyType(property);
            if (typeToken != null) {
                List<LocatableToken> typeTokens = List.of(typeToken);
                callbacks.invokeTypeSpec(typeTokens);
            }
            
            // 4. Field declaration
            PsiElement nameIdentifier = property.getNameIdentifier();
            if (nameIdentifier != null && propertyName != null) {
                LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                boolean hasInitializer = property.hasInitializer();
                callbacks.invokeField(propertyToken, nameToken, hasInitializer);
            }
            
            // 5. End field
            callbacks.invokeEndField(propertyToken, true);
            
            // 6. End field declarations
            callbacks.invokeEndFieldDeclarations(propertyToken, true);

            // TODO: that needs to be done only on failure
            // callbacks.invokeEndDecl(propertyToken);

            // Visit custom accessors if present (custom getter/setter bodies)
            super.visitProperty(property);
            
        } finally {
            clearModifierState();
            state.popScope();  // ALWAYS cleanup
        }
    }
    
    /**
     * Helper method to capitalize the first letter of a string.
     * Used for generating synthetic getter/setter names from property names.
     *
     * @param str The string to capitalize
     * @return The capitalized string, or empty string if input is null/empty
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * Processes the property type and returns a type token.
     *
     * <p>Handles both explicit types and type inference:</p>
     * <ul>
     *   <li>Explicit type: {@code val name: String} → "String" token</li>
     *   <li>Inferred type: {@code val count = 0} → "inferred" token</li>
     * </ul>
     *
     * <h3>Type Inference Limitation</h3>
     * <p>Phase 4 cannot resolve inferred types without Kotlin compiler integration.
     * Properties without explicit types are marked as "inferred" type.</p>
     *
     * @param property The property to extract type from
     * @return LocatableToken representing the property type, or null if unavailable
     */
    private LocatableToken processPropertyType(KtProperty property) {
        KtTypeReference typeRef = property.getTypeReference();
        if (typeRef != null) {
            // Explicit type annotation
            return createToken(typeRef, JavaTokenTypes.IDENT);
        } else {
            // Type inference - mark as inferred
            // Phase 4 limitation: Cannot resolve inferred types without compiler integration
            return createTokenWithText(property, "inferred", JavaTokenTypes.IDENT);
        }
    }
    
    /**
     * Visits an object declaration.
     *
     * <p>This method is invoked for Kotlin object declarations including:</p>
     * <ul>
     *   <li>Singleton objects ({@code object MySingleton})</li>
     *   <li>Companion objects ({@code companion object})</li>
     *   <li>Named companion objects ({@code companion object Factory})</li>
     * </ul>
     *
     * <p>Object declarations are mapped to class callbacks since BlueJ's ClassInfo
     * model doesn't have special object types. The callback sequence matches that
     * of {@link #visitClass(KtClass)} but uses {@code JavaTokenTypes.OBJECT_DEF}
     * as the token type.</p>
     *
     * <h3>Phase 3 Milestone 3.2: Object Declaration Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin declaration</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>{@code gotTypeDef(token, TYPEDEF_CLASS)} - Type definition (objects are classes)</li>
     *   <li>{@code gotTypeDefName(nameToken)} - Object or "Companion" name</li>
     *   <li>Supertype processing (objects can implement interfaces)</li>
     *   <li>{@code beginTypeBody(token)} - Begin body</li>
     *   <li>Visit members via recursive traversal</li>
     *   <li>{@code endTypeBody(token, true)} - End body</li>
     *   <li>{@code gotTypeDefEnd(token, true)} - End declaration</li>
     * </ol>
     *
     * <h3>Companion Object Handling</h3>
     * <p>Companion objects are detected via {@link KtObjectDeclaration#isCompanion()}.
     * If the companion has no explicit name, "Companion" is used as the default name.
     * Named companions like {@code companion object Factory} use their declared name.</p>
     *
     * @param declaration The object declaration PSI element
     */
    @Override
    public void visitObjectDeclaration(@NotNull KtObjectDeclaration declaration) {
        if (declaration == null) {
            return;
        }
        
        // Phase 2: Log object visit (retained for debugging)
        String objectName = declaration.getName();
        if (objectName == null && declaration.isCompanion()) {
            objectName = "Companion";
        }
        traversalLog.add("VISIT: OBJECT: " + (objectName != null ? objectName : "<anonymous>"));
        
        // State management with try-finally
        state.pushScope(declaration);
        try {
            // Skip if no callbacks configured
            if (callbacks == null) {
                super.visitObjectDeclaration(declaration);
                return;
            }
            
            // 1. Begin declaration
            LocatableToken objectToken = createToken(declaration.getObjectKeyword(), JavaTokenTypes.LITERAL_object);
            callbacks.invokeDeclBegin(objectToken);
            
            // 2. Process modifiers (objects can have visibility modifiers)
            KtModifierList modifierList = declaration.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }
            callbacks.invokeModifiersConsumed();
            
            // 3. Type definition - objects are mapped to classes
            callbacks.invokeTypeDef(objectToken, JavaTokenTypes.LITERAL_class);
            
            // 4. Object name (or "Companion" for companion objects)
            String name = declaration.getName();
            if (name != null && declaration.getNameIdentifier() != null) {
                // Named object or named companion
                LocatableToken nameToken = createToken(declaration.getNameIdentifier(), JavaTokenTypes.IDENT);
                callbacks.invokeTypeDefName(nameToken);
            } else if (declaration.isCompanion()) {
                // Companion object without explicit name - use "Companion" as synthetic name
                LocatableToken nameToken = createTokenWithText(declaration, "Companion", JavaTokenTypes.IDENT);
                callbacks.invokeTypeDefName(nameToken);
            }
            
            // 5. Process supertypes (objects can implement interfaces)
            processSuperTypes(declaration);
            
            // 6. Begin type body
            KtClassBody body = declaration.getBody();
            if (body != null) {
                PsiElement lBrace = body.getLBrace();
                PsiElement rBrace = body.getRBrace();
                
                if (lBrace != null && rBrace != null) {
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    callbacks.invokeBeginTypeBody(lBraceToken);
                    
                    // 7. Visit nested declarations (objects can contain nested classes/objects/functions/properties)
                    for (KtDeclaration decl : body.getDeclarations()) {
                        if (decl instanceof KtClass && !(decl instanceof KtEnumEntry)) {
                            decl.accept(this);
                        } else if (decl instanceof KtObjectDeclaration) {
                            decl.accept(this);
                        } else if (decl instanceof KtNamedFunction) {
                            decl.accept(this);  // Phase 4.1: Visit methods
                        } else if (decl instanceof KtProperty) {
                            decl.accept(this);  // Phase 4.3: Visit properties
                        }
                    }
                    
                    // 8. End type body
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    callbacks.invokeEndTypeBody(rBraceToken, true);
                }
            }
            
            // 9. End declaration
            callbacks.invokeTypeDefEnd(objectToken, true);
            
        } finally {
            state.popScope();  // ALWAYS cleanup
        }
    }
    
    /**
     * Verify that state stack is balanced after traversal.
     *
     * <p>MUST be called after {@link #visitKtFile(KtFile)} completes to ensure
     * all push/pop operations were properly balanced. An unbalanced stack indicates
     * a bug in the visitor implementation (missing finally block or exception during
     * traversal).</p>
     *
     * <p><b>Usage Pattern:</b></p>
     * <pre>{@code
     * ktFile.accept(visitor);
     * if (!visitor.validateState()) {
     *     throw new IllegalStateException("Visitor state unbalanced!");
     * }
     * }</pre>
     *
     * @return true if state is valid (stack empty), false otherwise
     * @throws IllegalStateException if state is unbalanced with detailed error message
     */
    public boolean validateState() {
        if (!state.isStackBalanced()) {
            throw new IllegalStateException(
                "State stack unbalanced: " + state.getStackSize() + " elements remaining");
        }
        return true;
    }
    
    /**
     * Get current visitor state (for testing).
     *
     * <p>Public to allow test access from bluej.parser.psi package.
     * Tests can use this to verify state management during traversal, check
     * nesting depth, or validate modifier accumulation.</p>
     *
     * <p><b>Not for production use</b> - only for testing visitor behavior.</p>
     *
     * @return The visitor state instance
     */
    public VisitorState getState() {
        return state;
    }
    
    // ==================== Phase 3 Helper Methods ====================
    
    /**
     * Creates a LocatableToken from a PSI element with proper line/column positions.
     *
     * <p>This helper method converts a PSI element into a token suitable for callback
     * invocation. It calculates accurate line and column positions by accessing the
     * Document via PsiDocumentManager.</p>
     *
     * <p><b>Position Calculation:</b> Uses IntelliJ Platform's Document API to convert
     * text offsets into line/column coordinates:</p>
     * <ol>
     *   <li>Get containing file from element</li>
     *   <li>Access Project from file</li>
     *   <li>Get Document via PsiDocumentManager</li>
     *   <li>Convert offsets to line numbers (0-based → 1-based)</li>
     *   <li>Calculate columns as offset from line start</li>
     * </ol>
     *
     * <p><b>Fallback Strategy:</b> If Document is unavailable (rare edge case), falls back
     * to placeholder positions to ensure graceful degradation.</p>
     *
     * @param element The PSI element to convert to a token
     * @param type The token type from {@link JavaTokenTypes}
     * @return LocatableToken with accurate line/column positions
     * @throws IllegalArgumentException if element is null
     */
    private LocatableToken createToken(PsiElement element, int type) {
        if (element == null) {
            throw new IllegalArgumentException("PSI element must not be null");
        }
        
        // Extract text from element
        String text = element.getText();
        if (text == null) {
            text = "";
        }
        
        return createTokenWithText(element, text, type);
    }
    
    /**
     * Creates a LocatableToken with custom text at the position of a PSI element.
     *
     * <p>This is used for synthesized tokens where we need to provide specific text
     * (like "Companion" for unnamed companion objects) while maintaining proper
     * position information from the PSI element.</p>
     *
     * @param element The PSI element to get position from
     * @param customText The text to use in the token
     * @param type The token type from {@link JavaTokenTypes}
     * @return LocatableToken with custom text and element's position
     * @throws IllegalArgumentException if element is null
     */
    private LocatableToken createTokenWithText(PsiElement element, String customText, int type) {
        if (element == null) {
            throw new IllegalArgumentException("PSI element must not be null");
        }
        
        int startOffset = element.getTextOffset();
        int endOffset = startOffset + customText.length();
        
        // Calculate positions using shared helper
        LineColPos[] positions = calculatePositions(element, startOffset, endOffset);
        
        return new LocatableToken(type, customText, positions[0], positions[1]);
    }
    
    /**
     * Calculates begin and end positions for a token.
     *
     * <p>This helper method centralizes the position calculation logic to eliminate
     * duplication between {@link #createToken(PsiElement, int)} and
     * {@link #createTokenWithText(PsiElement, String, int)}.</p>
     *
     * <p><b>Algorithm:</b></p>
     * <ol>
     *   <li>Get containing file from element</li>
     *   <li>If no file available, use fallback positions (offset as line/column)</li>
     *   <li>Otherwise, calculate actual line/column from file text and offsets</li>
     *   <li>Return array of [begin, end] positions</li>
     * </ol>
     *
     * @param element The PSI element to get position from
     * @param startOffset The start offset in the file (0-based)
     * @param endOffset The end offset in the file (0-based)
     * @return Array containing [begin, end] LineColPos
     */
    private LineColPos[] calculatePositions(PsiElement element, int startOffset, int endOffset) {
        // Get source file text for line/column calculation
        // Note: Document API (PsiDocumentManager) is unavailable in lightweight test PSI environments,
        // so we calculate positions directly from source text
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) {
            // Fallback if no containing file (shouldn't happen)
            return new LineColPos[] {
                new LineColPos(1, startOffset, startOffset),
                new LineColPos(1, endOffset, endOffset)
            };
        }
        
        String fileText = psiFile.getText();
        
        // Calculate start position
        int[] startLineCol = calculateLineColumn(fileText, startOffset);
        int startLine = startLineCol[0];
        int startColumn = startLineCol[1];
        
        // Calculate end position
        int[] endLineCol = calculateLineColumn(fileText, endOffset);
        int endLine = endLineCol[0];
        int endColumn = endLineCol[1];
        
        // Return positions array
        return new LineColPos[] {
            new LineColPos(startLine, startColumn, startOffset),
            new LineColPos(endLine, endColumn, endOffset)
        };
    }
    
    /**
     * Calculate line and column numbers from file text and offset.
     *
     * <p>Counts newlines from file start to offset to determine line number (1-based).
     * Then scans backwards to find line start and calculates column position (1-based).</p>
     *
     * <p><b>Algorithm:</b></p>
     * <ol>
     *   <li>Start at line 1, lineStartOffset = 0</li>
     *   <li>Scan through text from 0 to offset</li>
     *   <li>Each newline increments line, updates lineStartOffset to position after newline</li>
     *   <li>Column = (offset - lineStartOffset) + 1 to convert to 1-based</li>
     * </ol>
     *
     * <p><b>Position Conventions:</b> Both line and column are 1-based to match BlueJ
     * parser conventions. The first character on a line is column 1.</p>
     *
     * @param fileText The complete source file text
     * @param offset The character offset in the file (0-based)
     * @return Array of [line, column] where both are 1-based
     */
    private int[] calculateLineColumn(String fileText, int offset) {
        int line = 1;  // 1-based line number
        int lineStartOffset = 0;
        
        // Count newlines before offset to determine line number
        for (int i = 0; i < Math.min(offset, fileText.length()); i++) {
            if (fileText.charAt(i) == '\n') {
                line++;
                lineStartOffset = i + 1;  // Next line starts after newline
            }
        }
        
        // Column is offset from line start, converted to 1-based
        int column = (offset - lineStartOffset) + 1;
        
        return new int[] { line, column };
    }
    
    /**
     * Determines the typedef type constant for a Kotlin class.
     *
     * <p>Maps Kotlin class declarations to their JavaTokenTypes typedef constants:</p>
     * <ul>
     *   <li>Interfaces → {@link JavaTokenTypes#LITERAL_interface}</li>
     *   <li>Enum classes → {@link JavaTokenTypes#LITERAL_enum}</li>
     *   <li>Regular classes (including data, sealed, etc.) → {@link JavaTokenTypes#LITERAL_class}</li>
     * </ul>
     *
     * <p>This classification is used by {@code JavaParserCallbacks.gotTypeDef(LocatableToken, int)}
     * to inform the parser infrastructure about the type of declaration being processed.</p>
     *
     * @param ktClass The Kotlin class PSI element to classify
     * @return The appropriate TYPEDEF_* constant from {@link JavaTokenTypes}
     */
    private int determineTypeDefType(KtClass ktClass) {
        if (ktClass.isInterface()) {
            return JavaTokenTypes.LITERAL_interface;
        } else if (ktClass.isEnum()) {
            return JavaTokenTypes.LITERAL_enum;
        } else {
            // Regular class (includes data classes, sealed classes, etc.)
            return JavaTokenTypes.LITERAL_class;
        }
    }
    
    /**
     * Processes Kotlin modifiers and invokes corresponding callbacks.
     *
     * <p>This method extracts modifiers from a Kotlin modifier list and maps them
     * to Java equivalents for callback invocation. The mapping strategy ensures
     * compatibility with BlueJ's Java-centric parser infrastructure.</p>
     *
     * <h3>Modifier Mapping Strategy</h3>
     * <table border="1">
     *   <tr><th>Kotlin Modifier</th><th>Java Equivalent</th><th>Notes</th></tr>
     *   <tr><td>public</td><td>public</td><td>Direct mapping</td></tr>
     *   <tr><td>private</td><td>private</td><td>Direct mapping</td></tr>
     *   <tr><td>protected</td><td>protected</td><td>Direct mapping</td></tr>
     *   <tr><td>internal</td><td>public</td><td>Module-visible → public approximation</td></tr>
     *   <tr><td>abstract</td><td>abstract</td><td>Direct mapping</td></tr>
     *   <tr><td>final</td><td>final</td><td>Direct mapping</td></tr>
     *   <tr><td>open</td><td>(skipped)</td><td>Default Java behavior (non-final)</td></tr>
     * </table>
     *
     * <p><b>Processing Order:</b> Modifiers are processed in the order they appear
     * in the source code, which matches BlueJ's expectation for Java modifiers.</p>
     *
     * <p><b>Kotlin-Specific Modifiers:</b> Modifiers like {@code data}, {@code sealed},
     * {@code companion} don't have direct Java equivalents and are handled in later tasks.</p>
     *
     * @param modifierList The Kotlin modifier list to process (must not be null)
     */
    private void processModifiers(KtModifierList modifierList) {
        if (modifierList == null || callbacks == null) {
            return;
        }
        
        // Process visibility modifiers
        if (modifierList.hasModifier(KtTokens.PUBLIC_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.PUBLIC_KEYWORD),
                JavaTokenTypes.LITERAL_public
            );
            callbacks.invokeModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.PRIVATE_KEYWORD),
                JavaTokenTypes.LITERAL_private
            );
            callbacks.invokeModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.PROTECTED_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.PROTECTED_KEYWORD),
                JavaTokenTypes.LITERAL_protected
            );
            callbacks.invokeModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
            // Kotlin 'internal' modifier - module visibility
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.INTERNAL_KEYWORD),
                JavaTokenTypes.LITERAL_internal
            );
            callbacks.invokeModifier(token);
        }
        
        // Process inheritance modifiers
        if (modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.ABSTRACT_KEYWORD),
                JavaTokenTypes.ABSTRACT
            );
            callbacks.invokeModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.FINAL_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.FINAL_KEYWORD),
                JavaTokenTypes.FINAL
            );
            callbacks.invokeModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.OPEN_KEYWORD)) {
            // Kotlin 'open' modifier - allows inheritance (opposite of final)
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.OPEN_KEYWORD),
                JavaTokenTypes.LITERAL_open
            );
            callbacks.invokeModifier(token);
        }
        
        // Process property-specific modifiers (Phase 4.3)
        if (modifierList.hasModifier(KtTokens.LATEINIT_KEYWORD)) {
            // Kotlin 'lateinit' modifier - late initialization for var properties
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.LATEINIT_KEYWORD),
                JavaTokenTypes.LITERAL_lateinit
            );
            callbacks.invokeModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.CONST_KEYWORD)) {
            // Kotlin 'const' modifier - compile-time constant
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.CONST_KEYWORD),
                JavaTokenTypes.LITERAL_const
            );
            callbacks.invokeModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            // Kotlin 'override' modifier - applies to methods and properties
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.OVERRIDE_KEYWORD),
                JavaTokenTypes.LITERAL_override
            );
            callbacks.invokeModifier(token);
        }
        
        // Note: Static modifier handling for companion objects deferred to Task 3.2
        // Kotlin doesn't have static classes, only companion objects
    }
    
    /**
     * Clear modifier state after processing declaration (Phase 3).
     *
     * <p>This method MUST be called after invoking end* callbacks to reset
     * modifier state for the next declaration. Failing to clear modifiers will
     * cause them to leak into subsequent declarations.</p>
     *
     * <p><b>Phase 2:</b> Stub method - not implemented yet.</p>
     * <p><b>Phase 3:</b> Will call {@link VisitorState#clearModifiers()} after
     * each declaration is fully processed.</p>
     */
    private void clearModifierState() {
        state.clearModifiers();
    }
    
    /**
     * Processes superclass and implemented interfaces.
     *
     * <p>Kotlin syntax: First supertype with constructor call is superclass,
     * remaining supertypes are interfaces.</p>
     *
     * <p><b>Constructor Call Detection:</b> The critical distinction is whether
     * the supertype entry has a constructor call (parentheses). This determines
     * classification:</p>
     * <ul>
     *   <li>{@code class Child : Parent()} - Parent is superclass (has constructor call)</li>
     *   <li>{@code class Impl : Interface} - Interface is interface (no constructor call)</li>
     *   <li>{@code class Multi : Parent(), Interface1, Interface2} - Parent is superclass,
     *       Interface1 and Interface2 are interfaces</li>
     *   <li>{@code object MySingleton : MyInterface} - Objects can implement interfaces</li>
     * </ul>
     *
     * <p><b>Note:</b> This method works with both {@link KtClass} and {@link KtObjectDeclaration}
     * since they both implement the {@link KtClassOrObject} interface, which provides
     * the {@code getSuperTypeListEntries()} method.</p>
     *
     * @param classOrObject The Kotlin class or object with potential supertypes
     */
    private void processSuperTypes(KtClassOrObject classOrObject) {
        if (callbacks == null) {
            return;
        }
        
        List<KtSuperTypeListEntry> superTypeEntries = classOrObject.getSuperTypeListEntries();
        if (superTypeEntries.isEmpty()) {
            return; // No supertypes
        }
        
        // Find superclass (first entry with constructor call)
        KtSuperTypeListEntry superClassEntry = null;
        List<KtSuperTypeListEntry> interfaceEntries = new ArrayList<>();
        
        for (KtSuperTypeListEntry entry : superTypeEntries) {
            if (entry instanceof KtSuperTypeCallEntry) {
                // Has constructor call () → this is the superclass
                if (superClassEntry == null) {
                    superClassEntry = entry;
                } else {
                    // Multiple constructor calls? Shouldn't happen, but treat as interface

                    interfaceEntries.add(entry);
                }
            } else {
                // No constructor call → interface
                interfaceEntries.add(entry);
            }
        }

        // Process superclass
        if (superClassEntry != null) {
            processSuperClass(superClassEntry);
        }

        // Process interfaces
        if (!interfaceEntries.isEmpty()) {
            processInterfaces(interfaceEntries);
        }
    }

    
    // ==================== Phase 4 Method Declaration Helpers ====================
    
    /**
     * Processes Kotlin method-specific modifiers.
     *
     * <p>Handles special Kotlin function modifiers that don't have direct Java equivalents:</p>
     * <ul>
     *   <li>{@code operator} - Operator overloading</li>
     *   <li>{@code infix} - Infix function call syntax</li>
     *   <li>{@code inline} - Inline function (compiler optimization)</li>
     *   <li>{@code suspend} - Suspend function (coroutines)</li>
     *   <li>{@code tailrec} - Tail-recursive function optimization</li>
     *   <li>{@code external} - External implementation (JNI/JS)</li>
     * </ul>
     *
     * <p>These modifiers are extracted in addition to standard modifiers processed
     * by {@link #processModifiers(KtModifierList)}.</p>
     *
     * @param function The function to extract method-specific modifiers from
     */
    private void processMethodSpecificModifiers(KtNamedFunction function) {
        if (callbacks == null) {
            return;
        }
        
        KtModifierList modifiers = function.getModifierList();
        if (modifiers == null) {
            return;
        }
        
        // Operator overloading
        if (modifiers.hasModifier(KtTokens.OPERATOR_KEYWORD)) {
            PsiElement operatorModifier = modifiers.getModifier(KtTokens.OPERATOR_KEYWORD);
            if (operatorModifier != null) {
                LocatableToken token = createToken(operatorModifier, JavaTokenTypes.LITERAL_operator);
                callbacks.invokeModifier(token);
            }
        }
        
        // Infix function
        if (modifiers.hasModifier(KtTokens.INFIX_KEYWORD)) {
            PsiElement infixModifier = modifiers.getModifier(KtTokens.INFIX_KEYWORD);
            if (infixModifier != null) {
                LocatableToken token = createToken(infixModifier, JavaTokenTypes.LITERAL_infix);
                callbacks.invokeModifier(token);
            }
        }
        
        // Inline function
        if (modifiers.hasModifier(KtTokens.INLINE_KEYWORD)) {
            PsiElement inlineModifier = modifiers.getModifier(KtTokens.INLINE_KEYWORD);
            if (inlineModifier != null) {
                LocatableToken token = createToken(inlineModifier, JavaTokenTypes.LITERAL_inline);
                callbacks.invokeModifier(token);
            }
        }
        
        // Suspend function
        if (modifiers.hasModifier(KtTokens.SUSPEND_KEYWORD)) {
            PsiElement suspendModifier = modifiers.getModifier(KtTokens.SUSPEND_KEYWORD);
            if (suspendModifier != null) {
                LocatableToken token = createToken(suspendModifier, JavaTokenTypes.LITERAL_suspend);
                callbacks.invokeModifier(token);
            }
        }
        
        // Override modifier (method-specific context)
        if (modifiers.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            PsiElement overrideModifier = modifiers.getModifier(KtTokens.OVERRIDE_KEYWORD);
            if (overrideModifier != null) {
                LocatableToken token = createToken(overrideModifier, JavaTokenTypes.LITERAL_override);
                callbacks.invokeModifier(token);
            }
        }
    }
    
    /**
     * Extracts type reference as list of tokens.
     *
     * <p>Converts a Kotlin type reference into a list of {@link LocatableToken} instances
     * suitable for {@code JavaParserCallbacks.gotTypeSpec(List)}. The type reference text
     * may include:</p>
     * <ul>
     *   <li>Simple type: {@code String} → single token</li>
     *   <li>Qualified type: {@code kotlin.String} → NOT split, single token with full text</li>
     *   <li>Generic type: {@code List<String>} → single token with full text</li>
     *   <li>Nullable type: {@code String?} → single token with full text</li>
     * </ul>
     *
     * <p><b>Simplification Strategy:</b> Phase 4 treats type references as atomic tokens
     * rather than decomposing them into constituent parts. This is acceptable because
     * BlueJ's ClassInfo primarily needs the complete type string for signature matching.</p>
     *
     * <p><b>Future Enhancement:</b> Phase 5 or 6 may decompose complex types if needed
     * for more sophisticated type analysis.</p>
     *
     * @param typeRef The type reference to extract (must not be null)
     * @return List containing single token with complete type text
     */
    private List<LocatableToken> extractTypeTokens(KtTypeReference typeRef) {
        if (typeRef == null) {
            return List.of();
        }
        
        // Extract complete type text
        String typeText = typeRef.getText();
        if (typeText == null || typeText.isEmpty()) {
            return List.of();
        }
        
        // Create single token with complete type reference
        LocatableToken typeToken = createToken(typeRef, JavaTokenTypes.IDENT);
        return List.of(typeToken);
    }
    
    /**
     * Processes method-level generic type parameters.
     *
     * <p>Handles generic type parameter declarations like {@code <T : Number>} in:</p>
     * <pre>{@code
     * fun <T : Number> sum(values: List<T>): Double
     * }</pre>
     *
     * <p>This invokes the type parameter callback sequence:</p>
     * <ol>
     *   <li>{@code gotMethodTypeParamsBegin()} - Start type parameters</li>
     *   <li>For each type parameter:
     *     <ul>
     *       <li>{@code gotTypeParam(idToken)} - Type parameter name</li>
     *       <li>{@code gotTypeParamBound(tokens)} - Bound if present (e.g., {@code : Number})</li>
     *     </ul>
     *   </li>
     *   <li>{@code endMethodTypeParams()} - End type parameters</li>
     * </ol>
     *
     * @param typeParamList The type parameter list from function declaration
     */
    private void processMethodTypeParameters(KtTypeParameterList typeParamList) {
        if (callbacks == null || typeParamList == null) {
            return;
        }
        
        List<KtTypeParameter> typeParams = typeParamList.getParameters();
        if (typeParams.isEmpty()) {
            return;
        }
        
        callbacks.invokeMethodTypeParamsBegin();
        
        for (KtTypeParameter typeParam : typeParams) {
            // Type parameter name
            PsiElement nameIdentifier = typeParam.getNameIdentifier();
            if (nameIdentifier != null) {
                LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                callbacks.invokeTypeParam(nameToken);
            }
            
            // Type parameter bound (e.g., T : Number)
            KtTypeReference bound = typeParam.getExtendsBound();
            if (bound != null) {
                List<LocatableToken> boundTokens = extractTypeTokens(bound);
                callbacks.invokeTypeParamBound(boundTokens);
            }
        }
        
        callbacks.invokeEndMethodTypeParams();
    }
    
    /**
     * Processes method parameter list.
     *
     * <p>Invokes callbacks for each parameter in the function signature, including:</p>
     * <ul>
     *   <li>Parameter type via {@code gotTypeSpec()}</li>
     *   <li>Parameter name via {@code gotMethodParameter()}</li>
     *   <li>Vararg detection (ellipsis token for vararg parameters)</li>
     * </ul>
     *
     * <p>After all parameters are processed, invokes {@code gotAllMethodParameters()}.</p>
     *
     * <p><b>Vararg Handling:</b> Kotlin's {@code vararg} modifier is mapped to Java's
     * ellipsis (...) syntax. The ellipsis token is extracted from the parameter's
     * modifier list when vararg is present.</p>
     *
     * <p><b>Phase 4 Limitations:</b> Default parameter values are NOT processed
     * (would require expression parsing - Phase 6).</p>
     *
     * @param function The function whose parameters to process
     */
    private void processMethodParameters(KtNamedFunction function) {
        if (callbacks == null) {
            return;
        }
        
        List<KtParameter> params = function.getValueParameters();
        
        // Process each parameter
        for (KtParameter param : params) {
            // Parameter type
            KtTypeReference paramType = param.getTypeReference();
            if (paramType != null) {
                List<LocatableToken> typeTokens = extractTypeTokens(paramType);
                callbacks.invokeTypeSpec(typeTokens);
            }
            
            // Parameter name
            PsiElement nameIdentifier = param.getNameIdentifier();
            if (nameIdentifier != null) {
                LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                
                // Check for vararg modifier
                LocatableToken ellipsisToken = null;
                if (param.hasModifier(KtTokens.VARARG_KEYWORD)) {
                    // Extract vararg modifier as ellipsis token
                    KtModifierList paramModifiers = param.getModifierList();
                    if (paramModifiers != null) {
                        PsiElement varargModifier = paramModifiers.getModifier(KtTokens.VARARG_KEYWORD);
                        if (varargModifier != null) {
                            ellipsisToken = createToken(varargModifier, JavaTokenTypes.TRIPLE_DOT);
                        }
                    }
                }
                
                callbacks.invokeMethodParameter(nameToken, ellipsisToken);
            }
        }
        
        // All parameters processed
        callbacks.invokeAllMethodParameters();
    }
    
    /**
     * Processes method body boundaries without traversing contents.
     *
     * <p>Kotlin methods can have two body styles:</p>
     * <ul>
     *   <li><b>Block body:</b> {@code fun method() { statements }} - uses {@link KtNamedFunction#getBodyBlockExpression()}</li>
     *   <li><b>Expression body:</b> {@code fun method() = expression} - uses {@link KtNamedFunction#getBodyExpression()}</li>
     * </ul>
     *
     * <p>Abstract methods and interface methods without default implementation have no body.</p>
     *
     * <p><b>Phase 4 Behavior:</b> This method only marks body boundaries with
     * {@code beginMethodBody()} and {@code endMethodBody()} callbacks. Actual body
     * traversal (expressions/statements) is deferred to Phase 6.</p>
     *
     * @param function The function whose body to process
     */
    private void processMethodBody(KtNamedFunction function) {
        if (callbacks == null) {
            return;
        }
        
        if (function.hasBlockBody()) {
            // Block body: fun method() { ... }
            KtBlockExpression body = function.getBodyBlockExpression();
            if (body != null) {
                PsiElement lBrace = body.getLBrace();
                if (lBrace != null) {
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    callbacks.invokeBeginMethodBody(lBraceToken);
                }
                
                // Phase 4: Skip body traversal (expressions/statements - Phase 6)
                
                PsiElement rBrace = body.getRBrace();
                if (rBrace != null) {
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    callbacks.invokeEndMethodBody(rBraceToken, true);
                }
            }
        } else if (function.hasBody()) {
            // Expression body: fun method() = expr
            KtExpression body = function.getBodyExpression();
            if (body != null) {
                // Mark expression body boundaries
                LocatableToken bodyToken = createToken(body, JavaTokenTypes.ASSIGN);
                callbacks.invokeBeginMethodBody(bodyToken);
                
                // Phase 4: Skip expression traversal (Phase 6)
                
                callbacks.invokeEndMethodBody(bodyToken, true);
            }
        }
        // else: Abstract method or interface method - no body
    }
    /**
     * Processes constructor parameter list.
     *
     * <p>This helper method is shared between primary and secondary constructors to process
     * parameter declarations consistently. It handles:</p>
     * <ul>
     *   <li>Parameter types via {@code gotTypeSpec()}</li>
     *   <li>Parameter names via {@code gotMethodParameter()}</li>
     *   <li>Property parameters (val/var in primary constructor) - noted for Phase 4.3</li>
     * </ul>
     *
     * <p><b>Parameter Properties:</b> Primary constructor parameters with {@code val} or
     * {@code var} modifiers define both constructor parameters AND class properties. In Phase 4.2,
     * we process them as constructor parameters. The property aspect will be handled in
     * Milestone 4.3 when {@link #visitProperty(KtProperty)} is fully implemented.</p>
     *
     * <p><b>Delegation Strategy:</b> This method accepts both {@link KtPrimaryConstructor} and
     * {@link KtSecondaryConstructor} by using their common interface for accessing parameters.
     * Both constructor types share the same parameter list structure.</p>
     *
     * <p><b>Phase 4 Limitations:</b> Default parameter values are NOT processed
     * (would require expression parsing - Phase 6).</p>
     *
     * @param constructor The constructor whose parameters to process (primary or secondary)
     */
    private void processConstructorParameters(KtConstructor<?> constructor) {
        if (callbacks == null) {
            return;
        }
        
        List<KtParameter> params = constructor.getValueParameters();
        
        // Process each parameter
        for (KtParameter param : params) {
            // Parameter type
            KtTypeReference paramType = param.getTypeReference();
            if (paramType != null) {
                List<LocatableToken> typeTokens = extractTypeTokens(paramType);
                callbacks.invokeTypeSpec(typeTokens);
            }
            
            // Parameter name
            PsiElement nameIdentifier = param.getNameIdentifier();
            if (nameIdentifier != null) {
                LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                
                // Note: Constructors don't typically use vararg, but handle it for completeness
                // Kotlin allows vararg in constructors, though it's rare
                LocatableToken ellipsisToken = null;
                if (param.hasModifier(KtTokens.VARARG_KEYWORD)) {
                    KtModifierList paramModifiers = param.getModifierList();
                    if (paramModifiers != null) {
                        PsiElement varargModifier = paramModifiers.getModifier(KtTokens.VARARG_KEYWORD);
                        if (varargModifier != null) {
                            ellipsisToken = createToken(varargModifier, JavaTokenTypes.TRIPLE_DOT);
                        }
                    }
                }
                
                callbacks.invokeMethodParameter(nameToken, ellipsisToken);
            }
            
            // Note: If parameter has val/var, it also defines a property
            // Property visitor (Phase 4.3) will handle the field declaration aspect
            if (param.hasValOrVar()) {
                // This parameter creates a property - will be processed in visitProperty()
                // For Phase 4.2, we just process it as a constructor parameter
            }
        }
        
        // All parameters processed
        callbacks.invokeAllMethodParameters();
    }
    
    
    /**
     * Processes the superclass (extends).
     *
     * <p>Extracts the superclass name from the type reference and invokes
     * {@code beginTypeDefExtends} callback. The callback is invoked with a
     * token representing the superclass type reference.</p>
     *
     * <p><b>Type Name Cleaning:</b> The type reference text may include:</p>
     * <ul>
     *   <li>Type parameters: {@code Parent<T>} → cleaned to {@code Parent}</li>
     *   <li>Constructor call: {@code Parent()} → cleaned to {@code Parent}</li>
     * </ul>
     *
     * @param superClassEntry The supertype entry representing the superclass
     */
    private void processSuperClass(KtSuperTypeListEntry superClassEntry) {
        if (callbacks == null) return;
        
        // Get type reference
        KtTypeReference typeRef = superClassEntry.getTypeReference();
        if (typeRef == null) {
            return;
        }
        
        // Create token from the type reference
        LocatableToken extendsToken = createToken(typeRef, JavaTokenTypes.IDENT);
        
        // Invoke callbacks - begin/end pair for extends block
        callbacks.beginTypeDefExtends(extendsToken);
        callbacks.endTypeDefExtends();
        // Note: No individual type processing between begin/end
    }
    
    /**
     * Processes implemented interfaces.
     *
     * <p>Invokes {@code beginTypeDefImplements} callback with a token representing
     * the first interface type reference. The callback serves as a marker that
     * the class implements one or more interfaces.</p>
     *
     * <p><b>Single Callback:</b> Unlike individual interface processing, this
     * invokes a single {@code beginTypeDefImplements} callback regardless of
     * the number of interfaces. Individual interface name extraction is deferred
     * to later tasks.</p>
     *
     * @param interfaceEntries List of supertype entries representing interfaces
     */
    private void processInterfaces(List<KtSuperTypeListEntry> interfaceEntries) {
        if (callbacks == null) return;
        
        if (interfaceEntries.isEmpty()) {
            return;
        }
        
        // Begin implements block with first interface
        KtTypeReference firstInterface = interfaceEntries.get(0).getTypeReference();
        if (firstInterface != null) {
            LocatableToken implToken = createToken(firstInterface, JavaTokenTypes.IDENT);
            callbacks.beginTypeDefImplements(implToken);
            callbacks.endTypeDefImplements();
            // Note: No individual type processing between begin/end
        }
    }
    
    // TODO: Task 2.2.3 - Implement additional visitor methods (Phase 3):
    // - visitParameter (for function parameters)
    // - visitSuperTypeList (for inheritance)
    // - visitModifierList (for modifiers)
    // - visitTypeReference (for types)
}