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

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Factory for converting PSI elements into {@link LocatableToken} instances.
 * 
 * <p>This class bridges the gap between IntelliJ PSI's 0-based offset-based positioning
 * and BlueJ's 1-based line/column token format. It handles the coordinate transformation
 * and token synthesis required for the PSI visitor infrastructure.</p>
 * 
 * <h2>Positioning Model</h2>
 * <ul>
 *   <li><b>PSI Input:</b> 0-based character offsets into source text</li>
 *   <li><b>Document API:</b> 0-based line numbers via {@link Document#getLineNumber(int)}</li>
 *   <li><b>Token Output:</b> 1-based line and column numbers in {@link LineColPos}</li>
 * </ul>
 * 
 * <h2>Offset-to-Position Algorithm</h2>
 * <pre>{@code
 * Given offset O in source text:
 * 1. line_0based = document.getLineNumber(O)
 * 2. line_1based = line_0based + 1
 * 3. lineStart = document.getLineStartOffset(line_0based)
 * 4. column_1based = (O - lineStart) + 1
 * 5. Return LineColPos(line_1based, column_1based, O)
 * }</pre>
 * 
 * <h2>Multi-Line Element Handling</h2>
 * <p>Elements spanning multiple lines (e.g., multi-line strings, comments) are correctly
 * handled by computing begin and end positions independently. The end position's line
 * and column reflect the actual end location, not the length of the text.</p>
 * 
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe. All state is final and {@link Document}
 * operations are read-only.</p>
 * 
 * <h2>Boundary Conditions</h2>
 * <ul>
 *   <li><b>Line start (column 1):</b> First character of a line</li>
 *   <li><b>Line end:</b> Last character before newline</li>
 *   <li><b>File start (1:1):</b> First character in file</li>
 *   <li><b>File end:</b> Position after last character</li>
 *   <li><b>Empty file:</b> Start and end both at 1:1:0</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Document document = ...;
 * String sourceText = document.getText();
 * TokenFactory factory = new TokenFactory(document, sourceText);
 * 
 * KtFunction function = ...;
 * LocatableToken token = factory.create(function);
 * // token.getLine() returns 1-based line number
 * // token.getColumn() returns 1-based column number
 * // token.getPosition() returns 0-based offset
 * }</pre>
 * 
 * <h2>Implementation Notes</h2>
 * <ul>
 *   <li>Token type mapping uses {@link TokenTypeMapper} for comprehensive PSI-to-token-type conversion</li>
 *   <li>Document is required for line-based calculations even though sourceText could
 *       theoretically be used - Document provides O(log n) line lookup via efficient indexing</li>
 *   <li>Invalid offsets (negative or beyond document length) throw {@link IllegalArgumentException}</li>
 * </ul>
 * 
 * @see LocatableToken Token format used throughout BlueJ parser
 * @see LineColPos Position representation with line, column, and offset
 * @see Document IntelliJ document API for position calculations
 * @see <a href="file:///docs/planning/visitor-foundation/implementation-strategy.md">Implementation Strategy</a>
 */
@OnThread(Tag.Any)
public class TokenFactory {
    
    /**
     * IntelliJ document for efficient line number calculations.
     * Provides O(log n) offset-to-line conversion via internal line index.
     */
    private final Document document;
    
    /**
     * Complete source text for reference and validation.
     * Stored to ensure consistency with document state.
     */
    private final String sourceText;
    
    /**
     * Create a token factory for converting PSI elements to tokens.
     * 
     * @param document IntelliJ document for line/column calculations (must not be null)
     * @param sourceText Complete source code text (must not be null)
     * @throws IllegalArgumentException if document or sourceText is null
     */
    public TokenFactory(Document document, String sourceText) {
        if (document == null) {
            throw new IllegalArgumentException("Document must not be null");
        }
        if (sourceText == null) {
            throw new IllegalArgumentException("Source text must not be null");
        }
        
        this.document = document;
        this.sourceText = sourceText;
    }
    
    /**
     * Create a {@link LocatableToken} from a PSI element.
     * 
     * <p>Extracts the element's text range, converts start and end offsets to
     * 1-based line/column positions, and synthesizes a token with placeholder type.</p>
     * 
     * <p><b>Position Calculation:</b></p>
     * <ul>
     *   <li>Begin: Position of first character of element</li>
     *   <li>End: Position immediately after last character of element</li>
     * </ul>
     *
     * <p><b>Token Type:</b> Uses {@link TokenTypeMapper#mapPsiElementType(PsiElement)} for
     * accurate token type assignment based on PSI element type, keywords, and modifiers.</p>
     * 
     * <h2>Examples</h2>
     * <pre>{@code
     * // Single-line element
     * // Source: "fun hello() = 42"
     * // PSI element for "hello" at offset 4-9
     * // Returns: LocatableToken(line=1, col=5, type=IDENT, text="hello")
     * 
     * // Multi-line element
     * // Source: "fun hello(\n    name: String\n)"
     * // PSI element spans lines 1-3
     * // Begin: LineColPos(1, 5, 4)
     * // End:   LineColPos(3, 1, 29)
     * }</pre>
     * 
     * @param element PSI element to convert (must not be null)
     * @return LocatableToken with accurate position information
     * @throws IllegalArgumentException if element is null or has invalid text range
     */
    public LocatableToken create(PsiElement element) {
        if (element == null) {
            throw new IllegalArgumentException("PSI element must not be null");
        }
        
        // Extract text range from PSI element
        TextRange range = element.getTextRange();
        if (range == null) {
            throw new IllegalArgumentException("PSI element has no text range: " + element);
        }
        
        int startOffset = range.getStartOffset();
        int endOffset = range.getEndOffset();
        
        // Validate offsets
        if (startOffset < 0 || startOffset > sourceText.length()) {
            throw new IllegalArgumentException(
                "Invalid start offset " + startOffset + " for source length " + sourceText.length());
        }
        if (endOffset < startOffset || endOffset > sourceText.length()) {
            throw new IllegalArgumentException(
                "Invalid end offset " + endOffset + " for start offset " + startOffset);
        }
        
        // Convert offsets to 1-based line/column positions
        LineColPos begin = offsetToLineCol(startOffset);
        LineColPos end = offsetToLineCol(endOffset);
        
        // Extract element text
        String text = element.getText();
        if (text == null) {
            text = ""; // Handle null text gracefully
        }
        
        // Determine token type using TokenTypeMapper
        int tokenType = TokenTypeMapper.mapPsiElementType(element);
        
        // Create and return token
        return new LocatableToken(tokenType, text, begin, end);
    }
    
    /**
     * Convert 0-based character offset to 1-based line/column position.
     * 
     * <p>Uses IntelliJ's {@link Document} API for efficient line number lookup.
     * The Document maintains an internal line index providing O(log n) offset-to-line
     * conversion, which is significantly more efficient than scanning the source text.</p>
     * 
     * <h2>Conversion Algorithm</h2>
     * <ol>
     *   <li>Get 0-based line number: {@code line0 = document.getLineNumber(offset)}</li>
     *   <li>Convert to 1-based: {@code line1 = line0 + 1}</li>
     *   <li>Get line start offset: {@code lineStart = document.getLineStartOffset(line0)}</li>
     *   <li>Calculate 1-based column: {@code col1 = (offset - lineStart) + 1}</li>
     * </ol>
     * 
     * <h2>Edge Cases</h2>
     * <ul>
     *   <li><b>Offset 0 (file start):</b> Returns LineColPos(1, 1, 0)</li>
     *   <li><b>First character of line N:</b> Returns LineColPos(N, 1, offset)</li>
     *   <li><b>Last character of line N:</b> Returns LineColPos(N, col, offset) where col = line length</li>
     *   <li><b>After last character (EOF):</b> Returns position on last line or line after if file ends with newline</li>
     * </ul>
     * 
     * <h2>Multi-Byte Characters</h2>
     * <p>The offset represents character positions, not byte positions. Unicode characters
     * (including emojis and multi-byte UTF-8 sequences) are handled correctly by IntelliJ's
     * Document API.</p>
     * 
     * <h2>Performance</h2>
     * <ul>
     *   <li><b>Time Complexity:</b> O(log n) where n = number of lines</li>
     *   <li><b>Space Complexity:</b> O(1) excluding result object</li>
     * </ul>
     * 
     * @param offset 0-based character offset in source text
     * @return LineColPos with 1-based line and column, and 0-based position
     * @throws IllegalArgumentException if offset is negative or beyond document length
     */
    private LineColPos offsetToLineCol(int offset) {
        // Validate offset bounds
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative: " + offset);
        }
        
        int textLength = document.getTextLength();
        if (offset > textLength) {
            throw new IllegalArgumentException(
                "Offset " + offset + " exceeds document length " + textLength);
        }
        
        // Handle empty document edge case
        if (textLength == 0) {
            return new LineColPos(1, 1, 0);
        }
        
        // Get 0-based line number from document
        int lineNumber0Based = document.getLineNumber(offset);
        
        // Convert to 1-based line number
        int line1Based = lineNumber0Based + 1;
        
        // Get the start offset of this line (0-based)
        int lineStartOffset = document.getLineStartOffset(lineNumber0Based);
        
        // Calculate 1-based column: distance from line start + 1
        int column1Based = (offset - lineStartOffset) + 1;
        
        // Create and return position with 0-based offset preserved
        return new LineColPos(line1Based, column1Based, offset);
    }
}