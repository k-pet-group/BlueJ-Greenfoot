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

import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtLoopExpression;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhenExpression;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Walks a Kotlin PSI tree ({@link KtFile} or {@link KtBlockExpression}) and
 * creates corresponding BlueJ {@link ParsedNode} children for scope coloring
 * by {@code JavaSyntaxView}.
 *
 * <p>All scope nodes are created as {@link KotlinParentNode} instances with
 * the appropriate {@code NODETYPE_*} constant. This ensures that
 * {@code tokenizeText()} uses the Kotlin lexer throughout the entire parse
 * tree — not just at the root level.</p>
 *
 * <h3>PSI Node → BlueJ Node Mapping</h3>
 * <ul>
 *   <li>{@link KtClass} → {@link KotlinParentNode}(NODETYPE_TYPEDEF, green)</li>
 *   <li>{@link KtObjectDeclaration} → {@link KotlinParentNode}(NODETYPE_TYPEDEF, green)</li>
 *   <li>{@link KtNamedFunction} → {@link KotlinParentNode}(NODETYPE_METHODDEF, yellow)</li>
 *   <li>{@link KtIfExpression} → {@link KotlinParentNode}(NODETYPE_SELECTION, blue)</li>
 *   <li>{@link KtWhenExpression} → {@link KotlinParentNode}(NODETYPE_SELECTION, blue)</li>
 *   <li>{@link KtLoopExpression} (for/while/do-while) → {@link KotlinParentNode}(NODETYPE_ITERATION, pink)</li>
 * </ul>
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
public class KotlinPsiScopeBuilder
{
    private KotlinPsiScopeBuilder()
    {
    } // utility class

    // -----------------------------------------------------------------------
    // No-op listener for building the tree without event forwarding
    // -----------------------------------------------------------------------

    private static final NodeStructureListener NO_OP_LISTENER = new NodeStructureListener()
    {
        @Override
        public void nodeAdded(NodeAndPosition<ParsedNode> node) {}

        @Override
        public void nodeRemoved(NodeAndPosition<ParsedNode> node) {}

        @Override
        public void nodeChangedLength(NodeAndPosition<ParsedNode> node, int oldPos, int oldSize) {}
    };

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Build scope nodes from a full-file PSI parse.
     *
     * @param ktFile        the parsed KtFile from {@code KtPsiFactory.createFile()}
     * @param parent        the parent node to attach children to
     * @param parentAbsPos  absolute document position of the parent node (0 for root)
     * @param listener      structure listener for node change notifications
     */
    public static void buildScopesFromFile(KtFile ktFile, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        NodeStructureListener lsnr = listener != null ? listener : NO_OP_LISTENER;
        for (KtDeclaration decl : ktFile.getDeclarations())
        {
            processDeclaration(decl, parent, parentAbsPos, lsnr);
        }
    }

    /**
     * Build scope nodes from a full-file PSI parse (no listener).
     */
    public static void buildScopesFromFile(KtFile ktFile, JavaParentNode parent,
            int parentAbsPos)
    {
        buildScopesFromFile(ktFile, parent, parentAbsPos, NO_OP_LISTENER);
    }

    /**
     * Build scope nodes from a block-level PSI parse.
     * Used for incremental updates when editing within a method body.
     *
     * @param block         the parsed KtBlockExpression
     * @param parent        the parent node to attach children to
     * @param parentAbsPos  absolute document position of the parent node
     * @param listener      structure listener for node change notifications
     */
    public static void buildScopesFromBlock(KtBlockExpression block, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        NodeStructureListener lsnr = listener != null ? listener : NO_OP_LISTENER;
        processBlockContents(block, parent, parentAbsPos, lsnr);
    }

    // -----------------------------------------------------------------------
    // Internal: PSI element processing
    // -----------------------------------------------------------------------

    /**
     * Process a top-level or member declaration and create the appropriate
     * BlueJ node.
     *
     * @param parentAbsPos absolute document position of the parent BlueJ node
     */
    private static void processDeclaration(KtDeclaration decl, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        if (decl instanceof KtClass ktClass)
        {
            processClass(ktClass, parent, parentAbsPos, listener);
        }
        else if (decl instanceof KtObjectDeclaration ktObject)
        {
            processObject(ktObject, parent, parentAbsPos, listener);
        }
        else if (decl instanceof KtNamedFunction ktFunction)
        {
            processFunction(ktFunction, parent, parentAbsPos, listener);
        }
        // KtProperty and other declarations don't create scope nodes
    }

    /**
     * Process a class declaration → KotlinParentNode(NODETYPE_TYPEDEF).
     */
    private static void processClass(KtClass ktClass, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        TextRange range = ktClass.getTextRange();
        int absPos = range.getStartOffset();
        int relPos = absPos - parentAbsPos;
        int size = range.getLength();

        // Create KotlinParentNode for the class (TYPEDEF → green scope)
        KotlinParentNode typeNode = new KotlinParentNode(parent, ParsedNode.NODETYPE_TYPEDEF);
        String name = ktClass.getName();
        if (name != null)
        {
            typeNode.setName(name);
        }
        typeNode.setComplete(true);

        parent.insertNode(typeNode, relPos, size, listener);

        // Create inner node for class body (matches Java's container+inner pattern)
        KtClassBody body = ktClass.getBody();
        if (body != null)
        {
            KotlinParentNode innerNode = insertInnerNode(body, typeNode, absPos, listener);
            if (innerNode != null)
            {
                int innerAbsPos = body.getTextRange().getStartOffset() + 1;
                processClassBody(body, innerNode, innerAbsPos, listener);
            }
        }
    }

    /**
     * Process an object declaration → KotlinParentNode(NODETYPE_TYPEDEF).
     */
    private static void processObject(KtObjectDeclaration ktObject, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        // Skip companion objects — they don't form a visible scope
        if (ktObject.isCompanion())
        {
            // But process their members as if they belong to the parent
            KtClassBody body = ktObject.getBody();
            if (body != null)
            {
                processClassBody(body, parent, parentAbsPos, listener);
            }
            return;
        }

        TextRange range = ktObject.getTextRange();
        int absPos = range.getStartOffset();
        int relPos = absPos - parentAbsPos;
        int size = range.getLength();

        KotlinParentNode typeNode = new KotlinParentNode(parent, ParsedNode.NODETYPE_TYPEDEF);
        String name = ktObject.getName();
        if (name != null)
        {
            typeNode.setName(name);
        }
        typeNode.setComplete(true);

        parent.insertNode(typeNode, relPos, size, listener);

        KtClassBody body = ktObject.getBody();
        if (body != null)
        {
            KotlinParentNode innerNode = insertInnerNode(body, typeNode, absPos, listener);
            if (innerNode != null)
            {
                int innerAbsPos = body.getTextRange().getStartOffset() + 1;
                processClassBody(body, innerNode, innerAbsPos, listener);
            }
        }
    }

    /**
     * Process a class body — walk declarations and create child nodes.
     *
     * @param parentAbsPos absolute document position of the parent BlueJ node
     */
    private static void processClassBody(KtClassBody body, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        for (KtDeclaration decl : body.getDeclarations())
        {
            processDeclaration(decl, parent, parentAbsPos, listener);
        }
    }

    /**
     * Process a function declaration → KotlinParentNode(NODETYPE_METHODDEF).
     */
    private static void processFunction(KtNamedFunction ktFunction, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        TextRange range = ktFunction.getTextRange();
        int absPos = range.getStartOffset();
        int relPos = absPos - parentAbsPos;
        int size = range.getLength();

        String name = ktFunction.getName();
        KotlinParentNode methodNode = new KotlinParentNode(parent, ParsedNode.NODETYPE_METHODDEF);
        if (name != null)
        {
            methodNode.setName(name);
        }
        methodNode.setComplete(true);

        parent.insertNode(methodNode, relPos, size, listener);

        // Create inner node for function body (matches Java's container+inner pattern)
        if (ktFunction.hasBlockBody())
        {
            // Block body: fun f() { ... } — inner spans content between braces
            KtBlockExpression block = ktFunction.getBodyBlockExpression();
            if (block != null)
            {
                KotlinParentNode innerNode = insertInnerNode(block, methodNode, absPos, listener);
                if (innerNode != null)
                {
                    int innerAbsPos = block.getTextRange().getStartOffset() + 1;
                    processBlockContents(block, innerNode, innerAbsPos, listener);
                }
            }
        }
        else
        {
            // Expression body: fun f() = expr — inner spans the expression
            KtExpression exprBody = ktFunction.getBodyExpression();
            if (exprBody != null)
            {
                TextRange exprRange = exprBody.getTextRange();
                int innerAbsPos = exprRange.getStartOffset();
                int innerRelPos = innerAbsPos - absPos;
                int innerSize = exprRange.getLength();

                if (innerSize > 0)
                {
                    KotlinParentNode innerNode = new KotlinParentNode(methodNode, ParsedNode.NODETYPE_NONE);
                    innerNode.setInner(true);
                    innerNode.setComplete(true);
                    methodNode.insertNode(innerNode, innerRelPos, innerSize, listener);
                }
            }
        }
    }

    /**
     * Process the contents of a block expression, looking for nested
     * scope-creating constructs (if/when/for/while).
     *
     * @param parentAbsPos absolute document position of the parent BlueJ node
     */
    private static void processBlockContents(KtBlockExpression block, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        for (PsiElement child : block.getChildren())
        {
            processExpression(child, parent, parentAbsPos, listener);
        }
    }

    /**
     * Process an expression or statement, creating scope nodes for
     * control flow constructs.
     *
     * @param parentAbsPos absolute document position of the parent BlueJ node
     */
    private static void processExpression(PsiElement element, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        if (element instanceof KtIfExpression ktIf)
        {
            processControlFlow(ktIf, parent, parentAbsPos, ParsedNode.NODETYPE_SELECTION, listener);
        }
        else if (element instanceof KtWhenExpression ktWhen)
        {
            processControlFlow(ktWhen, parent, parentAbsPos, ParsedNode.NODETYPE_SELECTION, listener);
        }
        else if (element instanceof KtLoopExpression ktLoop)
        {
            // KtForExpression, KtWhileExpression, KtDoWhileExpression all extend KtLoopExpression
            processControlFlow(ktLoop, parent, parentAbsPos, ParsedNode.NODETYPE_ITERATION, listener);
        }
        // Recurse into nested blocks (e.g., lambda bodies, run { }, etc.)
        else if (element instanceof KtBlockExpression block)
        {
            processBlockContents(block, parent, parentAbsPos, listener);
        }
        // For other elements, check children for nested control flow
        else
        {
            for (PsiElement child : element.getChildren())
            {
                processExpression(child, parent, parentAbsPos, listener);
            }
        }
    }

    /**
     * Create and insert an inner node for a body element ({@link KtClassBody}
     * or {@link KtBlockExpression}). The inner node spans the content between
     * the opening and closing braces, matching Java's container+inner pattern
     * where containers get type-specific colors and inner nodes get neutral
     * C3+BK coloring.
     *
     * @param bodyElement     the PSI body element (must have braces)
     * @param container       the container BlueJ node to insert into
     * @param containerAbsPos absolute document position of the container
     * @param listener        structure listener
     * @return the created inner node, or null if body is too small
     */
    private static KotlinParentNode insertInnerNode(PsiElement bodyElement,
            JavaParentNode container, int containerAbsPos, NodeStructureListener listener)
    {
        TextRange bodyRange = bodyElement.getTextRange();
        int innerAbsPos = bodyRange.getStartOffset() + 1; // after '{'
        int innerRelPos = innerAbsPos - containerAbsPos;
        int innerSize = bodyRange.getLength() - 2; // exclude '{' and '}'

        if (innerSize <= 0)
        {
            return null;
        }

        KotlinParentNode innerNode = new KotlinParentNode(container, ParsedNode.NODETYPE_NONE);
        innerNode.setInner(true);
        innerNode.setComplete(true);
        container.insertNode(innerNode, innerRelPos, innerSize, listener);
        return innerNode;
    }

    /**
     * Process a control flow body expression as an inner node.
     *
     * <p>For block bodies ({@code { ... }}), the inner spans the content
     * between braces. For braceless bodies ({@code if (x) return y}),
     * the inner spans the expression itself. Kotlin allows braceless
     * bodies unlike Java, but they still need inner highlighting.</p>
     */
    private static void processBodyAsInner(KtExpression bodyExpr,
            JavaParentNode container, int containerAbsPos, NodeStructureListener listener)
    {
        if (bodyExpr == null)
        {
            return;
        }

        if (bodyExpr instanceof KtBlockExpression block)
        {
            // Block body — inner spans content between { and }
            KotlinParentNode innerNode = insertInnerNode(block, container, containerAbsPos, listener);
            if (innerNode != null)
            {
                int innerAbsPos = block.getTextRange().getStartOffset() + 1;
                processBlockContents(block, innerNode, innerAbsPos, listener);
            }
        }
        else
        {
            // Braceless body (e.g., if (x) return y) — inner spans the expression
            TextRange range = bodyExpr.getTextRange();
            int innerAbsPos = range.getStartOffset();
            int innerRelPos = innerAbsPos - containerAbsPos;
            int innerSize = range.getLength();

            if (innerSize > 0)
            {
                KotlinParentNode innerNode = new KotlinParentNode(container, ParsedNode.NODETYPE_NONE);
                innerNode.setInner(true);
                innerNode.setComplete(true);
                container.insertNode(innerNode, innerRelPos, innerSize, listener);
                // Recurse into expression for nested control flow
                processExpression(bodyExpr, innerNode, innerAbsPos, listener);
            }
        }
    }

    /**
     * Process a control flow construct (if/when/for/while/do-while)
     * as a KotlinParentNode with the appropriate scope type.
     *
     * <p>Uses type-specific PSI methods ({@code getThen()}, {@code getBody()},
     * etc.) to find body blocks. Kotlin PSI wraps bodies in
     * {@code KtContainerNode} elements, so generic {@code getChildren()}
     * iteration would miss them.</p>
     */
    private static void processControlFlow(PsiElement element, JavaParentNode parent,
            int parentAbsPos, int nodeType, NodeStructureListener listener)
    {
        TextRange range = element.getTextRange();
        int absPos = range.getStartOffset();
        int relPos = absPos - parentAbsPos;
        int size = range.getLength();

        KotlinParentNode scopeNode = new KotlinParentNode(parent, nodeType);
        scopeNode.setComplete(true);

        parent.insertNode(scopeNode, relPos, size, listener);

        // Use type-specific PSI getters for body blocks.
        // Kotlin PSI wraps bodies in KtContainerNode elements, so generic
        // getChildren() iteration would miss them.
        if (element instanceof KtLoopExpression ktLoop)
        {
            // for/while/do-while — single body via KtLoopExpression.getBody()
            processBodyAsInner(ktLoop.getBody(), scopeNode, absPos, listener);
        }
        else if (element instanceof KtIfExpression ktIf)
        {
            // if — two branches: then and else
            processBodyAsInner(ktIf.getThen(), scopeNode, absPos, listener);
            KtExpression elseExpr = ktIf.getElse();
            if (elseExpr instanceof KtIfExpression elseIf)
            {
                // else-if chain: nested selection scope
                processControlFlow(elseIf, scopeNode, absPos,
                        ParsedNode.NODETYPE_SELECTION, listener);
            }
            else
            {
                processBodyAsInner(elseExpr, scopeNode, absPos, listener);
            }
        }
        else if (element instanceof KtWhenExpression ktWhen)
        {
            // when — each entry has its own body expression
            for (KtWhenEntry entry : ktWhen.getEntries())
            {
                processBodyAsInner(entry.getExpression(), scopeNode, absPos, listener);
            }
        }
    }
}
