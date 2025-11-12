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

import bluej.parser.JavaParserCallbacksBase;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.LineColPos;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import org.jetbrains.annotations.NotNull;

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
    private final JavaParserCallbacksAdapter callbacks;
    
    /**
     * Visitor state for scope tracking and modifier management.V
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

    private LocatableToken tokenBase = null;
    private int psiStartOffset = 0;

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
    public PsiCallbackVisitor(JavaParserCallbacksAdapter callbacks) {
        this.callbacks = callbacks;
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

        // callbacks.finishedCU(2); /// who the hell knows what that means xD
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

        // TODO: we may or may not need to handle enums specially
        // see: parseEnumConstants
        var isEnum = ktClass.isEnum();
        
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

            callbacks.gotDeclBegin(classToken);
            
            // 2. Process modifiers
            KtModifierList modifierList = ktClass.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }
            callbacks.modifiersConsumed();
            
            // 3. Type definition
            callbacks.gotTypeDef(classToken, tdType);

            // 4. Type name
            if (className != null && ktClass.getNameIdentifier() != null) {
                LocatableToken nameToken = createToken(ktClass.getNameIdentifier(), JavaTokenTypes.IDENT);
                callbacks.gotTypeDefName(nameToken);
            }
            
            // 5. Process supertypes
            processSuperTypes(ktClass);
            
            // Phase 4.2: Process primary constructor BEFORE type body (class header element)
            KtPrimaryConstructor primaryConstructor = ktClass.getPrimaryConstructor();
            if (primaryConstructor != null) {
                visitPrimaryConstructor(primaryConstructor);
            }

            LocatableToken finalToken = null;
            
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
                    
                    callbacks.beginTypeBody(lBraceToken);
                    
                    // 7. Visit nested declarations explicitly
                    // Note: Kotlin PSI visitor requires explicit iteration over body declarations
                    // super.visitClass() does NOT automatically recurse into nested classes/objects/members
                    // CRITICAL: Filter to KtClass AND exclude KtEnumEntry (enum constants, not classes)
                    for (KtDeclaration declaration : body.getDeclarations()) {
//                        if (declaration instanceof KtClass && !(declaration instanceof KtEnumEntry)) {
//                            declaration.accept(this);  // Triggers visitClass() for nested classes
//                        } else if (declaration instanceof KtObjectDeclaration) {
//                            declaration.accept(this);  // Triggers visitObjectDeclaration() for companion/nested objects
//                        } else if (declaration instanceof KtNamedFunction) {
//                            declaration.accept(this);  // Phase 4.1: Visit member functions
//                        } else if (declaration instanceof KtSecondaryConstructor) {
//                            declaration.accept(this);  // Phase 4.2: Visit secondary constructors
//                        } else if (declaration instanceof KtAnonymousInitializer) {
//                            declaration.accept(this);  // Phase 4.2: Visit init blocks
//                        } else if (declaration instanceof KtProperty) {
//                            declaration.accept(this);  // Phase 4.3: Visit properties
//                        }
                        declaration.accept(this);
                        // Note: Enum entries handled separately
                    }
                    
                    // 8. End type body with separate closing brace token
                    callbacks.endTypeBody(rBraceToken, true);

                    finalToken = rBraceToken;
                }
                // Note: If braces missing (malformed code), no nested declarations to visit
            }

            if (finalToken == null) {
                var lastChild = ktClass.getLastChild();

                finalToken = createToken(lastChild, JavaTokenTypes.EOF); // TODO: figure out how to get proper token type
            }
            
            // 9. End declaration
            callbacks.gotTypeDefEnd(finalToken, true);

        } finally {
            state.popScope();  // ALWAYS cleanup
        }
    }

    @Override
    public void visitEnumEntry(@NotNull KtEnumEntry enumEntry) {
        // TODO: blackhole enums for now
        var enumEntryName = enumEntry.getName();
//        super.visitEnumEntry(enumEntry);
    }

    /**
     * Visits a function declaration (method).
     *
     * <p>This method is invoked for a blackhole enums for now
     *         vall named function declarations including:</p>
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
            callbacks.gotDeclBegin(declToken);
            
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
                callbacks.gotTypeSpec(returnTypeTokens);
            }
            else {
                // This will be interpreted as unit and hopefully not blow up anything
                callbacks.gotTypeSpec(null);
            }

            // 4. Method declaration (name + javadoc)
            PsiElement nameIdentifier = function.getNameIdentifier();
            if (nameIdentifier != null && functionName != null) {
                LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                
                // TODO Phase 4.4: Extract KDoc comments as javadocToken
                // KDoc format: /** ... */ above function declaration
                // Use KtDeclaration.getDocComment() for extraction
                // Map KDoc structure to LocatableToken (requires KDoc→JavaDoc conversion)
                LocatableToken javadocToken = null;  // Intentionally null until Phase 4.4
                
                callbacks.gotMethodDeclaration(nameToken, javadocToken);
            }
            
            // 5. Modifiers consumed
            callbacks.modifiersConsumed();
            
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
            LocatableToken endToken = processMethodBody(function);
            
            // 10. End declaration
            callbacks.endMethodDecl(endToken, true);
            
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
            callbacks.gotDeclBegin(declToken);

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

            callbacks.gotConstructorDecl(nameToken, javadocToken);

            // 4. Modifiers consumed
            callbacks.modifiersConsumed();

            // 5. Parameters (including property parameters with val/var)
            processConstructorParameters(constructor);

            // 6. End declaration (primary constructors have no explicit body)
            LocatableToken endToken = createToken(constructor, JavaTokenTypes.LITERAL_void);
            callbacks.endMethodDecl(endToken, true);

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
            callbacks.gotDeclBegin(declToken);
            
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
            
            callbacks.gotConstructorDecl(nameToken, javadocToken);
            
            // 4. Modifiers consumed
            callbacks.modifiersConsumed();
            
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
                    callbacks.beginMethodBody(lBraceToken);
                }
                
                // Phase 6: Traverse constructor body via visitor
                body.accept(this);
                
                PsiElement rBrace = body.getRBrace();
                if (rBrace != null) {
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    callbacks.endMethodBody(rBraceToken, true);
                }
            }
            
            // 8. End declaration
            LocatableToken endToken = createToken(constructor, JavaTokenTypes.LITERAL_void);
            callbacks.endMethodDecl(endToken, true);
            
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
                LocatableToken initToken = createToken(initKeyword, JavaTokenTypes.LITERAL_init);

                callbacks.gotDeclBegin(initToken);

                PsiElement lBrace = body.getLBrace();
                PsiElement rBrace = body.getRBrace();

                if (lBrace != null && rBrace != null) {
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    callbacks.beginInitBlock(initToken, lBraceToken);

                    // Phase 6: Traverse init block body via visitor
                    body.accept(this);

                    // 2. End init block
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    callbacks.endInitBlock(rBraceToken, true);
                }
                else {
                    callbacks.endDecl(initToken);
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
     * the field declaration pattern from {@link JavaParserCallbacksBase}:</p>
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
            callbacks.gotDeclBegin(propertyToken);
            callbacks.beginFieldDeclarations(propertyToken);
            
            // 2. Process modifiers
            KtModifierList modifierList = property.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }
            
            // 3. Property type
            LocatableToken typeToken = processPropertyType(property);
            if (typeToken != null) {
                List<LocatableToken> typeTokens = List.of(typeToken);
                 callbacks.gotTypeSpec(typeTokens);
            }
            
            // 4. Field declaration
            PsiElement nameIdentifier = property.getNameIdentifier();
            if (nameIdentifier != null && propertyName != null) {
                LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                boolean hasInitializer = property.hasInitializer();
                callbacks.gotField(propertyToken, nameToken, hasInitializer);
            }
            
            // 5. End field
            callbacks.endField(propertyToken, true);
            
            // 6. End field declarations
            callbacks.endFieldDeclarations(propertyToken, true);

            // TODO: that needs to be done only on failure
            // callbacks.endDecl(propertyToken);

            // Visit custom accessors if present (custom getter/setter bodies)
            // TODO?
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
            callbacks.gotDeclBegin(objectToken);
            
            // 2. Process modifiers (objects can have visibility modifiers)
            KtModifierList modifierList = declaration.getModifierList();
            if (modifierList != null) {
                processModifiers(modifierList);
            }
            callbacks.modifiersConsumed();
            
            // 3. Type definition - objects are mapped to classes
            callbacks.gotTypeDef(objectToken, JavaTokenTypes.LITERAL_class);
            
            // 4. Object name (or "Companion" for companion objects)
            String name = declaration.getName();
            if (name != null && declaration.getNameIdentifier() != null) {
                // Named object or named companion
                LocatableToken nameToken = createToken(declaration.getNameIdentifier(), JavaTokenTypes.IDENT);
                callbacks.gotTypeDefName(nameToken);
            } else if (declaration.isCompanion()) {
                // Companion object without explicit name - use "Companion" as synthetic name
                LocatableToken nameToken = createTokenWithText(declaration, "Companion", JavaTokenTypes.IDENT);
                callbacks.gotTypeDefName(nameToken);
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
                    callbacks.beginTypeBody(lBraceToken);
                    
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
                    callbacks.endTypeBody(rBraceToken, true);
                }
            }
            
            // 9. End declaration
            callbacks.gotTypeDefEnd(objectToken, true);
            
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

        if (tokenBase != null) {
            if (startLine == 1) { startColumn += tokenBase.getColumn() - 1; };
            if (endLine == 1) { endColumn += tokenBase.getColumn() - 1; };

            startLine += tokenBase.getLine() - 1;
            endLine += tokenBase.getLine() - 1;
            startOffset += tokenBase.getPosition();
            endOffset += tokenBase.getPosition();
        }
        
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
            callbacks.gotModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.PRIVATE_KEYWORD),
                JavaTokenTypes.LITERAL_private
            );
            callbacks.gotModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.PROTECTED_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.PROTECTED_KEYWORD),
                JavaTokenTypes.LITERAL_protected
            );
            callbacks.gotModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
            // Kotlin 'internal' modifier - module visibility
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.INTERNAL_KEYWORD),
                JavaTokenTypes.LITERAL_internal
            );
            callbacks.gotModifier(token);
        }
        
        // Process inheritance modifiers
        if (modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.ABSTRACT_KEYWORD),
                JavaTokenTypes.ABSTRACT
            );
            callbacks.gotModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.FINAL_KEYWORD)) {
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.FINAL_KEYWORD),
                JavaTokenTypes.FINAL
            );
            callbacks.gotModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.OPEN_KEYWORD)) {
            // Kotlin 'open' modifier - allows inheritance (opposite of final)
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.OPEN_KEYWORD),
                JavaTokenTypes.LITERAL_open
            );
            callbacks.gotModifier(token);
        }
        
        // Process property-specific modifiers (Phase 4.3)
        if (modifierList.hasModifier(KtTokens.LATEINIT_KEYWORD)) {
            // Kotlin 'lateinit' modifier - late initialization for var properties
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.LATEINIT_KEYWORD),
                JavaTokenTypes.LITERAL_lateinit
            );
            callbacks.gotModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.CONST_KEYWORD)) {
            // Kotlin 'const' modifier - compile-time constant
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.CONST_KEYWORD),
                JavaTokenTypes.LITERAL_const
            );
            callbacks.gotModifier(token);
        }
        
        if (modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            // Kotlin 'override' modifier - applies to methods and properties
            LocatableToken token = createToken(
                modifierList.getModifier(KtTokens.OVERRIDE_KEYWORD),
                JavaTokenTypes.LITERAL_override
            );
            callbacks.gotModifier(token);
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
                callbacks.gotModifier(token);
            }
        }
        
        // Infix function
        if (modifiers.hasModifier(KtTokens.INFIX_KEYWORD)) {
            PsiElement infixModifier = modifiers.getModifier(KtTokens.INFIX_KEYWORD);
            if (infixModifier != null) {
                LocatableToken token = createToken(infixModifier, JavaTokenTypes.LITERAL_infix);
                callbacks.gotModifier(token);
            }
        }
        
        // Inline function
        if (modifiers.hasModifier(KtTokens.INLINE_KEYWORD)) {
            PsiElement inlineModifier = modifiers.getModifier(KtTokens.INLINE_KEYWORD);
            if (inlineModifier != null) {
                LocatableToken token = createToken(inlineModifier, JavaTokenTypes.LITERAL_inline);
                callbacks.gotModifier(token);
            }
        }
        
        // Suspend function
        if (modifiers.hasModifier(KtTokens.SUSPEND_KEYWORD)) {
            PsiElement suspendModifier = modifiers.getModifier(KtTokens.SUSPEND_KEYWORD);
            if (suspendModifier != null) {
                LocatableToken token = createToken(suspendModifier, JavaTokenTypes.LITERAL_suspend);
                callbacks.gotModifier(token);
            }
        }
        
        // Override modifier (method-specific context)
        if (modifiers.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            PsiElement overrideModifier = modifiers.getModifier(KtTokens.OVERRIDE_KEYWORD);
            if (overrideModifier != null) {
                LocatableToken token = createToken(overrideModifier, JavaTokenTypes.LITERAL_override);
                callbacks.gotModifier(token);
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

        // TODO: pretend we have primitives for now, as some existing tests assume that to check for method existence
        var tokenType = switch (typeText) {
            case "Byte" -> JavaTokenTypes.LITERAL_byte;
            case "Short" -> JavaTokenTypes.LITERAL_short;
            case "Int" -> JavaTokenTypes.LITERAL_int;
            case "Long" -> JavaTokenTypes.LITERAL_long;

            case "Float" -> JavaTokenTypes.LITERAL_float;
            case "Double" -> JavaTokenTypes.LITERAL_double;

            case "Boolean" -> JavaTokenTypes.LITERAL_boolean;

            case "Char" -> JavaTokenTypes.LITERAL_char;

            default -> JavaTokenTypes.IDENT;
        };
        
        // Create single token with complete type reference
        LocatableToken typeToken = createToken(typeRef, tokenType);
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
        
        callbacks.gotMethodTypeParamsBegin();
        
        for (KtTypeParameter typeParam : typeParams) {
            // Type parameter name
            PsiElement nameIdentifier = typeParam.getNameIdentifier();
            if (nameIdentifier != null) {
                LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                callbacks.gotTypeParam(nameToken);
            }
            
            // Type parameter bound (e.g., T : Number)
            KtTypeReference bound = typeParam.getExtendsBound();
            if (bound != null) {
                List<LocatableToken> boundTokens = extractTypeTokens(bound);
                callbacks.gotTypeParamBound(boundTokens);
            }
        }
        
        callbacks.endMethodTypeParams();
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
                callbacks.gotTypeSpec(typeTokens);
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
                
                callbacks.gotMethodParameter(nameToken, ellipsisToken);
            }
        }
        
        // All parameters processed
        callbacks.gotAllMethodParameters();
    }
    
    /**
     * Processes method body with full statement and expression traversal.
     *
     * <p>Kotlin methods can have two body styles:</p>
     * <ul>
     *   <li><b>Block body:</b> {@code fun method() { statements }} - uses {@link KtNamedFunction#getBodyBlockExpression()}</li>
     *   <li><b>Expression body:</b> {@code fun method() = expression} - uses {@link KtNamedFunction#getBodyExpression()}</li>
     * </ul>
     *
     * <p>Abstract methods and interface methods without default implementation have no body.</p>
     *
     * <p><b>Phase 6 Behavior:</b> This method marks body boundaries with
     * {@code beginMethodBody()} and {@code endMethodBody()} callbacks AND traverses
     * the body contents via visitor pattern.</p>
     *
     * @param function The function whose body to process
     */
    private LocatableToken processMethodBody(KtNamedFunction function) {
        if (callbacks == null) {
            return null;
        }

        LocatableToken lastToken = null;
        
        if (function.hasBlockBody()) {
            // Block body: fun method() { ... }
            KtBlockExpression body = function.getBodyBlockExpression();
            if (body != null) {
                PsiElement lBrace = body.getLBrace();
                if (lBrace != null) {
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    callbacks.beginMethodBody(lBraceToken);
                }
                
                // Phase 6: Traverse body statements via visitor
                body.accept(this);
                
                PsiElement rBrace = body.getRBrace();
                if (rBrace != null) {
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    lastToken = rBraceToken;
                    callbacks.endMethodBody(rBraceToken, true);
                }
            }
        } else if (function.hasBody()) {
            // Expression body: fun method() = expr
            KtExpression body = function.getBodyExpression();
            if (body != null) {
                // Mark expression body boundaries
                LocatableToken bodyToken = createToken(body, JavaTokenTypes.ASSIGN);
                callbacks.beginMethodBody(bodyToken);
                
                // Phase 6: Traverse expression via visitor
                body.accept(this);

                lastToken = createToken(body.getLastChild(), JavaTokenTypes.LITERAL_void);

                callbacks.endMethodBody(bodyToken, true);
            }
        }
        else {
            // else: Abstract method or interface method - no body
            lastToken = createToken(function.getLastChild(), JavaTokenTypes.LITERAL_void);
        }

        return lastToken;
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
                callbacks.gotTypeSpec(typeTokens);
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
                
                callbacks.gotMethodParameter(nameToken, ellipsisToken);
            }
            
            // Note: If parameter has val/var, it also defines a property
            // Property visitor (Phase 4.3) will handle the field declaration aspect
            if (param.hasValOrVar()) {
                // This parameter creates a property - will be processed in visitProperty()
                // For Phase 4.2, we just process it as a constructor parameter
            }
        }
        
        // All parameters processed
        callbacks.gotAllMethodParameters();
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

    public void setTokenBase(LocatableToken currentToken) {
        if (currentToken.getHiddenBefore() != null) {
            currentToken = currentToken.getHiddenBefore();
        }

        if (currentToken.getPosition() > 0) {
            this.tokenBase = currentToken;
        }
    }

    public void setTokenBase(LineColPos position) {
        if (position.position() > 0) {
            this.tokenBase = new LocatableToken(
                JavaTokenTypes.LITERAL_void,
                "",
                position,
                position
            );
        }
    }

    public LocatableToken getTokenBase() {
        if (this.tokenBase != null) { return this.tokenBase; }

        return new LocatableToken(
            JavaTokenTypes.LITERAL_void,
            "",
            new LineColPos(1, 1, 0),
            new LineColPos(1, 1, 0)
        );
    }

    public void setEmitRangeStart(LocatableToken currentToken) {
        this.callbacks.setEmitRangeStart(currentToken);
        var offset = this.getTokenBase().getPosition();

        if (offset > 0) {
            this.psiStartOffset = currentToken.getPosition() - offset;
        }
    }

    public int getPsiStartOffset() {
        return this.psiStartOffset;
//        return this.getTokenBase().getPosition();
    }

     // ==================== Phase 6 Statement and Expression Visitor Methods ====================
    
     /**
      * Visits a block expression ({ statements }).
      *
      * <p>Block expressions are the primary containers for statements in Kotlin. They appear in:</p>
      * <ul>
      *   <li>Method bodies: {@code fun method() { statements }}</li>
      *   <li>Init blocks: {@code init { statements }}</li>
      *   <li>Control flow bodies: {@code if (x) { statements }}</li>
      *   <li>Lambda bodies: {@code { param -> statements }}</li>
      * </ul>
      *
      * <h3>Phase 6.1 Task 1: Block Expression Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginStmtblockBody(lBrace)} - Begin block</li>
      *   <li>For each statement:
      *     <ul>
      *       <li>{@code beginElement(statement)} - Begin statement</li>
      *       <li>Statement visitor (recursive)</li>
      *       <li>{@code endElement(statement, true)} - End statement</li>
      *     </ul>
      *   </li>
      *   <li>{@code endStmtblockBody(rBrace, true)} - End block</li>
      * </ol>
      *
      * <p><b>Statement Wrapping Pattern:</b> Following JavaParser convention from
      * {@link JavaParser#parseStmtBlock()}, each statement is wrapped with
      * {@code beginElement}/{@code endElement} callbacks.</p>
      *
      * @param block The block expression PSI element
      */
     @Override
     public void visitBlockExpression(@NotNull KtBlockExpression block) {
         if (block == null || callbacks == null) {
             return;
         }

         // TODO: this is also called as a part of parsing method body, where
         //       we don't want to call the statement block callbacks
         //       I think the proper solution would be to split this into separate visitors
        
         // 1. Begin statement block
         PsiElement lBrace = block.getLBrace();
         if (lBrace != null && !(block.getParent() instanceof KtFunction)) {
             LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
             callbacks.beginStmtblockBody(lBraceToken);
         }
        
         // 2. Visit each statement in the block
         List<KtExpression> statements = block.getStatements();
         for (KtExpression statement : statements) {
             // Wrap each statement with beginElement/endElement
             callbacks.beginElement(createToken(statement));
            
             // Recursively visit the statement
             statement.accept(this);
            
             // End statement successfully
             callbacks.endElement(createToken(statement), true);
         }
        
         // 3. End statement block
         PsiElement rBrace = block.getRBrace();
         if (rBrace != null && !(block.getParent() instanceof KtFunction)) {
             LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
             callbacks.endStmtblockBody(rBraceToken, true);
         }
     }
    
     /**
      * Visits an if expression (if/else statement or expression).
      *
      * <p>Kotlin if expressions can be used as both statements and expressions:</p>
      * <pre>{@code
      * // As statement:
      * if (x > 0) { println("positive") } else { println("negative") }
      *
      * // As expression:
      * val result = if (x > 0) "positive" else "negative"
      * }</pre>
      *
      * <p>Both forms use the same callback sequence - BlueJ doesn't distinguish
      * between statement and expression usage.</p>
      *
      * <h3>Phase 6.1 Task 2: If Expression Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginIfStmt(ifToken)} - Begin if statement</li>
      *   <li>Condition expression traversal</li>
      *   <li>{@code beginIfCondBlock(thenToken)} - Begin then block</li>
      *   <li>Then branch traversal</li>
      *   <li>{@code endIfCondBlock(thenToken, true)} - End then block</li>
      *   <li>For each else-if:
      *     <ul>
      *       <li>{@code gotElseIf(elseToken)} - Else-if marker</li>
      *       <li>Recursively process as if statement</li>
      *     </ul>
      *   </li>
      *   <li>For else branch (if present):
      *     <ul>
      *       <li>{@code beginIfCondBlock(elseToken)} - Begin else block</li>
      *       <li>Else branch traversal</li>
      *       <li>{@code endIfCondBlock(elseToken, true)} - End else block</li>
      *     </ul>
      *   </li>
      *   <li>{@code endIfStmt(endToken, true)} - End if statement</li>
      * </ol>
      *
      * <p><b>Else-If Chain Handling:</b> Kotlin else-if is represented as nested if expressions.
      * We detect this pattern and emit {@code gotElseIf} callback to match JavaParser behavior.</p>
      *
      * @param ifExpr The if expression PSI element
      */
     @Override
     public void visitIfExpression(@NotNull KtIfExpression ifExpr) {
         if (ifExpr == null || callbacks == null) {
             return;
         }
        
         // 1. Begin if statement
         PsiElement ifKeyword = ifExpr.getIfKeyword();
         if (ifKeyword != null) {
             LocatableToken ifToken = createToken(ifKeyword, JavaTokenTypes.LITERAL_if);
             callbacks.beginIfStmt(ifToken);
         }
        
         // 2. Parse condition expression
         KtExpression condition = ifExpr.getCondition();
         if (condition != null) {
             condition.accept(this);
         }
        
         // 3. Parse then branch
         KtExpression thenBranch = ifExpr.getThen();
         if (thenBranch != null) {
             LocatableToken thenToken = createToken(thenBranch);
             callbacks.beginIfCondBlock(thenToken);
             thenBranch.accept(this);
             callbacks.endIfCondBlock(thenToken, true);
         }
        
         // 4. Parse else branch (if present)
         KtExpression elseBranch = ifExpr.getElse();
         if (elseBranch != null) {
             PsiElement elseKeyword = ifExpr.getElseKeyword();
            
             // Check if else branch is another if expression (else-if chain)
             if (elseBranch instanceof KtIfExpression) {
                 // Emit gotElseIf marker and recursively process
                 if (elseKeyword != null) {
                     LocatableToken elseToken = createToken(elseKeyword, JavaTokenTypes.LITERAL_else);
                     callbacks.gotElseIf(elseToken);
                 }
                 // Recursively visit the nested if expression
                 elseBranch.accept(this);
             } else {
                 // Regular else block
                 LocatableToken elseToken = createToken(elseBranch);
                 callbacks.beginIfCondBlock(elseToken);
                 elseBranch.accept(this);
                 callbacks.endIfCondBlock(elseToken, true);
             }
         }
        
         // 5. End if statement
         LocatableToken endToken = createToken(ifExpr.getLastChild());
         callbacks.endIfStmt(endToken, true);
     }
    
     /**
      * Visits a return expression (return statement).
      *
      * <p>Kotlin return expressions can return values or return from labeled lambda/function:</p>
      * <pre>{@code
      * return              // Unit return (no value)
      * return 42           // Return with value
      * return@label expr   // Return from labeled function
      * }</pre>
      *
      * <h3>Phase 6.1 Task 3: Return Statement Callback Sequence</h3>
      * <ol>
      *   <li>{@code gotReturnStatement(hasValue)} - Return statement marker</li>
      *   <li>If hasValue: Expression traversal for return value</li>
      * </ol>
      *
      * <p><b>Labeled Returns:</b> Kotlin supports labeled returns ({@code return@label}).
      * The label information is not passed to callbacks in Phase 6.1 - it's part of
      * the return expression structure but not separately reported.</p>
      *
      * @param returnExpr The return expression PSI element
      */
     @Override
     public void visitReturnExpression(@NotNull KtReturnExpression returnExpr) {
         if (returnExpr == null || callbacks == null) {
             return;
         }
        
         // Check if return has a value
         KtExpression returnValue = returnExpr.getReturnedExpression();
         boolean hasValue = returnValue != null;
        
         // 1. Emit return statement callback
         callbacks.gotReturnStatement(hasValue);
        
         // 2. If has value, traverse the return expression
         if (hasValue) {
             returnValue.accept(this);
         }
     }
    
     /**
      * Visits a throw expression (throw statement).
      *
      * <p>Kotlin throw expressions throw exceptions:</p>
      * <pre>{@code
      * throw IllegalArgumentException("Invalid value")
      * }</pre>
      *
      * <h3>Phase 6.4: Throw Expression Callback Sequence</h3>
      * <ol>
      *   <li>{@code gotThrow(throwToken)} - Throw statement marker</li>
      *   <li>Exception expression traversal</li>
      * </ol>
      *
      * @param throwExpr The throw expression PSI element
      */
     @Override
     public void visitThrowExpression(@NotNull KtThrowExpression throwExpr) {
         if (throwExpr == null || callbacks == null) {
             return;
         }
        
         // 1. Get throw keyword
         PsiElement throwKeyword = throwExpr.getFirstChild();
         if (throwKeyword != null) {
             LocatableToken throwToken = createToken(throwKeyword, JavaTokenTypes.LITERAL_throw);
             callbacks.gotThrow(throwToken);
         }
        
         // 2. Traverse the thrown expression
         KtExpression thrownExpr = throwExpr.getThrownExpression();
         if (thrownExpr != null) {
             thrownExpr.accept(this);
         }
     }
    
     /**
      * Visits a break expression (break statement).
      *
      * <p>Kotlin break expressions can break from loops with optional labels:</p>
      * <pre>{@code
      * break           // Break from innermost loop
      * break@outer     // Break from labeled loop
      * }</pre>
      *
      * <h3>Phase 6.2 Task 4: Break Statement Callback</h3>
      * <p>Emits {@code gotBreakContinue(keywordToken, labelToken)} with:</p>
      * <ul>
      *   <li>keywordToken: The "break" keyword</li>
      *   <li>labelToken: The label if present, or null</li>
      * </ul>
      *
      * @param breakExpr The break expression PSI element
      */
     @Override
     public void visitBreakExpression(@NotNull KtBreakExpression breakExpr) {
         if (breakExpr == null || callbacks == null) {
             return;
         }
        
         // Get break keyword
         PsiElement breakKeyword = breakExpr.getFirstChild();
         LocatableToken keywordToken = null;
         if (breakKeyword != null) {
             keywordToken = createToken(breakKeyword, JavaTokenTypes.LITERAL_break);
         }
        
         // Get label if present
         LocatableToken labelToken = null;
         String labelName = breakExpr.getLabelName();
         if (labelName != null) {
             // Create token for label (label appears after @)
             labelToken = createTokenWithText(breakExpr, labelName, JavaTokenTypes.IDENT);
         }
        
         callbacks.gotBreakContinue(keywordToken, labelToken);
     }
    
     /**
      * Visits a continue expression (continue statement).
      *
      * <p>Kotlin continue expressions can continue loops with optional labels:</p>
      * <pre>{@code
      * continue           // Continue to next iteration of innermost loop
      * continue@outer     // Continue to next iteration of labeled loop
      * }</pre>
      *
      * <h3>Phase 6.2 Task 4: Continue Statement Callback</h3>
      * <p>Emits {@code gotBreakContinue(keywordToken, labelToken)} with:</p>
      * <ul>
      *   <li>keywordToken: The "continue" keyword</li>
      *   <li>labelToken: The label if present, or null</li>
      * </ul>
      *
      * @param continueExpr The continue expression PSI element
      */
     @Override
     public void visitContinueExpression(@NotNull KtContinueExpression continueExpr) {
         if (continueExpr == null || callbacks == null) {
             return;
         }
        
         // Get continue keyword
         PsiElement continueKeyword = continueExpr.getFirstChild();
         LocatableToken keywordToken = null;
         if (continueKeyword != null) {
             keywordToken = createToken(continueKeyword, JavaTokenTypes.LITERAL_continue);
         }
        
         // Get label if present
         LocatableToken labelToken = null;
         String labelName = continueExpr.getLabelName();
         if (labelName != null) {
             // Create token for label (label appears after @)
             labelToken = createTokenWithText(continueExpr, labelName, JavaTokenTypes.IDENT);
         }
        
         callbacks.gotBreakContinue(keywordToken, labelToken);
     }
    
     /**
      * Visits a for loop expression (for-in loop).
      *
      * <p>Kotlin for loops are for-each style only:</p>
      * <pre>{@code
      * for (item in collection) {
      *     println(item)
      * }
      * }</pre>
      *
      * <h3>Phase 6.2 Task 1: For Loop Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginForLoop(forToken)} - Begin for loop</li>
      *   <li>{@code beginForInitDecl(forToken)} - Begin loop variable declaration</li>
      *   <li>{@code gotTypeSpec()} - Loop variable type (if specified)</li>
      *   <li>{@code gotForInit(forToken, idToken)} - Loop variable name</li>
      *   <li>{@code endForInit(idToken, true)} - End variable declaration</li>
      *   <li>{@code endForInitDecls(idToken, true)} - End declarations</li>
      *   <li>{@code gotForTest(true)} - Test expression marker (always present for in)</li>
      *   <li>Range expression traversal</li>
      *   <li>{@code gotForIncrement(false)} - No increment in for-in loops</li>
      *   <li>{@code beginForLoopBody(bodyToken)} - Begin body</li>
      *   <li>Body traversal</li>
      *   <li>{@code endForLoopBody(bodyToken, true)} - End body</li>
      *   <li>{@code endForLoop(endToken, true)} - End for loop</li>
      * </ol>
      *
      * @param forExpr The for loop expression PSI element
      */
     @Override
     public void visitForExpression(@NotNull KtForExpression forExpr) {
         if (forExpr == null || callbacks == null) {
             return;
         }
        
         // 1. Begin for loop
         PsiElement forKeyword = forExpr.getFirstChild();
         LocatableToken forToken = createToken(forKeyword != null ? forKeyword : forExpr, JavaTokenTypes.LITERAL_for);
         callbacks.beginForLoop(forToken);
        
         // 2. Process loop parameter (variable declaration)
         KtParameter loopParam = forExpr.getLoopParameter();
         if (loopParam != null) {
             callbacks.beginForInitDecl(forToken);
            
             // Type if specified
             KtTypeReference paramType = loopParam.getTypeReference();
             if (paramType != null) {
                 List<LocatableToken> typeTokens = extractTypeTokens(paramType);
                 callbacks.gotTypeSpec(typeTokens);
             }
            
             // Variable name
             PsiElement nameIdentifier = loopParam.getNameIdentifier();
             if (nameIdentifier != null) {
                 LocatableToken idToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                 callbacks.gotForInit(forToken, idToken);
                 callbacks.endForInit(idToken, true);
             }
            
             callbacks.endForInitDecls(forToken, true);
             callbacks.modifiersConsumed();
         }

         // TODO: should we treat range loops differently? I'm not sure
         callbacks.determinedForLoop(true, false);
        
         // 3. For-in always has a test expression (the range/collection)
//         callbacks.gotForTest(true);
//         KtExpression loopRange = forExpr.getLoopRange();
//         if (loopRange != null) {
//             loopRange.accept(this);
//         }
        
         // 4. Kotlin for-in has no increment expression
//         callbacks.gotForIncrement(false);
        
         // 5. Parse loop body
         KtExpression body = forExpr.getBody();
         if (body != null) {
             LocatableToken openToken = createToken(body.getFirstChild());
             callbacks.beginForLoopBody(openToken);

             body.accept(this);

             LocatableToken closeToken = createToken(body.getLastChild());
             callbacks.endForLoopBody(closeToken, true);
         }
        
         // 6. End for loop
         LocatableToken endToken = createToken(forExpr.getLastChild());
         callbacks.endForLoop(endToken, true);
     }
    
     /**
      * Visits a while loop expression.
      *
      * <p>Kotlin while loops have standard while syntax:</p>
      * <pre>{@code
      * while (x > 0) {
      *     x--
      * }
      * }</pre>
      *
      * <h3>Phase 6.2 Task 2: While Loop Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginWhileLoop(whileToken)} - Begin while loop</li>
      *   <li>Condition expression traversal</li>
      *   <li>{@code beginWhileLoopBody(bodyToken)} - Begin body</li>
      *   <li>Body traversal</li>
      *   <li>{@code endWhileLoopBody(bodyToken, true)} - End body</li>
      *   <li>{@code endWhileLoop(endToken, true)} - End while loop</li>
      * </ol>
      *
      * @param whileExpr The while loop expression PSI element
      */
     @Override
     public void visitWhileExpression(@NotNull KtWhileExpression whileExpr) {
         if (whileExpr == null || callbacks == null) {
             return;
         }
        
         // 1. Begin while loop
         PsiElement whileKeyword = whileExpr.getFirstChild();
         LocatableToken whileToken = createToken(whileKeyword != null ? whileKeyword : whileExpr, JavaTokenTypes.LITERAL_while);
         callbacks.beginWhileLoop(whileToken);
        
         // 2. Parse condition
         KtExpression condition = whileExpr.getCondition();
         if (condition != null) {
             condition.accept(this);
         }
        
         // 3. Parse body
         KtExpression body = whileExpr.getBody();
         if (body != null) {
             LocatableToken bodyToken = createToken(body);
             callbacks.beginWhileLoopBody(bodyToken);
             body.accept(this);
             callbacks.endWhileLoopBody(bodyToken, true);
         }
        
         // 4. End while loop
         LocatableToken endToken = createToken(whileExpr.getLastChild());
         callbacks.endWhileLoop(endToken, true);
     }
    
     /**
      * Visits a do-while loop expression.
      *
      * <p>Kotlin do-while loops have standard do-while syntax:</p>
      * <pre>{@code
      * do {
      *     x--
      * } while (x > 0)
      * }</pre>
      *
      * <h3>Phase 6.2 Task 3: Do-While Loop Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginDoWhile(doToken)} - Begin do-while loop</li>
      *   <li>{@code beginDoWhileBody(bodyToken)} - Begin body</li>
      *   <li>Body traversal</li>
      *   <li>{@code endDoWhileBody(bodyToken, true)} - End body</li>
      *   <li>Condition expression traversal</li>
      *   <li>{@code endDoWhile(endToken, true)} - End do-while loop</li>
      * </ol>
      *
      * @param doWhileExpr The do-while loop expression PSI element
      */
     @Override
     public void visitDoWhileExpression(@NotNull KtDoWhileExpression doWhileExpr) {
         if (doWhileExpr == null || callbacks == null) {
             return;
         }
        
         // 1. Begin do-while loop
         PsiElement doKeyword = doWhileExpr.getFirstChild();
         LocatableToken doToken = createToken(doKeyword != null ? doKeyword : doWhileExpr, JavaTokenTypes.LITERAL_do);
         callbacks.beginDoWhile(doToken);
        
         // 2. Parse body first (do-while executes body before condition)
         KtExpression body = doWhileExpr.getBody();
         if (body != null) {
             LocatableToken bodyToken = createToken(body);
             callbacks.beginDoWhileBody(bodyToken);
             body.accept(this);
             callbacks.endDoWhileBody(bodyToken, true);
         }
        
         // 3. Parse condition
         KtExpression condition = doWhileExpr.getCondition();
         if (condition != null) {
             condition.accept(this);
         }
        
         // 4. End do-while loop
         LocatableToken endToken = createToken(doWhileExpr.getLastChild());
         callbacks.endDoWhile(endToken, true);
     }
     /**
      * Visits a when expression (Kotlin's enhanced switch).
      *
      * <p>Kotlin when expressions are more powerful than Java switch statements:</p>
      * <pre>{@code
      * when (x) {
      *     1, 2 -> println("one or two")      // Multi-condition
      *     in 3..10 -> println("range")       // Range check
      *     is String -> println("string")     // Type check
      *     else -> println("other")           // Else clause
      * }
      * }</pre>
      *
      * <h3>Phase 6.3 Tasks 1-3: When Expression Callback Sequence</h3>
      * <p>Maps Kotlin when to Java switch callbacks:</p>
      * <ol>
      *   <li>{@code beginSwitchStmt(whenToken, false)} - Begin switch (not switch expression)</li>
      *   <li>Subject expression traversal (if present)</li>
      *   <li>{@code beginSwitchBlock(lBrace)} - Begin switch block</li>
      *   <li>For each when entry:
      *     <ul>
      *       <li>{@code beginSwitchCase(entryToken)} - Begin case</li>
      *       <li>Condition expression(s) traversal</li>
      *       <li>{@code gotSwitchCaseType(arrow, true)} - Arrow syntax marker</li>
      *       <li>Case body traversal</li>
      *       <li>{@code endSwitchCase(arrow, true)} - End case</li>
      *     </ul>
      *   </li>
      *   <li>For else entry: {@code gotSwitchDefault()} - Default case</li>
      *   <li>{@code endSwitchBlock(rBrace)} - End switch block</li>
      *   <li>{@code endSwitchStmt(rBrace, true)} - End switch</li>
      * </ol>
      *
      * @param whenExpr The when expression PSI element
      */
     @Override
     public void visitWhenExpression(@NotNull KtWhenExpression whenExpr) {
         if (whenExpr == null || callbacks == null) {
             return;
         }
        
         // 1. Begin switch statement (when maps to switch)
         PsiElement whenKeyword = whenExpr.getFirstChild();
         LocatableToken whenToken = createToken(whenKeyword != null ? whenKeyword : whenExpr, JavaTokenTypes.LITERAL_switch);
         callbacks.beginSwitchStmt(whenToken, false);
        
         // 2. Parse subject expression (if present)
         KtExpression subject = whenExpr.getSubjectExpression();
         if (subject != null) {
             subject.accept(this);
         }
        
         // 3. Begin switch block
         LocatableToken lBraceToken = createToken(whenExpr, JavaTokenTypes.LCURLY);
         callbacks.beginSwitchBlock(lBraceToken);
        
         // 4. Parse when entries
         for (KtWhenEntry entry : whenExpr.getEntries()) {
             if (entry.isElse()) {
                 // Else clause
                 callbacks.gotSwitchDefault();
             } else {
                 // Regular case with conditions
                 LocatableToken entryToken = createToken(entry);
                 callbacks.beginSwitchCase(entryToken);
                
                 // Parse all conditions for this entry (multi-condition support)
                 for (KtWhenCondition condition : entry.getConditions()) {
                     // Traverse the condition expression
                     if (condition instanceof KtWhenConditionWithExpression) {
                         KtExpression condExpr = ((KtWhenConditionWithExpression) condition).getExpression();
                         if (condExpr != null) {
                             condExpr.accept(this);
                         }
                     } else if (condition instanceof KtWhenConditionInRange) {
                         KtExpression rangeExpr = ((KtWhenConditionInRange) condition).getRangeExpression();
                         if (rangeExpr != null) {
                             rangeExpr.accept(this);
                         }
                     } else if (condition instanceof KtWhenConditionIsPattern) {
                         KtTypeReference typeRef = ((KtWhenConditionIsPattern) condition).getTypeReference();
                         if (typeRef != null) {
                             List<LocatableToken> typeTokens = extractTypeTokens(typeRef);
                             callbacks.gotTypeSpec(typeTokens);
                         }
                     }
                 }
                
                 // Mark arrow syntax (Kotlin always uses ->)
                 PsiElement arrow = findArrowElement(entry);
                 LocatableToken arrowToken = createToken(arrow != null ? arrow : entry, JavaTokenTypes.LAMBDA);
                 callbacks.gotSwitchCaseType(arrowToken, true);
             }
            
             // Parse entry body
             KtExpression entryExpr = entry.getExpression();
             if (entryExpr != null) {
                 entryExpr.accept(this);
             }
            
             // End case
             if (!entry.isElse()) {
                 LocatableToken endToken = createToken(entry.getLastChild());
                 callbacks.endSwitchCase(endToken, true);
             }
         }
        
         // 5. End switch block
         LocatableToken rBraceToken = createToken(whenExpr.getLastChild(), JavaTokenTypes.RCURLY);
         callbacks.endSwitchBlock(rBraceToken);
        
         // 6. End switch statement
         callbacks.endSwitchStmt(rBraceToken, true);
     }
    
     /**
      * Helper method to find the arrow element in a when entry.
      * The arrow (->) is a child of the when entry.
      */
     private PsiElement findArrowElement(KtWhenEntry entry) {
         for (PsiElement child : entry.getChildren()) {
             if (child.getText().equals("->")) {
                 return child;
             }
         }
         return null;
     }
    
     /**
      * Visits a try expression (try-catch-finally).
      *
      * <p>Kotlin try expressions handle exceptions:</p>
      * <pre>{@code
      * try {
      *     riskyOperation()
      * } catch (e: IOException) {
      *     handleError(e)
      * } finally {
      *     cleanup()
      * }
      * }</pre>
      *
      * <h3>Phase 6.4 Tasks 1-4: Try-Catch-Finally Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginTryCatchSmt(tryToken, false)} - Begin try (no resource)</li>
      *   <li>{@code beginTryBlock(lBrace)} - Begin try block</li>
      *   <li>Try block body traversal</li>
      *   <li>{@code endTryBlock(rBrace, true)} - End try block</li>
      *   <li>For each catch clause:
      *     <ul>
      *       <li>{@code gotCatchFinally(catchToken)} - Catch marker</li>
      *       <li>{@code gotTypeSpec(exceptionType)} - Exception type</li>
      *       <li>{@code gotCatchVarName(varNameToken)} - Exception variable</li>
      *       <li>Catch block body traversal</li>
      *     </ul>
      *   </li>
      *   <li>For finally block:
      *     <ul>
      *       <li>{@code gotCatchFinally(finallyToken)} - Finally marker</li>
      *       <li>Finally block body traversal</li>
      *     </ul>
      *   </li>
      *   <li>{@code endTryCatchStmt(endToken, true)} - End try statement</li>
      * </ol>
      *
      * @param tryExpr The try expression PSI element
      */
     @Override
     public void visitTryExpression(@NotNull KtTryExpression tryExpr) {
         if (tryExpr == null || callbacks == null) {
             return;
         }
        
         // 1. Begin try-catch statement (Kotlin doesn't have try-with-resources)
         PsiElement tryKeyword = tryExpr.getTryKeyword();
         LocatableToken tryToken = createToken(tryKeyword != null ? tryKeyword : tryExpr, JavaTokenTypes.LITERAL_try);
         callbacks.beginTryCatchSmt(tryToken, false);
        
         // 2. Parse try block
         KtBlockExpression tryBlock = tryExpr.getTryBlock();
         if (tryBlock != null) {
             PsiElement lBrace = tryBlock.getLBrace();
             if (lBrace != null) {
                 LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                 callbacks.beginTryBlock(lBraceToken);
             }
            
             // Traverse try block statements directly (not the block itself to avoid double wrapping)
             List<KtExpression> statements = tryBlock.getStatements();
             for (KtExpression statement : statements) {
                 callbacks.beginElement(createToken(statement));
                 statement.accept(this);
                 callbacks.endElement(createToken(statement), true);
             }
            
             PsiElement rBrace = tryBlock.getRBrace();
             if (rBrace != null) {
                 LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                 callbacks.endTryBlock(rBraceToken, true);
             }
         }
        
         // 3. Parse catch clauses
         for (KtCatchClause catchClause : tryExpr.getCatchClauses()) {
             visitCatchClause(catchClause);
         }
        
         // 4. Parse finally block (if present)
         KtFinallySection finallySection = tryExpr.getFinallyBlock();
         if (finallySection != null) {
             // Get finally keyword from children
             PsiElement finallyKeyword = findChildByText(finallySection, "finally");
             if (finallyKeyword != null) {
                 LocatableToken finallyToken = createToken(finallyKeyword, JavaTokenTypes.LITERAL_finally);
                 callbacks.gotCatchFinally(finallyToken);
             }
            
             // Get finally block expression
             KtBlockExpression finallyBlock = finallySection.getFinalExpression();
             if (finallyBlock != null) {
                 finallyBlock.accept(this);
             }
         }
        
         // 5. End try-catch statement
         LocatableToken endToken = createToken(tryExpr.getLastChild());
         callbacks.endTryCatchStmt(endToken, true);
     }
    
     /**
      * Visits a catch clause within a try expression.
      *
      * <p>Processes catch clause structure:</p>
      * <pre>{@code
      * catch (e: IOException) {
      *     handleError(e)
      * }
      * }</pre>
      *
      * <h3>Callback Sequence</h3>
      * <ol>
      *   <li>{@code gotCatchFinally(catchToken)} - Catch marker</li>
      *   <li>{@code gotTypeSpec(exceptionType)} - Exception type</li>
      *   <li>{@code gotCatchVarName(varNameToken)} - Exception variable name</li>
      *   <li>Catch block body traversal</li>
      * </ol>
      *
      * @param catchClause The catch clause PSI element
      */
     private void visitCatchClause(KtCatchClause catchClause) {
         if (catchClause == null || callbacks == null) {
             return;
         }
        
         // 1. Emit catch marker
         PsiElement catchKeyword = findChildByText(catchClause, "catch");
         if (catchKeyword != null) {
             LocatableToken catchToken = createToken(catchKeyword, JavaTokenTypes.LITERAL_catch);
             callbacks.gotCatchFinally(catchToken);
         }
        
         // 2. Parse exception parameter
         KtParameter parameter = catchClause.getCatchParameter();
         if (parameter != null) {
             // Exception type
             KtTypeReference typeRef = parameter.getTypeReference();
             if (typeRef != null) {
                 List<LocatableToken> typeTokens = extractTypeTokens(typeRef);
                 callbacks.gotTypeSpec(typeTokens);
             }
            
             // Exception variable name
             PsiElement nameIdentifier = parameter.getNameIdentifier();
             if (nameIdentifier != null) {
                 LocatableToken varToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                 callbacks.gotCatchVarName(varToken);
             }
         }
        
         // 3. Parse catch block body
         KtExpression catchBody = catchClause.getCatchBody();
         if (catchBody != null) {
             // If it's a block, traverse statements directly
             if (catchBody instanceof KtBlockExpression) {
                 KtBlockExpression block = (KtBlockExpression) catchBody;
                 List<KtExpression> statements = block.getStatements();
                 for (KtExpression statement : statements) {
                     callbacks.beginElement(createToken(statement));
                     statement.accept(this);
                     callbacks.endElement(createToken(statement), true);
                 }
             } else {
                 // Single expression (unlikely but possible)
                 catchBody.accept(this);
             }
         }
     }
    
     // ==================== Phase 6.5 & 6.6: Expression Visitor Methods ====================
    
     /**
      * Visits a constant expression (literal).
      *
      * <p>Handles all Kotlin literal types:</p>
      * <ul>
      *   <li>Integer: {@code 42}, {@code 0xFF}, {@code 0b1010}</li>
      *   <li>Float: {@code 3.14}, {@code 1.0f}</li>
      *   <li>Boolean: {@code true}, {@code false}</li>
      *   <li>Character: {@code 'a'}</li>
      *   <li>String: {@code "text"} (simple, not templates)</li>
      *   <li>Null: {@code null}</li>
      * </ul>
      *
      * <h3>Phase 6.5 Task 1: Literal Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, false)} - Begin expression</li>
      *   <li>{@code gotLiteral(token)} - Literal value</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * @param expr The constant expression PSI element
      */
     @Override
     public void visitConstantExpression(@NotNull KtConstantExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
         callbacks.gotLiteral(token);
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a simple name expression (identifier reference).
      *
      * <p>Handles simple name references:</p>
      * <pre>{@code
      * x           // Variable reference
      * myVar       // Variable reference
      * myFunction  // Function reference (without call)
      * }</pre>
      *
      * <h3>Phase 6.5 Task 1: Identifier Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, false)} - Begin expression</li>
      *   <li>{@code gotIdentifier(token)} - Identifier</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * @param expr The simple name expression PSI element
      */
     @Override
     public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
         callbacks.gotIdentifier(token);
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a binary expression (binary operator).
      *
      * <p>Handles all binary operations:</p>
      * <ul>
      *   <li>Arithmetic: {@code +, -, *, /, %}</li>
      *   <li>Comparison: {@code ==, !=, <, >, <=, >=}</li>
      *   <li>Logical: {@code &&, ||}</li>
      *   <li>Bitwise: {@code and, or, xor, shl, shr, ushr}</li>
      *   <li>Assignment: {@code =, +=, -=, *=, /=, %=}</li>
      *   <li>Elvis: {@code ?:} (null coalescing)</li>
      *   <li>Range: {@code .., ..<}</li>
      * </ul>
      *
      * <h3>Phase 6.5 Task 2: Binary Operator Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, false)} - Begin expression</li>
      *   <li>Left operand traversal</li>
      *   <li>{@code gotBinaryOperator(opToken)} - Operator</li>
      *   <li>Right operand traversal</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * @param expr The binary expression PSI element
      */
     @Override
     public void visitBinaryExpression(@NotNull KtBinaryExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
        
         // 1. Left operand
         KtExpression left = expr.getLeft();
         if (left != null) {
             left.accept(this);
         }
        
         // 2. Operator
         PsiElement operationRef = expr.getOperationReference();
         if (operationRef != null) {
             LocatableToken opToken = createToken(operationRef);
             callbacks.gotBinaryOperator(opToken);
         }
        
         // 3. Right operand
         KtExpression right = expr.getRight();
         if (right != null) {
             right.accept(this);
         }
        
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a unary expression (prefix or postfix operator).
      *
      * <p>Handles unary operations:</p>
      * <ul>
      *   <li>Prefix: {@code +x, -x, !x, ++x, --x}</li>
      *   <li>Postfix: {@code x++, x--}</li>
      *   <li>Kotlin-specific: {@code x!!} (not-null assertion)</li>
      * </ul>
      *
      * <h3>Phase 6.5 Task 3: Unary Operator Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, false)} - Begin expression</li>
      *   <li>{@code gotUnaryOperator(opToken)} or {@code gotPostOperator(opToken)}</li>
      *   <li>Operand traversal</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * @param expr The unary expression PSI element
      */
     @Override
     public void visitUnaryExpression(@NotNull KtUnaryExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
        
         // Get operator reference
         PsiElement operationRef = expr.getOperationReference();
         if (operationRef != null) {
             LocatableToken opToken = createToken(operationRef);
            
             // Determine if prefix or postfix
             if (expr instanceof KtPrefixExpression) {
                 callbacks.gotUnaryOperator(opToken);
             } else if (expr instanceof KtPostfixExpression) {
                 callbacks.gotPostOperator(opToken);
             }
         }
        
         // Operand
         KtExpression baseExpr = expr.getBaseExpression();
         if (baseExpr != null) {
             baseExpr.accept(this);
         }
        
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a prefix expression (prefix unary operator).
      * Delegates to {@link #visitUnaryExpression(KtUnaryExpression)}.
      */
     @Override
     public void visitPrefixExpression(@NotNull KtPrefixExpression expr) {
         visitUnaryExpression(expr);
     }
    
     /**
      * Visits a postfix expression (postfix unary operator).
      * Delegates to {@link #visitUnaryExpression(KtUnaryExpression)}.
      */
     @Override
     public void visitPostfixExpression(@NotNull KtPostfixExpression expr) {
         visitUnaryExpression(expr);
     }
    
     /**
      * Visits a call expression (method or constructor call).
      *
      * <p>Handles function calls with arguments:</p>
      * <pre>{@code
      * myFunction()
      * myFunction(arg1, arg2)
      * myFunction(x = 1, y = 2)  // Named arguments
      * }</pre>
      *
      * <h3>Phase 6.5 Task 4: Method Call Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, false)} - Begin expression</li>
      *   <li>{@code gotMethodCall(nameToken)} - Method name</li>
      *   <li>{@code beginArgumentList(lParen)} - Begin arguments</li>
      *   <li>For each argument:
      *     <ul>
      *       <li>Argument expression traversal</li>
      *       <li>{@code endArgument()} - End argument</li>
      *     </ul>
      *   </li>
      *   <li>{@code endArgumentList(rParen)} - End arguments</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * @param expr The call expression PSI element
      */
     @Override
     public void visitCallExpression(@NotNull KtCallExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
        
         // 1. Method name (from call's callee expression)
         KtExpression calleeExpr = expr.getCalleeExpression();
         if (calleeExpr != null) {
             LocatableToken nameToken = createToken(calleeExpr);
             callbacks.gotMethodCall(nameToken);
         }
        
         // 2. Parse argument list
         KtValueArgumentList argList = expr.getValueArgumentList();
         if (argList != null) {
             PsiElement lParen = argList.getLeftParenthesis();
             if (lParen != null) {
                 LocatableToken lParenToken = createToken(lParen, JavaTokenTypes.LPAREN);
                 callbacks.beginArgumentList(lParenToken);
             }
            
             // Parse each argument
             for (KtValueArgument arg : argList.getArguments()) {
                 KtExpression argExpr = arg.getArgumentExpression();
                 if (argExpr != null) {
                     argExpr.accept(this);
                 }
                 // No endArgument() needed - arguments are just expressions within the list
             }
            
             PsiElement rParen = argList.getRightParenthesis();
             if (rParen != null) {
                 LocatableToken rParenToken = createToken(rParen, JavaTokenTypes.RPAREN);
                 callbacks.endArgumentList(rParenToken);
             }
         }
        
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a qualified expression (member access with dot operator).
      *
      * <p>Handles member access operations:</p>
      * <pre>{@code
      * object.property
      * object.method()
      * object?.safeProperty  // Safe call
      * }</pre>
      *
      * <h3>Phase 6.5 Task 5: Member Access Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, false)} - Begin expression</li>
      *   <li>Receiver expression traversal</li>
      *   <li>{@code gotMemberAccess(memberToken)} - Member access</li>
      *   <li>Selector expression traversal</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * <p><b>Safe Call Operator:</b> Kotlin's {@code ?.} operator is mapped to
      * member access with a special token type to indicate safe navigation.</p>
      *
      * @param expr The qualified expression PSI element
      */
     @Override
     public void visitQualifiedExpression(@NotNull KtQualifiedExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
        
         // 1. Receiver expression
         KtExpression receiver = expr.getReceiverExpression();
         if (receiver != null) {
             receiver.accept(this);
         }
        
         // 2. Member access (dot or safe call operator)
         PsiElement operationSign = expr.getOperationTokenNode().getPsi();
         if (operationSign != null) {
             LocatableToken memberToken = createToken(operationSign, JavaTokenTypes.DOT);
             callbacks.gotMemberAccess(memberToken);
         }
        
         // 3. Selector expression
         KtExpression selector = expr.getSelectorExpression();
         if (selector != null) {
             selector.accept(this);
         }
        
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a dot qualified expression.
      * Delegates to {@link #visitQualifiedExpression(KtQualifiedExpression)}.
      */
     @Override
     public void visitDotQualifiedExpression(@NotNull KtDotQualifiedExpression expr) {
         visitQualifiedExpression(expr);
     }
    
     /**
      * Visits a safe qualified expression (safe call operator ?.).
      * Delegates to {@link #visitQualifiedExpression(KtQualifiedExpression)}.
      */
     @Override
     public void visitSafeQualifiedExpression(@NotNull KtSafeQualifiedExpression expr) {
         visitQualifiedExpression(expr);
     }
    
     /**
      * Visits an array access expression (subscript operator).
      *
      * <p>Handles array element access:</p>
      * <pre>{@code
      * array[0]
      * matrix[i][j]
      * map["key"]
      * }</pre>
      *
      * <h3>Phase 6.5 Task 6: Array Access Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, false)} - Begin expression</li>
      *   <li>Array expression traversal</li>
      *   <li>Index expression traversal</li>
      *   <li>{@code gotArrayElementAccess()} - Array access marker</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * @param expr The array access expression PSI element
      */
     @Override
     public void visitArrayAccessExpression(@NotNull KtArrayAccessExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
        
         // 1. Array expression
         KtExpression arrayExpr = expr.getArrayExpression();
         if (arrayExpr != null) {
             arrayExpr.accept(this);
         }
        
         // 2. Index expressions (can be multiple for multi-dimensional)
         for (KtExpression indexExpr : expr.getIndexExpressions()) {
             indexExpr.accept(this);
         }
        
         // 3. Mark array element access
         callbacks.gotArrayElementAccess();
        
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a lambda expression.
      *
      * <p>Kotlin lambda expressions:</p>
      * <pre>{@code
      * { x -> x * 2 }                    // Single parameter
      * { x, y -> x + y }                 // Multiple parameters
      * { it * 2 }                        // Implicit 'it' parameter
      * { x: Int, y: Int -> x + y }       // Typed parameters
      * }</pre>
      *
      * <h3>Phase 6.6 Task 1: Lambda Expression Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, true)} - Begin expression (isLambdaBody=true)</li>
      *   <li>{@code beginLambdaBody(true, lBrace)} - Begin lambda body</li>
      *   <li>For each parameter:
      *     <ul>
      *       <li>{@code gotLambdaFormalParam()} - Parameter marker</li>
      *       <li>{@code gotLambdaFormalName(nameToken)} - Parameter name</li>
      *       <li>{@code gotLambdaFormalType(typeTokens)} - Parameter type (if specified)</li>
      *     </ul>
      *   </li>
      *   <li>Lambda body traversal</li>
      *   <li>{@code endLambdaBody(rBrace)} - End lambda body</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * @param expr The lambda expression PSI element
      */
     @Override
     public void visitLambdaExpression(@NotNull KtLambdaExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, true);
        
         // Get lambda literal (contains parameters and body)
         KtFunctionLiteral literal = expr.getFunctionLiteral();
         if (literal != null) {
             PsiElement lBrace = literal.getLBrace();
             LocatableToken lBraceToken = lBrace != null ? createToken(lBrace, JavaTokenTypes.LCURLY) : token;
             callbacks.beginLambdaBody(true, lBraceToken);
            
             // Process parameters
             KtParameterList paramList = literal.getValueParameterList();
             if (paramList != null) {
                 for (KtParameter param : paramList.getParameters()) {
                     callbacks.gotLambdaFormalParam();
                    
                     // Parameter name
                     PsiElement nameId = param.getNameIdentifier();
                     if (nameId != null) {
                         LocatableToken nameToken = createToken(nameId, JavaTokenTypes.IDENT);
                         callbacks.gotLambdaFormalName(nameToken);
                     }
                    
                     // Parameter type (if specified)
                     KtTypeReference typeRef = param.getTypeReference();
                     if (typeRef != null) {
                         List<LocatableToken> typeTokens = extractTypeTokens(typeRef);
                         callbacks.gotLambdaFormalType(typeTokens);
                     }
                 }
             }
            
             // Process lambda body
             KtBlockExpression bodyBlock = literal.getBodyExpression();
             if (bodyBlock != null) {
                 // Visit statements in lambda body
                 for (KtExpression statement : bodyBlock.getStatements()) {
                     statement.accept(this);
                 }
             }
            
             PsiElement rBrace = literal.getRBrace();
             LocatableToken rBraceToken = rBrace != null ? createToken(rBrace, JavaTokenTypes.RCURLY) : token;
             callbacks.endLambdaBody(rBraceToken);
         }
        
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a string template expression (string with interpolation).
      *
      * <p>Kotlin string templates support interpolation:</p>
      * <pre>{@code
      * "Hello $name"                    // Simple interpolation
      * "Sum: ${x + y}"                  // Expression interpolation
      * }</pre>
      *
      * <h3>Phase 6.6 Task 2: String Template Handling</h3>
      * <p>In Phase 6, we treat string templates as opaque string literals.
      * Future enhancement may parse template expressions separately.</p>
      *
      * @param expr The string template expression PSI element
      */
     @Override
     public void visitStringTemplateExpression(@NotNull KtStringTemplateExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         // Treat as string literal for Phase 6
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
         callbacks.gotLiteral(token);
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a this expression (this reference).
      *
      * <p>Kotlin this expressions:</p>
      * <pre>{@code
      * this            // Current class instance
      * this@Outer      // Outer class instance (labeled this)
      * }</pre>
      *
      * <h3>Phase 6.6 Task 4: This Expression Callback</h3>
      * <p>Treated as a special literal.</p>
      *
      * @param expr The this expression PSI element
      */
     @Override
     public void visitThisExpression(@NotNull KtThisExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
         callbacks.gotLiteral(token);  // "this" treated as literal
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a super expression (super reference).
      *
      * <p>Kotlin super expressions:</p>
      * <pre>{@code
      * super.method()       // Superclass method call
      * super<Base>.method() // Qualified super (multiple inheritance)
      * }</pre>
      *
      * <h3>Phase 6.6 Task 4: Super Expression Callback</h3>
      * <p>Treated as a special literal.</p>
      *
      * @param expr The super expression PSI element
      */
     @Override
     public void visitSuperExpression(@NotNull KtSuperExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
         callbacks.gotLiteral(token);  // "super" treated as literal
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits a binary expression with type RHS (type operations).
      *
      * <p>Handles Kotlin type operations:</p>
      * <ul>
      *   <li>{@code as} - Type cast: {@code x as String}</li>
      *   <li>{@code as?} - Safe cast: {@code x as? String}</li>
      *   <li>{@code is} - Type check: {@code x is String}</li>
      *   <li>{@code !is} - Negated type check: {@code x !is String}</li>
      * </ul>
      *
      * <h3>Phase 6.6 Task 5: Type Operation Callback Sequence</h3>
      * <ol>
      *   <li>{@code beginExpression(token, false)} - Begin expression</li>
      *   <li>Left operand traversal</li>
      *   <li>{@code gotTypeCast(typeTokens)} or {@code gotInstanceOfOperator(opToken)}</li>
      *   <li>{@code endExpression(token, false)} - End expression</li>
      * </ol>
      *
      * @param expr The binary with type RHS expression PSI element
      */
     @Override
     public void visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         LocatableToken token = createToken(expr);
         callbacks.beginExpression(token, false);
        
         // 1. Left operand
         KtExpression left = expr.getLeft();
         if (left != null) {
             left.accept(this);
         }
        
         // 2. Operator and type
         PsiElement operationRef = expr.getOperationReference();
         KtTypeReference typeRef = expr.getRight();
        
         if (operationRef != null && typeRef != null) {
             String opText = operationRef.getText();
             List<LocatableToken> typeTokens = extractTypeTokens(typeRef);
            
             if ("as".equals(opText) || "as?".equals(opText)) {
                 // Type cast
                 callbacks.gotTypeCast(typeTokens);
             } else if ("is".equals(opText) || "!is".equals(opText)) {
                 // Type check (instanceof)
                 LocatableToken opToken = createToken(operationRef, JavaTokenTypes.LITERAL_instanceof);
                 callbacks.gotInstanceOfOperator(opToken);
                 callbacks.gotTypeSpec(typeTokens);
             }
         }
        
         callbacks.endExpression(token, false);
     }
    
     /**
      * Visits an object literal expression (anonymous object).
      *
      * <p>Kotlin object expressions create anonymous instances:</p>
      * <pre>{@code
      * object : MyInterface {
      *     override fun method() = "impl"
      * }
      * }</pre>
      *
      * <h3>Phase 6.6 Task 3: Object Literal Callback Sequence</h3>
      * <p>Mapped to anonymous class body callbacks.</p>
      *
      * @param expr The object literal expression PSI element
      */
     @Override
     public void visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         // Get the object declaration within the literal
         KtObjectDeclaration objDecl = expr.getObjectDeclaration();
         if (objDecl != null) {
             KtClassBody body = objDecl.getBody();
             if (body != null) {
                 PsiElement lBrace = body.getLBrace();
                 if (lBrace != null) {
                     LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                     callbacks.beginAnonClassBody(lBraceToken, false);
                 }
                
                 // Visit object members
                 for (KtDeclaration decl : body.getDeclarations()) {
                     decl.accept(this);
                 }
                
                 PsiElement rBrace = body.getRBrace();
                 if (rBrace != null) {
                     LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                     callbacks.endAnonClassBody(rBraceToken, true);
                 }
             }
         }
     }
    
     /**
      * Visits a parenthesized expression.
      *
      * <p>Handles expressions in parentheses:</p>
      * <pre>{@code
      * (x + y)
      * ((nested))
      * }</pre>
      *
      * <p>Simply traverses the inner expression.</p>
      *
      * @param expr The parenthesized expression PSI element
      */
     @Override
     public void visitParenthesizedExpression(@NotNull KtParenthesizedExpression expr) {
         if (expr == null || callbacks == null) {
             return;
         }
        
         // Simply traverse the inner expression
         KtExpression innerExpr = expr.getExpression();
         if (innerExpr != null) {
             innerExpr.accept(this);
         }
     }
    
     /**
      * Helper method to create a token from PSI element with automatic type detection.
      * Used for expressions where we don't need a specific token type.
      */
     private LocatableToken createToken(PsiElement element) {
         return createToken(element, JavaTokenTypes.LITERAL_void);
     }
     /**
      * Helper method to find a child element by its text content.
      * Used for finding keyword elements like "catch", "finally", etc.
      *
      * @param parent The parent element to search in
      * @param text The text to match
      * @return The first child matching the text, or null if not found
      */
     private PsiElement findChildByText(PsiElement parent, String text) {
         if (parent == null || text == null) {
             return null;
         }
        
         for (PsiElement child : parent.getChildren()) {
             if (text.equals(child.getText())) {
                 return child;
             }
         }
         return null;
     }



    // TODO: Task 2.2.3 - Implement additional visitor methods (Phase 3):
    // - visitParameter (for function parameters)
    // - visitSuperTypeList (for inheritance)
    // - visitModifierList (for modifiers)
    // - visitTypeReference (for types)
}