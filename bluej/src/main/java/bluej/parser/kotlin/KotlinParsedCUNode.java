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

/**
 * Root parse node for a Kotlin source file. Uses the Kotlin PSI parser
 * for full-file reparsing and scope building via
 * {@link KotlinPsiScopeBuilder}.
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

        removeAllChildren(nodePos, listener);

        if (docLength == 0)
        {
            document.markSectionParsed(offset, 0);
            complete = true;
            return ALL_OK;
        }

        String source = KotlinParserUtils.readDocumentText(document, 0, docLength);
        if (source.isEmpty())
        {
            document.markSectionParsed(offset, 0);
            complete = true;
            return ALL_OK;
        }

        KtPsiFactory psiFactory = KotlinEnvironmentManager.getPsiFactory();
        KtFile ktFile = psiFactory.createFile(source);

        KotlinPsiScopeBuilder.buildScopesFromFile(ktFile, this, 0, listener);

        // Mark the entire document as parsed
        document.markSectionParsed(nodePos, getSize());
        complete = true;

        return ALL_OK;
    }

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

}
