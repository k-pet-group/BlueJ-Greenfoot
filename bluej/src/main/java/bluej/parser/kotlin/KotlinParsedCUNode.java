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
package bluej.parser.kotlin;

import bluej.parser.Token;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParseParams;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ReparseableDocument;

import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.IOException;
import java.io.Reader;

/**
 * Root parse node for a Kotlin source file. Extends {@link ParsedCUNode}
 * to be compatible with {@code JavaSyntaxView} which expects a
 * {@code ParsedCUNode} root.
 *
 * <p>Unlike the Java {@link ParsedCUNode} which uses {@code EditorParser}
 * for token-by-token incremental parsing, this class overrides
 * {@link #reparseNode} to use the Kotlin PSI parser from
 * {@code kotlin-compiler-embeddable}. On every reparse:</p>
 * <ol>
 *   <li>All existing child nodes are removed</li>
 *   <li>The full document text is read</li>
 *   <li>{@link KtPsiFactory#createFile} produces a PSI tree</li>
 *   <li>{@link KotlinPsiScopeBuilder#buildScopesFromFile} converts the PSI
 *       tree into {@link KotlinParentNode} children with the correct
 *       scope types for {@code JavaSyntaxView} coloring</li>
 * </ol>
 *
 * <p>This full-reparse approach is appropriate for the MVP: educational
 * Kotlin files are typically 100-500 lines, and PSI parsing takes
 * ~50-100ms for a 500-line file.</p>
 *
 * <p>Entity resolution (code completion, imports) from {@code ParsedCUNode}
 * is inherited but unused for Kotlin MVP - the parent resolver is null.</p>
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
public class KotlinParsedCUNode extends ParsedCUNode
{
    /**
     * Create a root node for Kotlin parsing.
     * No entity resolver is provided (Kotlin MVP does not support
     * code completion or type-aware features).
     */
    public KotlinParsedCUNode()
    {
        super(null);
    }

    // -----------------------------------------------------------------------
    // PSI-based reparse — replaces the Java EditorParser loop
    // -----------------------------------------------------------------------

    /**
     * Perform a full PSI reparse of the Kotlin document. This completely
     * replaces the incremental Java parsing in
     * {@link bluej.parser.nodes.IncrementalParsingNode#reparseNode}.
     *
     * <p>The approach: clear all children, parse the full document with
     * {@code KtPsiFactory.createFile()}, then walk the PSI tree via
     * {@link KotlinPsiScopeBuilder} to rebuild the scope node tree.</p>
     */
    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos,
            int offset, int maxParse, NodeStructureListener listener)
    {
        int docLength = document.getLength();

        // 1. Remove all existing child nodes
        removeAllChildren(nodePos, listener);

        // 2. Empty document — nothing to parse
        if (docLength == 0)
        {
            document.markSectionParsed(offset, 0);
            complete = true;
            return ALL_OK;
        }

        // 3. Read document text
        String source = readDocumentText(document, 0, docLength);
        if (source.isEmpty())
        {
            document.markSectionParsed(offset, 0);
            complete = true;
            return ALL_OK;
        }

        // 4. PSI parse
        KtPsiFactory psiFactory = KotlinEnvironmentManager.getPsiFactory();
        KtFile ktFile = psiFactory.createFile(source);

        // 5. Build scope tree from PSI
        KotlinPsiScopeBuilder.buildScopesFromFile(ktFile, this, 0, listener);

        // 6. Mark the entire document as parsed (full-file reparse)
        // Unlike Java's incremental parser which only parses from offset
        // forward, we rebuilt the entire tree — so mark from nodePos for
        // the full size to ensure all lines get repainted.
        document.markSectionParsed(nodePos, getSize());
        complete = true;

        return ALL_OK;
    }

    // -----------------------------------------------------------------------
    // Multiline string handling — bypasses MultilineStringTracker
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} — Kotlin handles multiline strings via
     * {@link KotlinStringNode} in the parse tree, so
     * {@code MultilineStringTracker}'s line-level override should be
     * skipped in {@code JavaSyntaxView.getTokenStylesFor()}.
     */
    @Override
    public boolean handlesMultilineStrings()
    {
        return true;
    }

    // -----------------------------------------------------------------------
    // Kotlin tokenization — overrides Java lexer with KotlinLexer
    // -----------------------------------------------------------------------

    /**
     * Tokenize text using the Kotlin lexer. This is called by
     * {@link bluej.parser.nodes.JavaParentNode#getMarkTokensFor} for text
     * gaps between child scope nodes (e.g., import statements, top-level
     * code between classes).
     */
    @Override
    protected Token tokenizeText(ReparseableDocument document, int pos, int length)
    {
        return KotlinParentNode.doKotlinTokenization(document, pos, length);
    }

    // -----------------------------------------------------------------------
    // Override doPartialParse (not used — reparseNode is overridden)
    // -----------------------------------------------------------------------

    /**
     * Not used — {@link #reparseNode} is overridden to use PSI parsing
     * instead of the {@code EditorParser}-based partial parse loop.
     */
    @Override
    protected int doPartialParse(ParseParams params, int state)
    {
        // This method is never called because reparseNode() is overridden.
        // Return PP_OK as a safe default.
        return PP_OK;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Remove all child nodes from this root, notifying the listener
     * of each removal.
     */
    private void removeAllChildren(int nodePos, NodeStructureListener listener)
    {
        NodeAndPosition<ParsedNode> child = findNodeAtOrAfter(nodePos, nodePos);
        while (child != null)
        {
            NodeAndPosition<ParsedNode> next = child.nextSibling();
            removeChild(child, listener);
            child = next;
        }
    }

    /**
     * Read a section of the document into a String.
     */
    private static String readDocumentText(ReparseableDocument document,
            int start, int end)
    {
        try
        {
            Reader reader = document.makeReader(start, end);
            StringBuilder sb = new StringBuilder(end - start);
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1)
            {
                sb.append(buf, 0, n);
            }
            reader.close();
            return sb.toString();
        }
        catch (IOException e)
        {
            return "";
        }
    }
}
