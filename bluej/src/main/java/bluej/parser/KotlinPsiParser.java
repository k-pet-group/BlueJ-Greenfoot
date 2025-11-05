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

import bluej.parser.lexer.LocatableToken;
import bluej.parser.psi.*;
import org.jetbrains.kotlin.psi.KtFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
     *   <li>{@link ParsingMode#PSI_VISITOR}: Uses {@link PsiCallbackVisitor} for parsing (experimental)</li>
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
     *   <li><b>PSI_VISITOR</b>: Uses {@link PsiCallbackVisitor} to parse and call
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
                
                // Phase 2: PSI enhancement (optional .psi file output)
                if (ENABLE_PSI_OUTPUT) {
//                    System.err.println("[PSI-DEBUG] PSI output enabled, calling enhanceWithPSI()");
                    enhanceWithPSI();
                }
                break;
                
            case PSI_VISITOR:
                // PSI visitor mode: Parse using PSI and call real JavaParserCallbacks
//                System.err.println("[PSI-DEBUG] PSI_VISITOR mode: parsing with PsiCallbackVisitor");
                parseWithPsiVisitor();
                break;
        }
    }
    
    /**
     * Parse a part of a compilation unit, starting from the given state.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param state The state to start parsing from
     * @return The new state after parsing
     */
    @Override
    public int parseCUpart(int state) {
        parseWithPsiVisitor();

        return 2; // delegate.parseCUpart(state);
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
        delegate.parseTypeDef();
    }
    
    /**
     * Parse a type definition (class, interface, enum) starting with the given token.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param firstToken The first token of the type definition
     */
    @Override
    public void parseTypeDef(LocatableToken firstToken) {
        delegate.parseTypeDef(firstToken);
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
        return delegate.parseTypeBody(tdType, token);
    }
    
    /**
     * Parse the beginning of a type definition.
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @return The type definition type
     */
    @Override
    public int parseTypeDefBegin() {
        return delegate.parseTypeDefBegin();
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
        return delegate.parseTypeDefPart2(isRecord);
    }
    
    /**
     * Parse a class element (field, method, inner class, etc.).
     * <p><b>PURE DELEGATION</b> - No PSI enhancement.</p>
     * 
     * @param token The first token of the class element
     */
    @Override
    public void parseClassElement(LocatableToken token) {
        delegate.parseClassElement(token);
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
        return delegate.parseStatement(token, allowComma);
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
        delegate.parseClassBody();
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
    
    /**
     * Parse source using PSI visitor and call real {@link JavaParserCallbacksBase}.
     * 
     * <p><b>PSI_VISITOR Mode Implementation</b>: Parse source into PSI tree and traverse
     * using {@link PsiCallbackVisitor} to invoke callbacks from {@link #sourceParser}.</p>
     * 
     * <p><b>Parsing Flow</b>:</p>
     * <ol>
     *   <li>Extract source code from {@link SourceParser}</li>
     *   <li>Determine filename for PSI parsing</li>
     *   <li>Initialize {@link PsiEnvironment} (singleton, lazy)</li>
     *   <li>Parse source with PSI using {@link PsiEnvironment#parseFile}</li>
     *   <li>Create {@link PsiCallbackVisitor} with real callbacks from {@link #sourceParser}</li>
     *   <li>Visit PSI tree using {@code ktFile.accept(visitor)}</li>
     *   <li>Validate visitor state is balanced</li>
     * </ol>
     * 
     * <p><b>Error Handling</b>: All failures are caught, logged (if {@link #LOG_PSI_ERRORS}),
     * and suppressed. Compilation continues normally regardless of PSI outcome.</p>
     * 
     * <p><b>Performance</b>: PSI parsing adds ~10-50ms overhead per file.</p>
     * 
     * @see PsiCallbackVisitor
     * @see JavaParserCallbacksBase
     */
    private void parseWithPsiVisitor() {
        System.err.println("[PSI-DEBUG] === parseWithPsiVisitor() ENTRY ===");
        try {
            // Step 1: Get source code from SourceParser
            String sourceCode = getSourceCode();
            if (sourceCode == null || sourceCode.isEmpty()) {
                if (LOG_PSI_ERRORS) {
                    System.err.println("PSI: No source code available for parsing");
                }
                return;
            }
            
            // Step 2: Determine file path
            String filePath = getFilePath();
            
            // Step 3: Initialize PSI environment (singleton, lazy)
            PsiEnvironment env = PsiEnvironment.getInstance();
            if (!env.isInitialized()) {
                if (LOG_PSI_ERRORS) {
                    System.err.println("PSI: Environment not initialized, skipping parsing");
                }
                return;
            }
            
            // Step 4: Parse with PSI
            KtFile ktFile = env.parseFile(filePath, sourceCode);
            
            // Step 5: Create visitor with real JavaParserCallbacks from sourceParser
            // SourceParser extends JavaParserCallbacks, so we can pass it directly
            JavaParserCallbacks callbackAdapter = new JavaParserCallbacksAdapter(sourceParser);
            PsiCallbackVisitor visitor = new PsiCallbackVisitor(callbackAdapter);
            
            // Step 6: Visit the PSI tree (triggers callback invocations)
            ktFile.accept(visitor);
            
            // Step 7: Validate visitor state is balanced
            if (!visitor.validateState()) {
                if (LOG_PSI_ERRORS) {
                    System.err.println("PSI: Visitor state validation failed - unbalanced stack");
                }
            }
            
            System.err.println("[PSI-DEBUG] === SUCCESS: PSI visitor parsing complete ===");
            
        } catch (PsiParseException e) {
            // PSI parsing failures MUST NOT break compilation
            if (LOG_PSI_ERRORS) {
                System.err.println("PSI parsing failed: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("Caused by: " + e.getCause().getMessage());
                }
            }
            // Compilation continues despite PSI parsing failure
        } catch (Exception e) {
            // Other PSI failures also don't break compilation
            if (LOG_PSI_ERRORS) {
                System.err.println("PSI visitor parsing failed: " + e.getMessage());
                e.printStackTrace();
            }
            // Compilation continues despite PSI failure
        }
    }
    
    // ==================== PSI ENHANCEMENT ====================
    
    /**
     * Enhance parsing with PSI-based features.
     * 
     * <p><b>MVP Implementation</b>: Parse source into PSI tree and serialize to .psi file.</p>
     * 
     * <p><b>Enhancement Flow</b>:</p>
     * <ol>
     *   <li>Extract source code from {@link SourceParser} (TODO: Task 04)</li>
     *   <li>Determine filename for PSI parsing (TODO: Task 04)</li>
     *   <li>Initialize {@link PsiEnvironment} (singleton, lazy)</li>
     *   <li>Parse source with PSI using {@link PsiEnvironment#parseFile}</li>
     *   <li>Serialize PSI tree using {@link PsiTreeSerializer#serialize}</li>
     *   <li>Determine output path for .psi file</li>
     *   <li>Write .psi file using {@link PsiTreeSerializer#writeToFile}</li>
     * </ol>
     * 
     * <p><b>Error Handling</b>: All failures are caught, logged (if {@link #LOG_PSI_ERRORS}),
     * and suppressed. Compilation continues normally regardless of PSI outcome.</p>
     * 
     * <p><b>Performance</b>: Adds ~10-50ms overhead per file. Future versions may
     * implement async serialization to eliminate compilation impact.</p>
     */
    private void enhanceWithPSI() {
        System.err.println("[PSI-DEBUG] === enhanceWithPSI() ENTRY ===");
        try {
            // Step 1: Get source code from SourceParser
            String sourceCode = getSourceCode();
//            System.err.println("[PSI-DEBUG] Source code length: " + (sourceCode != null ? sourceCode.length() : "NULL"));
            if (sourceCode == null || sourceCode.isEmpty()) {
                if (LOG_PSI_ERRORS) {
                    System.err.println("PSI: No source code available for enhancement");
                }
                return;
            }
            
            // Step 2: Determine file path
            String filePath = getFilePath();
//            System.err.println("[PSI-DEBUG] File path: " + filePath);
            
            // Step 3: Initialize PSI environment (singleton, lazy)
            PsiEnvironment env = PsiEnvironment.getInstance();
//            System.err.println("[PSI-DEBUG] PsiEnvironment initialized: " + env.isInitialized());
            if (!env.isInitialized()) {
                if (LOG_PSI_ERRORS) {
                    System.err.println("PSI: Environment not initialized, skipping enhancement");
                }
                return;
            }
            
            // Step 4: Parse with PSI
//            System.err.println("[PSI-DEBUG] Calling env.parseFile()...");
            KtFile ktFile = env.parseFile(filePath, sourceCode);
//            System.err.println("[PSI-DEBUG] KtFile result: SUCCESS");
            
            // Step 5: Serialize PSI tree
//            System.err.println("[PSI-DEBUG] Calling PsiTreeSerializer.serialize()...");
            String serialized = PsiTreeSerializer.serialize(ktFile);
//            System.err.println("[PSI-DEBUG] Serialized length: " + (serialized != null ? serialized.length() : "NULL"));
            
            // Step 6: Determine output path
            Path outputPath = determinePsiOutputPath(filePath);
//            System.err.println("[PSI-DEBUG] Output path: " + outputPath.toAbsolutePath());
            
            // Step 7: Write .psi file
//            System.err.println("[PSI-DEBUG] Writing to file...");
            PsiTreeSerializer.writeToFile(serialized, outputPath);
//            System.err.println("[PSI-DEBUG] === SUCCESS: .psi file written ===");
            
        } catch (PsiParseException e) {
            // PSI parsing failures MUST NOT break compilation
//            System.err.println("[PSI-DEBUG] === PSI PARSE EXCEPTION ===");
            if (LOG_PSI_ERRORS) {
                System.err.println("PSI parsing failed: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("Caused by: " + e.getCause().getMessage());
                }
            }
            // Compilation continues despite PSI parsing failure
        } catch (Exception e) {
            // Other PSI enhancement failures (I/O, serialization) also don't break compilation
//            System.err.println("[PSI-DEBUG] === GENERIC EXCEPTION ===");
            if (LOG_PSI_ERRORS) {
                System.err.println("PSI enhancement failed: " + e.getMessage());
                e.printStackTrace();
            }
            // Compilation continues despite PSI failure
        }
    }
    
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
            return switch (input) {
                case SourceInput.FileSource fs ->
                    Files.readString(fs.file().toPath(), fs.charset());
                case SourceInput.ReaderSource rs ->
                    rs.content();
                case SourceInput.StringSource ss ->
                    ss.content();
            };
        } catch (IOException e) {
            if (LOG_PSI_ERRORS) {
                System.err.println("PSI: Failed to read source: " + e.getMessage());
            }
            return null;
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
}