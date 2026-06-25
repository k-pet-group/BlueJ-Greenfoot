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
package bluej.parser.kotlin;

import java.io.IOException;
import java.io.Reader;

import bluej.parser.SourceLocation;
import bluej.parser.SourceSpan;
import bluej.parser.nodes.ReparseableDocument;
import bluej.parser.symtab.Selection;

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPackageDirective;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Shared utility methods for Kotlin parser nodes.
 */
public final class KotlinParserUtils
{
    private KotlinParserUtils()
    {
    }

    /**
     * Compute the JVM facade class name the Kotlin compiler produces for a
     * top-level-functions file with the given source-file stem.
     *
     * <p>K2 capitalises the first letter of the stem and appends {@code "Kt"},
     * so {@code utils.kt} compiles to {@code UtilsKt.class}, {@code myUtil.kt}
     * compiles to {@code MyUtilKt.class}, and {@code Utils.kt} compiles to
     * {@code UtilsKt.class}. The casing of the rest of the stem is preserved.
     *
     * <p>This is verified against {@code K2JVMCompiler} 2.1.20 and is consistent
     * with {@code @file:JvmName} default behaviour. Use this from every site
     * that needs to derive the facade class name from a source-file stem so
     * the rule lives in exactly one place.
     *
     * @param stem the source-file stem (no extension), e.g. {@code "utils"}
     * @return the facade class simple name, e.g. {@code "UtilsKt"}
     */
    public static String kotlinFacadeClassName(String stem)
    {
        if (stem == null || stem.isEmpty()) {
            return stem;
        }
        return Character.toUpperCase(stem.charAt(0)) + stem.substring(1) + "Kt";
    }

    /**
     * Convert a 0-based character offset within {@code source} to a 1-based
     * {@link Selection}-compatible [line, column] pair. The end of the
     * conversion is inclusive: the returned column is the column of the
     * character at {@code offset} (or one past the end of the last line if
     * {@code offset} equals {@code source.length()}).
     *
     * @param source the full source text
     * @param offset 0-based character offset (clamped to {@code source.length()})
     * @return [line, column], both 1-based
     */
    public static int[] offsetToLineColumn(String source, int offset)
    {
        if (offset < 0) {
            offset = 0;
        }
        if (offset > source.length()) {
            offset = source.length();
        }
        int line = 1;
        int column = 1;
        for (int i = 0; i < offset; i++) {
            if (source.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }

    /**
     * Build a {@link Selection} spanning the 0-based, half-open offset range
     * {@code [start, end)} in {@code source}.
     */
    @OnThread(Tag.FXPlatform)
    public static Selection rangeToSelection(String source, int start, int end)
    {
        int[] startPos = offsetToLineColumn(source, start);
        int[] endPos = offsetToLineColumn(source, end);
        SourceLocation startLoc = new SourceLocation(startPos[0], startPos[1]);
        SourceLocation endLoc = new SourceLocation(endPos[0], endPos[1]);
        return new Selection(new SourceSpan(startLoc, endLoc));
    }

    /**
     * Build the three {@link Selection}s the BlueJ source-rewriter needs for a
     * Kotlin file's package directive: the {@code package} keyword token, the
     * package-name expression, and a zero-length sentinel selection used in
     * place of Java's trailing semicolon (Kotlin has no semicolons after
     * package directives).
     *
     * <p>Positions are derived from the live PSI tree, so the rewriter can
     * accurately strip or replace the directive via
     * {@link bluej.pkgmgr.target.ClassTarget#replaceSelection}.
     *
     * @param ktFile the parsed Kotlin file
     * @param source the original source text the file was parsed from
     * @return three {@link Selection}s for the {@code package} keyword, the
     *         name, and a zero-length post-name marker; or {@code null} when
     *         the file has no package directive in source
     */
    @OnThread(Tag.FXPlatform)
    public static Selection[] packageSelections(KtFile ktFile, String source)
    {
        KtPackageDirective directive = ktFile.getPackageDirective();
        if (directive == null) {
            return null;
        }
        PsiElement keyword = directive.getPackageKeyword();
        PsiElement nameExpr = directive.getPackageNameExpression();
        if (keyword == null || nameExpr == null) {
            return null;
        }

        TextRange keywordRange = keyword.getTextRange();
        TextRange nameRange = nameExpr.getTextRange();

        Selection pkgStatement = rangeToSelection(source,
                keywordRange.getStartOffset(), keywordRange.getEndOffset());
        Selection pkgName = rangeToSelection(source,
                nameRange.getStartOffset(), nameRange.getEndOffset());
        // Kotlin has no semicolon; use a zero-length selection right after
        // the package name so callers that expect a "post-name" cursor still
        // get a sane position when they ask for it.
        Selection pkgSemi = rangeToSelection(source,
                nameRange.getEndOffset(), nameRange.getEndOffset());

        return new Selection[]{pkgStatement, pkgName, pkgSemi};
    }

    /**
     * Read text from a document between two positions.
     *
     * @return the document text, or empty string on error
     */
    @OnThread(Tag.FXPlatform)
    public static String readDocumentText(ReparseableDocument document,
            int start, int end)
    {
        try (Reader reader = document.makeReader(start, end)) {
            return readFully(reader);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Read all content from a Reader into a String.
     *
     * @return the full content, or empty string on error
     */
    public static String readFully(Reader reader)
    {
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }
}
