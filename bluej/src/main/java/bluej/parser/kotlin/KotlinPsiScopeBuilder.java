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
        for (PsiElement child : ktFile.getChildren()) {
            if (child instanceof PsiComment psiComment) {
                insertCommentNode(psiComment, parent, parentAbsPos, lsnr);
            } else if (child instanceof KtDeclaration decl) {
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
        if (decl instanceof KtClass ktClass) {
            processTypeDeclaration(ktClass, ktClass.getName(), ktClass.getBody(),
                    parent, parentAbsPos, listener);
        } else if (decl instanceof KtObjectDeclaration ktObject) {
            processTypeDeclaration(ktObject, ktObject.getName(), ktObject.getBody(),
                    parent, parentAbsPos, listener);
        } else if (decl instanceof KtNamedFunction ktFunction) {
            processFunction(ktFunction, parent, parentAbsPos, listener);
        } else if (decl instanceof KtSecondaryConstructor ktCtor) {
            processConstructor(ktCtor, parent, parentAbsPos, listener);
        } else if (decl instanceof KtClassInitializer ktInit) {
            processInitBlock(ktInit, parent, parentAbsPos, listener);
        }
        // KtPrimaryConstructor — no block body (parameter declarations only),
        // so no scope node. Kotlin uses init blocks for initialization logic.
        // KtProperty and other declarations don't create scope nodes
    }

    /**
     * Process a class or object declaration → KotlinParentNode(NODETYPE_TYPEDEF).
     * Companion objects are inlined into the parent scope.
     */
    private static void processTypeDeclaration(PsiElement element, String name,
            KtClassBody body, JavaParentNode parent, int parentAbsPos,
            NodeStructureListener listener)
    {
        // Skip companion objects — they don't form a visible scope
        if (element instanceof KtObjectDeclaration ktObj && ktObj.isCompanion()) {
            if (body != null) {
                processClassBody(body, parent, parentAbsPos, listener);
            }
            return;
        }

        KotlinParentNode typeNode = createAndInsertScopeNode(
                element, parent, parentAbsPos, ParsedNode.NODETYPE_TYPEDEF, name, listener);

        if (body != null) {
            int absPos = element.getTextRange().getStartOffset();
            insertInnerBlockAndProcessClassBody(body, typeNode, absPos, listener);
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
        for (PsiElement child : body.getChildren()) {
            if (child instanceof PsiComment psiComment) {
                insertCommentNode(psiComment, parent, parentAbsPos, listener);
            } else if (child instanceof KtDeclaration decl) {
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
        KotlinParentNode methodNode = createAndInsertScopeNode(
                ktFunction, parent, parentAbsPos,
                ParsedNode.NODETYPE_METHODDEF, ktFunction.getName(), listener);

        int absPos = ktFunction.getTextRange().getStartOffset();

        // Create inner node for function body (matches Java's container+inner pattern)
        if (ktFunction.hasBlockBody()) {
            KtBlockExpression block = ktFunction.getBodyBlockExpression();
            if (block != null) {
                insertInnerBlockAndProcessContents(block, methodNode, absPos, listener);
            }
        } else {
            // Expression body: fun f() = expr — inner spans the expression
            KtExpression exprBody = ktFunction.getBodyExpression();
            if (exprBody != null) {
                TextRange exprRange = exprBody.getTextRange();
                int innerAbsPos = exprRange.getStartOffset();
                int innerRelPos = innerAbsPos - absPos;
                int innerSize = exprRange.getLength();

                if (innerSize > 0) {
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
     * Process a KtSecondaryConstructor, creating a scope node for
     * constructor parameters and body.
     */
    private static void processConstructor(KtSecondaryConstructor ktCtor, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        KotlinParentNode ctorNode = createAndInsertScopeNode(
                ktCtor, parent, parentAbsPos,
                ParsedNode.NODETYPE_METHODDEF, "constructor", listener);

        KtBlockExpression block = ktCtor.getBodyBlockExpression();
        if (block != null) {
            insertInnerBlockAndProcessContents(block, ctorNode,
                    ktCtor.getTextRange().getStartOffset(), listener);
        }
    }

    /**
     * Process a KtClassInitializer (init block), creating a scope
     * node for the initializer body.
     */
    private static void processInitBlock(KtClassInitializer ktInit, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        KotlinParentNode initNode = createAndInsertScopeNode(
                ktInit, parent, parentAbsPos,
                ParsedNode.NODETYPE_METHODDEF, "init", listener);

        KtExpression body = ktInit.getBody();
        if (body instanceof KtBlockExpression block) {
            insertInnerBlockAndProcessContents(block, initNode,
                    ktInit.getTextRange().getStartOffset(), listener);
        }
    }

    /**
     * Recurse into the children of a block expression, processing
     * nested declarations and statements.
     */
    private static void processBlockContents(KtBlockExpression block, JavaParentNode parent,
            int parentAbsPos, NodeStructureListener listener)
    {
        // Walk AST children to find both statements and comments.
        // PSI getChildren() on KtBlockExpression doesn't return comments.
        for (ASTNode child = block.getNode().getFirstChildNode();
             child != null; child = child.getTreeNext()) {
            PsiElement psi = child.getPsi();
            if (psi instanceof PsiComment psiComment) {
                insertCommentNode(psiComment, parent, parentAbsPos, listener);
            } else if (!(psi instanceof PsiWhiteSpace)
                && child.getElementType() != KtTokens.LBRACE
                && child.getElementType() != KtTokens.RBRACE) {
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
        if (element instanceof KtIfExpression ktIf) {
            processControlFlow(ktIf, parent, parentAbsPos, ParsedNode.NODETYPE_SELECTION, listener);
        } else if (element instanceof KtWhenExpression ktWhen) {
            processControlFlow(ktWhen, parent, parentAbsPos, ParsedNode.NODETYPE_SELECTION, listener);
        } else if (element instanceof KtLoopExpression ktLoop) {
            // KtForExpression, KtWhileExpression, KtDoWhileExpression all extend KtLoopExpression
            processControlFlow(ktLoop, parent, parentAbsPos, ParsedNode.NODETYPE_ITERATION, listener);
        }
        // Strings with block template entries (${...}) or triple-quoted strings
        // need a KotlinStringNode — otherwise control flow inside ${...} would
        // create scope nodes that split the string during getMarkTokensFor().
        else if (element instanceof KtStringTemplateExpression stringExpr
                && (isMultilineString(stringExpr) || hasBlockTemplateEntry(stringExpr))) {
            insertStringNode(stringExpr, parent, parentAbsPos, listener);
        }
        // Recurse into nested blocks (e.g., lambda bodies, run { }, etc.)
        else if (element instanceof KtBlockExpression block) {
            processBlockContents(block, parent, parentAbsPos, listener);
        }
        // For other elements, check children for nested control flow
        else {
            for (PsiElement child : element.getChildren()) {
                processExpression(child, parent, parentAbsPos, listener);
            }
        }
    }

    /**
     * Create a {@link KotlinParentNode} scope node for a PSI element, insert
     * it into the parent, and attach any leading comments. This consolidates
     * the common range→position→create→insert→comments boilerplate shared
     * by processTypeDeclaration, processFunction, processConstructor, and
     * processInitBlock.
     *
     * @param element      the PSI element that defines the scope
     * @param parent       the parent BlueJ node to insert into
     * @param parentAbsPos absolute document position of the parent
     * @param nodeType     one of the NODETYPE_* constants (TYPEDEF, METHODDEF, etc.)
     * @param name         display name for the node (may be null)
     * @param listener     structure listener for node change notifications
     * @return the newly created and inserted scope node
     */
    private static KotlinParentNode createAndInsertScopeNode(PsiElement element,
            JavaParentNode parent, int parentAbsPos, int nodeType, String name,
            NodeStructureListener listener)
    {
        TextRange range = element.getTextRange();
        int absPos = range.getStartOffset();
        int relPos = absPos - parentAbsPos;
        int size = range.getLength();

        KotlinParentNode node = new KotlinParentNode(parent, nodeType);
        if (name != null) {
            node.setName(name);
        }
        node.setComplete(true);

        parent.insertNode(node, relPos, size, listener);
        insertAttachedComments(element, node, absPos, listener);

        return node;
    }

    /**
     * Insert an inner node for a braced block and process its contents
     * as block statements. Shared by processFunction, processConstructor,
     * and processInitBlock.
     *
     * @param block          the KtBlockExpression to process
     * @param container      the container scope node
     * @param containerAbsPos absolute position of the container element
     * @param listener       structure listener
     */
    private static void insertInnerBlockAndProcessContents(KtBlockExpression block,
            KotlinParentNode container, int containerAbsPos, NodeStructureListener listener)
    {
        KotlinParentNode innerNode = insertInnerNode(block, container, containerAbsPos, listener);
        if (innerNode != null) {
            int innerAbsPos = block.getTextRange().getStartOffset() + 1;
            processBlockContents(block, innerNode, innerAbsPos, listener);
        }
    }

    /**
     * Insert an inner node for a class body and process its members
     * (declarations and comments). Used by processTypeDeclaration.
     *
     * @param body            the KtClassBody to process
     * @param container       the container scope node
     * @param containerAbsPos absolute position of the container element
     * @param listener        structure listener
     */
    private static void insertInnerBlockAndProcessClassBody(KtClassBody body,
            KotlinParentNode container, int containerAbsPos, NodeStructureListener listener)
    {
        KotlinParentNode innerNode = insertInnerNode(body, container, containerAbsPos, listener);
        if (innerNode != null) {
            int innerAbsPos = body.getTextRange().getStartOffset() + 1;
            processClassBody(body, innerNode, innerAbsPos, listener);
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

        if (size <= 0) {
            return;
        }

        TokenType commentType;
        if (comment.getTokenType() == KtTokens.DOC_COMMENT) {
            commentType = TokenType.COMMENT_JAVADOC;
        } else {
            // EOL_COMMENT, BLOCK_COMMENT, or SHEBANG_COMMENT
            commentType = TokenType.COMMENT_NORMAL;
        }

        KotlinCommentNode node = new KotlinCommentNode(parent, commentType);
        node.setComplete(true);
        parent.insertNode(node, relPos, size, listener);
    }

    /**
     * Insert comment nodes for comments attached to (preceding) a declaration.
     * Walks AST children since PSI getChildren() omits regular comments.
     */
    private static void insertAttachedComments(PsiElement declaration,
            JavaParentNode container, int containerAbsPos, NodeStructureListener listener)
    {
        // Walk AST children — PSI getChildren() doesn't return regular comments
        for (ASTNode child = declaration.getNode().getFirstChildNode();
             child != null; child = child.getTreeNext()) {
            PsiElement psi = child.getPsi();
            if (psi instanceof PsiComment psiComment) {
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

    // Check whether a string contains a block template entry (${...}).
    private static boolean hasBlockTemplateEntry(KtStringTemplateExpression stringExpr)
    {
        for (KtStringTemplateEntry entry : stringExpr.getEntries()) {
            if (entry instanceof KtBlockStringTemplateEntry) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a KotlinStringNode for a string literal and populate its
     * children for template expression highlighting.
     */
    private static void insertStringNode(KtStringTemplateExpression stringExpr,
            JavaParentNode parent, int parentAbsPos, NodeStructureListener listener)
    {
        TextRange range = stringExpr.getTextRange();
        int relPos = range.getStartOffset() - parentAbsPos;
        int size = range.getLength();

        if (size <= 0) {
            return;
        }

        KotlinStringNode stringNode = new KotlinStringNode(parent);
        stringNode.setComplete(true);
        parent.insertNode(stringNode, relPos, size, listener);

        int stringAbsPos = range.getStartOffset();

        for (KtStringTemplateEntry entry : stringExpr.getEntries()) {
            if (entry instanceof KtSimpleNameStringTemplateEntry simpleEntry) {
                // $name → child covering the identifier after $
                PsiElement nameElement = simpleEntry.getExpression();
                if (nameElement != null) {
                    TextRange nameRange = nameElement.getTextRange();
                    int childRelPos = nameRange.getStartOffset() - stringAbsPos;
                    int childSize = nameRange.getLength();
                    if (childSize > 0) {
                        KotlinParentNode child = new KotlinParentNode(stringNode);
                        child.setComplete(true);
                        stringNode.insertNode(child, childRelPos, childSize, listener);
                    }
                }
            } else if (entry instanceof KtBlockStringTemplateEntry blockEntry) {
                // ${expr} → child covering expression body between ${ and }
                // Recurse into the expression to detect nested control flow
                // (e.g., ${if (A) B else C} gets blue scope highlighting).
                PsiElement expression = blockEntry.getExpression();
                if (expression != null) {
                    TextRange exprRange = expression.getTextRange();
                    int childRelPos = exprRange.getStartOffset() - stringAbsPos;
                    int childSize = exprRange.getLength();
                    if (childSize > 0) {
                        KotlinParentNode child = new KotlinParentNode(stringNode);
                        child.setComplete(true);
                        stringNode.insertNode(child, childRelPos, childSize, listener);
                        int childAbsPos = exprRange.getStartOffset();
                        processExpression(expression, child, childAbsPos, listener);
                    }
                }
            }
        }
    }

    /**
     * Create an inner (method-body) node spanning from opening brace
     * to closing brace of a code block.
     */
    private static KotlinParentNode insertInnerNode(PsiElement bodyElement,
            JavaParentNode container, int containerAbsPos, NodeStructureListener listener)
    {
        TextRange bodyRange = bodyElement.getTextRange();
        int innerAbsPos = bodyRange.getStartOffset() + 1; // after '{'
        int innerRelPos = innerAbsPos - containerAbsPos;
        int innerSize = bodyRange.getLength() - 2; // exclude '{' and '}'

        if (innerSize <= 0) {
            return null;
        }

        KotlinParentNode innerNode = new KotlinParentNode(container, ParsedNode.NODETYPE_NONE);
        innerNode.setInner(true);
        innerNode.setComplete(true);
        container.insertNode(innerNode, innerRelPos, innerSize, listener);
        return innerNode;
    }

    /**
     * Process a body element as an inner node. Handles both braced
     * blocks and braceless single-expression bodies.
     */
    private static void processBodyAsInner(KtExpression bodyExpr,
            JavaParentNode container, int containerAbsPos, NodeStructureListener listener)
    {
        if (bodyExpr == null) {
            return;
        }

        if (bodyExpr instanceof KtBlockExpression block) {
            // Block body — inner spans content between { and }
            KotlinParentNode innerNode = insertInnerNode(block, container, containerAbsPos, listener);
            if (innerNode != null) {
                int innerAbsPos = block.getTextRange().getStartOffset() + 1;
                processBlockContents(block, innerNode, innerAbsPos, listener);
            }
        } else {
            // Braceless body (e.g., if (x) return y) — inner spans the expression
            TextRange range = bodyExpr.getTextRange();
            int innerAbsPos = range.getStartOffset();
            int innerRelPos = innerAbsPos - containerAbsPos;
            int innerSize = range.getLength();

            if (innerSize > 0) {
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
     * Process a control flow statement (if/when/for/while) by creating
     * a container node and recursing into its body.
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
        if (element instanceof KtLoopExpression ktLoop) {
            // for/while/do-while — single body via KtLoopExpression.getBody()
            processBodyAsInner(ktLoop.getBody(), scopeNode, absPos, listener);
        } else if (element instanceof KtIfExpression ktIf) {
            // if — two branches: then and else
            processBodyAsInner(ktIf.getThen(), scopeNode, absPos, listener);
            KtExpression elseExpr = ktIf.getElse();
            if (elseExpr instanceof KtIfExpression elseIf) {
                // else-if chain: nested selection scope
                processControlFlow(elseIf, scopeNode, absPos,
                        ParsedNode.NODETYPE_SELECTION, listener);
            } else {
                processBodyAsInner(elseExpr, scopeNode, absPos, listener);
            }
        } else if (element instanceof KtWhenExpression ktWhen) {
            // when — each entry has its own body expression
            for (KtWhenEntry entry : ktWhen.getEntries()) {
                processBodyAsInner(entry.getExpression(), scopeNode, absPos, listener);
            }
        }
    }
}
