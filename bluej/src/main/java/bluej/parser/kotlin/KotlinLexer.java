/*
 This file is part of the BlueJ program.
 Copyright (C) 2025,2026  Michael Kolling and John Rosenberg

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
package bluej.parser.kotlin;

import java.io.IOException;
import java.io.Reader;

import bluej.parser.TokenStream;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.LineColPos;

import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Kotlin tokenizer for the BlueJ editor. Wraps the PSI KotlinLexer from
 * kotlin-compiler-embeddable and produces LocatableTokens compatible with
 * BlueJ's parser infrastructure.
 *
 * @author BlueJ Team
 */
@OnThread(Tag.Any)
public final class KotlinLexer implements TokenStream
{
    // FQN required: name collision with this class (bluej.parser.kotlin.KotlinLexer)
    private final org.jetbrains.kotlin.lexer.KotlinLexer psiLexer;

    /** The full source text being tokenized. */
    private final CharSequence source;

    // Position tracking (1-based line/column, 0-based position)
    private int currentLine;
    private int currentColumn;
    private int currentPosition;

    /**
     * Construct from a CharSequence. This is the primary constructor —
     * no conversion is needed since the PSI lexer operates on CharSequence.
     *
     * @param source the Kotlin source text to tokenize
     */
    public KotlinLexer(CharSequence source)
    {
        this(source, 1, 1, 0);
    }

    /**
     * Construct from a CharSequence with an explicit starting position.
     * Used when tokenizing a region that doesn't start at line 1, column 1.
     *
     * @param source   the Kotlin source text to tokenize
     * @param line     starting line number (1-based)
     * @param col      starting column number (1-based)
     * @param position starting character offset (0-based)
     */
    public KotlinLexer(CharSequence source, int line, int col, int position)
    {
        this.source = source;
        this.currentLine = line;
        this.currentColumn = col;
        this.currentPosition = position;
        this.psiLexer = new org.jetbrains.kotlin.lexer.KotlinLexer();
        this.psiLexer.start(source, 0, source.length());
    }

    /**
     * Construct from a Reader. Reads the entire content into a String
     * for the PSI lexer. Starts at line 1, column 1, position 0.
     *
     * @param in the Reader over Kotlin source text
     */
    public KotlinLexer(Reader in)
    {
        this(KotlinParserUtils.readFully(in), 1, 1, 0);
    }

    /**
     * Construct from a Reader with an explicit starting position.
     *
     * @param in       the Reader over Kotlin source text
     * @param line     starting line number (1-based)
     * @param col      starting column number (1-based)
     * @param position starting character offset (0-based)
     */
    public KotlinLexer(Reader in, int line, int col, int position)
    {
        this(KotlinParserUtils.readFully(in), line, col, position);
    }

    /**
     * Holds the result of compound operator merging: the (possibly merged)
     * token type and text.
     */
    private record MergedToken(int type, String text) {}

    /**
     * Returns the next token from the Kotlin source.
     *
     * <p>Delegates to the PSI lexer, maps the IElementType to a BlueJ token
     * type via {@link KotlinToken#mapTokenType}, tracks line/column through
     * the token text, and wraps the result in a {@link LocatableToken}.</p>
     *
     * @return next LocatableToken, or an EOF token at end of input
     */
    @Override
    public LocatableToken nextToken()
    {
        IElementType psiType = psiLexer.getTokenType();

        // EOF — PSI lexer returns null when exhausted
        if (psiType == null)
        {
            LineColPos pos = new LineColPos(currentLine, currentColumn, currentPosition);
            return new LocatableToken(KotlinToken.EOF, "", pos, pos);
        }

        // Read token text and advance PSI past it (enables peek-ahead
        // for compound operator merging)
        String tokenText = source.subSequence(
            psiLexer.getTokenStart(), psiLexer.getTokenEnd()).toString();
        psiLexer.advance();

        // Record begin position, then advance through token characters
        LineColPos begin = new LineColPos(currentLine, currentColumn, currentPosition);
        advancePosition(tokenText);

        // Map PSI type → BlueJ type, reclassify soft keywords
        int blueJType = reclassifySoftKeyword(
            KotlinToken.mapTokenType(psiType), tokenText);

        // Merge compound operators that PSI splits (?. ?: !!)
        MergedToken merged = tryMergeCompoundOperator(blueJType, tokenText);

        // End position reflects the final character (after any merge)
        LineColPos end = new LineColPos(currentLine, currentColumn, currentPosition);

        return new LocatableToken(merged.type, merged.text, begin, end);
    }

    /**
     * Reclassify soft keywords that PSI returns as IDENTIFIER. Kotlin's
     * soft keywords ({@code abstract}, {@code open}, {@code sealed},
     * {@code data}, etc.) are context-sensitive, so the PSI lexer emits
     * them as identifiers. We reclassify by text for syntax highlighting.
     *
     * @param blueJType the mapped BlueJ token type
     * @param tokenText the token's text
     * @return the reclassified type if a soft keyword, otherwise the
     *         original type
     */
    private int reclassifySoftKeyword(int blueJType, String tokenText)
    {
        if (blueJType == KotlinToken.IDENTIFIER)
        {
            int softKw = KotlinToken.mapSoftKeywordByText(tokenText);
            if (softKw >= 0)
            {
                return softKw;
            }
        }
        return blueJType;
    }

    /**
     * Merge compound operators that PSI splits into separate tokens.
     * PSI tokenizes {@code ?.} as QUEST+DOT, {@code ?:} as QUEST+COLON,
     * and {@code !!} as EXCL+EXCL. This method peeks at the next PSI token
     * and, if a merge is possible, advances the position and PSI lexer.
     *
     * @param blueJType the current token's BlueJ type
     * @param tokenText the current token's text
     * @return a {@link MergedToken} with the (possibly merged) type and text
     */
    private MergedToken tryMergeCompoundOperator(int blueJType, String tokenText)
    {
        if (blueJType == KotlinToken.QUEST)
        {
            IElementType nextPsi = psiLexer.getTokenType();
            if (nextPsi != null)
            {
                int nextType = KotlinToken.mapTokenType(nextPsi);
                if (nextType == KotlinToken.DOT)
                {
                    // ?. → SAFE_ACCESS
                    advancePosition(".");
                    psiLexer.advance();
                    return new MergedToken(KotlinToken.SAFE_ACCESS, "?.");
                }
                else if (nextType == KotlinToken.COLON)
                {
                    // ?: → ELVIS
                    advancePosition(":");
                    psiLexer.advance();
                    return new MergedToken(KotlinToken.ELVIS, "?:");
                }
            }
        }
        else if (blueJType == KotlinToken.EXCL)
        {
            IElementType nextPsi = psiLexer.getTokenType();
            if (nextPsi != null)
            {
                int nextType = KotlinToken.mapTokenType(nextPsi);
                if (nextType == KotlinToken.EXCL)
                {
                    // !! → EXCLEXCL
                    advancePosition("!");
                    psiLexer.advance();
                    return new MergedToken(KotlinToken.EXCLEXCL, "!!");
                }
            }
        }
        return new MergedToken(blueJType, tokenText);
    }

    /**
     * Advance line/column/position tracking through the given text.
     */
    private void advancePosition(String text)
    {
        for (int i = 0; i < text.length(); i++)
        {
            char ch = text.charAt(i);
            if (ch == '\n')
            {
                currentLine++;
                currentColumn = 1;
            }
            else
            {
                currentColumn++;
            }
            currentPosition++;
        }
    }
}
