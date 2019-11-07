/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2011,2012,2013,2014,2016,2017,2019  Michael Kolling and John Rosenberg 
 
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
package bluej.parser;

import bluej.debugger.gentype.Reflective;
import bluej.parser.nodes.ReparseableDocument.Element;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.IntersectionTypeEntity;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.PositionedResolver;
import bluej.parser.entity.TparEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.UnresolvedArray;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.*;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.symtab.Selection;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

/**
 * Parser which builds parse node tree.
 * 
 * @author Davin McCall
 */
public class EditorParser extends JavaParser
{
    private final NodeStructureListener nodeStructureListener;
    protected Stack<JavaParentNode> scopeStack = new Stack<JavaParentNode>();
    private ParsedTypeNode innermostType;
    
    private LocatableToken pcuStmtBegin;
    private ParsedCUNode pcuNode;
    private List<LocatableToken> commentQueue = new LinkedList<LocatableToken>();
    private List<LocatableToken> lastTypeSpec;
    private FieldNode lastField;
    private int arrayDecls;
    private String declaredPkg = "";
    
    class TypeParam
    {
        String name;
        List<List<LocatableToken>> bounds;
        
        TypeParam(String name, List<List<LocatableToken>> bounds)
        {
            this.name = name;
            this.bounds = bounds;
        }
    }
    
    private List<TypeParam> typeParams;
    private String lastTypeParamName;
    private List<List<LocatableToken>> lastTypeParBounds;
    
    private List<JavaEntity> extendedTypes;
    private List<JavaEntity> implementedTypes;
    
    private ReparseableDocument document;
    
    private boolean gotExtends = false;
    private boolean gotImplements = false;
    
    private boolean gotNewType = true;  // whether we've seen the type in a "new TYPE(..." expression,
        // assuming we're in such an expression. (If false, we have seen new, but not the type).
    
    /** Stack of types instantiated via "new ...()" expression */
    private Stack<List<LocatableToken>> newTypes = new Stack<List<LocatableToken>>();
    
    private int currentModifiers = 0;
    
    /**
     * Constructor for use by subclasses (InfoReader).
     */
    protected EditorParser(Reader r, EntityResolver resolver)
    {
        super(r);
        nodeStructureListener = new NodeStructureListener()
        {
            @Override
            public void nodeAdded(NodeAndPosition<ParsedNode> node)
            {
            }

            @Override
            public void nodeRemoved(NodeAndPosition<ParsedNode> node)
            {
            }

            @Override
            public void nodeChangedLength(NodeAndPosition<ParsedNode> node, int oldPos, int oldSize)
            {
            }
        };
        pcuNode = new ParsedCUNode(resolver);
    }
    
    public EditorParser(ReparseableDocument document, Reader r, int line, int col, int pos, Stack<JavaParentNode> scopeStack, NodeStructureListener nodeStructureListener)
    {
        super(r, line, col, pos);
        this.document = document;
        this.scopeStack = scopeStack;
        this.nodeStructureListener = nodeStructureListener;
        pcuNode = (ParsedCUNode) scopeStack.get(0);
    }
    
    /**
     * Get the types following the "extends" keyword, if we have some. Used in incremental parsing.
     */
    public List<JavaEntity> getExtendedTypes()
    {
        return extendedTypes;
    }
    
    @Override
    // This tag is hacky, but if document is an instanceof MoeSyntaxDocument, the parsing
    // should be happening on the FX thread:
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected void error(String msg, int beginLine, int beginColumn, int endLine, int endColumn)
    {
        Element lineEl = document.getDefaultRootElement().getElement(beginLine - 1);
        int position = lineEl.getStartOffset() + beginColumn - 1;
        if (endLine != beginLine) {
            lineEl = document.getDefaultRootElement().getElement(endLine - 1);
        }
        int endPos = lineEl.getStartOffset() + endColumn - 1;
    }
    
    @Override
    public void parseCU()
    {
        scopeStack.push(pcuNode);
        super.parseCU();
        scopeStack.pop();
        completedNode(pcuNode, 0, pcuNode.getSize());
    }
    
    /**
     * Convert a line and column number to an absolute position within the document
     * @param line  Line number (1..N)
     * @param col   Column number (1..N)
     * @return   The absolute position (0..N)
     */
    protected int lineColToPosition(int line, int col)
    {
        if (document == null) {
            return 0;
        }
        return document.getDefaultRootElement().getElement(line - 1).getStartOffset() + col - 1;
    }

    /**
     * Close the node which is currently at the top of the node stack.
     * @param token     The token which finishes the node
     * @param included  Whether the token itself is part of the node
     */
    private void endTopNode(LocatableToken token, boolean included)
    {
        int topPos = getTopNodeOffset();
        ParsedNode top = scopeStack.pop();

        int endPos;
        if (included) {
            endPos = lineColToPosition(token.getEndLine(), token.getEndColumn());
        }
        else {
            endPos = lineColToPosition(token.getLine(), token.getColumn());
        }
        top.resize(endPos - topPos);
        NodeAndPosition<ParsedNode> child = new NodeAndPosition<ParsedNode>(top, topPos, endPos - topPos);
        scopeStack.peek().childResized(null, topPos - top.getOffsetFromParent(), child);
        
        completedNode(top, topPos, endPos - topPos);
    }
    
    /**
     * Check whether a type specification (list of tokens) is "var", the magical non-keyword used
     * to enable type inference.
     */
    private boolean typeSpecIsVar(List<LocatableToken> typeSpec)
    {
        if (typeSpec.size() == 1)
        {
            if (typeSpec.get(0).getText().equals("var"))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * A node end has been reached. This method adds any appropriate comment nodes as
     * children of the new node.
     * 
     * @param node  The new node
     * @param position  The absolute position of the new node
     * @param size  The size of the new node
     */
    public void completedNode(ParsedNode node, int position, int size)
    {
        ListIterator<LocatableToken> i = commentQueue.listIterator();
        while (i.hasNext()) {
            LocatableToken token = i.next();
            int startpos = lineColToPosition(token.getLine(), token.getColumn());
            if (startpos >= position && startpos < (position + size)) {
                int endpos = lineColToPosition(token.getEndLine(), token.getEndColumn());
                CommentNode cn = new CommentNode(node, token);
                node.insertNode(cn, startpos - position, endpos - startpos, nodeStructureListener);
                i.remove();
            }
        }
    }
    
    /**
     * Prepare to begin a new node at the given position (document position).
     */
    protected void beginNode(int position)
    {
        // If there are comments in the queue, and their position precedes that of the node
        // just being created, then we have to create their nodes now.
        ListIterator<LocatableToken> i = commentQueue.listIterator();
        while (i.hasNext()) {
            LocatableToken token = i.next();
            int startpos = lineColToPosition(token.getLine(), token.getColumn());
            int endpos = lineColToPosition(token.getEndLine(), token.getEndColumn());
            if (startpos >= position) {
                break;
            }
            i.remove();
            int topOffset = getTopNodeOffset();
            CommentNode cn = new CommentNode(scopeStack.peek(), token);
            scopeStack.peek().insertNode(cn, startpos - topOffset, endpos - startpos, nodeStructureListener);
        }
    }
    
    /**
     * Get the start position of the top node in the scope stack.
     */
    protected int getTopNodeOffset()
    {
        Iterator<JavaParentNode> i = scopeStack.iterator();
        if (!i.hasNext()) {
            return 0;
        }
        
        int rval = 0;
        i.next();
        while (i.hasNext()) {
            rval += i.next().getOffsetFromParent();
        }
        return rval;
    }
    
    /**
     * Join a sequence of tokens together to form a string.
     */
    protected static String joinTokens(List<LocatableToken> tokens)
    {
        StringBuffer r = new StringBuffer();
        for (LocatableToken token : tokens) {
            r.append(token.getText());
        }
        return r.toString();
    }
    
    /**
     * Get the current query source - a fully qualified class name representing
     * the current context. (This is mainly used to determine what members of a
     * class are accessible).
     */
    protected Reflective currentQuerySource()
    {
        ListIterator<JavaParentNode> i = scopeStack.listIterator(scopeStack.size());
        while (i.hasPrevious()) {
            ParsedNode pn = i.previous();
            if (pn.getNodeType() == ParsedNode.NODETYPE_TYPEDEF) {
                ParsedTypeNode ptn = (ParsedTypeNode)pn;
                return new ParsedReflective(ptn);
            }
        }
        
        return null;
    }
    
    
    //  -------------- Callbacks from the superclass ----------------------

    @Override
    protected void gotModifier(LocatableToken token)
    {
        switch (token.getType()) {
        case JavaTokenTypes.ABSTRACT:
            currentModifiers |= Modifier.ABSTRACT;
            break;
        case JavaTokenTypes.LITERAL_private:
            currentModifiers |= Modifier.PRIVATE;
            break;
        case JavaTokenTypes.LITERAL_public:
            currentModifiers |= Modifier.PUBLIC;
            break;
        case JavaTokenTypes.LITERAL_protected:
            currentModifiers |= Modifier.PROTECTED;
            break;
        case JavaTokenTypes.FINAL:
            currentModifiers |= Modifier.FINAL;
            break;
        case JavaTokenTypes.LITERAL_synchronized:
            currentModifiers |= Modifier.SYNCHRONIZED;
            break;
        case JavaTokenTypes.STRICTFP:
            currentModifiers |= Modifier.STRICT;
            break;
        case JavaTokenTypes.LITERAL_native:
            currentModifiers |= Modifier.NATIVE;
            break;
        case JavaTokenTypes.LITERAL_static:
            currentModifiers |= Modifier.STATIC;
            break;
        default:
        }
    }
    
    @Override
    protected void modifiersConsumed()
    {
        currentModifiers = 0;
    }
    
    @Override
    protected void beginPackageStatement(LocatableToken token)
    {
        pcuStmtBegin = token;
    }
    
    @Override
    protected void gotPackage(List<LocatableToken> pkgTokens)
    {
        super.gotPackage(pkgTokens);
        declaredPkg = joinTokens(pkgTokens);
    }

    @Override
    protected void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken)
    {
        EntityResolver parentResolver = pcuNode.getParentResolver();
        if (parentResolver == null) {
            return;
        }
        
        if (isStatic) {
            // Apparently static inner classes can be imported with or without the "static" keyword
            // So, a static import imports a field and/or method and/or class.
            // That's right - the same import statement pulls in all three.
            
            // We want to pull the name out (and remove the intermediate dot)
            int newSize = tokens.size() - 2;
            String memberName = tokens.get(newSize + 1).getText();
            
            List<LocatableToken> newList = new ArrayList<LocatableToken>(newSize);
            Iterator<LocatableToken> i = tokens.iterator();
            while (newSize > 0) {
                newList.add(i.next());
                newSize--;
            }
            JavaEntity entity = ParseUtils.getImportEntity(parentResolver,
                    currentQuerySource(), newList);
            TypeEntity tentity = (entity != null) ? entity.resolveAsType() : null;
            if (tentity != null) {
                pcuNode.getImports().addStaticImport(memberName, tentity, importToken, semiColonToken);
            }
        }
        else {
            String memberName = tokens.get(tokens.size() - 1).getText();
            JavaEntity entity = ParseUtils.getImportEntity(parentResolver,
                    currentQuerySource(), tokens);
            if (entity != null) {
                pcuNode.getImports().addNormalImport(memberName, entity, importToken, semiColonToken);
            }
        }
    }
    
    @Override
    protected void gotWildcardImport(List<LocatableToken> tokens,
                                     boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken)
    {
        EntityResolver parentResolver = pcuNode.getParentResolver();
        if (parentResolver == null) {
            return;
        }

        JavaEntity importEntity = ParseUtils.getImportEntity(parentResolver,
                currentQuerySource(), tokens);
        if (importEntity == null) {
            return;
        }
        if (! isStatic) {
            pcuNode.getImports().addWildcardImport(importEntity, importToken, semiColonToken);
        }
        else {
            TypeEntity tentity = importEntity.resolveAsType();
            if (tentity != null) {
                pcuNode.getImports().addStaticWildcardImport(tentity, importToken, semiColonToken);
            }
        }
    }
    
    @Override
    protected void gotDeclBegin(LocatableToken token)
    {
        // This notifies us of the beginning of some sort of declaration, but we're not sure
        // of the declaration type yet. We'll add in a placeholder node so that any child
        // nodes (eg parts of annotations) have somewhere to go. We'll remove the placeholder
        // node when we find out what sort of node we *really* want.
        pcuStmtBegin = token;
        DeclarationNode placeHolder = new DeclarationNode(scopeStack.peek());
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getLine(), token.getColumn());
        // Note, we don't want to soak up comments yet, so we don't call beginNode(...) here.
        scopeStack.peek().insertNode(placeHolder, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(placeHolder);
    }
    
    @Override
    protected void endDecl(LocatableToken token)
    {
        // Failed declaration; just throw away the node
        scopeStack.pop().remove();
    }
    
    @Override
    protected void gotTypeDef(LocatableToken firstToken, int tdType)
    {
        endDecl(firstToken); // remove placeholder
        Reflective ref = currentQuerySource();
        String prefix;
        if (ref != null) {
            prefix = ref.getName() + '$';
        }
        else {
            prefix = (declaredPkg.length() == 0) ? "" : (declaredPkg + ".");
        }
        
        innermostType = new ParsedTypeNode(scopeStack.peek(), innermostType, tdType, prefix, currentModifiers);
        int curOffset = getTopNodeOffset();
        LocatableToken hidden = firstToken.getHiddenBefore();
        if (hidden != null && hidden.getType() == JavaTokenTypes.ML_COMMENT) {
            firstToken = hidden;
            innermostType.setCommentAttached(true);
        }
        int insPos = lineColToPosition(firstToken.getLine(), firstToken.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(innermostType, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(innermostType);
        initializeTypeExtras();
    }
    
    /**
     * Initialize the lists for holding type parameters, supertypes, etc.
     */
    public final void initializeTypeExtras()
    {
        typeParams = new LinkedList<TypeParam>();
        extendedTypes = new LinkedList<JavaEntity>();
        implementedTypes = new LinkedList<JavaEntity>();
    }
    
    @Override
    protected void gotMethodTypeParamsBegin()
    {
        typeParams = new LinkedList<TypeParam>();
    }
    
    @Override
    protected void gotTypeDefName(LocatableToken nameToken)
    {
        ParsedTypeNode tnode = (ParsedTypeNode) scopeStack.peek();
        tnode.setName(nameToken.getText());
    }
    
    @Override
    protected void gotTypeParam(LocatableToken idToken)
    {
        if (lastTypeParamName != null) {
            typeParams.add(new TypeParam(lastTypeParamName, lastTypeParBounds));
        }
        lastTypeParamName = idToken.getText();
        lastTypeParBounds = new ArrayList<List<LocatableToken>>();
    }
    
    @Override
    protected void gotTypeParamBound(List<LocatableToken> tokens)
    {
        lastTypeParBounds.add(tokens);
        typeParams.add(new TypeParam(lastTypeParamName, lastTypeParBounds));
    }
    
    /**
     * Get a list of the recently processed type parameters as a list of TparEntity.
     * The given resolver must be able to resolve the type parameter names
     * themselves before the returned type parameter entities are resolved (because
     * type parameters may have other type parameters as bounds).
     */
    public final List<TparEntity> getTparList(EntityResolver resolver)
    {
        if (typeParams == null) {
            return null;
        }
        
        if (lastTypeParamName != null) {
            typeParams.add(new TypeParam(lastTypeParamName, lastTypeParBounds));
            lastTypeParamName = null;
        }
        
        Reflective querySource = currentQuerySource();
        List<TparEntity> rlist = new ArrayList<TparEntity>(typeParams.size());
        for (TypeParam tpar : typeParams) {
            List<JavaEntity> bounds = new ArrayList<JavaEntity>(tpar.bounds.size());
            for (List<LocatableToken> boundTokens : tpar.bounds) {
                bounds.add(ParseUtils.getTypeEntity(resolver, querySource, boundTokens));
            }
            JavaEntity boundsEnt = IntersectionTypeEntity.getIntersectionEntity(bounds, scopeStack.peek());
            rlist.add(new TparEntity(tpar.name, boundsEnt));
        }
        
        return rlist;
    }
    
    @Override
    protected void beginTypeBody(LocatableToken token)
    {
        ParsedTypeNode top = (ParsedTypeNode) scopeStack.peek();
        top.setTypeParams(getTparList(top));
        top.setExtendedTypes(extendedTypes);
        top.setImplementedTypes(implementedTypes);
        gotExtends = false;
        gotImplements = false;
        
        TypeInnerNode bodyNode = new TypeInnerNode(scopeStack.peek());
        bodyNode.setInner(true);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getEndLine(), token.getEndColumn());
        beginNode(insPos);
        top.insertInner(bodyNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(bodyNode);
    }
    
    @Override
    protected void beginForLoop(LocatableToken token)
    {
        JavaParentNode loopNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_ITERATION);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getLine(), token.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(loopNode);
    }
    
    @Override
    protected void beginForLoopBody(LocatableToken token)
    {
        // If the token is an LCURLY, it will be seen as a compound statement and scope
        // handling is done by beginStmtBlockBody
        if (token.getType() != JavaTokenTypes.LCURLY) {
            JavaParentNode loopNode = new InnerNode(scopeStack.peek());
            loopNode.setInner(true);
            int curOffset = getTopNodeOffset();
            int insPos = lineColToPosition(token.getLine(), token.getColumn());
            beginNode(insPos);
            scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
            scopeStack.push(loopNode);
        }
    }
    
    @Override
    protected void endForLoopBody(LocatableToken token, boolean included)
    {
        if (scopeStack.peek().getNodeType() != ParsedNode.NODETYPE_ITERATION) {
            endTopNode(token, included);
        }
    }
    
    @Override
    protected void beginWhileLoop(LocatableToken token)
    {
        JavaParentNode loopNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_ITERATION);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getLine(), token.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(loopNode);
    }
    
    @Override
    protected void beginWhileLoopBody(LocatableToken token)
    {
        // If the token is an LCURLY, it will be seen as a compound statement and scope
        // handling is done by beginStmtBlockBody
        if (token.getType() != JavaTokenTypes.LCURLY) {
            JavaParentNode loopNode = new InnerNode(scopeStack.peek());
            loopNode.setInner(true);
            int curOffset = getTopNodeOffset();
            int insPos = lineColToPosition(token.getLine(), token.getColumn());
            beginNode(insPos);
            scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
            scopeStack.push(loopNode);
        }
    }
    
    @Override
    protected void endWhileLoopBody(LocatableToken token, boolean included)
    {
        if (scopeStack.peek().getNodeType() != ParsedNode.NODETYPE_ITERATION) {
            endTopNode(token, included);
        }
    }
    
    @Override
    protected void beginDoWhile(LocatableToken token)
    {
        JavaParentNode loopNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_ITERATION);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getLine(), token.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(loopNode);
    }
    
    @Override
    protected void beginDoWhileBody(LocatableToken token)
    {
        // If the token is an LCURLY, it will be seen as a compound statement and scope
        // handling is done by beginStmtBlockBody
        if (token.getType() != JavaTokenTypes.LCURLY) {
            JavaParentNode loopNode = new InnerNode(scopeStack.peek());
            loopNode.setInner(true);
            int curOffset = getTopNodeOffset();
            int insPos = lineColToPosition(token.getLine(), token.getColumn());
            beginNode(insPos);
            scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
            scopeStack.push(loopNode);
        }
    }
    
    @Override
    protected void endDoWhileBody(LocatableToken token, boolean included)
    {
        if (scopeStack.peek().getNodeType() != ParsedNode.NODETYPE_ITERATION) {
            endTopNode(token, included);
        }
    }
        
    @Override
    protected void beginIfStmt(LocatableToken token)
    {
        JavaParentNode loopNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_SELECTION);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getLine(), token.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(loopNode);
    }
    
    @Override
    protected void beginIfCondBlock(LocatableToken token)
    {
        // If the token is an LCURLY, it will be seen as a compound statement and scope
        // handling is done by beginStmtBlockBody
        if (token.getType() != JavaTokenTypes.LCURLY) {
            JavaParentNode loopNode = new InnerNode(scopeStack.peek());
            loopNode.setInner(true);
            int curOffset = getTopNodeOffset();
            int insPos = lineColToPosition(token.getLine(), token.getColumn());
            beginNode(insPos);
            scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
            scopeStack.push(loopNode);
        }
    }
    
    @Override
    protected void endIfCondBlock(LocatableToken token, boolean included)
    {
        // If the inner block is a statement block delimited by curlies ie '{' and '}', it's already
        // been closed. In that case the scopestack top is the outer 'if' node, which we don't want
        // to close.
        if (scopeStack.peek().getNodeType() != ParsedNode.NODETYPE_SELECTION) {
            // If the stack top is *not* the outer 'if' node, we can close it.
            endTopNode(token, included);
        }
    }
    
    @Override
    protected void endIfStmt(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
    }

    @Override
    protected void beginSwitchStmt(LocatableToken token)
    {
        beginIfStmt(token);
    }
    
    @Override
    protected void beginSwitchBlock(LocatableToken token)
    {
        JavaParentNode loopNode = new InnerNode(scopeStack.peek());
        loopNode.setInner(true);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getEndLine(), token.getEndColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(loopNode);
    }
    
    @Override
    protected void endSwitchBlock(LocatableToken token)
    {
        endTopNode(token, false);
    }
    
    @Override
    protected void endSwitchStmt(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
    }
    
    @Override
    protected void beginTryCatchSmt(LocatableToken token, boolean hasResource)
    {
        JavaParentNode tryNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_SELECTION);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getLine(), token.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(tryNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(tryNode);
    }
    
    @Override
    protected void beginTryBlock(LocatableToken token)
    {
        JavaParentNode tryBlockNode = new InnerNode(scopeStack.peek());
        tryBlockNode.setInner(true);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getEndLine(), token.getEndColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(tryBlockNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(tryBlockNode);
    }
    
    @Override
    protected void endTryBlock(LocatableToken token, boolean included)
    {
        // Even if the end token is '}' we don't want it as part of the inner block,
        // so pass included=false.
        endTopNode(token, false);
    }
    
    @Override
    protected void endTryCatchStmt(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
    }
    
    @Override
    protected void beginStmtblockBody(LocatableToken token)
    {
        int curOffset = getTopNodeOffset();
        if (! scopeStack.peek().isContainer()) {
            // This is conditional, because the outer block may be a loop or selection
            // statement which already exists.
            JavaParentNode blockNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_NONE);
            blockNode.setInner(false);
            int insPos = lineColToPosition(token.getLine(), token.getColumn());
            beginNode(insPos);
            scopeStack.peek().insertNode(blockNode, insPos - curOffset, 0, nodeStructureListener);
            scopeStack.push(blockNode);
            curOffset = insPos;
        }
        JavaParentNode blockInner = new InnerNode(scopeStack.peek());
        blockInner.setInner(true);
        int insPos = lineColToPosition(token.getEndLine(), token.getEndColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(blockInner, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(blockInner);
    }
   
    @Override
    protected void endStmtblockBody(LocatableToken token, boolean included)
    {
        endTopNode(token, false); // Don't include the final curly as part of inner block
        if (scopeStack.peek().getNodeType() == ParsedNode.NODETYPE_NONE) {
            // This is a statement block that is not part of a loop or definition,
            // i.e. it is just a pair of curly braces appearing as a regular statement.
            // We need to close that statement now.
            endTopNode(token, included);
        }
    }
    
    @Override
    protected void beginSynchronizedBlock(LocatableToken token)
    {
        JavaParentNode tryNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_NONE);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getLine(), token.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(tryNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(tryNode);
    }
    
    @Override
    protected void beginInitBlock(LocatableToken first, LocatableToken lcurly)
    {
        endDecl(first); // remove placeholder node
        int curOffset = getTopNodeOffset();
        JavaParentNode blockNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_NONE);
        blockNode.setInner(false);
        int insPos = lineColToPosition(first.getLine(), first.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(blockNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(blockNode);
        curOffset = insPos;

        JavaParentNode blockInner = new InnerNode(scopeStack.peek());
        blockInner.setInner(true);
        insPos = lineColToPosition(lcurly.getEndLine(), lcurly.getEndColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(blockInner, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(blockInner);
    }
    
    @Override
    protected void endInitBlock(LocatableToken rcurly, boolean included)
    {
        endStmtblockBody(rcurly, included);
    }
    
    @Override
    protected void beginElement(LocatableToken token)
    {
        pcuStmtBegin = token;
    }
    
    @Override
    protected void endTypeBody(LocatableToken token, boolean included)
    {
        endTopNode(token, false); // Don't include the final curly as part of inner block
    }
    
    @Override
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
        innermostType = innermostType.getContainingClass();
        gotExtends = false;
        gotImplements = false;
    }
    
    @Override
    protected void endForLoop(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
    }
    
    @Override
    protected void endWhileLoop(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
    }
    
    @Override
    protected void endDoWhile(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
    }

    /*
     * We have the end of a package statement.
     */
    @Override
    protected void gotPackageSemi(LocatableToken token)
    {
        Selection s = new Selection(pcuStmtBegin.getLine(), pcuStmtBegin.getColumn());
        s.extendEnd(token.getLine(), token.getColumn() + token.getLength());
        
        int startpos = lineColToPosition(s.getLine(), s.getColumn());
        int endpos = lineColToPosition(s.getEndLine(), s.getEndColumn());
        
        PkgStmtNode psn = new PkgStmtNode(pcuNode);
        beginNode(startpos);
        pcuNode.insertNode(psn, startpos, endpos - startpos, nodeStructureListener);
        completedNode(psn, startpos, endpos - startpos);
    }
    
    @Override
    protected void gotImportStmtSemi(LocatableToken token)
    {
        Selection s = new Selection(pcuStmtBegin.getLine(), pcuStmtBegin.getColumn());
        s.extendEnd(token.getLine(), token.getColumn() + token.getLength());
        
        int startpos = lineColToPosition(s.getLine(), s.getColumn());
        int endpos = lineColToPosition(s.getEndLine(), s.getEndColumn());
        
        ParentParsedNode cn = new ImportNode(pcuNode);
        cn.setComplete(true);
        beginNode(startpos);
        pcuNode.insertNode(cn, startpos, endpos - startpos, nodeStructureListener);
        completedNode(cn, startpos, endpos - startpos);
    }
    
    @Override
    public void gotComment(LocatableToken token)
    {
        commentQueue.add(token);
    }
    
    @Override
    protected void gotConstructorDecl(LocatableToken token,
                                      LocatableToken hiddenToken)
    {
        endDecl(token); // remove placeholder
        LocatableToken start = pcuStmtBegin;
        String jdcomment = null;
        if (hiddenToken != null) {
            start = hiddenToken;
            jdcomment = hiddenToken.getText();
        }
        
        MethodNode pnode = new MethodNode(scopeStack.peek(), token.getText(), jdcomment);
        pnode.setModifiers(currentModifiers);
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(start.getLine(), start.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(pnode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(pnode);
    }
    
    @Override
    protected void gotMethodDeclaration(LocatableToken token,
                                        LocatableToken hiddenToken)
    {
        endDecl(token); // remove placeholder
        LocatableToken start = pcuStmtBegin;
        String jdcomment = null;
        if (hiddenToken != null) {
            start = hiddenToken;
            jdcomment = hiddenToken.getText();
        }
        
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(start.getLine(), start.getColumn());

        MethodNode pnode = new MethodNode(scopeStack.peek(), token.getText(), jdcomment);
        JavaEntity returnType = ParseUtils.getTypeEntity(pnode, currentQuerySource(), lastTypeSpec);
        pnode.setReturnType(returnType);
        pnode.setModifiers(currentModifiers);
        pnode.setTypeParams(getTparList(pnode));
        typeParams = null;
        
        beginNode(insPos);
        scopeStack.peek().insertNode(pnode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(pnode);
    }
    
    @Override
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken)
    {
        if (lastTypeSpec == null) return;
        JavaEntity paramType = ParseUtils.getTypeEntity(scopeStack.peek(),
                currentQuerySource(), lastTypeSpec);
        if (paramType == null) {
            return;
        }
        while (arrayDecls-- > 0) {
            paramType = new UnresolvedArray(paramType);
        }
        MethodNode mNode = (MethodNode) scopeStack.peek();
        if (ellipsisToken != null) {
            mNode.setVarArgs(true);
            paramType = new UnresolvedArray(paramType);            
        }
        mNode.addParameter(token.getText(), paramType);
    }
    
    @Override
    protected void endMethodDecl(LocatableToken token, boolean included)
    {
        MethodNode mNode = (MethodNode) scopeStack.peek();
        mNode.setComplete(included);
        endTopNode(token, included);
        TypeInnerNode topNode = (TypeInnerNode) scopeStack.peek();
        topNode.methodAdded(mNode);
    }
    
    @Override
    protected void beginMethodBody(LocatableToken token)
    {
        JavaParentNode pnode = new MethodBodyNode(scopeStack.peek());
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(token.getEndLine(), token.getEndColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(pnode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(pnode);
    }
    
    @Override
    protected void endMethodBody(LocatableToken token, boolean included)
    {
        scopeStack.peek().setComplete(included);
        endTopNode(token, false); // Don't include the final curly as part of inner block
    }
    
    @Override
    protected void gotExprNew(LocatableToken token)
    {
        gotNewType = false;
    }
    
    @Override
    protected void endExprNew(LocatableToken token, boolean included)
    {
        if (gotNewType) {
            // We have a type on the top of the stack
            newTypes.pop();
        }
        gotNewType = true; // outer "new" has type
    }
    
    @Override
    protected void gotTypeSpec(List<LocatableToken> tokens)
    {
        if (gotExtends) {
            JavaEntity supert = ParseUtils.getTypeEntity(scopeStack.peek(), currentQuerySource(), tokens);
            if (supert != null) {
                extendedTypes.add(supert);
            }
        }
        else if (gotImplements) {
            JavaEntity supert = ParseUtils.getTypeEntity(scopeStack.peek(), currentQuerySource(), tokens);
            if (supert != null) {
                implementedTypes.add(supert);
            }
        }
        else if (! gotNewType) {
            gotNewType = true;
            newTypes.push(tokens);
        }
        else {
            lastTypeSpec = tokens;
            arrayDecls = 0;
        }
    }
    
    @Override
    protected void gotArrayDeclarator()
    {
        arrayDecls++;
    }
        
    @Override
    protected void beginFieldDeclarations(LocatableToken first)
    {
        arrayDecls = 0;
        endDecl(first); // remove placeholder
    }
    
    /**
     * Saw a field or a variable declaration. This may be part of multiple declarations
     * (eg  "int a, b, c = 3;") and an initialisation expression may follow.
     * 
     * @param first       First token of the declaration (part of the type).
     * @param idToken     The token with the identifier (field/variable name)
     * @param initExpressionFollows   Whether an initialisation is present
     * @param isVariable  Whether this is a variable rather than a field.
     */
    private void gotFieldOrVar(LocatableToken first, LocatableToken idToken,
            boolean initExpressionFollows, boolean isVariable)
    {
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(first.getLine(), first.getColumn());
        EntityResolver resolver = new PositionedResolver(scopeStack.peek(), insPos - curOffset);
        
        boolean declaredVar = isVariable && initExpressionFollows && typeSpecIsVar(lastTypeSpec);
        
        JavaEntity fieldType;
        if (declaredVar)
        {
            fieldType = null; // we will infer the type from the expression
            lastField = new FieldNode(scopeStack.peek(), idToken.getText(), arrayDecls,
                    currentModifiers, document);
        }
        else
        {
            fieldType = ParseUtils.getTypeEntity(resolver, currentQuerySource(), lastTypeSpec);
            lastField = new FieldNode(scopeStack.peek(), idToken.getText(), fieldType,
                    arrayDecls, currentModifiers);
        }
        
        arrayDecls = 0;
        beginNode(insPos);
        
        JavaParentNode top = scopeStack.peek();
        top.insertField(lastField, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(lastField);
    }
    
    @Override
    protected void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows)
    {
        gotFieldOrVar(first, idToken, initExpressionFollows, false);
    }
    
    @Override
    protected void gotSubsequentField(LocatableToken first,
                                      LocatableToken idToken, boolean initFollows)
    {
        FieldNode field = new FieldNode(scopeStack.peek(), idToken.getText(), lastField, arrayDecls);
        arrayDecls = 0;
        int curOffset = getTopNodeOffset();
        int insPos = lineColToPosition(first.getLine(), first.getEndColumn());
        beginNode(insPos);
        
        if (lastField.getFieldType() != null) {
            JavaParentNode top = scopeStack.peek();
            top.insertField(field, insPos - curOffset, 0, nodeStructureListener);
        }
        else {
            scopeStack.peek().insertNode(field, insPos - curOffset, 0, nodeStructureListener);
        }
        
        scopeStack.push(field);
    }
    
    @Override
    protected void endField(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
    }
    
    // Variables can be treated exactly like fields:
    
    @Override
    protected void beginVariableDecl(LocatableToken first)
    {
        beginFieldDeclarations(first);
    }
    
    @Override
    protected void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited)
    {
        gotFieldOrVar(first, idToken, inited, true);
    }
    
    @Override
    protected void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited)
    {
        gotSubsequentField(first, idToken, inited);
    }
    
    @Override
    protected void endVariable(LocatableToken token, boolean included)
    {
        endField(token, included);
    }
    
    // For-initializers are like variables/fields
    
    @Override
    protected void beginForInitDecl(LocatableToken first)
    {
        arrayDecls = 0;
    }
    
    @Override
    protected void gotForInit(LocatableToken first, LocatableToken idToken)
    {
        gotVariableDecl(first, idToken, true);
    }
    
    @Override
    protected void gotSubsequentForInit(LocatableToken first,
                                        LocatableToken idToken, boolean initFollows)
    {
        gotSubsequentVar(first, idToken, true);
    }
    
    @Override
    protected void endForInit(LocatableToken token, boolean included)
    {
        endVariable(token, included);
    }
    
    @Override
    protected void beginAnonClassBody(LocatableToken token, boolean isEnumMember)
    {
        ParsedTypeNode pnode = new ParsedTypeNode(scopeStack.peek(), innermostType,
                JavaParser.TYPEDEF_CLASS, null, 0); // TODO generate Abc$1 ?
                
        innermostType = pnode;
        int curOffset = getTopNodeOffset();
        LocatableToken begin = token;
        int insPos = lineColToPosition(begin.getLine(), begin.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(pnode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(pnode);
        
        JavaEntity supert;
        if (! isEnumMember) {
            EntityResolver resolver = new PositionedResolver(scopeStack.peek(), insPos - curOffset);
            supert = ParseUtils.getTypeEntity(resolver, currentQuerySource(), newTypes.peek());
        }
        else {
            supert = new TypeEntity(new ParsedReflective(innermostType));
        }
        List<JavaEntity> superts = new ArrayList<JavaEntity>(1);
        superts.add(supert);
        pnode.setExtendedTypes(superts);
        
        TypeInnerNode bodyNode = new TypeInnerNode(scopeStack.peek());
        bodyNode.setInner(true);
        curOffset = getTopNodeOffset();
        insPos = lineColToPosition(token.getEndLine(), token.getEndColumn());
        beginNode(insPos);
        pnode.insertInner(bodyNode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(bodyNode);
    }
    
    @Override
    protected void endAnonClassBody(LocatableToken token, boolean included)
    {
        endTopNode(token, false);  // inner node
        endTopNode(token, included);  // outer node
        innermostType = innermostType.getContainingClass();
    }
    
    @Override
    protected void beginExpression(LocatableToken token)
    {
        ExpressionNode nnode = new ExpressionNode(scopeStack.peek());
        int curOffset = getTopNodeOffset();
        LocatableToken begin = token;
        int insPos = lineColToPosition(begin.getLine(), begin.getColumn());
        beginNode(insPos);
        scopeStack.peek().insertNode(nnode, insPos - curOffset, 0, nodeStructureListener);
        scopeStack.push(nnode);
    }
    
    @Override
    protected void endExpression(LocatableToken token, boolean isEmpty)
    {
        endTopNode(token, false);
        arrayDecls = 0;
    }

    @Override
    protected void beginTypeDefExtends(LocatableToken extendsToken)
    {
        gotExtends = true;
        gotImplements = false;
    }
    
    @Override
    protected void beginTypeDefImplements(LocatableToken implementsToken)
    {
        gotImplements = true;
        gotExtends = false;
    }
    
}
