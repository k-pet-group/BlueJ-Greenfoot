/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2014,2016,2017,2019,2021,2022  Michael Kolling and John Rosenberg
 
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
package bluej.parser.nodes;

import java.io.Reader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;
import bluej.parser.nodes.ReparseableDocument.Element;
import bluej.parser.Token;
import bluej.parser.Token.TokenType;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.JavaParser;
import bluej.parser.TokenStream;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.utility.GeneralCache;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A ParentParsedNode extension with Java specific functionality.
 * Amongst other things this extends the ParsedNode into an
 * EntityResolver implementation.
 * 
 * @author Davin McCall
 */
public abstract class JavaParentNode extends ParentParsedNode
    implements EntityResolver
{
    protected GeneralCache<String,JavaEntity> valueEntityCache =
        new GeneralCache<String,JavaEntity>(10);
    protected GeneralCache<String,PackageOrClass> pocEntityCache =
        new GeneralCache<String,PackageOrClass>(10);

    protected JavaParentNode parentNode;
    
    protected Map<String,ParsedNode> classNodes = new HashMap<>();
    protected Map<String,Set<VariableDeclaration>> variables = new HashMap<>();
    
    // This flag is specifically used for handling the indentation of switches statements:
    // the scope in switch statements are for the whole switch, so we cannot
    // add a new node in the scope stack when a statement label (case/default) is found. 
    // Therefore, everything in a switch block node are statements, and the auto-indentation 
    // works it out to check if we're in a switch block and do the proper indentation.
    private boolean isSwitchBlockNode = false;

    public JavaParentNode(JavaParentNode parent)
    {
        super(parent);
        parentNode = parent;
    }
    
    @Override
    public JavaParentNode getParentNode()
    {
        return parentNode;
    }
    
    @Override
    public void insertNode(ParsedNode child, int position, int size, NodeStructureListener nodeStructureListener)
    {
        super.insertNode(child, position, size, nodeStructureListener);
        int childType = child.getNodeType();
        String childName = child.getName();
        if (childName != null) {
            if (childType == NODETYPE_TYPEDEF) {
                classNodes.put(childName, child);
            }
        }
    }
    
    /**
     * Insert a FieldNode representing a variable/field declaration into this node.
     */
    public void insertField(FieldNode child, int position, int size, NodeStructureListener nodeStructureListener)
    {
        super.insertNode(child, position, size, nodeStructureListener);

        Set<VariableDeclaration> varList = variables.get(child.getName());
        if (varList == null) {
            varList = new HashSet<VariableDeclaration>(1);
            variables.put(child.getName(), varList);
        }

        varList.add(child);
    }

    /**
     * Adds a VariableDeclaration that represents an instanceof variable to
     * this node's set of variables
     */
    public void insertInstanceofVar(VariableDeclaration variableDeclaration)
    {
        variables.computeIfAbsent(variableDeclaration.getName(), s -> new HashSet<>(1)).add(variableDeclaration);
    }
    
    @Override
    public void childChangedName(ParsedNode child, String oldName)
    {
        super.childChangedName(child, oldName);
        if (child.getNodeType() == NODETYPE_TYPEDEF) {
            if (classNodes.get(oldName) == child) {
                classNodes.remove(oldName);
            }
            classNodes.put(child.getName(), child);
        }
        if (child.getNodeType() == NODETYPE_FIELD) {
            Set<VariableDeclaration> varset = variables.get(oldName);
            if (varset != null) {
                varset.remove(child);
                if (varset.isEmpty()) {
                    variables.remove(oldName);
                }
            }
            varset = variables.get(child.getName());
            if (varset == null) {
                varset = new HashSet<>();
                variables.put(child.getName(), varset);
            }
            varset.add((FieldNode) child);
        }
    }

    @Override
    protected void childRemoved(NodeAndPosition<ParsedNode> child,
            NodeStructureListener listener)
    {
        super.childRemoved(child, listener);
        String childName = child.getNode().getName();
        if (childName != null) {
            if (classNodes.get(childName) == child.getNode()) {
                classNodes.remove(childName);
            }
            Set<VariableDeclaration> varset = variables.get(childName);
            if (varset != null) {
                varset.remove(child.getNode());
                if (varset.isEmpty())
                {
                    variables.remove(childName);
                }
            }
        }
    }
    
    /**
     * Find a type node for a type definition with the given name.
     */
    public ParsedNode getTypeNode(String name)
    {
        return classNodes.get(name);
    }
    
    // Sets the flag indicating that the node is a Switch Block to true.
    public void  markAsSwitchBlockNode()
    {
        isSwitchBlockNode = true;
    }

    // Gets the flag indicating if the node is a Switch Label Block 
    public boolean isSwitchBlockNode()
    {
        return isSwitchBlockNode;
    }    

    // =================== EntityResolver interface ====================
    
    /*
     * @see bluej.parser.entity.EntityResolver#resolveQualifiedClass(java.lang.String)
     */
    public TypeEntity resolveQualifiedClass(String name)
    {
        if (parentNode != null) {
            return parentNode.resolveQualifiedClass(name);
        }
        return null;
    }
    
    /*
     * @see bluej.parser.entity.EntityResolver#resolvePackageOrClass(java.lang.String, java.lang.String)
     */
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
    {
        ParsedNode cnode = classNodes.get(name);
        if (cnode != null) {
            return new TypeEntity(new ParsedReflective((ParsedTypeNode) cnode));
        }
        
        String accessp = name + ":" + (querySource != null ? querySource.getName() : ""); 
        PackageOrClass rval = pocEntityCache.get(accessp);
        if (rval != null || pocEntityCache.containsKey(accessp)) {
            return rval;
        }
        
        if (parentNode != null) {
            rval = parentNode.resolvePackageOrClass(name, querySource);
            pocEntityCache.put(accessp, rval);
        }
        return rval;
    }
    
    /**
     * Resolve a package or type, based on what is visible from the given position in the node.
     * This allows for forward declarations not being visible.
     */
    @OnThread(Tag.FXPlatform)
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource, int fromPosition)
    {
        return resolvePackageOrClass(name, querySource);
    }
    
    /*
     * @see bluej.parser.entity.EntityResolver#getValueEntity(java.lang.String, java.lang.String)
     */
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        Set<VariableDeclaration> varset = variables.get(name);
        if (varset != null && ! varset.isEmpty()) {
            VariableDeclaration var = varset.iterator().next();
            JavaEntity fieldType = var.getFieldType().resolveAsType();
            if (fieldType != null) {
                return new ValueEntity(fieldType.getType());
            }
        }
        
        String accessp = name + ":" + (querySource != null ? querySource.getName() : ""); 
        JavaEntity rval = valueEntityCache.get(accessp);
        if (rval != null || valueEntityCache.containsKey(accessp)) {
            return rval;
        }
        
        if (parentNode != null) {
            rval = parentNode.getValueEntity(name, querySource, getOffsetFromParent());
        }
        
        if (rval == null) {
            rval = resolvePackageOrClass(name, querySource, getOffsetFromParent());
        }
        
        valueEntityCache.put(accessp, rval);
        return rval;
    }
    
    /**
     * Resolve a value, based on what is visible from a given position within the node.
     * This allows for forward declarations not being visible.
     */
    @OnThread(Tag.FXPlatform)
    public JavaEntity getValueEntity(String name, Reflective querySource, int fromPosition)
    {
        return getValueEntity(name, querySource);
    }
    
    /**
     * Resolve a value, based on what is visible from a given position within the node,
     * not allowing for forward declarations.
     * 
     * @param name    reference symbol
     * @param querySource  container of reference symbol for resolution purpose
     * @param fromPosition   position within the node to resolve at
     * @return    A JavaEntity reflecting the result of the resolution
     */
    @OnThread(Tag.FXPlatform)
    protected JavaEntity getPositionedValueEntity(String name, Reflective querySource, int fromPosition)
    {
        Set<VariableDeclaration> varset = variables.get(name);
        if (varset != null) {
            // We need to go through them in order from last position
            // to first position, as the later variables (especially from
            // things like instanceof variables) override the earlier ones
            // Negate the position to sort descending:
            List<VariableDeclaration> sortedVars = varset.stream().sorted(Comparator.comparingInt(v -> -v.getOffsetFromParent())).collect(Collectors.toList());
            for (VariableDeclaration var : sortedVars)
            {
                if (var.getOffsetFromParent() <= fromPosition) {
                    JavaEntity fieldType = var.getFieldType().resolveAsType();
                    if (fieldType != null) {
                        return new ValueEntity(fieldType.getType());
                    }
                }
            }
        }
        
        JavaEntity rval = null;
        if (parentNode != null) {
            rval = parentNode.getValueEntity(name, querySource, getOffsetFromParent());
        }
        
        if (rval == null) {
            rval = resolvePackageOrClass(name, querySource, fromPosition);
        }
        
        return rval;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    protected ExpressionTypeInfo getExpressionType(int pos, int nodePos, JavaEntity defaultType, ReparseableDocument document, ExpressionNode largestPlainExpressionNode)
    {
        // Clear the caches now to remove any entries which have become invalid due
        // to editing.
        valueEntityCache.clear();
        pocEntityCache.clear();
        
        NodeAndPosition<ParsedNode> child = getNodeTree().findNodeAtOrBefore(pos, nodePos);
        if (child != null && child.getEnd() >= pos) {
            return child.getNode().getExpressionType(pos, child.getPosition(), defaultType, document, null);
        }
        
        int startpos = nodePos;
        if (child != null) {
            startpos = child.getEnd();
        }
        
        // We want to find the relevant suggestion token.
        
        Element map = document.getDefaultRootElement();
        int line = map.getElementIndex(pos) + 1;
        Element lineEl = map.getElement(line - 1);
        startpos = Math.max(startpos, lineEl.getStartOffset());
        int col = startpos - map.getElement(line - 1).getStartOffset() + 1;
        Reader r = document.makeReader(startpos, pos);
        
        JavaLexer lexer = new JavaLexer(r, line, col, startpos);
        JavaTokenFilter filter = new JavaTokenFilter(lexer);
        LocatableToken token = filter.nextToken();
        LocatableToken prevToken = null;
        while (token.getType() != JavaTokenTypes.EOF) {
            prevToken = token;
            token = filter.nextToken();
        }
        
        if (prevToken != null && prevToken.getEndLine() != token.getEndLine()) {
            if (prevToken.getEndColumn() != token.getEndColumn()) {
                // If the token doesn't end right at the completion point, it's not used.
                prevToken = null;
            }
        }
        
        if (prevToken != null && startpos == nodePos) {
            // The completion position is at the end of some token.
            // The parent might have a prior expression sibling which might end in a dot.
            if (parentNode != null) {
                int offset = getOffsetFromParent();
                int ppos = nodePos - offset;
                child = parentNode.getNodeTree().findNodeAtOrBefore(nodePos - 1, ppos);
                if (child != null && child.getNode().getNodeType() == ParsedNode.NODETYPE_EXPRESSION
                        && child.getEnd() == nodePos) {
                    ExpressionTypeInfo suggests = ExpressionNode.suggestAsExpression(pos, child.getPosition(),
                            this, defaultType, document);
                    if (suggests != null) {
                        return suggests;
                    }
                }
            }
        }
        
        // No identifiable expression. The suggestion type is the enclosing type.
        
        GenTypeClass atype = (defaultType != null) ? defaultType.getType().asClass() : null;
        if (atype == null) {
            return null;
        }
        boolean isStaticCtxt = (defaultType.resolveAsType() != null);
        if (prevToken != null && ! Character.isJavaIdentifierPart(prevToken.getText().codePointAt(0))) {
            prevToken = null;
        }
        return new ExpressionTypeInfo(atype, atype, prevToken, isStaticCtxt, true);
    }

    @Override
    public TokenAndScope getMarkTokensFor(final int pos, final int length, final int nodePos,
            final ReparseableDocument document)
    {
        Token tok = new Token(0, TokenType.END); // dummy
        if (length == 0) {
            return new TokenAndScope(tok, pos);
        }
        Token dummyTok = tok;
        
        NodeAndPosition<ParsedNode> np = getNodeTree().findNodeAtOrAfter(pos, nodePos);
        while (np != null && np.getEnd() == pos) np = np.nextSibling(); 
        
        int startLatestNode = 0;
        int cp = pos;
        while (np != null && np.getPosition() < (pos + length)) {
            if (np.getPosition() <= pos)
                startLatestNode = Math.max(startLatestNode, np.getPosition());
            
            if (cp < np.getPosition()) {
                int nextTokLen = np.getPosition() - cp;
                tok.next = tokenizeText(document, cp, nextTokLen);
                while (tok.next.id != TokenType.END) tok = tok.next;
                cp = np.getPosition();
            }
            
            int remaining = pos + length - cp;
            remaining = Math.min(remaining, np.getEnd() - cp);
            
            if (remaining != 0) {
                TokenAndScope tas = np.getNode().getMarkTokensFor(cp, remaining, np.getPosition(), document);
                if (tas.startLatestScope() <= pos)
                    startLatestNode = Math.max(startLatestNode, tas.startLatestScope());
                tok.next = tas.tokenLinkedList();
                cp += remaining;
                while (tok.next.id != TokenType.END) {
                    tok = tok.next;
                }
            }
            np = np.nextSibling();
        }
        
        // There may be a section left
        if (cp < pos + length) {
            int nextTokLen = pos + length - cp;
            tok.next = tokenizeText(document, cp, nextTokLen);
            while (tok.next.id != TokenType.END) tok = tok.next;
        }

        tok.next = new Token(0, TokenType.END);
        return new TokenAndScope(dummyTok.next, startLatestNode);
    }
    
    protected static Token tokenizeText(ReparseableDocument document, int pos, int length)
    {
        Reader dr = document.makeReader(pos, pos+length);
        TokenStream lexer = JavaParser.getLexer(dr, true, false);
        TokenStream tokenStream = new JavaTokenFilter(lexer, null);

        Token dummyTok = new Token(0, TokenType.END);
        Token token = dummyTok;

        boolean lastWasWildcard = false;
        int curcol = 1;
        while (length > 0) {
            LocatableToken lt = (LocatableToken) tokenStream.nextToken();

            if (lt.getLine() > 1 || lt.getColumn() - curcol >= length) {
                token.next = new Token(length, TokenType.DEFAULT);
                token = token.next;
                break;
            }
            if (lt.getColumn() > curcol) {
                // some space before the token
                token.next = new Token(lt.getColumn() - curcol, TokenType.DEFAULT);
                token = token.next;
                length -= token.length;
                curcol += token.length;
            }

            TokenType tokType = TokenType.DEFAULT;
            if (JavaParser.isPrimitiveType(lt)) {
                tokType = TokenType.PRIMITIVE;
            }
            else if (JavaParser.isModifier(lt)) {
                tokType = TokenType.KEYWORD1;
            }
            else if (lt.getType() == JavaTokenTypes.STRING_LITERAL || lt.getType() == JavaTokenTypes.STRING_LITERAL_MULTILINE) {
                tokType = TokenType.STRING_LITERAL;
            }
            else if (lt.getType() == JavaTokenTypes.CHAR_LITERAL) {
                tokType = TokenType.CHAR_LITERAL;
            }
            else {
                switch (lt.getType()) {
                case JavaTokenTypes.LITERAL_assert:
                case JavaTokenTypes.LITERAL_for:
                case JavaTokenTypes.LITERAL_switch:
                case JavaTokenTypes.LITERAL_while:
                case JavaTokenTypes.LITERAL_do:
                case JavaTokenTypes.LITERAL_try:
                case JavaTokenTypes.LITERAL_catch:
                case JavaTokenTypes.LITERAL_throw:
                case JavaTokenTypes.LITERAL_throws:
                case JavaTokenTypes.LITERAL_finally:
                case JavaTokenTypes.LITERAL_return:
                case JavaTokenTypes.LITERAL_case:
                case JavaTokenTypes.LITERAL_default:
                case JavaTokenTypes.LITERAL_break:
                case JavaTokenTypes.LITERAL_continue:
                case JavaTokenTypes.LITERAL_if:
                case JavaTokenTypes.LITERAL_else:
                case JavaTokenTypes.LITERAL_new:
                case JavaTokenTypes.LITERAL_yield:
                    tokType = TokenType.KEYWORD1;
                    break;

                case JavaTokenTypes.LITERAL_class:
                case JavaTokenTypes.LITERAL_package:
                case JavaTokenTypes.LITERAL_import:
                case JavaTokenTypes.LITERAL_extends:
                case JavaTokenTypes.LITERAL_interface:
                case JavaTokenTypes.LITERAL_enum:
                case JavaTokenTypes.LITERAL_implements:
                    tokType = TokenType.KEYWORD2;
                    break;

                case JavaTokenTypes.LITERAL_super:
                    if (lastWasWildcard)
                        tokType = TokenType.KEYWORD2;
                    else
                        tokType = TokenType.KEYWORD3;
                    break;
                case JavaTokenTypes.LITERAL_this:
                case JavaTokenTypes.LITERAL_null:

                case JavaTokenTypes.LITERAL_true:
                case JavaTokenTypes.LITERAL_false:
                    tokType = TokenType.KEYWORD3;
                    break;
                
                case JavaTokenTypes.LITERAL_instanceof:
                    tokType = TokenType.OPERATOR;
                    break;

                default:
                }
            }
            lastWasWildcard = lt.getType() == JavaTokenTypes.QUESTION;
            int toklen = lt.getLength();
            if (lt.getEndLine() > 1) {
                toklen = length;
            }
            token.next = new Token(toklen, tokType);
            token = token.next;
            length -= toklen;
            curcol += toklen;
        }
        
        token.next = new Token(0, TokenType.END);
        return dummyTok.next;
    }
}
