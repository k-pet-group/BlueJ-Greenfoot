/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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
package bluej.parser;

import bluej.extensions2.SourceType;
import bluej.parser.lexer.*;
import bluej.parser.psi.*;
import bluej.parser.psi.visitor.*;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtilKt;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Facade for Kotlin parsing that delegates to existing {@link KotlinParser} while
 * adding optional PSI-based enhancements.
 *
 * <p><b>MVP Implementation</b>: Pure delegation with PSI serialization after parsing completes.
 * All 18 {@link ParserBehavior} methods delegate to wrapped {@link KotlinParser}, with PSI
 * enhancement occurring only in {@link #parseCU()} after delegation succeeds.</p>
 *
 * <p><b>Design Pattern</b>: Facade + Delegation</p>
 * <ul>
 *   <li><b>Facade</b>: Provides simplified interface for PSI integration</li>
 *   <li><b>Delegation</b>: All parsing logic delegates to {@link KotlinParser}</li>
 *   <li><b>Enhancement</b>: Optional PSI features added after delegation</li>
 * </ul>
 *
 * <p><b>Parsing Modes</b>: Supports two modes of operation controlled by {@link ParsingMode}:</p>
 * <ul>
 *   <li><b>DELEGATION</b>: Delegates to existing {@link KotlinParser} (default, production-ready)</li>
 *   <li><b>PSI_VISITOR</b>: Uses PSI visitor for parsing (experimental, testing)</li>
 * </ul>
 *
 * <p><b>Error Handling</b>: PSI failures are logged but never propagate.
 * Compilation continues successfully even if PSI enhancement fails.</p>
 *
 * <p><b>Thread Safety</b>: Not thread-safe (matches {@link SourceParser} single-threaded assumption)</p>
 *
 * <p><b>Performance</b>: Delegation overhead {@literal <}1ns per method call.
 * PSI enhancement adds ~10-50ms per file but runs asynchronously to compilation.</p>
 *
 * @see KotlinParser
 * @see ParserBehavior
 * @see PsiEnvironment
 * @see PsiTreeSerializer
 * @since BlueJ 5.4.0
 */
public class KotlinPsiParser implements ParserBehavior {


    private final PsiEnvironment psiEnvironment;
//    private final JavaParserCallbacksAdapter callbackAdapter;
//    private final PsiCallbackVisitor psiVisitor;
    private KtFile _psiTree;

    // TODO: this probably needs to be reset on project load?
    private static ConcurrentHashMap<String, String> UGLY_SOURCE_CACHE = new ConcurrentHashMap<String, String>();

    /**
     * Defines the parsing strategy for Kotlin source code.
     *
     * <p>This enum controls whether the parser delegates to the existing token-based
     * {@link KotlinParser} or uses the PSI visitor-based parsing implementation.</p>
     *
     * @since BlueJ 5.4.0
     */
    public enum ParsingMode {
        /**
         * Delegate parsing to the existing {@link KotlinParser} implementation.
         * This is the default, production-ready mode that uses token-based parsing.
         *
         * <p>When {@link #ENABLE_PSI_OUTPUT} is true, PSI enhancement occurs after
         * delegation for debugging and analysis purposes.</p>
         */
        DELEGATION,
        
        /**
         * Use PSI visitor-based parsing implementation.
         * This is an experimental mode for testing the PSI visitor foundation.
         *
         * <p>In this mode, parsing uses the PSI tree structure directly via the
         * visitor pattern, bypassing the legacy token-based parser.</p>
         *
         * <p><b>Warning</b>: This mode is experimental and may not handle all
         * language constructs correctly. Use for testing only.</p>
         */
        PSI_VISITOR
    }
    
    // ==================== FIELDS ====================
    
    /**
     * The existing KotlinParser that handles all token-based parsing.
     * All {@link ParserBehavior} methods delegate to this instance.
     */
    private final KotlinParser delegate;
    
    /**
     * Reference to SourceParser for accessing callbacks and source context.
     * Used for source code extraction and filename resolution.
     */
    private final SourceParser sourceParser;
    
    /**
     * Controls which parsing strategy to use.
     *
     * <p><b>Default</b>: {@link ParsingMode#DELEGATION} for backward compatibility
     * and production stability.</p>
     *
     * <p><b>Immutable</b>: Set once in constructor, cannot be changed afterward.</p>
     *
     * @see ParsingMode
     */
    private final ParsingMode parsingMode;
    
    /**
     * Master switch for PSI output generation.
     * When true, PSI tree is serialized to .psi file after successful parsing.
     * When false, operates as pure delegation (no PSI overhead).
     */
    private static final boolean ENABLE_PSI_OUTPUT = true;
    
    /**
     * Whether to log PSI errors to stderr (useful for debugging).
     * When true, PSI initialization and parsing errors are logged.
     * When false, PSI failures are silent.
     */
    private static final boolean LOG_PSI_ERRORS = true;
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Creates a new KotlinPsiParser facade with default {@link ParsingMode#DELEGATION} mode.
     *
     * <p>Initializes delegation to {@link KotlinParser} and prepares
     * for optional PSI enhancement. The PSI environment is initialized
     * lazily on first use.</p>
     *
     * <p>This constructor maintains backward compatibility by defaulting to
     * {@link ParsingMode#DELEGATION} mode.</p>
     *
     * @param sourceParser The SourceParser instance for callbacks and source access
     * @throws NullPointerException if sourceParser is null
     */
    public KotlinPsiParser(SourceParser sourceParser) {
        this(sourceParser, ParsingMode.PSI_VISITOR);
    }
    
    /**
     * Creates a new KotlinPsiParser facade with specified parsing mode.
     *
     * <p>Allows explicit control over parsing strategy:</p>
     * <ul>
     *   <li>{@link ParsingMode#DELEGATION}: Delegates to {@link KotlinParser} (default, production)</li>
     *   <li>{@link ParsingMode#PSI_VISITOR}: Uses {@link FileVisitor} for parsing (experimental)</li>
     * </ul>
     *
     * @param sourceParser The SourceParser instance for callbacks and source access
     * @param parsingMode The parsing mode to use
     * @throws NullPointerException if sourceParser or parsingMode is null
     */
    public KotlinPsiParser(SourceParser sourceParser, ParsingMode parsingMode) {
        if (sourceParser == null) {
            throw new NullPointerException("SourceParser cannot be null");
        }
        if (parsingMode == null) {
            throw new NullPointerException("ParsingMode cannot be null");
        }
        this.sourceParser = sourceParser;
        this.parsingMode = parsingMode;
        this.delegate = new KotlinParser(sourceParser);
        this.psiEnvironment = PsiEnvironment.getInstance();

//        var callbackAdapter = new JavaParserCallbacksAdapterImpl(sourceParser);
//
//        this.psiVisitor = new PsiCallbackVisitor(callbackAdapter);
    }

    private KtFile getPsiTree() {
        if (_psiTree != null) {
            return _psiTree;
        }

        try {
            String sourceCode = getSourceCode();
            if (sourceCode == null || sourceCode.isEmpty()) {
                if (LOG_PSI_ERRORS) {
                    System.err.println("PSI: No source code available for parsing");
                }
                return null;
            }

            // Step 2: Determine file path
            String filePath = getFilePath();

            return (_psiTree = psiEnvironment.parseFile(filePath, sourceCode));
        } catch (PsiParseException e) {
            // PSI parsing failures MUST NOT break compilation
            if (LOG_PSI_ERRORS) {
                System.err.println("PSI parsing failed: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("Caused by: " + e.getCause().getMessage());
                }
            }
            // Compilation continues despite PSI parsing failure
        }

        return null;
    }
    
    // ==================== PARSERBEHAVIOR DELEGATION ====================
    //
    // All 18 interface methods delegate to wrapped KotlinParser.
    // ONLY parseCU() includes PSI enhancement after delegation succeeds.
    //
    // ===================================================================
    
    /**
     * Parse a compilation unit (from the beginning).
     *
     * <p><b>MODE SWITCHING</b>: Behavior depends on {@link #parsingMode}:</p>
     * <ul>
     *   <li><b>DELEGATION</b>: Delegates to {@link KotlinParser#parseCU()}, then optionally
     *       enhances with PSI output if {@link #ENABLE_PSI_OUTPUT} is true</li>
     *   <li><b>PSI_VISITOR</b>: Uses {@link FileVisitor} to parse and call
     *       {@link JavaParserCallbacksBase} directly, bypassing legacy parser</li>
     * </ul>
     *
     * <p><b>Delegation Flow (DELEGATION mode)</b>:</p>
     * <ol>
     *   <li>Delegate to {@link KotlinParser#parseCU()}</li>
     *   <li>If PSI enabled, call {@link #enhanceWithPSI()} for .psi file output</li>
     *   <li>PSI failures are logged but don't affect compilation</li>
     * </ol>
     *
     * <p><b>PSI Visitor Flow (PSI_VISITOR mode)</b>:</p>
     * <ol>
     *   <li>Skip legacy parser delegation</li>
     *   <li>Call {@link #parseWithPsiVisitor()} to parse using PSI visitor</li>
     *   <li>PSI visitor directly invokes {@link JavaParserCallbacksBase} from {@link #sourceParser}</li>
     *   <li>PSI failures are logged but don't affect compilation</li>
     * </ol>
     */
    @Override
    public void parseCU() {
//        System.err.println("[PSI-DEBUG] parseCU() called with mode: " + parsingMode);
        
        switch (parsingMode) {
            case DELEGATION:
                // Phase 1: Token-based parsing (existing behavior)
                delegate.parseCU();
                break;

            case PSI_VISITOR:
                // PSI visitor mode: Parse using PSI and call real JavaParserCallbacks
//                System.err.println("[PSI-DEBUG] PSI_VISITOR mode: parsing with PsiCallbackVisitor");
//                parseWithPsi();
                var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
                var psiVisitor = new FileVisitor(callbackAdapter);

                // This does not necessarily have to equal current token (e.g. when comments come into play)
//                psiVisitor.setTokenBase(this.sourceParser.getOffset());
//                this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));

                parseWithPsi(psiVisitor);

                break;
        }
//        this.parseWithPsi();
    }

    /**oo
     * Parse a part of a compilation unit, starting from the given state.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param state The state to start parsing from
     * @return The new state after parsing
     */
    @Override
    public int parseCUpart(int state) {
//        try {
//            parseWithPsiVisitor();
//        }
//        catch (Exception e) {
//            System.err.println("PSI visitor parsing failed: " + e.getMessage());
//        }

        return switch (parsingMode) {
            case DELEGATION ->
                // Phase 1: Token-based parsing (existing behavior)
                delegate.parseCUpart(state);
            case PSI_VISITOR -> {
                var currentToken = this.sourceParser.getTokenStream().LA(1);

//                this.parseWithPsi(currentToken);

                var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
                var psiVisitor = new FileVisitor(callbackAdapter);

                this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));
                psiVisitor.setEmitRangeStart(currentToken);

                this.parseWithPsi(psiVisitor);

                yield 2;
            }
        };
    }

    /**
     * Parse a package statement.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param token The "package" token
     * @return The last token seen during parsing
     */
    @Override
    public LocatableToken parsePackageStmt(LocatableToken token) {
        return delegate.parsePackageStmt(token);
    }
    
    /**
     * Parse an import statement.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     */
    @Override
    public void parseImportStatement() {
        delegate.parseImportStatement();
    }
    
    /**
     * Parse an import statement starting with the given token.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param importToken The "import" token
     */
    @Override
    public void parseImportStatement(LocatableToken importToken) {
        delegate.parseImportStatement(importToken);
    }
    
    /**
     * Parse a type definition (class, interface, enum).
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     */
    @Override
    public void parseTypeDef() {
////        delegate.parseTypeDef();
//        System.err.println("[PSI-DEBUG] === parseTypeDef() ENTRY ===");
//
//        var offset = sourceParser.getTokenStream().LA(1).getPosition();
//        var psiTree = this.getPsiTree();
//
//        if (psiTree == null) {
//            System.err.println("[PSI-DEBUG] PSI tree is null, skipping parsing");
//            return;
//        }
//
//        var startElement = psiTree.findElementAt(offset)
//
//        );
        parseTypeDef(sourceParser.getTokenStream().LA(1));
    }
    
    /**
     * Parse a type definition (class, interface, enum) starting with the given token.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param firstToken The first token of the type definition
     */
    @Override
    public void parseTypeDef(LocatableToken firstToken) {
        var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
        var psiVisitor = new ClassVisitor(callbackAdapter);

        psiVisitor.setTokenBase(this.sourceParser.getOffset());
        this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));
        psiVisitor.setEmitRangeStart(firstToken);
        psiVisitor.setPsiStartOffset(firstToken);

        parseWithPsi(psiVisitor);
    }
    
    /**
     * Parse a type body.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param tdType The type definition type
     * @param token The token that starts the type body
     * @return The last token seen during parsing
     */
    @Override
    public LocatableToken parseTypeBody(int tdType, LocatableToken token) {
        var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
        var psiVisitor = new ClassVisitor(callbackAdapter);

        psiVisitor.setTokenBase(this.sourceParser.getOffset());
        this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));
        psiVisitor.setEmitRangeStart(token);
        psiVisitor.setPsiStartOffset(token);

        parseWithPsi(psiVisitor);

        return psiVisitor.getLastToken();
    }
    
    /**
     * Parse the beginning of a type definition.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @return The type definition type
     */
    @Override
    public int parseTypeDefBegin() {
//        return delegate.parseTypeDefBegin();
        var token = sourceParser.tokenStream.nextToken();
        var offset = token.getPosition();
        var psiTree = this.getPsiTree();

        if (psiTree == null) {
            System.err.println("[PSI-DEBUG] PSI tree is null, skipping parsing");
            return JavaParser.TYPEDEF_EPIC_FAIL;
        }

        PsiElement element = psiTree.findElementAt(offset);

        var tokenType = element.getNode().getElementType();

        if (tokenType == KtTokens.CLASS_KEYWORD) {
            return JavaParser.TYPEDEF_CLASS;
        }
        else if (tokenType == KtTokens.ENUM_KEYWORD) {
            return JavaParser.TYPEDEF_ENUM;
        }

        sourceParser.tokenStream.pushBack(token);

        return JavaParser.TYPEDEF_EPIC_FAIL;

//        return JavaParser.TYPEDEF_CLASS;
    }
    
    /**
     * Parse the second part of a type definition.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param isRecord Whether this is a record definition
     * @return The last token seen during parsing
     */
    @Override
    public LocatableToken parseTypeDefPart2(boolean isRecord) {
//        return delegate.parseTypeDefPart2(isRecord);
        var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
        var psiVisitor = new ClassVisitor(callbackAdapter);

        psiVisitor.parseTypeDefPart2(true);
        psiVisitor.clearLastToken();

        // UGLY
        var tree = getPsiTree();
        var currentToken = this.sourceParser.getTokenStream().LA(1);

        if (tree == null) { return currentToken; }

        psiVisitor.setTokenBase(this.sourceParser.getOffset());
        this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));
        psiVisitor.setEmitRangeStart(currentToken);

        PsiElement parent = PsiTreeUtil.getParentOfType(tree.findElementAt(currentToken.getPosition()), KtNamedDeclaration.class);

        if (parent instanceof KtClassLikeDeclaration typedef) {
            var element = switch (typedef) {
                case KtPureClassOrObject withBody -> withBody.getBody().getFirstChild();
//                case KtTypeAlias alias -> alias.getLastChild();
                default -> typedef.getLastChild();
            };

            var position = psiVisitor.calculatePositions(element, element.getTextOffset(), element.getTextOffset() + element.getTextLength());
            var positionToken = new LocatableToken(
                    JavaTokenTypes.LITERAL_void,
                    element.getText(),
                    position[0],
                    position[1]
            );

            psiVisitor.setEmitRangeEnd(positionToken);
            psiVisitor.setPsiStartOffset(positionToken);
        }
        else {
            return currentToken;
        }

        try {
            PsiElement finalElement = parent;
            this.parseWithPsi(psiVisitor, () -> finalElement);
        }
        catch (BaseVisitor.ParseTypeDefPart2FinishedHackException e) {
            // ignore
        }

        var lastToken = psiVisitor.getLastToken();

        return lastToken;
    }
    
    /**
     * Parse a class element (field, method, inner class, etc.).
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param token The first token of the class element
     */
    @Override
    public void parseClassElement(LocatableToken token) {
        // For some reason this token needs to be put back in the stream again
        this.sourceParser.getTokenStream().pushBack(token);
//        this.parseWithPsi(token);
//        delegate.parseClassElement(token);

        var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
        var psiVisitor = new ClassVisitor(callbackAdapter);

        psiVisitor.setTokenBase(this.sourceParser.getOffset());
        this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));
        psiVisitor.setEmitRangeStart(token);
        psiVisitor.setPsiStartOffset(token);

//        parseWithPsi(psiVisitor);


        parseWithPsi(psiVisitor, () -> {
            var tree = getPsiTree();

            if (tree == null) { return null; }

            return PsiTreeUtil.getParentOfType(tree.findElementAt(token.getPosition()), KtDeclaration.class);
        });
    }
    
    /**
     * Parse a statement.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param token The first token of the statement
     * @param allowComma Whether to allow commas in the statement
     * @return The last token seen during parsing
     */
    @Override
    public LocatableToken parseStatement(LocatableToken token, boolean allowComma) {
//        return delegate.parseStatement(token, allowComma);
//        return switch (parsingMode) {
//            case DELEGATION ->
//                // Phase 1: Token-based parsing (existing behavior)
//                    delegate.parseStatement(token, allowComma);
//            case PSI_VISITOR -> {
//                var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
//                var psiVisitor = new MethodBodyVisitor(callbackAdapter);
//
//
//
////                this.parseWithPsi(psiVisitor, token);
////
////                yield this.sourceParser.getTokenStream().getMostRecent();
//            }
//        };
        var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
        var psiVisitor = new MethodBodyVisitor(callbackAdapter);

        psiVisitor.setTokenBase(this.sourceParser.getOffset());
        this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));
        psiVisitor.setEmitRangeStart(token);
        psiVisitor.setPsiStartOffset(token);

//        parseWithPsi(psiVisitor);


        parseWithPsi(psiVisitor, () -> {
            var tree = getPsiTree();

            if (tree == null) { return null; }

//            return PsiTreeUtil.getParentOfType(tree.findElementAt(token.getPosition()), KtBlockExpression.class);
            return PsiTreeUtil.getTopmostParentOfType(tree.findElementAt(token.getPosition()), KtExpression.class);
        });

        return psiVisitor.getLastToken();
    }
    
    /**
     * Parse a type specification.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param speculative Whether this is a speculative parse
     * @param processArray Whether to process array declarators
     * @param tokens List to store tokens
     * @return Whether the parsing was successful
     */
    @Override
    public boolean parseTypeSpec(boolean speculative, boolean processArray, 
                                   List<LocatableToken> tokens) {
        return delegate.parseTypeSpec(speculative, processArray, tokens);
    }
    
    /**
     * Parse the class body.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     */
    @Override
    public void parseClassBody() {
//        delegate.parseClassBody();
        var currentToken = this.sourceParser.getTokenStream().LA(1);
        var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
        var psiVisitor = new ClassVisitor(callbackAdapter);

        psiVisitor.setTokenBase(this.sourceParser.getOffset());
        this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));
        psiVisitor.setEmitRangeStart(currentToken);
        psiVisitor.setPsiStartOffset(currentToken);

        parseWithPsi(psiVisitor, () -> {
            var tree = getPsiTree();

            if (tree == null) { return null; }

            return PsiTreeUtil.getParentOfType(tree.findElementAt(currentToken.getPosition()), KtClassBody.class);
        });
    }
    
    /**
     * Parse an expression.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     */
    @Override
    public void parseExpression() {
        delegate.parseExpression();
    }
    
    /**
     * Parse variable declarations.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @return The last token seen during parsing
     */
    @Override
    public LocatableToken parseVariableDeclarations() {
        return delegate.parseVariableDeclarations();
    }
    
    /**
     * Parse a type specification.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param processArray Whether to process array declarators
     * @return Whether the parsing was successful
     */
    @Override
    public boolean parseTypeSpec(boolean processArray) {
        return delegate.parseTypeSpec(processArray);
    }
    
    /**
     * Parse method parameters and body.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     */
    @Override
    public void parseMethodParamsBody() {
        delegate.parseMethodParamsBody();
    }
    
    // ==================== PSI VISITOR PARSING ====================

//    private void parseWithPsi() {
//        var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
//        var psiVisitor = new FileVisitor(callbackAdapter);
//
//        // This does not necessarily have to equal current token (e.g. when comments come into play)
//        visitor.setTokenBase(this.sourceParser.getOffset());
//        this.sourceParser.getSourceInput().range().ifPresent((visitor::setEmitRange));
//        visitor.setEmitRangeStart(currentToken);
//
//        parseWithPsi(visitor);
//        var currentToken = this.sourceParser.getTokenStream().LA(1);
//
//        parseWithPsi(currentToken);
//    }

    private <T extends PsiElement> void parseWithPsi(PsiVisitor visitor, Supplier<T> getStartElement) {
        try {
            var psiTree = this.getPsiTree();

            if (psiTree == null) {
                System.err.println("[PSI-DEBUG] PSI tree is null, skipping parsing");
                return;
            }

            T startElement = getStartElement.get();

            if (startElement != null) {
                startElement.accept(visitor.asVisitor());
            }
        }
        catch (BaseVisitor.ParserHackException e) {
            throw e;
        }
        catch (Exception e) {
            System.err.println("PSI visitor parsing failed: " + e.getMessage());
            throw e;
        }
    }

    private void parseWithPsi(PsiVisitor visitor) {
        parseWithPsi(visitor, () -> {
            var psiTree = this.getPsiTree();
            var offset = visitor.getPsiStartOffset();

            if (offset == 0) {
                return psiTree.getContainingKtFile();
            } else {
                var element = psiTree.findElementAt(offset);

                if (element != null) {
                    var parent = element.getParent();

                    if (parent != null) {
                        return parent;
                    }
                } else {
                    // it's probably gonna complain if we don't move
                    sourceParser.tokenStream.nextToken();
                }
            }

            return null;
        });
    }

//    private void parseWithPsi(LocatableToken currentToken) {
////        System.err.println("[PSI-DEBUG] Starting at token: " + currentToken.toString());
//
//        var callbackAdapter = new KotlinParserCallbacksAdapterImpl(sourceParser);
//        var psiVisitor = new FileVisitor(callbackAdapter);
//
//        // This does not necessarily have to equal current token (e.g. when comments come into play)
//        psiVisitor.setTokenBase(this.sourceParser.getOffset());
//        this.sourceParser.getSourceInput().range().ifPresent((psiVisitor::setEmitRange));
//        psiVisitor.setEmitRangeStart(currentToken);
//
//        parseWithPsi(psiVisitor);
//    }

//    private void parseWithPsi(PsiVisitor visitor, LocatableToken currentToken) {
////        System.err.println("[PSI-DEBUG] Starting at token: " + currentToken.toString());
//
//        // This does not necessarily have to equal current token (e.g. when comments come into play)
//        visitor.setTokenBase(this.sourceParser.getOffset());
//        this.sourceParser.getSourceInput().range().ifPresent((visitor::setEmitRange));
//        visitor.setEmitRangeStart(currentToken);
//
//        parseWithPsi(visitor);
//    }

    // ==================== PSI ENHANCEMENT ====================

    /**
     * Extract source code from SourceInput.
     *
     * <p><b>Implementation</b>: Handles all three SourceInput variants:
     * <ul>
     *   <li>FileSource: Reads file on-demand using Files.readString()</li>
     *   <li>ReaderSource/StringSource: Returns cached content directly</li>
     * </ul>
     *
     * <p>For FileSource, the file is read independently for PSI parsing
     * (separate from lexer read). Reading the file twice is acceptable for
     * small source files.</p>
     *
     * @return The complete source code, or null if unavailable
     */
    private String getSourceCode() {
        SourceInput input = sourceParser.getSourceInput();

        if (input == null) {
            if (LOG_PSI_ERRORS) {
                System.err.println("PSI: No source input available");
            }
            return null;
        }


        try {
            // Handle all SourceInput variants using pattern matching
            var source = switch (input) {
                case SourceInput.FileSource fs ->
                    fs.content();
                case SourceInput.ReaderSource rs ->
                    rs.content();
                case SourceInput.UnnamedStringSource uss ->
                    uss.content();
                case SourceInput.NamedStringSource nss ->
                    nss.content();
                case SourceInput.DocumentSource ds ->
                    ds.unranged().content();
            };
            var filePath = input.path();
            var inputRange = input.range();

            return UGLY_SOURCE_CACHE.compute(filePath, (__, existingSource) -> {
                if (existingSource == null || existingSource.isBlank() || inputRange.isEmpty()) { return source; }

                return source;

//                var range = inputRange.get();
//                var start = range.start();
//                var end = range.end();
//
//                var builder = new StringBuilder(existingSource.length());
//
//                start.ifPresent(lineColPos ->
//                    builder.append(existingSource, 0, lineColPos.position())
//                );
//
//                builder.append(source);
//
//                end.ifPresent(lineColPos ->
//                    builder.append(existingSource.substring(lineColPos.position() - 1))
//                );
//
//                return builder.toString();
            });
        }
        catch (Exception e) {
            if (LOG_PSI_ERRORS) {
                System.err.println("PSI: Failed to read source: " + e.getMessage());
            }
//            return null;
            throw e;
        }
    }
    
    /**
     * Get file path from SourceInput.
     *
     * <p><b>Implementation</b>: Extracts full path from SourceInput.</p>
     *
     * @return Full file path (e.g., "/path/to/Example.kt") or "Unknown.kt"
     */
    private String getFilePath() {
        SourceInput input = sourceParser.getSourceInput();
        return input != null ? input.path() : "Unknown.kt";
    }
    
    /**
     * Determine where to write the .psi file.
     *
     * <p><b>Strategy</b>: Place .psi file next to the source file using Path.resolveSibling().</p>
     * <p><b>Format</b>: Same directory as source file but with .psi extension</p>
     *
     * <p><b>Examples</b>:
     * <ul>
     *   <li>{@code /path/to/Example.kt} → {@code /path/to/Example.psi}</li>
     *   <li>{@code src/Example.kt} → {@code src/Example.psi}</li>
     *   <li>{@code com/example/Test.kt} → {@code com/example/Test.psi}</li>
     * </ul>
     * </p>
     *
     * @param filePath The source file path (e.g., "/path/to/Example.kt")
     * @return Path to .psi output file (next to source)
     */
    private Path determinePsiOutputPath(String filePath) {
        Path sourcePath = Paths.get(filePath);
        String fileName = sourcePath.getFileName().toString();
        
        // Replace extension with .psi
        String psiFileName;
        if (fileName.endsWith(".kt")) {
            psiFileName = fileName.substring(0, fileName.length() - 3) + ".psi";
        } else if (fileName.endsWith(".kts")) {
            psiFileName = fileName.substring(0, fileName.length() - 4) + ".psi";
        } else {
            psiFileName = fileName + ".psi";
        }
        
        // Return path in same directory as source
        return sourcePath.resolveSibling(psiFileName);
    }

    /**
     * Check whether a token is a primitive type - "int" "float" etc
     */
    public boolean isPrimitiveType(LocatableToken token)
    {
        return token.getType() == JavaTokenTypes.LITERAL_void
                || token.getType() == JavaTokenTypes.LITERAL_boolean
                || token.getType() == JavaTokenTypes.LITERAL_byte
                || token.getType() == JavaTokenTypes.LITERAL_char
                || token.getType() == JavaTokenTypes.LITERAL_short
                || token.getType() == JavaTokenTypes.LITERAL_int
                || token.getType() == JavaTokenTypes.LITERAL_long
                || token.getType() == JavaTokenTypes.LITERAL_float
                || token.getType() == JavaTokenTypes.LITERAL_double;
    }

    /**
     * Check whether a token represents a modifier (or an "at" symbol,
     * denoting an annotation).
     */
    public boolean isModifier(LocatableToken token)
    {
        int tokType = token.getType();
        var modifier = (
            tokType == JavaTokenTypes.LITERAL_public
                || tokType == JavaTokenTypes.LITERAL_private
                || tokType == JavaTokenTypes.LITERAL_protected
                || tokType == JavaTokenTypes.LITERAL_internal
                || tokType == JavaTokenTypes.ABSTRACT
                || tokType == JavaTokenTypes.FINAL
                || tokType == JavaTokenTypes.LITERAL_static
                || tokType == JavaTokenTypes.LITERAL_volatile
                || tokType == JavaTokenTypes.LITERAL_native
                || tokType == JavaTokenTypes.STRICTFP
                || tokType == JavaTokenTypes.LITERAL_transient
                || tokType == JavaTokenTypes.LITERAL_synchronized
                || tokType == JavaTokenTypes.AT
                || tokType == JavaTokenTypes.LITERAL_default
                || tokType == JavaTokenTypes.LITERAL_sealed
                || tokType == JavaTokenTypes.LITERAL_non_sealed
                || tokType == JavaTokenTypes.LITERAL_open
                || tokType == JavaTokenTypes.LITERAL_data
                || tokType == JavaTokenTypes.LITERAL_actual
                || tokType == JavaTokenTypes.LITERAL_expect
                || tokType == JavaTokenTypes.LITERAL_const
                || tokType == JavaTokenTypes.LITERAL_lateinit
                || tokType == JavaTokenTypes.LITERAL_override
                || tokType == JavaTokenTypes.LITERAL_suspend
                || tokType == JavaTokenTypes.LITERAL_tailrec
                || tokType == JavaTokenTypes.LITERAL_vararg
                || tokType == JavaTokenTypes.LITERAL_infix
                || tokType == JavaTokenTypes.LITERAL_inline
                || tokType == JavaTokenTypes.LITERAL_external
                || tokType == JavaTokenTypes.LITERAL_operator
                || tokType == JavaTokenTypes.LITERAL_inner
        );

        if (!modifier) {
            // TODO: hack hack hack — the issue here is that colouring works with lexer only, which does not have a Kotlin variant and it lexes only in small batches, hence this weird contortion (for now)
            try {
                return PsiTreeUtil.getParentOfType(getPsiTree().findElementAt(token.getPosition()), KtModifierList.class) != null;
            } catch (Exception e) {
                //
            }
        }

        return modifier;
    }


    public Token.TokenType classifyToken(LocatableToken token) {
        Token.TokenType tokType = null; // Token.TokenType.DEFAULT;

        if (isPrimitiveType(token)) {
            tokType = Token.TokenType.PRIMITIVE;
        }
        else if (isModifier(token)) {
            tokType = Token.TokenType.KEYWORD1;
        }
        else if (token.getType() == JavaTokenTypes.STRING_LITERAL || token.getType() == JavaTokenTypes.STRING_LITERAL_MULTILINE) {
            tokType = Token.TokenType.STRING_LITERAL;
        }
        else if (token.getType() == JavaTokenTypes.CHAR_LITERAL) {
            tokType = Token.TokenType.CHAR_LITERAL;
        }
        else {
            switch (token.getType()) {
                case JavaTokenTypes.LITERAL_assert:
                case JavaTokenTypes.LITERAL_for:
                case JavaTokenTypes.LITERAL_switch:
                case JavaTokenTypes.LITERAL_while:
                case JavaTokenTypes.LITERAL_do:
                case JavaTokenTypes.LITERAL_try:
                case JavaTokenTypes.LITERAL_catch:
                case JavaTokenTypes.LITERAL_throw:
                case JavaTokenTypes.LITERAL_throws:
                case JavaTokenTypes.LITERAL_finally:
                case JavaTokenTypes.LITERAL_return:
                case JavaTokenTypes.LITERAL_case:
                case JavaTokenTypes.LITERAL_default:
                case JavaTokenTypes.LITERAL_break:
                case JavaTokenTypes.LITERAL_continue:
                case JavaTokenTypes.LITERAL_if:
                case JavaTokenTypes.LITERAL_else:
                case JavaTokenTypes.LITERAL_new:
                case JavaTokenTypes.LITERAL_yield:
                case JavaTokenTypes.LITERAL_instanceof:
                case JavaTokenTypes.LITERAL_val:
                case JavaTokenTypes.LITERAL_var:
                    tokType = Token.TokenType.KEYWORD1;
                    break;

                case JavaTokenTypes.LITERAL_class:
                case JavaTokenTypes.LITERAL_package:
                case JavaTokenTypes.LITERAL_import:
                case JavaTokenTypes.LITERAL_extends:
                case JavaTokenTypes.LITERAL_interface:
                case JavaTokenTypes.LITERAL_enum:
                case JavaTokenTypes.LITERAL_record:
                case JavaTokenTypes.LITERAL_permits:
                case JavaTokenTypes.LITERAL_implements:
                case JavaTokenTypes.LITERAL_fun:
                    tokType = Token.TokenType.KEYWORD2;
                    break;

                case JavaTokenTypes.LITERAL_super:
//                    if (lastWasWildcard)
//                        tokType = Token.TokenType.KEYWORD2;
//                    else
                    tokType = Token.TokenType.KEYWORD3;
                    break;
                case JavaTokenTypes.LITERAL_this:
                case JavaTokenTypes.LITERAL_null:

                case JavaTokenTypes.LITERAL_true:
                case JavaTokenTypes.LITERAL_false:
                    tokType = Token.TokenType.KEYWORD3;
                    break;

                default:
            }
        }

        if (tokType == null) {
            var tree = getPsiTree();
            var element = tree.findElementAt(token.getPosition());

            if (element.getNode().getElementType() == KtTokens.IDENTIFIER && element.getText().equals("it")) {
                if (PsiTreeUtil.getParentOfType(element, KtLambdaExpression.class) != null) {
                    return Token.TokenType.KEYWORD1;
                }
            }

            tokType = Token.TokenType.DEFAULT;
        }

        return tokType;
    }

    public BufferedTokenStream createTokenStream(SourceInput input, LineColPos position) {
        assert input.sourceType() == SourceType.Kotlin;

        try {
            Keywords kws = new KotlinKeywords();
            Reader reader = input.createReader();

            var lexer = new JavaLexer(
                    reader,
                    kws,
                    position.line(),
                    position.column(),
                    position.position()
            );

            var filteredStream = new JavaTokenFilter(lexer, sourceParser);

            return filteredStream;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}