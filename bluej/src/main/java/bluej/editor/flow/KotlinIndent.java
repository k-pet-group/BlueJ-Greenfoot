/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import java.util.Arrays;

import bluej.Config;
import bluej.parser.kotlin.KotlinLexer;
import bluej.parser.kotlin.KotlinToken;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ReparseableDocument;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Token-driven indenter for Kotlin source. Indent depth comes from brace
 * and paren tokens, so correctness no longer depends on the parse tree.
 *
 * <p>{@link #computeIndents} is pure and {@code Tag.Any};
 * {@link #calculateIndentsAndApply} inherits the package default
 * {@code Tag.FXPlatform} because it mutates the {@link Document}.
 */
public final class KotlinIndent
{
    private static final int TAB_SIZE = Config.getPropInteger("bluej.editor.tabsize", 4);

    /** Sentinel: this line must not be re-indented (inside a multi-line string). */
    public static final int LEAVE_UNTOUCHED = -1;

    private KotlinIndent()
    {
    }

    /**
     * Compute the target indent width per line of {@code source}, 0-based.
     * Lines inside a {@code """...""" } string (including the closing line)
     * are flagged {@link #LEAVE_UNTOUCHED}.
     *
     * @param source the full document text
     * @return one entry per source line, in characters; {@link #LEAVE_UNTOUCHED}
     *         marks lines the apply pass must skip
     */
    @OnThread(Tag.Any)
    public static int[] computeIndents(CharSequence source)
    {
        int totalLines = countLines(source);
        int[] indents = new int[totalLines];
        Arrays.fill(indents, LEAVE_UNTOUCHED);

        KotlinLexer lex = new KotlinLexer(source);
        // Stack of indent steps per open LBRACE. Default is 1 step per brace;
        // a property accessor body opens with step 2 so its body sits at +2
        // relative to the property declaration line.
        int[] braceSteps = new int[64];
        int braceTop = -1;
        // +1 step to attach to the *next* LBRACE on the current accessor line.
        int pendingExtraStep = 0;
        // +1 indent the current line carries (only set on `get` / `set` lines).
        int lineContinuation = 0;

        int parenDepth = 0;
        int currentLine = 1;
        int lineStartSumSteps = 0;
        int lineStartParenDepth = 0;
        int lineStartFirstTokType = -1;
        int lineStartContinuation = 0;
        boolean lineStartInString = false;
        boolean inMultilineString = false;
        boolean firstTokenOnLine = true;

        LocatableToken tok;
        while ((tok = lex.nextToken()).getType() != KotlinToken.EOF) {
            int tokLine = tok.getLine();

            if (tokLine > currentLine) {
                finaliseLine(indents, currentLine,
                        lineStartSumSteps + lineStartContinuation,
                        lineStartParenDepth, lineStartFirstTokType, lineStartInString);
                int sum = sumSteps(braceSteps, braceTop);
                for (int line = currentLine + 1; line < tokLine; line++) {
                    indents[line - 1] = inMultilineString
                            ? LEAVE_UNTOUCHED
                            : (sum + parenDepth) * TAB_SIZE;
                }
                currentLine = tokLine;
                firstTokenOnLine = true;
                pendingExtraStep = 0;
                lineContinuation = 0;
            }

            if (firstTokenOnLine) {
                // Property-accessor line: `get` or `set` as the first token,
                // directly inside a class body (not inside parens). Give the
                // line a +1 continuation indent and the next LBRACE +1 step.
                if ((tok.getType() == KotlinToken.KW_GET
                        || tok.getType() == KotlinToken.KW_SET)
                        && braceTop >= 0 && parenDepth == 0) {
                    lineContinuation = 1;
                    pendingExtraStep = 1;
                }
                lineStartSumSteps = sumSteps(braceSteps, braceTop);
                lineStartParenDepth = parenDepth;
                lineStartFirstTokType = tok.getType();
                lineStartContinuation = lineContinuation;
                // Capture string state at line start, not at finalize: a line
                // whose first token is CLOSING_QUOTE flips the live flag too
                // early and would be wrongly re-indented otherwise.
                lineStartInString = inMultilineString;
                firstTokenOnLine = false;
            }

            switch (tok.getType()) {
                case KotlinToken.LBRACE -> {
                    int step = 1 + pendingExtraStep;
                    pendingExtraStep = 0;
                    if (braceTop + 1 >= braceSteps.length) {
                        braceSteps = Arrays.copyOf(braceSteps, braceSteps.length * 2);
                    }
                    braceSteps[++braceTop] = step;
                }
                case KotlinToken.RBRACE -> {
                    if (braceTop >= 0) {
                        braceTop--;
                    }
                }
                case KotlinToken.LPAR, KotlinToken.LBRACKET -> parenDepth++;
                case KotlinToken.RPAR, KotlinToken.RBRACKET ->
                        parenDepth = Math.max(0, parenDepth - 1);
                case KotlinToken.OPEN_QUOTE -> inMultilineString = isTripleQuote(tok);
                case KotlinToken.CLOSING_QUOTE -> inMultilineString = false;
                default -> { }
            }
        }
        finaliseLine(indents, currentLine,
                lineStartSumSteps + lineStartContinuation,
                lineStartParenDepth, lineStartFirstTokType, lineStartInString);
        return indents;
    }

    // A line whose first token is a closer ( } ] ) ) sits one step less than
    // its body content.
    @OnThread(Tag.Any)
    private static void finaliseLine(int[] indents, int line1based,
            int lineStartDepthSteps, int lineStartParenDepth,
            int firstTokType, boolean inMultilineString)
    {
        int idx = line1based - 1;
        if (idx < 0 || idx >= indents.length) {
            return;
        }
        if (inMultilineString) {
            indents[idx] = LEAVE_UNTOUCHED;
            return;
        }
        if (firstTokType == -1) {
            return;
        }
        int depth = lineStartDepthSteps + lineStartParenDepth;
        if (firstTokType == KotlinToken.RBRACE
                || firstTokType == KotlinToken.RBRACKET
                || firstTokType == KotlinToken.RPAR) {
            depth = Math.max(0, depth - 1);
        }
        indents[idx] = depth * TAB_SIZE;
    }

    @OnThread(Tag.Any)
    private static int sumSteps(int[] stack, int top)
    {
        int sum = 0;
        for (int i = 0; i <= top; i++) {
            sum += stack[i];
        }
        return sum;
    }

    // PSI emits """ as a single OPEN_QUOTE for raw strings, " for regular.
    @OnThread(Tag.Any)
    private static boolean isTripleQuote(LocatableToken openQuote)
    {
        return "\"\"\"".equals(openQuote.getText());
    }

    @OnThread(Tag.Any)
    private static int countLines(CharSequence source)
    {
        int lines = 1;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    /**
     * Apply Kotlin auto-indent to {@code doc}. Collapses consecutive blank
     * lines, strips whitespace-only blanks, and rewrites each line's leading
     * whitespace to the token-driven target. Lines flagged
     * {@link #LEAVE_UNTOUCHED} (multi-line string interior) are never touched.
     *
     * @param parser     accepted for signature parity with
     *                   {@link FlowIndent#calculateIndentsAndApply}; may be null
     * @param doc        document to mutate
     * @param mlst       accepted for signature parity; may be null (string
     *                   detection comes from the token stream)
     * @param startPos   start of the region to re-indent
     * @param endPos     end of the region to re-indent
     * @param prevCaretPos caret offset before re-indent
     * @return indent result with the adjusted caret and an {@code isPerfect}
     *         flag that is {@code true} iff no edit was needed
     */
    public static FlowIndent.AutoIndentInformation calculateIndentsAndApply(
            ReparseableDocument parser, Document doc,
            MultilineStringTracker mlst,
            int startPos, int endPos, int prevCaretPos)
    {
        int caret = prevCaretPos;
        boolean perfect = true;

        BlankLineResult blank = collapseBlankLines(doc, startPos, endPos, caret);
        caret = blank.caret;
        perfect = perfect && blank.perfect;
        endPos = blank.adjustedEndPos;

        CharSequence content = doc.getContent(0, doc.getLength());
        int[] target = computeIndents(content);

        IndentApplyResult apply = applyIndents(doc, target, startPos, endPos, caret, content);
        caret = apply.caret;
        perfect = perfect && apply.perfect;

        return new FlowIndent.AutoIndentInformation(perfect, caret);
    }

    private record BlankLineResult(int caret, boolean perfect, int adjustedEndPos) { }

    private record IndentApplyResult(int caret, boolean perfect) { }

    // Top-down walk: each delete shifts subsequent offsets, so we re-read the
    // document and recompute line starts after every edit. Mirrors the loop
    // in FlowIndent.calculateIndentsAndApply.
    private static BlankLineResult collapseBlankLines(Document doc,
            int startPos, int endPos, int caretIn)
    {
        int caret = caretIn;
        boolean perfect = true;
        int adjustedEndPos = endPos;

        CharSequence content = doc.getContent(0, doc.getLength());
        int[] target = computeIndents(content);
        int[] lineStarts = computeLineStartOffsets(content);

        boolean lastLineWasBlank = false;
        int line = 0;
        while (line < lineStarts.length) {
            int lineStart = lineStarts[line];
            int lineEnd = (line + 1 < lineStarts.length) ? lineStarts[line + 1] : doc.getLength();

            boolean untouchable = line < target.length && target[line] == LEAVE_UNTOUCHED;
            if (lineEnd <= startPos || lineStart >= adjustedEndPos || untouchable) {
                CharSequence cur = doc.getContent(lineStart, lineEnd);
                lastLineWasBlank = !untouchable && isWhiteSpaceOnly(cur);
                line++;
                continue;
            }

            CharSequence cur = doc.getContent(lineStart, lineEnd);
            boolean thisLineBlank = isWhiteSpaceOnly(cur);
            if (thisLineBlank) {
                if (caret >= lineStart && caret < lineEnd) {
                    caret = lineStart;
                }
                if (lastLineWasBlank && lineEnd <= doc.getLength()) {
                    int removed = lineEnd - lineStart;
                    doc.replaceText(lineStart, lineEnd, "");
                    perfect = false;
                    if (caret > lineEnd) {
                        caret -= removed;
                    }
                    else if (caret >= lineStart) {
                        caret = lineStart;
                    }
                    adjustedEndPos -= removed;
                    content = doc.getContent(0, doc.getLength());
                    target = computeIndents(content);
                    lineStarts = computeLineStartOffsets(content);
                    continue;
                }
                else {
                    // Single blank line — strip trailing whitespace, keep the newline.
                    int rmlen = lineEnd - lineStart - 1;
                    if (rmlen > 0) {
                        doc.replaceText(lineStart, lineStart + rmlen, "");
                        if (caret > lineStart + rmlen) {
                            caret -= rmlen;
                        }
                        else if (caret > lineStart) {
                            caret = lineStart;
                        }
                        adjustedEndPos -= rmlen;
                        content = doc.getContent(0, doc.getLength());
                        target = computeIndents(content);
                        lineStarts = computeLineStartOffsets(content);
                        // Whitespace-on-blank-line is a tidy, not a real edit — keep perfect.
                    }
                }
            }
            lastLineWasBlank = thisLineBlank;
            line++;
        }
        return new BlankLineResult(caret, perfect, adjustedEndPos);
    }

    // Caret shifts mirror FlowIndent.DocumentIndentAction:
    //   before this line   → unchanged
    //   inside leading WS  → moved to end of new indent
    //   after leading WS   → shifted by (newWidth - oldWidth)
    private static IndentApplyResult applyIndents(Document doc, int[] target,
            int startPos, int endPos, int caretIn, CharSequence initialContent)
    {
        int caret = caretIn;
        boolean perfect = true;
        int[] lineStarts = computeLineStartOffsets(initialContent);

        for (int line = 0; line < lineStarts.length && line < target.length; line++) {
            if (target[line] == LEAVE_UNTOUCHED) {
                continue;
            }
            int lineStart = lineStarts[line];
            int lineEnd = (line + 1 < lineStarts.length) ? lineStarts[line + 1] : doc.getLength();

            if (lineEnd <= startPos || lineStart >= endPos) {
                continue;
            }

            CharSequence cur = doc.getContent(lineStart, lineEnd);
            int existingWs = leadingWhitespaceLength(cur);

            // Pure-whitespace lines are handled by the blank-line pass.
            if (existingWs == cur.length()
                    || (existingWs == cur.length() - 1 && cur.charAt(cur.length() - 1) == '\n')) {
                continue;
            }

            int targetWidth = target[line];
            // Block-comment `*` continuation gets a one-space pad after the parent indent.
            if (existingWs < cur.length() && cur.charAt(existingWs) == '*') {
                targetWidth += 1;
            }

            boolean hasTabs = containsTab(cur, existingWs);
            String desired = spaces(targetWidth);
            if (!hasTabs && existingWs == targetWidth) {
                continue;
            }

            doc.replaceText(lineStart, lineStart + existingWs, desired);
            int delta = targetWidth - existingWs;
            if (caret >= lineStart + existingWs) {
                caret += delta;
            }
            else if (caret >= lineStart) {
                caret = lineStart + targetWidth;
            }
            perfect = false;

            for (int j = line + 1; j < lineStarts.length; j++) {
                lineStarts[j] += delta;
            }
            endPos += delta;
        }

        return new IndentApplyResult(caret, perfect);
    }

    @OnThread(Tag.Any)
    private static int[] computeLineStartOffsets(CharSequence content)
    {
        int total = countLines(content);
        int[] starts = new int[total];
        starts[0] = 0;
        int line = 1;
        for (int i = 0; i < content.length() && line < total; i++) {
            if (content.charAt(i) == '\n') {
                starts[line++] = i + 1;
            }
        }
        return starts;
    }

    @OnThread(Tag.Any)
    private static boolean isWhiteSpaceOnly(CharSequence s)
    {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }

    @OnThread(Tag.Any)
    private static int leadingWhitespaceLength(CharSequence line)
    {
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') {
                i++;
            }
            else {
                break;
            }
        }
        return i;
    }

    @OnThread(Tag.Any)
    private static boolean containsTab(CharSequence line, int upTo)
    {
        for (int i = 0; i < upTo; i++) {
            if (line.charAt(i) == '\t') {
                return true;
            }
        }
        return false;
    }

    @OnThread(Tag.Any)
    private static String spaces(int n)
    {
        if (n <= 0) {
            return "";
        }
        char[] buf = new char[n];
        Arrays.fill(buf, ' ');
        return new String(buf);
    }
}
