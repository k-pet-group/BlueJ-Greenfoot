package bluej.parser.psi.visitor;

import bluej.parser.lexer.*;
import bluej.parser.psi.JavaParserCallbacksAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.psi.*;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Base visitor providing shared, context-agnostic helper methods for PSI traversal.
 *
 * <h2>Context-Agnostic Design Principle</h2>
 * <p><b>CRITICAL ARCHITECTURAL CONSTRAINT:</b> All helper methods in this class are
 * <em>context-agnostic</em> - they extract and return data only. They NEVER:</p>
 * <ul>
 *   <li>Make callback decisions based on context</li>
 *   <li>Use {@code PsiTreeUtil.getParentOfType()} to detect context</li>
 *   <li>Invoke callbacks directly (that's the concrete visitor's responsibility)</li>
 *   <li>Determine behavior based on whether element is inside a class or at top-level</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <p>Concrete visitors ({@link FileVisitor}, future ClassVisitor) call these helpers
 * to extract data, then make their own context-specific decisions about what callbacks
 * to invoke:</p>
 * <pre>{@code
 * // In FileVisitor (file-level context):
 * FunctionParametersResult result = extractFunctionParameters(function);
 * if (result.hasParameterList()) {
 *     callbacks.gotMethodDeclaration(nameToken, null);
 *     // ... invoke appropriate callbacks for top-level function
 * }
 *
 * // In ClassVisitor (class-level context):
 * FunctionParametersResult result = extractFunctionParameters(function);
 * if (result.hasParameterList()) {
 *     callbacks.gotMethodDeclaration(nameToken, null);
 *     // ... invoke appropriate callbacks for class method
 * }
 * }</pre>
 *
 * <h2>Result Types</h2>
 * <p>Helper methods return immutable record types containing extracted data:</p>
 * <ul>
 *   <li>{@link ModifierSet} - Set of modifiers with their tokens</li>
 *   <li>{@link FunctionParametersResult} - Parameter list data and tokens</li>
 *   <li>{@link FunctionBodyResult} - Method body data (braces, expressions)</li>
 *   <li>{@link FunctionSignatureResult} - Combined signature data for functions</li>
 * </ul>
 *
 * @see FileVisitor Concrete visitor for file-level constructs
 * @see MethodBodyVisitor Concrete visitor for method body statements
 */
public class BaseVisitor extends KtVisitorVoid implements PsiVisitor {

    protected boolean parseTypeDefPart2 = false;
    protected String fileText = null;

    // ==================== Context-Agnostic Result Types ====================

    /**
     * Enumeration of Kotlin modifiers that can be extracted from declarations.
     *
     * <p>These map to both standard Java modifiers and Kotlin-specific modifiers.
     * The concrete visitor decides how to translate these to callbacks.</p>
     */
    public enum Modifier {
        // Visibility modifiers
        PUBLIC, PRIVATE, PROTECTED, INTERNAL,
        // Inheritance modifiers
        ABSTRACT, FINAL, OPEN, OVERRIDE,
        // Function modifiers
        OPERATOR, INFIX, INLINE, SUSPEND, TAILREC, EXTERNAL,
        // Property modifiers
        LATEINIT, CONST,
        // Other
        DATA, SEALED, COMPANION, VARARG
    }

    /**
     * Result of modifier extraction - contains set of modifiers with their tokens.
     *
     * <p><b>Context-Agnostic:</b> This record only holds extracted data.
     * The concrete visitor decides which callbacks to invoke based on context.</p>
     *
     * @param modifiers Set of modifiers found in the modifier list
     * @param modifierTokens List of tokens for each modifier (in source order)
     * @param firstModifierToken Token of the first modifier (for gotDeclBegin positioning)
     */
    public record ModifierSet(
            EnumSet<Modifier> modifiers,
            List<ModifierToken> modifierTokens,
            @Nullable LocatableToken firstModifierToken
    ) {
        /**
         * Creates an empty modifier set (no modifiers present).
         */
        public static ModifierSet empty() {
            return new ModifierSet(EnumSet.noneOf(Modifier.class), List.of(), null);
        }

        /**
         * Checks if a specific modifier is present.
         */
        public boolean has(Modifier modifier) {
            return modifiers.contains(modifier);
        }

        /**
         * Checks if any visibility modifier is present.
         */
        public boolean hasVisibility() {
            return has(Modifier.PUBLIC) || has(Modifier.PRIVATE) ||
                    has(Modifier.PROTECTED) || has(Modifier.INTERNAL);
        }
    }

    /**
     * A single modifier with its associated token.
     *
     * @param modifier The modifier type
     * @param token The locatable token for this modifier
     * @param tokenType The JavaTokenTypes constant for this modifier
     */
    public record ModifierToken(Modifier modifier, LocatableToken token, int tokenType) {}

    /**
     * Result of function parameter extraction.
     *
     * <p><b>Context-Agnostic:</b> This record only holds extracted parameter data.
     * The concrete visitor decides what callbacks to invoke:</p>
     * <ul>
     *   <li>FileVisitor: May call {@code endDecl()} for incomplete top-level functions</li>
     *   <li>ClassVisitor: May call {@code endMethodDecl()} for class methods</li>
     * </ul>
     *
     * @param hasParameterList Whether the function has a parameter list (has '(')
     * @param leftParenToken Token for '(' if present
     * @param rightParenToken Token for ')' if present
     * @param parameters List of extracted parameter data
     * @param lastToken The last token processed (for positioning)
     */
    public record FunctionParametersResult(
            boolean hasParameterList,
            @Nullable LocatableToken leftParenToken,
            @Nullable LocatableToken rightParenToken,
            List<ParameterData> parameters,
            @Nullable LocatableToken lastToken
    ) {
        /**
         * Creates an empty result (no parameter list).
         */
        public static FunctionParametersResult empty() {
            return new FunctionParametersResult(false, null, null, List.of(), null);
        }

        /**
         * Checks if the parameter list is complete (has both '(' and ')').
         */
        public boolean isComplete() {
            return hasParameterList && rightParenToken != null;
        }
    }

    /**
     * Data for a single function parameter.
     *
     * @param nameToken Token for parameter name
     * @param colonToken Token for ':' separator (if present)
     * @param typeTokens Type tokens (if explicit type)
     * @param isVararg Whether this parameter has vararg modifier
     * @param varargToken Token for vararg modifier (if present)
     * @param firstToken First token of this parameter (for beginFormalParameter)
     */
    public record ParameterData(
            @Nullable LocatableToken nameToken,
            @Nullable LocatableToken colonToken,
            @Nullable List<LocatableToken> typeTokens,
            boolean isVararg,
            @Nullable LocatableToken varargToken,
            @Nullable LocatableToken firstToken
    ) {}

    /**
     * Result of function body extraction.
     *
     * <p><b>Context-Agnostic:</b> This record only holds body structure data.
     * The concrete visitor decides how to handle the body:</p>
     * <ul>
     *   <li>Whether to call {@code beginMethodBody()} / {@code endMethodBody()}</li>
     *   <li>Whether to delegate to MethodBodyVisitor</li>
     *   <li>What token to use for {@code endMethodDecl()}</li>
     * </ul>
     *
     * @param hasBody Whether the function has any body
     * @param isBlockBody Whether the body is a block (has braces)
     * @param isExpressionBody Whether the body is an expression (= expr)
     * @param lBraceToken Token for '{' (if block body)
     * @param rBraceToken Token for '}' (if block body)
     * @param equalsToken Token for '=' (if expression body)
     * @param bodyExpression The body expression PSI element
     * @param lastToken The last token of the body (for positioning)
     */
    public record FunctionBodyResult(
            boolean hasBody,
            boolean isBlockBody,
            boolean isExpressionBody,
            @Nullable LocatableToken lBraceToken,
            @Nullable LocatableToken rBraceToken,
            @Nullable LocatableToken equalsToken,
            @Nullable KtExpression bodyExpression,
            @Nullable LocatableToken lastToken
    ) {
        /**
         * Creates an empty result (no body - abstract/interface method).
         */
        public static FunctionBodyResult empty() {
            return new FunctionBodyResult(false, false, false, null, null, null, null, null);
        }

        /**
         * Checks if the block body is complete (has both '{' and '}').
         */
        public boolean isBlockComplete() {
            return isBlockBody && rBraceToken != null;
        }
    }

    /**
     * Combined result of function signature extraction.
     *
     * <p><b>Context-Agnostic:</b> This record combines all signature data.
     * The concrete visitor can use this for complete function processing.</p>
     *
     * @param funKeywordToken Token for 'fun' keyword
     * @param nameToken Token for function name (if present)
     * @param returnTypeTokens Return type tokens (if explicit)
     * @param typeParameters Type parameter data (if generic)
     * @param modifiers Extracted modifiers
     * @param parameters Extracted parameters
     */
    public record FunctionSignatureResult(
            @Nullable LocatableToken funKeywordToken,
            @Nullable LocatableToken nameToken,
            @Nullable List<LocatableToken> returnTypeTokens,
            @Nullable TypeParametersResult typeParameters,
            ModifierSet modifiers,
            FunctionParametersResult parameters
    ) {}

    /**
     * Result of type parameter extraction.
     *
     * @param hasTypeParameters Whether type parameters are present
     * @param typeParameters List of type parameter data
     */
    public record TypeParametersResult(
            boolean hasTypeParameters,
            List<TypeParameterData> typeParameters
    ) {
        public static TypeParametersResult empty() {
            return new TypeParametersResult(false, List.of());
        }
    }

    /**
     * Data for a single type parameter.
     *
     * @param nameToken Token for type parameter name
     * @param boundTokens Bound type tokens (if bounded)
     */
    public record TypeParameterData(
            @Nullable LocatableToken nameToken,
            @Nullable List<LocatableToken> boundTokens
    ) {}


    protected final JavaParserCallbacksAdapter callbacks;

    private LocatableToken tokenBase = null;
    private static LocatableToken lastToken;
    private int psiStartOffset = 0;

    /**
     * Creates a new method body visitor.
     *
     * @param callbacks The callback adapter for parser integration (must not be null)
     */
    public BaseVisitor(@NotNull JavaParserCallbacksAdapter callbacks) {
        this.callbacks = callbacks;
    }

    public LocatableToken getLastToken() {
        return callbacks.getLastToken();
    }

    public void clearLastToken() {
//        return callbacks.clear
    }

    protected BufferedTokenStream getTokenStream() {
        return this.callbacks.getTokenStream();
    }

//
//    /**
//     * Entry point for PSI traversal - visits a Kotlin file.
//     *
//     * <p>This method is the starting point for traversing a Kotlin PSI tree.
//     * It logs the file visit and then explicitly visits all declarations in the file.</p>
//     *
//     * <p><b>Critical:</b> Unlike some visitor patterns, Kotlin's {@link KtVisitorVoid}
//     * does NOT automatically traverse children when calling super. We must explicitly
//     * iterate over and visit each declaration.</p>
//     *
//     *
//     * <h3>Phase 2 Behavior</h3>
//     * <p>Logs: "VISIT: FILE: &lt;fileName&gt;"</p>
//     * <p>Then explicitly visits all top-level declarations (classes, functions, properties).</p>
//     *
//     * <h3>Phase 3 Migration</h3>
//     * <p>Will invoke {@code callbacks.beginParsing()} before traversal and
//     * {@code callbacks.endParsing()} after traversal.</p>
//     *
//     * @param file The Kotlin file PSI element to traverse
//     */
//    @Override
//    public void visitKtFile(@NotNull KtFile file) {
//        if (file == null) {
//            return; // Gracefully handle null
//        }
//
//        // Phase 2: Log file visit
//        String fileName = file.getName();
//
//        // TODO: no package statements, so let's assume 1
//        callbacks.reachedCUstate(1);
//
////        var facadeClassName = Paths.get(file.getName()).getFileName().toString().replace(".kt", "Kt");
//////
////        var topLevelStart = createTokenWithText(file.getFirstChild(), facadeClassName, JavaTokenTypes.LITERAL_class);
////
////        callbacks.gotDeclBegin(topLevelStart);
////        callbacks.gotTypeDef(topLevelStart, JavaTokenTypes.LITERAL_class);
////        callbacks.gotTypeDefName(topLevelStart);
////        callbacks.beginTypeBody(topLevelStart);
//
//        // Explicitly visit all declarations in the file
//        // Note: Kotlin PSI visitor requires explicit iteration over children
//        for (KtDeclaration declaration : file.getDeclarations()) {
//            declaration.accept(this);
//        }
//
//        // TODO: hack
//        var lastToken = getLastToken();
//
////        var topLevelEnd = createEofToken(file.getLastChild());
////
////        callbacks.endTypeBody(topLevelEnd, true);
////        callbacks.gotTypeDefEnd(topLevelEnd, true);
//
//        if (lastToken != null && (lastToken.getType() != JavaTokenTypes.EOF && lastToken.getType() != JavaTokenTypes.LCURLY)) {
//            callbacks.finishedCU(2); /// who the hell knows what that means xD
//        }
//    }

    @Override
    public void visitErrorElement(@NotNull PsiErrorElement error) {
        var errorRange = error.getTextRange();
        var positions = calculatePositions(error, errorRange.getStartOffset(), errorRange.getEndOffset());

        callbacks.error(error.getErrorDescription(), positions[0].line(), positions[0].column(), positions[1].line(), positions[1].column());
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
    protected List<LocatableToken> processPropertyType(KtProperty property) {
        KtTypeReference typeRef = property.getTypeReference();
        if (typeRef != null) {
            // Explicit type annotation
            return List.of(createToken(typeRef, JavaTokenTypes.IDENT));
        } else {
            // Type inference - mark as inferred
            // Phase 4 limitation: Cannot resolve inferred types without compiler integration
//            return createTokenWithText(property, "inferred", JavaTokenTypes.IDENT);
            return List.of();
        }
    }

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
    protected LocatableToken createToken(PsiElement element, int type) {
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

    protected LocatableToken createEofToken(PsiElement element) {
        if (element == null) {
            throw new IllegalArgumentException("PSI element must not be null");
        }

        return createTokenWithText(element, "", JavaTokenTypes.EOF);
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
    protected LocatableToken createTokenWithText(PsiElement element, String customText, int type) {
        if (element == null) {
            throw new IllegalArgumentException("PSI element must not be null");
        }

        int startOffset = element.getTextOffset();
        int endOffset = startOffset + customText.length();

        // Calculate positions using shared helper
        LineColPos[] positions = calculatePositions(element, startOffset, endOffset);

        var token = new LocatableToken(type, customText, positions[0], positions[1]);

        if (callbacks.isInEmitRange(token)) {
            this.lastToken = token;
        }

        return token;
    }

    /**
     * Helper method to create a token from PSI element with automatic type detection.
     * Used for expressions where we don't need a specific token type.
     */
    protected LocatableToken createToken(PsiElement element) {
        return createToken(element, guessTokenType(element));
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
    public LineColPos[] calculatePositions(PsiElement element, int startOffset, int endOffset) {
        // Get source file text for line/column calculation
        // Note: Document API (PsiDocumentManager) is unavailable in lightweight test PSI environments,
        // so we calculate positions directly from source text
        if (fileText == null) {
            PsiFile psiFile = element.getContainingFile();
            if (psiFile == null) {
                // Fallback if no containing file (shouldn't happen)
                return new LineColPos[]{
                        new LineColPos(1, startOffset, startOffset),
                        new LineColPos(1, endOffset, endOffset)
                };
            }

            fileText = psiFile.getText();
        }

        // Calculate start position
        int[] startLineCol = calculateLineColumn(fileText, startOffset);
        int startLine = startLineCol[0];
        int startColumn = startLineCol[1];

        // Calculate end position
        int[] endLineCol = calculateLineColumn(fileText, endOffset);
        int endLine = endLineCol[0];
        int endColumn = endLineCol[1];

//        if (tokenBase != null) {
//            if (startLine == 1) { startColumn += tokenBase.getColumn() - 1; };
//            if (endLine == 1) { endColumn += tokenBase.getColumn() - 1; };
//
//            startLine += tokenBase.getLine() - 1;
//            endLine += tokenBase.getLine() - 1;
//            startOffset += tokenBase.getPosition();
//            endOffset += tokenBase.getPosition();
//        }

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
    protected int determineTypeDefType(KtClass ktClass) {
        if (ktClass.isInterface()) {
            return JavaTokenTypes.LITERAL_interface;
        } else if (ktClass.isEnum()) {
            return JavaTokenTypes.LITERAL_enum;
        } else {
            // Regular class (includes data classes, sealed classes, etc.)
            return JavaTokenTypes.LITERAL_class;
        }
    }


    @Override
    public PsiElementVisitor asVisitor() {
        return this;
    }

    @Override
    public void setTokenBase(LocatableToken currentToken) {
        if (currentToken.getHiddenBefore() != null) {
            currentToken = currentToken.getHiddenBefore();
        }

        if (currentToken.getPosition() > 0) {
            this.tokenBase = currentToken;
        }
    }

    @Override
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

    @Override
    public LocatableToken getTokenBase() {
        if (this.tokenBase != null) { return this.tokenBase; }

        return new LocatableToken(
                JavaTokenTypes.LITERAL_void,
                "",
                new LineColPos(1, 1, 0),
                new LineColPos(1, 1, 0)
        );
    }

    @Override
    public void setEmitRangeStart(LocatableToken currentToken) {
        this.callbacks.setEmitRangeStart(currentToken);
//        var offset = currentToken.getPosition();
//
////        if (offset > 0) {
////            this.psiStartOffset = offset;
////        }
    }

    @Override
    public void setEmitRangeEnd(LocatableToken currentToken) {
        this.callbacks.setEmitRangeEnd(currentToken);
//        var offset = this.getTokenBase().getPosition();
//
//        if (offset > 0) {
//            this.psiStartOffset = currentToken.getPosition() - offset;
//        }
    }


    @Override
    public int getPsiStartOffset() {
        return this.psiStartOffset;
//        return this.getTokenBase().getPosition();
    }

    public void setPsiStartOffset(LocatableToken token) {
        var offset = token.getPosition();

        this.psiStartOffset = offset;
    }

    protected int guessTokenType(PsiElement element) {
        return switch (element) {
            case LeafPsiElement leaf -> {
                String name = leaf.getElementType().getDebugName();

                yield switch (name) {
                    case "LBRACE" -> JavaTokenTypes.LCURLY;
                    case "RBRACE" -> JavaTokenTypes.RCURLY;
                    case "LPARENTH" -> JavaTokenTypes.LPAREN;
                    case "RPARENTH" -> JavaTokenTypes.RPAREN;
                    case "COLON" -> JavaTokenTypes.COLON;
                    case "FUN" -> JavaTokenTypes.LITERAL_fun;
                    case "PLUSPLUS" -> JavaTokenTypes.INC;
                    case "MINUSMINUS" -> JavaTokenTypes.DEC;
                    case "DOT" -> JavaTokenTypes.DOT;
                    case "MINUS" -> JavaTokenTypes.MINUS;
                    case "PLUS" -> JavaTokenTypes.PLUS;
                    case "DIV" -> JavaTokenTypes.DIV;
                    case "ASTERISK" -> JavaTokenTypes.STAR;
                    default -> JavaTokenTypes.LITERAL_void;
                };
            }
            default -> JavaTokenTypes.LITERAL_void;
        };
//        return JavaTokenTypes.LITERAL_void;
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
    protected List<LocatableToken> extractTypeTokens(KtTypeReference typeRef) {
        if (typeRef == null) {
            return null; // List.of();
        }

        // Extract complete type text
        String typeText = typeRef.getText();
        if (typeText == null || typeText.isEmpty()) {
//            return List.of();
            return null;
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

    // ==================== Context-Agnostic Helper Methods ====================

    /**
     * Extracts modifiers from a Kotlin modifier list as pure data.
     *
     * <p><b>Context-Agnostic:</b> This method extracts modifier data without invoking
     * any callbacks. The concrete visitor decides what callbacks to invoke based on
     * its context (file-level vs class-level).</p>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * KtModifierList modifierList = function.getModifierList();
     * ModifierSet modifiers = extractModifiers(modifierList);
     *
     * // Concrete visitor decides what to do with modifiers
     * for (ModifierToken mt : modifiers.modifierTokens()) {
     *     callbacks.gotModifier(mt.token());
     * }
     * }</pre>
     *
     * @param modifierList The Kotlin modifier list (may be null)
     * @return ModifierSet containing all extracted modifier data
     */
    protected ModifierSet extractModifiers(@Nullable KtModifierList modifierList) {
        if (modifierList == null) {
            return ModifierSet.empty();
        }

        EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        List<ModifierToken> tokens = new ArrayList<>();
        LocatableToken firstToken = null;

        // Process visibility modifiers
        if (modifierList.hasModifier(KtTokens.PUBLIC_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.PUBLIC_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_public);
            modifiers.add(Modifier.PUBLIC);
            tokens.add(new ModifierToken(Modifier.PUBLIC, token, JavaTokenTypes.LITERAL_public));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.PRIVATE_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_private);
            modifiers.add(Modifier.PRIVATE);
            tokens.add(new ModifierToken(Modifier.PRIVATE, token, JavaTokenTypes.LITERAL_private));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.PROTECTED_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.PROTECTED_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_protected);
            modifiers.add(Modifier.PROTECTED);
            tokens.add(new ModifierToken(Modifier.PROTECTED, token, JavaTokenTypes.LITERAL_protected));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.INTERNAL_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_internal);
            modifiers.add(Modifier.INTERNAL);
            tokens.add(new ModifierToken(Modifier.INTERNAL, token, JavaTokenTypes.LITERAL_internal));
            if (firstToken == null) firstToken = token;
        }

        // Process inheritance modifiers
        if (modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.ABSTRACT_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.ABSTRACT);
            modifiers.add(Modifier.ABSTRACT);
            tokens.add(new ModifierToken(Modifier.ABSTRACT, token, JavaTokenTypes.ABSTRACT));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.FINAL_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.FINAL_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.FINAL);
            modifiers.add(Modifier.FINAL);
            tokens.add(new ModifierToken(Modifier.FINAL, token, JavaTokenTypes.FINAL));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.OPEN_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.OPEN_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_open);
            modifiers.add(Modifier.OPEN);
            tokens.add(new ModifierToken(Modifier.OPEN, token, JavaTokenTypes.LITERAL_open));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.OVERRIDE_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_override);
            modifiers.add(Modifier.OVERRIDE);
            tokens.add(new ModifierToken(Modifier.OVERRIDE, token, JavaTokenTypes.LITERAL_override));
            if (firstToken == null) firstToken = token;
        }

        // Process function-specific modifiers
        if (modifierList.hasModifier(KtTokens.OPERATOR_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.OPERATOR_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_operator);
            modifiers.add(Modifier.OPERATOR);
            tokens.add(new ModifierToken(Modifier.OPERATOR, token, JavaTokenTypes.LITERAL_operator));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.INFIX_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.INFIX_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_infix);
            modifiers.add(Modifier.INFIX);
            tokens.add(new ModifierToken(Modifier.INFIX, token, JavaTokenTypes.LITERAL_infix));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.INLINE_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.INLINE_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_inline);
            modifiers.add(Modifier.INLINE);
            tokens.add(new ModifierToken(Modifier.INLINE, token, JavaTokenTypes.LITERAL_inline));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.SUSPEND_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.SUSPEND_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_suspend);
            modifiers.add(Modifier.SUSPEND);
            tokens.add(new ModifierToken(Modifier.SUSPEND, token, JavaTokenTypes.LITERAL_suspend));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.TAILREC_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.TAILREC_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_void); // No direct mapping
            modifiers.add(Modifier.TAILREC);
            tokens.add(new ModifierToken(Modifier.TAILREC, token, JavaTokenTypes.LITERAL_void));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.EXTERNAL_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.EXTERNAL_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_void); // No direct mapping
            modifiers.add(Modifier.EXTERNAL);
            tokens.add(new ModifierToken(Modifier.EXTERNAL, token, JavaTokenTypes.LITERAL_void));
            if (firstToken == null) firstToken = token;
        }

        // Process property-specific modifiers
        if (modifierList.hasModifier(KtTokens.LATEINIT_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.LATEINIT_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_lateinit);
            modifiers.add(Modifier.LATEINIT);
            tokens.add(new ModifierToken(Modifier.LATEINIT, token, JavaTokenTypes.LITERAL_lateinit));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.CONST_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.CONST_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_const);
            modifiers.add(Modifier.CONST);
            tokens.add(new ModifierToken(Modifier.CONST, token, JavaTokenTypes.LITERAL_const));
            if (firstToken == null) firstToken = token;
        }

        // Process class-specific modifiers
        if (modifierList.hasModifier(KtTokens.DATA_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.DATA_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_void); // No direct mapping
            modifiers.add(Modifier.DATA);
            tokens.add(new ModifierToken(Modifier.DATA, token, JavaTokenTypes.LITERAL_void));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.SEALED_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.SEALED_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_void); // No direct mapping
            modifiers.add(Modifier.SEALED);
            tokens.add(new ModifierToken(Modifier.SEALED, token, JavaTokenTypes.LITERAL_void));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.COMPANION_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.COMPANION_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.LITERAL_void); // No direct mapping
            modifiers.add(Modifier.COMPANION);
            tokens.add(new ModifierToken(Modifier.COMPANION, token, JavaTokenTypes.LITERAL_void));
            if (firstToken == null) firstToken = token;
        }

        if (modifierList.hasModifier(KtTokens.VARARG_KEYWORD)) {
            PsiElement mod = modifierList.getModifier(KtTokens.VARARG_KEYWORD);
            LocatableToken token = createToken(mod, JavaTokenTypes.TRIPLE_DOT);
            modifiers.add(Modifier.VARARG);
            tokens.add(new ModifierToken(Modifier.VARARG, token, JavaTokenTypes.TRIPLE_DOT));
            if (firstToken == null) firstToken = token;
        }

        return new ModifierSet(modifiers, tokens, firstToken);
    }

    /**
     * Extracts function parameter list data as pure data.
     *
     * <p><b>Context-Agnostic:</b> This method extracts parameter data without invoking
     * any callbacks. The concrete visitor decides what callbacks to invoke based on
     * its context.</p>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * FunctionParametersResult params = extractFunctionParameters(function);
     * if (params.hasParameterList()) {
     *     callbacks.skipToToken(params.leftParenToken());
     *     callbacks.gotMethodDeclaration(nameToken, null);
     *
     *     for (ParameterData param : params.parameters()) {
     *         callbacks.beginFormalParameter(param.firstToken());
     *         callbacks.gotTypeSpec(param.typeTokens());
     *         callbacks.gotMethodParameter(param.nameToken(), param.varargToken());
     *     }
     *
     *     if (params.rightParenToken() != null) {
     *         callbacks.skipToToken(params.rightParenToken());
     *         callbacks.gotAllMethodParameters();
     *     }
     * }
     * }</pre>
     *
     * @param function The function to extract parameters from
     * @return FunctionParametersResult containing all parameter data
     */
    protected FunctionParametersResult extractFunctionParameters(@NotNull KtNamedFunction function) {
        KtParameterList paramList = function.getValueParameterList();

        if (paramList == null || paramList.getLeftParenthesis() == null) {
            return FunctionParametersResult.empty();
        }

        LocatableToken leftParen = createToken(paramList.getLeftParenthesis(), JavaTokenTypes.LPAREN);
        LocatableToken rightParen = paramList.getRightParenthesis() != null
                ? createToken(paramList.getRightParenthesis(), JavaTokenTypes.RPAREN)
                : null;

        List<ParameterData> parameters = new ArrayList<>();
        LocatableToken lastToken = leftParen;

        for (KtParameter param : function.getValueParameters()) {
            LocatableToken firstToken = createToken(param.getFirstChild(), JavaTokenTypes.LITERAL_void);
            LocatableToken nameToken = param.getNameIdentifier() != null
                    ? createToken(param.getNameIdentifier(), JavaTokenTypes.IDENT)
                    : null;
            LocatableToken colonToken = param.getColon() != null
                    ? createToken(param.getColon(), JavaTokenTypes.COLON)
                    : null;

            List<LocatableToken> typeTokens = extractTypeTokens(param.getTypeReference());

            // Check for vararg
            boolean isVararg = param.hasModifier(KtTokens.VARARG_KEYWORD);
            LocatableToken varargToken = null;
            if (isVararg) {
                KtModifierList paramModifiers = param.getModifierList();
                if (paramModifiers != null) {
                    PsiElement varargMod = paramModifiers.getModifier(KtTokens.VARARG_KEYWORD);
                    if (varargMod != null) {
                        varargToken = createToken(varargMod, JavaTokenTypes.TRIPLE_DOT);
                    }
                }
            }

            parameters.add(new ParameterData(nameToken, colonToken, typeTokens, isVararg, varargToken, firstToken));

            if (nameToken != null) {
                lastToken = nameToken;
            }
            if (typeTokens != null && !typeTokens.isEmpty()) {
                lastToken = typeTokens.get(typeTokens.size() - 1);
            }
        }

        if (rightParen != null) {
            lastToken = rightParen;
        }

        return new FunctionParametersResult(true, leftParen, rightParen, parameters, lastToken);
    }

    /**
     * Extracts function body data as pure data.
     *
     * <p><b>Context-Agnostic:</b> This method extracts body structure data without
     * invoking any callbacks or traversing body contents. The concrete visitor
     * decides how to handle the body based on its context.</p>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * FunctionBodyResult body = extractFunctionBody(function);
     * if (body.hasBody()) {
     *     if (body.isBlockBody()) {
     *         callbacks.beginMethodBody(body.lBraceToken());
     *
     *         // Delegate to MethodBodyVisitor
     *         MethodBodyVisitor bodyVisitor = new MethodBodyVisitor(callbacks);
     *         body.bodyExpression().accept(bodyVisitor);
     *
     *         if (body.isBlockComplete()) {
     *             callbacks.endMethodBody(body.rBraceToken(), true);
     *         }
     *     } else if (body.isExpressionBody()) {
     *         // Handle expression body
     *         body.bodyExpression().accept(bodyVisitor);
     *     }
     * }
     * }</pre>
     *
     * @param function The function to extract body from
     * @return FunctionBodyResult containing body structure data
     */
    protected FunctionBodyResult extractFunctionBody(@NotNull KtNamedFunction function) {
        if (!function.hasBody()) {
            return FunctionBodyResult.empty();
        }

        if (function.hasBlockBody() && function.getBodyBlockExpression() != null) {
            KtBlockExpression block = function.getBodyBlockExpression();
            PsiElement lBrace = block.getLBrace();
            PsiElement rBrace = block.getRBrace();

            LocatableToken lBraceToken = lBrace != null
                    ? createToken(lBrace, JavaTokenTypes.LCURLY)
                    : null;
            LocatableToken rBraceToken = rBrace != null
                    ? createToken(rBrace, JavaTokenTypes.RCURLY)
                    : null;

            LocatableToken lastToken = rBraceToken != null ? rBraceToken : lBraceToken;

            return new FunctionBodyResult(
                    true, true, false,
                    lBraceToken, rBraceToken, null,
                    block, lastToken
            );
        } else if (function.hasBody()) {
            // Expression body: fun method() = expr
            KtExpression body = function.getBodyExpression();
            PsiElement equalsElement = function.getEqualsToken();

            LocatableToken equalsToken = equalsElement != null
                    ? createToken(equalsElement, JavaTokenTypes.ASSIGN)
                    : null;

            LocatableToken lastToken = body != null
                    ? createToken(body.getLastChild())
                    : equalsToken;

            return new FunctionBodyResult(
                    true, false, true,
                    null, null, equalsToken,
                    body, lastToken
            );
        }

        return FunctionBodyResult.empty();
    }

    /**
     * Extracts complete function signature data as pure data.
     *
     * <p><b>Context-Agnostic:</b> This method combines modifier, parameter, and
     * return type extraction into a single result. The concrete visitor can use
     * this for complete function processing.</p>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * FunctionSignatureResult sig = extractFunctionSignature(function);
     *
     * // Begin declaration
     * LocatableToken declToken = sig.modifiers().firstModifierToken();
     * if (declToken == null) declToken = sig.funKeywordToken();
     * callbacks.gotDeclBegin(declToken);
     *
     * // Process modifiers
     * for (ModifierToken mt : sig.modifiers().modifierTokens()) {
     *     callbacks.gotModifier(mt.token());
     * }
     *
     * // Return type
     * callbacks.gotTypeSpec(sig.returnTypeTokens());
     *
     * // ... continue with parameters, etc.
     * }</pre>
     *
     * @param function The function to extract signature from
     * @return FunctionSignatureResult containing all signature data
     */
    protected FunctionSignatureResult extractFunctionSignature(@NotNull KtNamedFunction function) {
        // Fun keyword
        PsiElement funKeyword = function.getFunKeyword();
        LocatableToken funKeywordToken = funKeyword != null
                ? createToken(funKeyword, JavaTokenTypes.LITERAL_fun)
                : null;

        // Name
        PsiElement nameId = function.getNameIdentifier();
        LocatableToken nameToken = nameId != null
                ? createToken(nameId, JavaTokenTypes.IDENT)
                : null;

        // Return type
        KtTypeReference returnTypeRef = function.getTypeReference();
        List<LocatableToken> returnTypeTokens = extractTypeTokens(returnTypeRef);

        // Modifiers
        ModifierSet modifiers = extractModifiers(function.getModifierList());

        // Type parameters
        TypeParametersResult typeParams = extractTypeParameters(function.getTypeParameterList());

        // Parameters
        FunctionParametersResult parameters = extractFunctionParameters(function);

        return new FunctionSignatureResult(
                funKeywordToken, nameToken, returnTypeTokens,
                typeParams, modifiers, parameters
        );
    }

    /**
     * Extracts type parameter data as pure data.
     *
     * <p><b>Context-Agnostic:</b> This method extracts generic type parameter data
     * without invoking any callbacks.</p>
     *
     * @param typeParamList The type parameter list (may be null)
     * @return TypeParametersResult containing type parameter data
     */
    protected TypeParametersResult extractTypeParameters(@Nullable KtTypeParameterList typeParamList) {
        if (typeParamList == null || typeParamList.getParameters().isEmpty()) {
            return TypeParametersResult.empty();
        }

        List<TypeParameterData> typeParams = new ArrayList<>();

        for (KtTypeParameter typeParam : typeParamList.getParameters()) {
            PsiElement nameId = typeParam.getNameIdentifier();
            LocatableToken nameToken = nameId != null
                    ? createToken(nameId, JavaTokenTypes.IDENT)
                    : null;

            KtTypeReference bound = typeParam.getExtendsBound();
            List<LocatableToken> boundTokens = extractTypeTokens(bound);

            typeParams.add(new TypeParameterData(nameToken, boundTokens));
        }

        return new TypeParametersResult(true, typeParams);
    }

    /**
     * Extracts constructor parameter data as pure data.
     *
     * <p><b>Context-Agnostic:</b> This method extracts constructor parameter data
     * without invoking any callbacks. Works for both primary and secondary constructors.</p>
     *
     * @param constructor The constructor to extract parameters from
     * @return FunctionParametersResult containing parameter data
     */
    protected FunctionParametersResult extractConstructorParameters(@NotNull KtConstructor<?> constructor) {
        KtParameterList paramList = constructor.getValueParameterList();

        if (paramList == null || paramList.getLeftParenthesis() == null) {
            return FunctionParametersResult.empty();
        }

        LocatableToken leftParen = createToken(paramList.getLeftParenthesis(), JavaTokenTypes.LPAREN);
        LocatableToken rightParen = paramList.getRightParenthesis() != null
                ? createToken(paramList.getRightParenthesis(), JavaTokenTypes.RPAREN)
                : null;

        List<ParameterData> parameters = new ArrayList<>();
        LocatableToken lastToken = leftParen;

        for (KtParameter param : constructor.getValueParameters()) {
            LocatableToken firstToken = createToken(param.getFirstChild(), JavaTokenTypes.LITERAL_void);
            LocatableToken nameToken = param.getNameIdentifier() != null
                    ? createToken(param.getNameIdentifier(), JavaTokenTypes.IDENT)
                    : null;
            LocatableToken colonToken = param.getColon() != null
                    ? createToken(param.getColon(), JavaTokenTypes.COLON)
                    : null;

            List<LocatableToken> typeTokens = extractTypeTokens(param.getTypeReference());

            // Check for vararg
            boolean isVararg = param.hasModifier(KtTokens.VARARG_KEYWORD);
            LocatableToken varargToken = null;
            if (isVararg) {
                KtModifierList paramModifiers = param.getModifierList();
                if (paramModifiers != null) {
                    PsiElement varargMod = paramModifiers.getModifier(KtTokens.VARARG_KEYWORD);
                    if (varargMod != null) {
                        varargToken = createToken(varargMod, JavaTokenTypes.TRIPLE_DOT);
                    }
                }
            }

            parameters.add(new ParameterData(nameToken, colonToken, typeTokens, isVararg, varargToken, firstToken));

            if (nameToken != null) {
                lastToken = nameToken;
            }
            if (typeTokens != null && !typeTokens.isEmpty()) {
                lastToken = typeTokens.get(typeTokens.size() - 1);
            }
        }

        if (rightParen != null) {
            lastToken = rightParen;
        }

        return new FunctionParametersResult(true, leftParen, rightParen, parameters, lastToken);
    }

    /**
     * Finds the last non-error element in a PSI tree.
     *
     * @param element The root element to search from
     * @return The last non-error element
     */
    protected PsiElement findLastNonErrorElement(PsiElement element) {
        PsiElement lastElement = element;
        while (true) {
            if (lastElement.getChildren().length == 0) {
                break;
            }

            var children = lastElement.getChildren();
            var onlyErrors = true;
            for (int i = children.length - 1; i >= 0; i--) {
                var child = children[i];
                if (!(child instanceof PsiErrorElement)) {
                    lastElement = child;
                    onlyErrors = false;
                    break;
                }
            }
            if (onlyErrors) {
                break;
            }
        }
        return lastElement;
    }

    public void parseTypeDefPart2(boolean value) {
        parseTypeDefPart2 = value;
    }

    public class ParserHackException extends RuntimeException {}
    public class ParseTypeDefPart2FinishedHackException extends ParserHackException {}
    public class ParseClassBodyFinishedHackException extends ParserHackException {}
}
