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

import bluej.parser.Token.TokenType;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;

import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtClassInitializer;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtLoopExpression;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtPrimaryConstructor;
import org.jetbrains.kotlin.psi.KtSecondaryConstructor;
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhenExpression;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Walks a Kotlin PSI tree and creates corresponding BlueJ scope nodes
 * ({@link KotlinParentNode}, {@link KotlinCommentNode}, {@link KotlinStringNode})
 * for scope coloring by {@code JavaSyntaxView}.
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
public class KotlinPsiScopeBuilder
{
    private KotlinPsiScopeBuilder()
    {
    } // utility class

    private static final NodeStructureListener NO_OP_LISTENER = new NodeStructureListener()
    {
        @Override
        public void nodeAdded(NodeAndPosition<ParsedNode> node) {}

        @Override
        public void nodeRemoved(NodeAndPosition<ParsedNode> node) {}

        @Override
        public void nodeChangedLength(NodeAndPosition<ParsedNode> node, int oldPos, int oldSize) {}
    };

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
        // Iterate getChildren() instead of getDeclarations() to see PsiComment elements
        for (PsiElement child : ktFile.getChildren())
        {
            if (child instanceof PsiComment psiComment)
            {
                insertCommentNode(psiComment, parent, parentAbsPos, lsnr);
            }
            else if (child instanceof KtDeclaration decl)
            {
                processDeclaration(decl, parent, parentAbsPos, lsnr);
            }
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
        else if (decl instanceof KtSecondaryConstructor ktCtor)
        {
            processConstructor(ktCtor, parent, parentAbsPos, listener);
        }
        else if (decl instanceof KtClassInitializer ktInit)
        {
            processInitBlock(ktInit, parent, parentAbsPos, listener);
        }
        // KtPrimaryConstructor — no block body (parameter declarations only),
        // so no scope node. Kotlin uses init blocks for initialization logic.
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

        // Insert comment nodes for KDoc/comments attached to this declaration
        insertAttachedComments(ktClass, typeNode, absPos, listener);

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

        // Insert comment nodes for KDoc/comments attached to this declaration
        insertAttachedComments(ktObject, typeNode, absPos, listener);

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
     * Process a class body — walk children (declarations and comments) and
     * create child nodes.
     *
     * @param parentAbsPos absolute document position of the parent BlueJ node
     */
    private static void processClassBody(KtClassBody body, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        // Iterate getChildren() instead of getDeclarations() to see PsiComment elements
        for (PsiElement child : body.getChildren())
        {
            if (child instanceof PsiComment psiComment)
            {
                insertCommentNode(psiComment, parent, parentAbsPos, listener);
            }
            else if (child instanceof KtDeclaration decl)
            {
                processDeclaration(decl, parent, parentAbsPos, listener);
            }
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

        // Insert comment nodes for KDoc/comments attached to this declaration
        insertAttachedComments(ktFunction, methodNode, absPos, listener);

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
                    // Recurse into expression body for nested control flow (if/when/for/while)
                    processExpression(exprBody, innerNode, innerAbsPos, listener);
                }
            }
        }
    }

    /**
     * Process a secondary constructor → KotlinParentNode(NODETYPE_METHODDEF).
     *
     * <p>Follows the same container+inner pattern as {@link #processFunction}. Secondary
     * constructors always have block bodies (no expression body variant). The delegation
     * call ({@code this(...)} or {@code super(...)}) is part of the text range but does
     * not produce a separate scope node.</p>
     */
    private static void processConstructor(KtSecondaryConstructor ktCtor, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        TextRange range = ktCtor.getTextRange();
        int absPos = range.getStartOffset();
        int relPos = absPos - parentAbsPos;
        int size = range.getLength();

        // Container node (NODETYPE_METHODDEF → yellow scope)
        KotlinParentNode ctorNode = new KotlinParentNode(parent, ParsedNode.NODETYPE_METHODDEF);
        ctorNode.setName("constructor");
        ctorNode.setComplete(true);

        parent.insertNode(ctorNode, relPos, size, listener);

        // Insert comment nodes for KDoc/comments attached to this declaration
        insertAttachedComments(ktCtor, ctorNode, absPos, listener);

        // Inner node for constructor body (block body only — no expression body for constructors)
        KtBlockExpression block = ktCtor.getBodyBlockExpression();
        if (block != null)
        {
            KotlinParentNode innerNode = insertInnerNode(block, ctorNode, absPos, listener);
            if (innerNode != null)
            {
                int innerAbsPos = block.getTextRange().getStartOffset() + 1;
                processBlockContents(block, innerNode, innerAbsPos, listener);
            }
        }
    }

    /**
     * Process an init block → KotlinParentNode(NODETYPE_METHODDEF).
     *
     * <p>Init blocks ({@code init { ... }}) are initializer code analogous to constructors.
     * They produce {@code NODETYPE_METHODDEF} (yellow scope) matching the treatment of
     * constructors and methods. Multiple init blocks in the same class each get their own
     * container+inner scope node.</p>
     */
    private static void processInitBlock(KtClassInitializer ktInit, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        TextRange range = ktInit.getTextRange();
        int absPos = range.getStartOffset();
        int relPos = absPos - parentAbsPos;
        int size = range.getLength();

        // Container node (NODETYPE_METHODDEF → yellow scope)
        KotlinParentNode initNode = new KotlinParentNode(parent, ParsedNode.NODETYPE_METHODDEF);
        initNode.setName("init");
        initNode.setComplete(true);

        parent.insertNode(initNode, relPos, size, listener);

        // Insert comment nodes for KDoc/comments attached to this declaration
        insertAttachedComments(ktInit, initNode, absPos, listener);

        // Inner node for init body
        KtExpression body = ktInit.getBody();
        if (body instanceof KtBlockExpression block)
        {
            KotlinParentNode innerNode = insertInnerNode(block, initNode, absPos, listener);
            if (innerNode != null)
            {
                int innerAbsPos = block.getTextRange().getStartOffset() + 1;
                processBlockContents(block, innerNode, innerAbsPos, listener);
            }
        }
    }

    /**
     * Process the contents of a block expression, looking for nested
     * scope-creating constructs (if/when/for/while) and comments.
     *
     * <p>Uses AST-level traversal because {@code KtBlockExpression.getChildren()}
     * does not return comments — they are only visible at the AST node level.</p>
     *
     * @param parentAbsPos absolute document position of the parent BlueJ node
     */
    private static void processBlockContents(KtBlockExpression block, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        // Walk AST children to find both statements and comments.
        // PSI getChildren() on KtBlockExpression doesn't return comments.
        for (ASTNode child = block.getNode().getFirstChildNode();
             child != null; child = child.getTreeNext())
        {
            PsiElement psi = child.getPsi();
            if (psi instanceof PsiComment psiComment)
            {
                insertCommentNode(psiComment, parent, parentAbsPos, listener);
            }
            else if (!(psi instanceof PsiWhiteSpace)
                && child.getElementType() != KtTokens.LBRACE
                && child.getElementType() != KtTokens.RBRACE)
            {
                // Detect comments attached to declarations within the block
                // (e.g., KDoc/block/line comments before a property like val x = 5).
                // PSI includes these comments inside the declaration's AST node,
                // so the block's AST walk sees the declaration — not the comment.
                insertAttachedComments(psi, parent, parentAbsPos, listener);
                processExpression(psi, parent, parentAbsPos, listener);
            }
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
        // Multiline triple-quoted strings → KotlinStringNode with template children.
        // Single-line strings fall through to the catch-all else (recurse into children).
        else if (element instanceof KtStringTemplateExpression stringExpr
                && isMultilineString(stringExpr))
        {
            insertStringNode(stringExpr, parent, parentAbsPos, listener);
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
     * Insert a {@link KotlinCommentNode} for a PSI comment element.
     * Determines the comment type (KDoc → COMMENT_JAVADOC, others → COMMENT_NORMAL)
     * and single-line flag from the PSI token type.
     *
     * @param comment      the PSI comment element
     * @param parent       the parent BlueJ node to insert into
     * @param parentAbsPos absolute document position of the parent
     * @param listener     structure listener
     */
    private static void insertCommentNode(PsiComment comment, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        TextRange range = comment.getTextRange();
        int relPos = range.getStartOffset() - parentAbsPos;
        int size = range.getLength();

        if (size <= 0)
        {
            return;
        }

        TokenType commentType;
        if (comment.getTokenType() == KtTokens.DOC_COMMENT)
        {
            commentType = TokenType.COMMENT_JAVADOC;
        }
        else
        {
            // EOL_COMMENT, BLOCK_COMMENT, or SHEBANG_COMMENT
            commentType = TokenType.COMMENT_NORMAL;
        }

        KotlinCommentNode node = new KotlinCommentNode(parent, commentType);
        node.setComplete(true);
        parent.insertNode(node, relPos, size, listener);
    }

    /**
     * Insert comment nodes for comments that are attached to a declaration.
     *
     * <p>In Kotlin PSI, comments preceding a declaration (KDoc, block comments,
     * and line comments) are included within the declaration's text range.
     * However, only KDoc appears via {@code getChildren()} — regular comments
     * are only visible at the AST node level. We walk AST children to catch
     * all comment types.</p>
     *
     * @param declaration  the PSI declaration element (class, function, object)
     * @param container    the container BlueJ node for this declaration
     * @param containerAbsPos absolute document position of the container
     * @param listener     structure listener
     */
    private static void insertAttachedComments(PsiElement declaration,
            JavaParentNode container, int containerAbsPos, NodeStructureListener listener)
    {
        // Walk AST children — PSI getChildren() doesn't return regular comments
        for (ASTNode child = declaration.getNode().getFirstChildNode();
             child != null; child = child.getTreeNext())
        {
            PsiElement psi = child.getPsi();
            if (psi instanceof PsiComment psiComment)
            {
                insertCommentNode(psiComment, container, containerAbsPos, listener);
            }
        }
    }

    /**
     * Check whether a string template expression is a multiline triple-quoted
     * string ({@code """..."""}). Single-line strings ({@code "..."}) are
     * handled correctly by per-line tokenization and don't need a parse
     * tree node.
     */
    private static boolean isMultilineString(KtStringTemplateExpression stringExpr)
    {
        String text = stringExpr.getText();
        return text != null && text.startsWith("\"\"\"");
    }

    /**
     * Insert a {@link KotlinStringNode} for a multiline triple-quoted string,
     * with child {@link KotlinParentNode} nodes for template expression bodies.
     *
     * <p>The string node covers the entire {@code """..."""} region.
     * {@code KotlinStringNode.tokenizeText()} returns {@code STRING_LITERAL}
     * for all gap content (plain text, {@code $}, {@code ${}, {@code }}).
     * Child nodes cover template expression bodies, getting normal Kotlin
     * tokenization (identifiers render as black).</p>
     *
     * @param stringExpr   the PSI string template expression (must be multiline)
     * @param parent       the parent BlueJ node to insert into
     * @param parentAbsPos absolute document position of the parent
     * @param listener     structure listener
     */
    private static void insertStringNode(KtStringTemplateExpression stringExpr,
            JavaParentNode parent, int parentAbsPos, NodeStructureListener listener)
    {
        TextRange range = stringExpr.getTextRange();
        int relPos = range.getStartOffset() - parentAbsPos;
        int size = range.getLength();

        if (size <= 0)
        {
            return;
        }

        KotlinStringNode stringNode = new KotlinStringNode(parent);
        stringNode.setComplete(true);
        parent.insertNode(stringNode, relPos, size, listener);

        int stringAbsPos = range.getStartOffset();

        // Create child nodes for template expression bodies
        for (KtStringTemplateEntry entry : stringExpr.getEntries())
        {
            if (entry instanceof KtSimpleNameStringTemplateEntry simpleEntry)
            {
                // $name → child covering the identifier after $
                PsiElement nameElement = simpleEntry.getExpression();
                if (nameElement != null)
                {
                    TextRange nameRange = nameElement.getTextRange();
                    int childRelPos = nameRange.getStartOffset() - stringAbsPos;
                    int childSize = nameRange.getLength();
                    if (childSize > 0)
                    {
                        KotlinParentNode child = new KotlinParentNode(stringNode);
                        child.setComplete(true);
                        stringNode.insertNode(child, childRelPos, childSize, listener);
                    }
                }
            }
            else if (entry instanceof KtBlockStringTemplateEntry blockEntry)
            {
                // ${expr} → child covering expression body between ${ and }
                // Recurse into the expression to detect nested control flow
                // (e.g., ${if (A) B else C} gets blue scope highlighting).
                PsiElement expression = blockEntry.getExpression();
                if (expression != null)
                {
                    TextRange exprRange = expression.getTextRange();
                    int childRelPos = exprRange.getStartOffset() - stringAbsPos;
                    int childSize = exprRange.getLength();
                    if (childSize > 0)
                    {
                        KotlinParentNode child = new KotlinParentNode(stringNode);
                        child.setComplete(true);
                        stringNode.insertNode(child, childRelPos, childSize, listener);
                        // Build scope nodes inside the expression (if/when/for/while)
                        int childAbsPos = exprRange.getStartOffset();
                        processExpression(expression, child, childAbsPos, listener);
                    }
                }
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
