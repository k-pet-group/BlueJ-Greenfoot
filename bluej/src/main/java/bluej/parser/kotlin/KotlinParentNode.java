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
import java.util.ArrayList;
import java.util.List;

import bluej.parser.Token;
import bluej.parser.Token.TokenType;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ReparseableDocument;

import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
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
     * Attempt block-level PSI reparse for inner body nodes.
     * Non-inner nodes return REMOVE_NODE to cascade up to full-file reparse.
     *
     * <p>Block-level reparse uses {@code KtPsiFactory.createBlock()} to parse
     * just the inner content (between braces) instead of the whole file.
     * This is ~3-10× faster than full-file reparse for edits inside
     * function bodies. The content offsets are derived from the PSI tree
     * itself (LBRACE/RBRACE positions) rather than hardcoded.</p>
     */
    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos,
            int offset, int maxParse, NodeStructureListener listener)
    {
        // Only inner body nodes attempt block-level reparse.
        // Containers, non-inner nodes, and class body inners (which need
        // processClassBody()) all cascade to full-file reparse.
        if (!isInner()) {
            return REMOVE_NODE;
        }

        ParsedNode parent = getParentNode();
        if (parent != null && parent.getNodeType() == NODETYPE_TYPEDEF) {
            return REMOVE_NODE;
        }

        // Boundary check: inner node must not extend past the document.
        int innerEnd = nodePos + getSize();
        if (innerEnd > document.getLength()) {
            return REMOVE_NODE;
        }

        String innerContent = KotlinParserUtils.readDocumentText(document, nodePos, innerEnd);

        try
        {
            // createBlock() wraps innerContent as "fun foo() {\n<content>\n}"
            // and returns the body KtBlockExpression.
            KtPsiFactory psiFactory = KotlinEnvironmentManager.getPsiFactory();
            KtBlockExpression block = psiFactory.createBlock(innerContent);

            // Derive the content offset from the PSI tree structure:
            // the content starts one character after LBRACE (the \n separator).
            PsiElement lBrace = block.getLBrace();
            PsiElement rBrace = block.getRBrace();
            if (lBrace == null || rBrace == null) {
                return REMOVE_NODE;
            }
            int contentPsiStart = lBrace.getTextRange().getEndOffset() + 1;

            // Check that PSI consumed all inner content. If RBRACE matched
            // a '}' inside the content (not the wrapper's '}'), the inner
            // node is oversized — it covers text belonging to a parent scope.
            int psiContentLength = rBrace.getTextRange().getStartOffset() - contentPsiStart;
            if (psiContentLength < getSize()) {
                return REMOVE_NODE;
            }

            removeAllChildren(nodePos, listener);
            KotlinPsiScopeBuilder.buildScopesFromBlock(block, this,
                    contentPsiStart, listener);

            // createBlock()'s synthetic '}' can cause PSI error recovery to
            // extend scope nodes past the actual content. If so, cascade to
            // full-file reparse which uses the real document structure.
            if (hasChildBeyondBoundary(nodePos)) {
                removeAllChildren(nodePos, listener);
                return REMOVE_NODE;
            }

            document.markSectionParsed(nodePos, getSize());
            return ALL_OK;
        }
        catch (Exception e)
        {
            return REMOVE_NODE;
        }
    }

    // Check whether any child node extends beyond this node's boundary.
    private boolean hasChildBeyondBoundary(int nodePos)
    {
        int mySize = getSize();
        NodeAndPosition<ParsedNode> child = findNodeAtOrAfter(nodePos, nodePos);
        while (child != null) {
            if ((child.getPosition() - nodePos) + child.getSize() > mySize) {
                return true;
            }
            child = child.nextSibling();
        }
        return false;
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
     *
     * <p>Reads tokens from KotlinLexer, maps each to a display
     * {@link TokenType}, and builds a linked token list bounded by
     * {@code length} characters. Multiline tokens (comments, strings)
     * consume all remaining length.</p>
     */
    static Token doKotlinTokenization(ReparseableDocument document, int pos, int length)
    {
        Reader dr = document.makeReader(pos, pos + length);
        KotlinLexer lexer = new KotlinLexer(dr);

        List<Token> tokens = new ArrayList<>();
        int remaining = length;
        int curcol = 1;

        while (remaining > 0)
        {
            LocatableToken lt = lexer.nextToken();
            if (lt.getType() == KotlinToken.EOF)
            {
                if (remaining > 0)
                    tokens.add(new Token(remaining, TokenType.DEFAULT));
                break;
            }

            // Fill gap before token with DEFAULT
            if (lt.getColumn() > curcol)
            {
                int gap = lt.getColumn() - curcol;
                tokens.add(new Token(gap, TokenType.DEFAULT));
                remaining -= gap;
                curcol += gap;
            }

            // Multiline tokens consume all remaining length
            int len = (lt.getEndLine() > 1 || lt.getLength() >= remaining)
                ? remaining : lt.getLength();
            tokens.add(new Token(len, KotlinToken.toDisplayType(lt.getType())));
            remaining -= len;
            curcol += len;
        }

        tokens.add(new Token(0, TokenType.END));
        return linkTokens(tokens);
    }

    /**
     * Link a list of tokens into a singly-linked chain and return the head.
     */
    private static Token linkTokens(List<Token> tokens)
    {
        for (int i = 0; i < tokens.size() - 1; i++)
        {
            tokens.get(i).next = tokens.get(i + 1);
        }
        return tokens.isEmpty()
            ? new Token(0, TokenType.END) : tokens.get(0);
    }
}
