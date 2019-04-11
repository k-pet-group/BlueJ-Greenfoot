/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2019  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.JavaParser;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TparEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Collections;
import java.util.List;



/**
 * A node representing a parsed type (class, interface, enum)
 * 
 * @author Davin McCall
 */
public class ParsedTypeNode extends IncrementalParsingNode
{
    private String name;
    private String prefix;
    private TypeInnerNode inner;
    private List<TparEntity> typeParams;
    private List<JavaEntity> extendedTypes;
    private List<JavaEntity> implementedTypes;
    private int modifiers;
    private ParsedTypeNode containingClass;
    
    private int type; // one of JavaParser.TYPEDEF_CLASS, INTERFACE, ENUM, ANNOTATION
    
    /**
     * Construct a new ParsedTypeNode.
     * 
     * @param parent  The parent node
     * @param containingClass   The node representing the class containing this one
     * @param type    The type of this type: JavaParser.{TYPEDEF_CLASS,_INTERFACE,_ENUM or _ANNOTATION}
     * @param prefix  The prefix of the name, including the final ".", to make this a full
     *                type name
     * @param modifiers  The class modifiers (see java.lang.reflect.Modifier)
     */
    public ParsedTypeNode(JavaParentNode parent, ParsedTypeNode containingClass, int type, String prefix, int modifiers)
    {
        super(parent);
        stateMarkers = new int[2];
        marksEnd = new boolean[2];
        stateMarkers[0] = -1;
        stateMarkers[1] = -1;
        this.type = type;
        this.prefix = prefix;
        this.modifiers = modifiers;
        this.containingClass = containingClass;
        
        // Set defaults for various members
        typeParams = Collections.emptyList();
        extendedTypes = Collections.emptyList();
        implementedTypes = Collections.emptyList();
    }
    
    /**
     * Gets the kind of type which this node represents. Returns one of:
     * JavaParser.TYPEDEF_CLASS, _INTERFACE, _ENUM or _ANNOTATION
     */
    public int getTypeKind()
    {
        return type;
    }
    
    /**
     * Get the modifiers of the type this node represents (see java.lang.reflect.Modifier).
     */
    public int getModifiers()
    {
        return modifiers;
    }
    
    /**
     * Get the node representing the class containing this one.
     */
    public ParsedTypeNode getContainingClass()
    {
        return containingClass;
    }
    
    /**
     * Set the type parameters for this type (empty list for none).
     */
    public void setTypeParams(List<TparEntity> typeParams)
    {
        this.typeParams = typeParams;
    }
    
    /**
     * Get the type parameters for this type (empty list if none).
     */
    public List<TparEntity> getTypeParams()
    {
        return typeParams;
    }
    
    /**
     * Set the types that this type is declared to implement (empty list for none).
     */
    public void setImplementedTypes(List<JavaEntity> implementedTypes)
    {
        this.implementedTypes = implementedTypes;
    }
    
    /**
     * Get the types this type is declared to implement (empty list if none).
     */
    public List<JavaEntity> getImplementedTypes()
    {
        return implementedTypes;
    }
    
    /**
     * Specify which types this type explicitly extends (empty list for none).
     */
    public void setExtendedTypes(List<JavaEntity> extendedTypes)
    {
        this.extendedTypes = extendedTypes;
    }
    
    /**
     * Return the types which this type explicit extends.
     * For an anonymous inner class, the returned list will contain a single
     * type which may be a class or interface.
     */
    public List<JavaEntity> getExtendedTypes()
    {
        return extendedTypes;
    }
    
    @Override
    public int getNodeType()
    {
        return NODETYPE_TYPEDEF;
    }
    
    @Override
    public boolean isContainer()
    {
        return true;
    }
    
    /**
     * Set the unqualified name of the type this node represents.
     */
    public void setName(String name)
    {
        String oldName = this.name;
        this.name = name;
        getParentNode().childChangedName(this, oldName);
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    /**
     * Get the package qualification prefix for the type this node represents.
     */
    public String getPrefix()
    {
        return prefix;
    }
    
    /**
     * Insert the inner node for the type definition.
     * The inner node will hold the field definitions etc.
     */
    public void insertInner(TypeInnerNode child, int position, int size, NodeStructureListener nodeStructureListener)
    {
        super.insertNode(child, position, size, nodeStructureListener);
        inner = child;
        stateMarkers[1] = position + size;
    }
    
    /**
     * Get the inner node for the type, if one exists. May return null.
     */
    public TypeInnerNode getInner()
    {
        return inner;
    }
    
    @Override
    protected void childRemoved(NodeAndPosition<ParsedNode> child,
            NodeStructureListener listener)
    {
        if (child.getNode() == inner) {
            inner = null;
            stateMarkers[1] = -1;
        }
        super.childRemoved(child, listener);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    protected int doPartialParse(ParseParams params, int state)
    {
        if (state == 0) {
            // [modifiers] {class|interface|enum|@interface} name [<type params>] [extends...]
            LocatableToken la = params.tokenStream.LA(1);
            setCommentAttached(la.getHiddenBefore() != null);
            int r = params.parser.parseTypeDefBegin();
            if (r == JavaParser.TYPEDEF_EPIC_FAIL) {
                return PP_EPIC_FAIL;
            }
            
            type = r;
            params.parser.initializeTypeExtras();
            
            LocatableToken token = params.tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.IDENT) {
                last = token;
                return PP_INCOMPLETE;
            }
            setName(token.getText());
            
            token = params.parser.parseTypeDefPart2();
            if (token == null) {
                last = params.tokenStream.LA(1);
                return PP_INCOMPLETE;
            }
            last = token;
            params.tokenStream.pushBack(token);
            setExtendedTypes(params.parser.getExtendedTypes());
            setTypeParams(params.parser.getTparList(this));
            return PP_BEGINS_NEXT_STATE;
        }
        else if (state == 1) {
            // '{' and class body
            last = params.tokenStream.nextToken();
            if (last.getType() != JavaTokenTypes.LCURLY) {
                return PP_REGRESS_STATE;
            }
            
            if (inner == null) {
                int oldStateMarker = stateMarkers[1];
                last = params.parser.parseTypeBody(type, last);
                if (last.getType() == JavaTokenTypes.RCURLY) {
                    // "inner" is now non-null due to the call to parseTypeBody above,
                    // so this line is not erroneous:
                    inner.setComplete(true);
                }
                params.tokenStream.pushBack(last);
                stateMarkers[1] = oldStateMarker; // let state transition magic work
                return PP_BEGINS_NEXT_STATE;
            }
            
            // If we already have an inner we pull it into position.
            NodeAndPosition<ParsedNode> nextChild = params.childQueue.peek();
            while (nextChild != null && nextChild.getNode() != inner) {
                childRemoved(nextChild, params.listener);
                params.childQueue.poll();
                nextChild = params.childQueue.peek();
            }
            
            params.abortPos = lineColToPos(params.document, last.getEndLine(), last.getEndColumn());
            return PP_PULL_UP_CHILD;
        }
        else if (state == 2) {
            // '}'
            last = params.tokenStream.nextToken();

            int innerOffset = inner.getOffsetFromParent();
            int innerPos = innerOffset + params.nodePos;
            int innerSize = inner.getSize();

            if (last.getType() != JavaTokenTypes.RCURLY) {
                // Extend the inner.
                inner.setComplete(false);
                inner.setSize(getSize() - innerOffset);
                params.listener.nodeChangedLength(
                        new NodeAndPosition<ParsedNode>(inner, innerPos, getSize() - innerOffset),
                        innerPos,
                        innerSize);
                stateMarkers[1] = getSize();
                params.document.scheduleReparse(innerPos + innerSize, getSize() - innerOffset - innerSize);
                params.abortPos = innerPos + innerSize;
                complete = false;
                return PP_ABORT;
            }
            
            // Extend the inner node up to the token we just pulled.
            int lastPos = lineColToPos(params.document, last.getLine(), last.getColumn());
            if ((innerPos + innerSize) != lastPos || ! inner.complete) {
                // Expand the inner node to cover the RCURLY, which hopefully actually closes it,
                // and re-parse
                inner.complete = false;
                lastPos = lineColToPos(params.document, last.getEndLine(), last.getEndColumn());
                inner.setSize(lastPos - innerPos);
                params.listener.nodeChangedLength(
                        new NodeAndPosition<ParsedNode>(inner, innerPos, lastPos - innerPos),
                        innerPos,
                        innerSize);
                stateMarkers[1] = lastPos - params.nodePos;
                params.document.scheduleReparse(innerPos + innerSize, lastPos - innerPos - innerSize);
                params.abortPos = innerPos + innerSize;
                return PP_ABORT;
            }
            
            return PP_ENDS_NODE_AFTER;
        }
        
        return PP_EPIC_FAIL;
    }
    
    @Override
    protected boolean isDelimitingNode(NodeAndPosition<ParsedNode> nap)
    {
        return nap.getNode().isInner();
    }
    
    @Override
    protected boolean isNodeEndMarker(int tokenType)
    {
        return false;
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
    
    @Override
    public void childResized(ReparseableDocument document, int nodePos, NodeAndPosition<ParsedNode> child)
    {
        if (child.getNode() == inner) {
            stateMarkers[1] = child.getEnd() - nodePos;
        }
    }
    
    @Override
    public ExpressionTypeInfo getExpressionType(int pos, int nodePos, JavaEntity defaultType, ReparseableDocument document)
    {
        valueEntityCache.clear();
        pocEntityCache.clear();
        
        // The default type if the expression is not known should be this type
        ValueEntity myType = new ValueEntity(new GenTypeClass(new ParsedReflective(this)));
        NodeAndPosition<ParsedNode> child = getNodeTree().findNode(pos, nodePos);
        if (child != null) {
            return child.getNode().getExpressionType(pos, child.getPosition(), myType, document);
        }
        
        // We don't return the specified default type (which must be an outer type). There
        // can be no completions because no completions can occur except in the context
        // of child nodes.
        return null;
    }
    
    @Override
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
    {
        if (typeParams != null) {
            JavaEntity ent = null;
            for (TparEntity tent : typeParams) {
                if (tent.getName().equals(name)) {
                    ent = tent;
                    break;
                }
            }
            if (ent != null) {
                TypeEntity tent = ent.resolveAsType();
                if (tent != null) {
                    return tent;
                }
            }
        }
        return super.resolvePackageOrClass(name, querySource);
    }
}
