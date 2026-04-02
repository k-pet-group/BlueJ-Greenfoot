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

import java.io.Reader;

import bluej.parser.Token;
import bluej.parser.Token.TokenType;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ReparseableDocument;

import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtPsiFactory;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Parent node for Kotlin parse tree elements. Overrides tokenization to use
 * {@link KotlinLexer} instead of the Java lexer, providing Kotlin syntax
 * highlighting. Supports configurable node types for classes, functions,
 * and control-flow scopes.
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
public class KotlinParentNode extends JavaParentNode
{
    private final int nodeType;
    private final boolean isContainerNode;
    private String name;

    /**
     * Construct a Kotlin parent node with the given parent and default
     * NODETYPE_NONE.
     *
     * @param parent the parent node (may be null for root nodes)
     */
    public KotlinParentNode(JavaParentNode parent)
    {
        this(parent, NODETYPE_NONE);
    }

    /**
     * Construct a Kotlin parent node with the given parent and explicit
     * node type. The node is marked as a container if the type is
     * TYPEDEF, METHODDEF, SELECTION, or ITERATION.
     *
     * @param parent   the parent node (may be null for root nodes)
     * @param nodeType one of the NODETYPE_* constants from {@link bluej.parser.nodes.ParsedNode}
     */
    public KotlinParentNode(JavaParentNode parent, int nodeType)
    {
        super(parent);
        this.nodeType = nodeType;
        this.isContainerNode = (nodeType == NODETYPE_TYPEDEF
            || nodeType == NODETYPE_METHODDEF
            || nodeType == NODETYPE_SELECTION
            || nodeType == NODETYPE_ITERATION);
    }

    @Override
    public int getNodeType()
    {
        return nodeType;
    }

    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Set the display name for this node (class name, function name, etc.).
     */
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isContainer()
    {
        return isContainerNode;
    }

    @Override
    protected boolean marksOwnEnd()
    {
        // Scope nodes in Kotlin contain their own closing brace
        return isContainerNode;
    }

    /**
     * Attempt block-level PSI reparse for inner body nodes. Containers
     * and non-inner nodes return REMOVE_NODE to cascade up to full-file reparse.
     */
    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos,
            int offset, int maxParse, NodeStructureListener listener)
    {
        // Containers always delegate up — their boundaries (class/function
        // signatures) can only be validated by full-file parsing
        if (isContainer())
        {
            return REMOVE_NODE;
        }

        // Only inner body nodes attempt block-level reparse
        if (!isInner())
        {
            return REMOVE_NODE;
        }

        // Class body inner nodes need processClassBody(), not createBlock(). Cascade to full reparse.
        ParsedNode parent = getParentNode();
        if (parent != null && parent.getNodeType() == NODETYPE_TYPEDEF)
        {
            return REMOVE_NODE;
        }

        // Read the inner content (text between the enclosing braces).
        // Boundary check: if the inner node extends past the document
        // (e.g., closing brace was deleted), cascade to full-file reparse.
        int innerEnd = nodePos + getSize();
        if (innerEnd > document.getLength())
        {
            return REMOVE_NODE;
        }

        String innerContent = KotlinParserUtils.readDocumentText(document, nodePos, innerEnd);

        try
        {
            // Parse the inner content as a block using createBlock().
            // Pass just the inner content WITHOUT braces — createBlock()
            // wraps it internally as "fun x() {\n<content>\n}" and returns
            // the body KtBlockExpression.
            KtPsiFactory psiFactory = KotlinEnvironmentManager.getPsiFactory();
            KtBlockExpression block = psiFactory.createBlock(innerContent);

            // Remove all existing children
            removeAllChildren(nodePos, listener);

            // createBlock() wraps as 'fun x() {\n...\n}', so content starts 2 chars after LBRACE
            int contentPsiOffset = block.getTextRange().getStartOffset() + 2;
            KotlinPsiScopeBuilder.buildScopesFromBlock(block, this,
                    contentPsiOffset, listener);

            // Mark section parsed
            document.markSectionParsed(nodePos, getSize());
            return ALL_OK;
        }
        catch (Exception e)
        {
            // Syntax error breaks block structure — cascade to parent
            return REMOVE_NODE;
        }
    }

    /**
     * Remove all child nodes from this node, notifying the listener
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
     * Tokenize a text region using KotlinLexer and map tokens to
     * TokenType for syntax highlighting.
     */
    @Override
    protected Token tokenizeText(ReparseableDocument document, int pos, int length)
    {
        return doKotlinTokenization(document, pos, length);
    }

    /**
     * Static tokenization helper shared by {@link KotlinParentNode} and
     * {@link KotlinParsedCUNode} (which extends {@code IncrementalParsingNode}
     * and cannot inherit this class).
     */
    static Token doKotlinTokenization(ReparseableDocument document, int pos, int length)
    {
        Reader dr = document.makeReader(pos, pos + length);
        KotlinLexer lexer = new KotlinLexer(dr);

        Token dummyTok = new Token(0, TokenType.END);
        Token token = dummyTok;

        int curcol = 1;
        int remaining = length;
        while (remaining > 0)
        {
            LocatableToken lt = lexer.nextToken();

            // EOF — done tokenizing
            if (lt.getType() == KotlinToken.EOF)
            {
                if (remaining > 0)
                {
                    token.next = new Token(remaining, TokenType.DEFAULT);
                    token = token.next;
                }
                break;
            }

            // Whitespace tokens — emit as DEFAULT
            if (lt.getType() == KotlinToken.WHITE_SPACE || lt.getType() == KotlinToken.DANGLING_NEWLINE)
            {
                // If whitespace crosses a line boundary, only count up to remaining
                int wsLen = lt.getLength();
                if (lt.getEndLine() > 1 || wsLen >= remaining)
                {
                    token.next = new Token(remaining, TokenType.DEFAULT);
                    token = token.next;
                    break;
                }
                token.next = new Token(wsLen, TokenType.DEFAULT);
                token = token.next;
                remaining -= wsLen;
                curcol += wsLen;
                continue;
            }

            // If the token starts beyond our current position (gap), fill with DEFAULT
            if (lt.getColumn() > curcol)
            {
                int gap = lt.getColumn() - curcol;
                token.next = new Token(gap, TokenType.DEFAULT);
                token = token.next;
                remaining -= gap;
                curcol += gap;
            }

            // Map Kotlin token type to display type
            TokenType tokType = KotlinToken.toDisplayType(lt.getType());

            int tokLen = lt.getLength();
            if (lt.getEndLine() > 1)
            {
                tokLen = remaining;
            }

            token.next = new Token(tokLen, tokType);
            token = token.next;
            remaining -= tokLen;
            curcol += tokLen;
        }

        token.next = new Token(0, TokenType.END);
        return dummyTok.next;
    }
}
