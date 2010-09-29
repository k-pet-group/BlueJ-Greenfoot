/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.util.HashMap;
import java.util.Map;

import javax.swing.text.Document;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;
import bluej.editor.moe.Token;
import bluej.parser.CodeSuggestions;
import bluej.parser.DocumentReader;
import bluej.parser.JavaParser;
import bluej.parser.TokenStream;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.utility.GeneralCache;

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
    private GeneralCache<String,JavaEntity> valueEntityCache =
        new GeneralCache<String,JavaEntity>(10);
    private GeneralCache<String,PackageOrClass> pocEntityCache =
        new GeneralCache<String,PackageOrClass>(10);

    private JavaParentNode parentNode;
    
    private Map<String,ParsedNode> classNodes = new HashMap<String,ParsedNode>();
    protected Map<String,FieldNode> variables = new HashMap<String,FieldNode>();

    public JavaParentNode(JavaParentNode parent)
    {
        super(parent);
        parentNode = parent;
    }
    
    @Override
    protected JavaParentNode getParentNode()
    {
        return parentNode;
    }
    
    public void insertNode(ParsedNode child, int position, int size)
    {
        getNodeTree().insertNode(child, position, size);
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
    public void insertVariable(FieldNode varNode, int pos, int size)
    {
        super.insertNode(varNode, pos, size);
        if (! variables.containsKey(varNode.getName())) {
            variables.put(varNode.getName(), varNode);
        }
    }
    
    /**
     * Insert a field child (alias for insertVariable).
     */
    public void insertField(FieldNode child, int position, int size)
    {
        insertVariable(child, position, size);
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
            if (variables.get(oldName) == child) {
                variables.remove(oldName);
            }
            variables.put(child.getName(), (FieldNode) child);
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
            if (variables.get(childName) == child.getNode()) {
                variables.remove(childName);
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
    
    /*
     * @see bluej.parser.entity.EntityResolver#getValueEntity(java.lang.String, java.lang.String)
     */
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        FieldNode var = variables.get(name);
        if (var != null) {
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
            rval = parentNode.getValueEntity(name, querySource);
        }
        
        if (rval == null) {
            rval = resolvePackageOrClass(name, querySource);
        }
        
        valueEntityCache.put(accessp, rval);
        return rval;
    }
    
    @Override
    protected CodeSuggestions getExpressionType(int pos, int nodePos, JavaEntity defaultType, Document document)
    {
        // Clear the caches now to remove any entries which have become invalid due
        // to editing.
        valueEntityCache.clear();
        pocEntityCache.clear();
        
        NodeAndPosition<ParsedNode> child = getNodeTree().findNode(pos, nodePos);
        if (child != null) {
            return child.getNode().getExpressionType(pos, child.getPosition(), defaultType, document);
        }
        
        child = getNodeTree().findNodeAtOrBefore(pos, nodePos);
        if (child != null && child.getNode().getNodeType() == ParsedNode.NODETYPE_EXPRESSION) {
            // There's no child at the suggestion point, but, there is an expression immediately
            // prior. It's possible that it ends with a dot.
            CodeSuggestions suggests = ExpressionNode.suggestAsExpression(pos, child.getPosition(), this, defaultType, document);
            if (suggests != null && suggests.getSuggestionToken() != null) {
                return suggests;
            }
        }
        else if (child == null && parentNode != null) {
            // Likewise, if we have no prior sibling, perhaps the parent node does.
            int offset = getOffsetFromParent();
            int ppos = nodePos - offset;
            child = parentNode.getNodeTree().findNodeAtOrBefore(nodePos - 1, ppos);
            if (child != null && child.getNode().getNodeType() == ParsedNode.NODETYPE_EXPRESSION) {
                CodeSuggestions suggests = ExpressionNode.suggestAsExpression(pos, child.getPosition(), this, defaultType, document);
                if (suggests != null && suggests.getSuggestionToken() != null) {
                    return suggests;
                }
            }
        }
        
        GenTypeClass atype = (defaultType != null) ? defaultType.getType().asClass() : null;
        if (atype == null) {
            return null;
        }
        boolean isStaticCtxt = (defaultType.resolveAsType() != null);
        return new CodeSuggestions(atype, atype, null, isStaticCtxt);
    }

    @Override
    public Token getMarkTokensFor(int pos, int length, int nodePos,
            Document document)
    {
        Token tok = new Token(0, Token.END); // dummy
        if (length == 0) {
            return tok;
        }
        Token dummyTok = tok;
        
        NodeAndPosition<ParsedNode> np = getNodeTree().findNodeAtOrAfter(pos, nodePos);
        while (np != null && np.getEnd() == pos) np = np.nextSibling(); 
        
        int cp = pos;
        while (np != null && np.getPosition() < (pos + length)) {
            if (cp < np.getPosition()) {
                int nextTokLen = np.getPosition() - cp;
                tok.next = tokenizeText(document, cp, nextTokLen);
                while (tok.next.id != Token.END) tok = tok.next;
                cp = np.getPosition();
            }
            
            int remaining = pos + length - cp;
            remaining = Math.min(remaining, np.getEnd() - cp);
            
            if (remaining != 0) {
                tok.next = np.getNode().getMarkTokensFor(cp, remaining, np.getPosition(), document);
                cp += remaining;
                while (tok.next.id != Token.END) {
                    tok = tok.next;
                }
            }
            np = np.nextSibling();
        }
        
        // There may be a section left
        if (cp < pos + length) {
            int nextTokLen = pos + length - cp;
            tok.next = tokenizeText(document, cp, nextTokLen);
            while (tok.next.id != Token.END) tok = tok.next;
        }

        tok.next = new Token(0, Token.END);
        return dummyTok.next;
    }
    
    protected static Token tokenizeText(Document document, int pos, int length)
    {
        DocumentReader dr = new DocumentReader(document, pos, pos+length);
        TokenStream lexer = JavaParser.getLexer(dr,1,1);
        TokenStream tokenStream = new JavaTokenFilter(lexer, null);

        Token dummyTok = new Token(0, Token.END);
        Token token = dummyTok;
        
        int curcol = 1;
        while (length > 0) {
            LocatableToken lt = (LocatableToken) tokenStream.nextToken();

            if (lt.getLine() > 1 || lt.getColumn() - curcol >= length) {
                token.next = new Token(length, Token.NULL);
                token = token.next;
                break;
            }
            if (lt.getColumn() > curcol) {
                // some space before the token
                token.next = new Token(lt.getColumn() - curcol, Token.NULL);
                token = token.next;
                length -= token.length;
                curcol += token.length;
            }

            byte tokType = Token.NULL;
            if (JavaParser.isPrimitiveType(lt)) {
                tokType = Token.PRIMITIVE;
            }
            else if (JavaParser.isModifier(lt)) {
                tokType = Token.KEYWORD1;
            }
            else if (lt.getType() == JavaTokenTypes.STRING_LITERAL) {
                tokType = Token.LITERAL1;
            }
            else if (lt.getType() == JavaTokenTypes.CHAR_LITERAL) {
                tokType = Token.LITERAL2;
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
                    tokType = Token.KEYWORD1;
                    break;

                case JavaTokenTypes.LITERAL_class:
                case JavaTokenTypes.LITERAL_package:
                case JavaTokenTypes.LITERAL_import:
                case JavaTokenTypes.LITERAL_extends:
                case JavaTokenTypes.LITERAL_interface:
                case JavaTokenTypes.LITERAL_enum:
                case JavaTokenTypes.LITERAL_implements:
                    tokType = Token.KEYWORD2;
                    break;

                case JavaTokenTypes.LITERAL_this:
                case JavaTokenTypes.LITERAL_null:
                case JavaTokenTypes.LITERAL_super:
                case JavaTokenTypes.LITERAL_true:
                case JavaTokenTypes.LITERAL_false:
                    tokType = Token.KEYWORD3;
                    break;
                
                case JavaTokenTypes.LITERAL_instanceof:
                    tokType = Token.OPERATOR;
                    break;

                default:
                }
            }
            int toklen = lt.getLength();
            if (lt.getEndLine() > 1) {
                toklen = length;
            }
            token.next = new Token(toklen, tokType);
            token = token.next;
            length -= toklen;
            curcol += toklen;
        }
        
        token.next = new Token(0, Token.END);
        return dummyTok.next;
    }
}
