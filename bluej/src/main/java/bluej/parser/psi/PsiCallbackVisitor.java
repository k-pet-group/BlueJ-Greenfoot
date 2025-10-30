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
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.psi.PsiDocumentManager;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.psi.*;

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
 *   <li>Implement {@link #extractModifiers(KtModifierListOwner)} to populate state modifiers</li>
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
    public void visitKtFile(KtFile file) {
        if (file == null) {
            return; // Gracefully handle null
        }
        
        // Phase 2: Log file visit
        String fileName = file.getName();
        traversalLog.add("VISIT: FILE: " + (fileName != null ? fileName : "<unnamed>"));
        
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
    public void visitClass(KtClass ktClass) {
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
            LocatableToken classToken = createToken(ktClass, JavaTokenTypes.LITERAL_class);
            callbacks.invokeDeclBegin(classToken);
            
            // 2. Modifiers consumed (empty for now - Task 2 will implement)
            // TODO Task 2: Extract modifiers from ktClass.getModifierList()
            callbacks.invokeModifiersConsumed();
            
            // 3. Type definition
            int tdType = determineTypeDefType(ktClass);
            callbacks.invokeTypeDef(classToken, tdType);
            
            // 4. Type name
            if (className != null && ktClass.getNameIdentifier() != null) {
                LocatableToken nameToken = createToken(ktClass.getNameIdentifier(), JavaTokenTypes.IDENT);
                callbacks.invokeTypeDefName(nameToken);
            }
            
            // 5. Supertypes (deferred to Task 3 - skip for now)
            // TODO Task 3: Process extends/implements clauses
            
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
                    
                    // 7. Visit members - super.visitClass recurses into nested declarations
                    super.visitClass(ktClass);
                    
                    // 8. End type body with separate closing brace token
                    callbacks.invokeEndTypeBody(rBraceToken, true);
                } else {
                    // Braces missing - malformed code, but handle gracefully
                    super.visitClass(ktClass);
                }
            } else {
                // No body - just visit to be safe
                super.visitClass(ktClass);
            }
            
            // 9. End declaration
            callbacks.invokeTypeDefEnd(classToken, true);
            
        } finally {
            state.popScope();  // ALWAYS cleanup
        }
    }
    
    /**
     * Visits a function declaration.
     *
     * <p>This method is invoked for all named function declarations including:</p>
     * <ul>
     *   <li>Top-level functions</li>
     *   <li>Member functions (methods)</li>
     *   <li>Extension functions</li>
     *   <li>Local functions (nested in other functions)</li>
     * </ul>
     *
     * <h3>Phase 2 Behavior</h3>
     * <p>Logs: "VISIT: FUNCTION: &lt;functionName&gt;"</p>
     * <p>Manages state with push/pop pattern (though body not traversed in Phase 2).</p>
     * <p>Does not traverse function body in Phase 2.</p>
     *
     * <h3>Phase 3 Migration</h3>
     * <p>Will extract modifiers via {@link #extractModifiers(KtModifierListOwner)},
     * invoke {@code callbacks.gotMethodDeclaration()} with function signature details,
     * and clear modifiers with {@link #clearModifierState()}.</p>
     *
     * @param function The function declaration PSI element
     */
    @Override
    public void visitNamedFunction(KtNamedFunction function) {
        if (function == null) {
            return;
        }
        
        // Phase 2: Log function visit
        String functionName = function.getName();
        traversalLog.add("VISIT: FUNCTION: " + (functionName != null ? functionName : "<anonymous>"));
        
        // State management with try-finally
        state.pushScope(function);
        try {
            // TODO Phase 3: Extract modifiers and track with state.addModifier()
            // TODO Phase 3: Invoke gotMethodDeclaration callback
            
            // Phase 2: Do not traverse function body
            // Phase 3: Will visit parameters and handle body traversal based on callback requirements
            
            // TODO Phase 3: Clear modifiers with clearModifierState()
        } finally {
            state.popScope();  // ALWAYS cleanup
        }
        
        // Note: Explicitly NOT calling super to avoid traversing function body in Phase 2
    }
    
    /**
     * Visits a property declaration (val or var).
     *
     * <p>This method is invoked for property declarations including:</p>
     * <ul>
     *   <li>Class member properties</li>
     *   <li>Top-level properties</li>
     *   <li>Local variables (in some contexts)</li>
     * </ul>
     *
     * <h3>Phase 2 Behavior</h3>
     * <p>Logs: "VISIT: PROPERTY: &lt;propertyName&gt;"</p>
     * <p>Manages state with push/pop pattern (though initializer not traversed in Phase 2).</p>
     *
     * <h3>Phase 3 Migration</h3>
     * <p>Will extract modifiers via {@link #extractModifiers(KtModifierListOwner)},
     * invoke {@code callbacks.gotFieldDeclaration()} with property details,
     * and clear modifiers with {@link #clearModifierState()}.</p>
     *
     * @param property The property declaration PSI element
     */
    @Override
    public void visitProperty(KtProperty property) {
        if (property == null) {
            return;
        }
        
        // Phase 2: Log property visit
        String propertyName = property.getName();
        traversalLog.add("VISIT: PROPERTY: " + (propertyName != null ? propertyName : "<anonymous>"));
        
        // State management with try-finally
        state.pushScope(property);
        try {
            // TODO Phase 3: Extract modifiers and track with state.addModifier()
            // TODO Phase 3: Invoke gotFieldDeclaration callback
            
            // Phase 2: Do not traverse property initializer or accessors
            // Phase 3: Will extract property details based on callback requirements
            
            // TODO Phase 3: Clear modifiers with clearModifierState()
        } finally {
            state.popScope();  // ALWAYS cleanup
        }
        
        // Note: NOT calling super to avoid deep traversal in Phase 2
    }
    
    /**
     * Visits an object declaration.
     *
     * <p>This method is invoked for Kotlin object declarations including:</p>
     * <ul>
     *   <li>Singleton objects ({@code object MySingleton})</li>
     *   <li>Companion objects ({@code companion object})</li>
     *   <li>Object expressions (anonymous objects)</li>
     * </ul>
     *
     * <h3>Phase 2 Behavior</h3>
     * <p>Logs: "VISIT: OBJECT: &lt;objectName&gt;"</p>
     * <p>Manages state with push/pop pattern using try-finally for balanced stack.</p>
     * <p>Continues traversal to visit object members.</p>
     *
     * <h3>Phase 3 Migration</h3>
     * <p>Will extract modifiers via {@link #extractModifiers(KtModifierListOwner)},
     * invoke {@code callbacks.beginTypeDecl()} treating object as a special class type,
     * visit members, and invoke {@code callbacks.endTypeDecl()}.</p>
     *
     * @param declaration The object declaration PSI element
     */
    @Override
    public void visitObjectDeclaration(KtObjectDeclaration declaration) {
        if (declaration == null) {
            return;
        }
        
        // Phase 2: Log object visit
        String objectName = declaration.getName();
        traversalLog.add("VISIT: OBJECT: " + (objectName != null ? objectName : "<anonymous>"));
        
        // State management with try-finally
        state.pushScope(declaration);
        try {
            // TODO Phase 3: Extract modifiers and track with state.addModifier()
            // TODO Phase 3: Implement object declaration callback sequence
            //   (similar to class but mark as object type)
            
            // Continue traversal to visit object members
            super.visitObjectDeclaration(declaration);
            
            // TODO Phase 3: Clear modifiers with clearModifierState()
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
        
        // Extract text and offset
        String text = element.getText();
        if (text == null) {
            text = "";
        }
        
        int startOffset = element.getTextOffset();
        int endOffset = startOffset + text.length();
        
        // Get source file text for line/column calculation
        // Note: Document API (PsiDocumentManager) is unavailable in lightweight test PSI environments,
        // so we calculate positions directly from source text
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) {
            // Fallback if no containing file (shouldn't happen)
            LineColPos begin = new LineColPos(1, startOffset, startOffset);
            LineColPos end = new LineColPos(1, endOffset, endOffset);
            return new LocatableToken(type, text, begin, end);
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
        
        // Create token with accurate positions
        LineColPos begin = new LineColPos(startLine, startColumn, startOffset);
        LineColPos end = new LineColPos(endLine, endColumn, endOffset);
        
        return new LocatableToken(type, text, begin, end);
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
     * <p>This classification is used by {@link JavaParserCallbacks#gotTypeDef(LocatableToken, int)}
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
     * Extract modifiers from PSI element (Phase 3 - Task 2).
     *
     * <p>This method will parse the modifier list from a Kotlin declaration and
     * add each modifier to the visitor state via {@link VisitorState#addModifier(String)}.
     * Common modifiers include: public, private, protected, internal, static, final,
     * abstract, override, open, data, sealed.</p>
     *
     * <p><b>Phase 2:</b> Stub method - not implemented yet.</p>
     * <p><b>Phase 3:</b> Will be implemented to extract modifiers from
     * {@link org.jetbrains.kotlin.psi.KtModifierListOwner#getModifierList()}.</p>
     *
     * @param element The PSI element with potential modifiers
     */
    private void extractModifiers(KtModifierListOwner element) {
        // TODO Phase 3: Implement modifier extraction from KtModifierListOwner
        // Example implementation:
        // KtModifierList modifierList = element.getModifierList();
        // if (modifierList != null) {
        //     for (PsiElement modifier : modifierList.getChildren()) {
        //         state.addModifier(modifier.getText());
        //     }
        // }
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
    
    // TODO: Task 2.2.3 - Implement additional visitor methods (Phase 3):
    // - visitParameter (for function parameters)
    // - visitSuperTypeList (for inheritance)
    // - visitModifierList (for modifiers)
    // - visitTypeReference (for types)
}