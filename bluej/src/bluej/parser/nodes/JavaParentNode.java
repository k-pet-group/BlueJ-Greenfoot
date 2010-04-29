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

import bluej.debugger.gentype.Reflective;
import bluej.editor.moe.Token;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;
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
        if (child.getNodeType() == NODETYPE_TYPEDEF && child.getName() != null) {
            classNodes.put(child.getName(), child);
        }
    }
    
    public void childChangedName(ParsedNode child, String oldName)
    {
        if (child.getNodeType() == NODETYPE_TYPEDEF) {
            if (classNodes.get(oldName) == child) {
                classNodes.remove(oldName);
            }
            classNodes.put(child.getName(), child);
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
}
