package bluej.parser.psi.visitor;

import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.psi.JavaParserCallbacksAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.TokenType;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.lexer.KotlinLexer;
import org.jetbrains.kotlin.psi.*;

import java.util.List;

public class BaseVisitor extends KtVisitorVoid implements PsiVisitor {

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
        return lastToken;
    }

    void clearLastToken() {
        lastToken = null;
    }

    protected JavaTokenFilter getTokenStream() {
        return this.callbacks.getTokenStream();
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

        // TODO: no package statements, so let's assume 1
        callbacks.reachedCUstate(1);

        // Explicitly visit all declarations in the file
        // Note: Kotlin PSI visitor requires explicit iteration over children
        for (KtDeclaration declaration : file.getDeclarations()) {
            declaration.accept(this);
        }

        // TODO: hack
        var lastToken = getLastToken();
        if (lastToken != null && (lastToken.getType() != JavaTokenTypes.EOF && lastToken.getType() != JavaTokenTypes.LCURLY)) {
            callbacks.finishedCU(2); /// who the hell knows what that means xD
        }
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

        this.lastToken = token;

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
        var offset = currentToken.getPosition();

        if (offset > 0) {
            this.psiStartOffset = offset;
        }
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

    protected int guessTokenType(PsiElement element) {
        return switch (element) {
            case LeafPsiElement leaf -> {
                String name = leaf.getElementType().getDebugName();

                yield switch (name) {
                    case "LBRACE" -> JavaTokenTypes.LCURLY;
                    case "RBRACE" -> JavaTokenTypes.RCURLY;
                    default -> JavaTokenTypes.LITERAL_void;
                };
            }
            default -> JavaTokenTypes.LITERAL_void;
        };
//        return JavaTokenTypes.LITERAL_void;
    }
}
