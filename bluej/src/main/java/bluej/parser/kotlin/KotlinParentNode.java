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

import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtPsiFactory;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Parent node for Kotlin parse tree elements. Overrides tokenization to use
 * {@link KotlinLexer} instead of the Java lexer, providing Kotlin syntax
 * highlighting. Supports configurable node types for classes, functions,
 * and control-flow scopes.
 */
@OnThread(Tag.FXPlatform)
public class KotlinParentNode extends JavaParentNode
{
    private final int nodeType;
    private final boolean isContainerNode;
    private String name;

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

    // Refuse to absorb text appended exactly at our end boundary.
    // ParentParsedNode.textInserted delegates into a child when
    // findNodeAtOrAfter returns it (using >=, so nodeEnd == insPos
    // matches). For Kotlin, this causes nodes to absorb text that
    // belongs to a sibling scope (e.g., "else" absorbed into if's
    // then-inner, or "fun main()" absorbed into expression-body "20").
    // Returning REMOVE_NODE forces the parent to handle the insertion.
    @Override
    public int textInserted(ReparseableDocument document, int nodePos,
                            int insPos, int length, NodeStructureListener listener)
    {
        if (insPos >= nodePos + getSize()) {
            return REMOVE_NODE;
        }
        return super.textInserted(document, nodePos, insPos, length, listener);
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
     * <p>Wraps inner content as {@code "fun _() {" + content} (no closing
     * brace) and parses via {@code createFile()}. Since no synthetic {@code }}
     * is added, PSI brace matching uses only real braces from the document.
     * If the inner node is oversized (contains a {@code }} that belongs to a
     * parent scope), PSI closes the function body at that brace and
     * {@code body.getRBrace() != null} — detected and cascaded to full-file
     * reparse.</p>
     */
    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos,
            int offset, int maxParse, NodeStructureListener listener)
    {
        // Only inner body nodes of methods/control-flow attempt block-level
        // reparse. Class body inners need processClassBody().
        if (!isInner()) {
            return REMOVE_NODE;
        }
        ParsedNode parent = getParentNode();
        if (parent != null && parent.getNodeType() == NODETYPE_TYPEDEF) {
            return REMOVE_NODE;
        }

        int innerEnd = nodePos + getSize();
        if (innerEnd > document.getLength()) {
            return REMOVE_NODE;
        }

        String innerContent = KotlinParserUtils.readDocumentText(document, nodePos, innerEnd);

        try {
            // Wrap WITHOUT a closing '}' — all braces come from the real
            // document. PSI error recovery works with the actual brace
            // structure instead of a synthetic wrapper.
            KtPsiFactory psiFactory = KotlinEnvironmentManager.getPsiFactory();
            KtFile ktFile = psiFactory.createFile("fun _() {" + innerContent);

            KtBlockExpression body = findFunctionBody(ktFile);
            if (body == null || body.getLBrace() == null) {
                return REMOVE_NODE;
            }

            // Detect structural issues via PSI errors at the block level:
            // - Extra '}' (oversized node): body.getRBrace() != null
            // - Orphan keywords like 'else'/'catch': PsiErrorElement with
            //   non-empty text (empty errors are just the missing '}' from
            //   our wrapper — always present, harmless)
            if (body.getRBrace() != null || hasBlockLevelErrors(body)) {
                return REMOVE_NODE;
            }

            int contentPsiStart = body.getLBrace().getTextRange().getEndOffset();

            removeAllChildren(nodePos, listener);
            KotlinPsiScopeBuilder.buildScopesFromBlock(body, this,
                    contentPsiStart, listener);

            document.markSectionParsed(nodePos, getSize());
            return ALL_OK;
        }
        catch (Exception e) {
            return REMOVE_NODE;
        }
    }

    // Check for non-empty PsiErrorElement children at the block level.
    // These indicate structural keywords (else, catch, finally) that PSI
    // couldn't connect — meaning the inner node absorbed parent-level text.
    private static boolean hasBlockLevelErrors(KtBlockExpression body)
    {
        for (ASTNode child = body.getNode().getFirstChildNode();
             child != null; child = child.getTreeNext()) {
            if (child.getPsi() instanceof PsiErrorElement error
                    && error.getTextLength() > 0) {
                return true;
            }
        }
        return false;
    }

    // Extract the body block from the first function in a parsed file.
    private static KtBlockExpression findFunctionBody(KtFile ktFile)
    {
        for (KtDeclaration decl : ktFile.getDeclarations()) {
            if (decl instanceof KtNamedFunction func && func.hasBlockBody()) {
                return func.getBodyBlockExpression();
            }
        }
        return null;
    }

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
                if (remaining > 0) {
                    tokens.add(new Token(remaining, TokenType.DEFAULT));
                }
                break;
            }

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
