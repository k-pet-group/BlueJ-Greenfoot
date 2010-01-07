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

import bluej.editor.moe.Token;
import bluej.parser.entity.ClassEntity;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

public abstract class ParsedNode implements EntityResolver
{
    public static final int NODETYPE_NONE = 0;
    public static final int NODETYPE_TYPEDEF = 1;
    public static final int NODETYPE_METHODDEF = 2;
    public static final int NODETYPE_ITERATION = 3; // for, while, etc
    public static final int NODETYPE_SELECTION = 4; // if/then, try/catch
    
    public static final int NODETYPE_FIELD = 5;  // field declaration
    public static final int NODETYPE_EXPRESSION = 6; // expression
    
    public static final int NODETYPE_COMMENT = 7;
    
    /** The NodeTree containing the child nodes of this node */
    private NodeTree nodeTree;
    /** The NodeTree node (belonging to the parent parse node) which contains this node */
    private NodeTree containingNodeTree;
    /** The parent ParsedNode which contains us */
    private ParsedNode parentNode;
    
    private Map<String,ParsedNode> classNodes = new HashMap<String,ParsedNode>();
    
    private boolean isInner = false;
	
    public ParsedNode()
    {
        nodeTree = new NodeTree();
    }
	
    ParsedNode(ParsedNode parentNode)
    {
        this();
        this.parentNode = parentNode;
    }

    /**
     * Get the type of this node. One of:
     * <ul>
     * <li>NODETYPE_NONE - unspecified
     * <li>NODETYPE_TYPEDEF - a type definition (class, interface etc)
     * <li>NODETYPE_METHODDEF - a method defintion
     * <li>NODETYPE_ITERATION - an iteration construct (for loop etc)
     * <li>NODETYPE_SELECTION - a selection construct (if/else etc)
     * </ul>
     */
    public int getNodeType()
    {
        return NODETYPE_NONE;
    }
    
    /**
     * Get the name of the entity this node represents. For methods, returns the method name.
     * May return null.
     */
    public String getName()
    {
        return null;
    }
    
    public boolean equals(Object obj)
    {
        return obj == this;
    }
    
    public void insertNode(ParsedNode child, int position, int size)
    {
        getNodeTree().insertNode(child, position, size);
        if (child.getNodeType() == NODETYPE_TYPEDEF && child.getName() != null) {
            classNodes.put(child.getName(), child);
        }
    }
    
    /**
     * Find the child node (if any) at the given position
     * @param position   The position of the child node to find
     * @param startpos   The position of this node
     */
    public final NodeAndPosition findNodeAt(int position, int startpos)
    {
        return nodeTree.findNode(position, startpos);
    }

    /**
     * Find the child node (if any) at or after the given position
     * @param position   The position of the child node to find
     * @param startpos   The position of this node
     */
    public final NodeAndPosition findNodeAtOrAfter(int position, int startpos)
    {
        return nodeTree.findNodeAtOrAfter(position, startpos);
    }    
    
    /**
     * Set the size of this node. Following nodes shift position according to the change in
     * size; this should normally be used when inserting or removing text from the node.
     * @param newSize  The new node size
     */
    public void setNodeSize(int newSize)
    {
        getContainingNodeTree().setNodeSize(newSize);
    }
    
    /**
     * Get the offset of this node from its parent node.
     */
    public int getOffsetFromParent()
    {
        if (containingNodeTree == null) {
            return 0;
        }
        return getContainingNodeTree().getPosition();
    }
    
    /**
     * Remove this node from the parent, without disturbing the position of any sibling nodes.
     */
    public void remove()
    {
        getContainingNodeTree().remove();
    }
    
    /**
     * Set the containing node tree. This is normally only called by NodeTree when inserting
     * this node into the tree.
     */
    void setContainingNodeTree(NodeTree cnode)
    {
        containingNodeTree = cnode;
    }
    
    /**
     * Is this a "container" scope for highlighting purposes
     * @return
     */
    public boolean isContainer()
    {
        return false;
    }
    
    /**
     * Is this an "inner" scope for highlighting purposes
     */
    public boolean isInner()
    {
        return isInner;
    }
    
    /**
     * Set whether this is an inner scope for highlighting purposes
     */
    public void setInner(boolean inner)
    {
        isInner = inner;
    }
    
    /**
     * Get the size of this node.
     */
    public int getSize()
    {
        return getContainingNodeTree().getNodeSize();
    }

    /**
     * Insert the given text.
     * 
     * The result can be:
     * - text absorbed, no change to node structure
     * - node terminates earlier (eg ';' or '}' inserted)
     * - subnode created, and this node extended (eg insert '{')
     */
    public abstract void textInserted(Document document, int nodePos, int insPos, int length);
	
    public abstract void textRemoved(Document document, int nodePos, int delPos, int length);

    public abstract Token getMarkTokensFor(int pos, int length, int nodePos, Document document);

    protected ParsedNode getParentNode()
    {
        return parentNode;
    }

    protected NodeTree getNodeTree()
    {
        return nodeTree;
    }

    protected NodeTree getContainingNodeTree()
    {
        return containingNodeTree;
    }

    /**
     * This node is shortened, it no longer needs all the text assigned to it.
     */
    protected void nodeShortened(int newLength) {}

    /**
     * This node has become incomplete (needs to be extended).
     */
    protected void nodeIncomplete() {}

    /**
     * This node should be re-parsed from the specified point.
     */
    protected void reparseNode(Document document, int nodePos, int offset) {}
    
    /**
     * Get the expression type at a given point. Returns null if there is no
     * determinable expression type.
     * @param document TODO
     */
    public ClassEntity getExpressionType(int pos, Document document)
    {
        return getExpressionType(pos, 0, null, document);
    }
    
    /**
     * Find a type node for a type definition with the given name.
     */
    public ParsedNode getTypeNode(String name)
    {
        return classNodes.get(name);
    }
    
    /**
     * Get the expression type at a given point. Returns the specified default if there
     * is no determinable expression type.
     * @param pos     The position at which to determine the expression type
     * @param nodePos   The position of this node
     * @param defaultType  The type to return if there is no explicit type at the given location 
     * @param document  The source document
     */
    protected ClassEntity getExpressionType(int pos, int nodePos, ClassEntity defaultType, Document document)
    {
        NodeAndPosition child = getNodeTree().findNode(pos, nodePos);
        if (child != null) {
            return child.getNode().getExpressionType(pos, child.getPosition(), defaultType, document);
        }
        return defaultType;
    }
    
    protected Map<String,ParsedNode> getClassNodes()
    {
        return classNodes;
    }
    
    // =================== EntityResolver interface ====================
    
    /*
     * @see bluej.parser.entity.EntityResolver#resolveQualifiedClass(java.lang.String)
     */
    public ClassEntity resolveQualifiedClass(String name)
    {
        if (parentNode != null) {
            return parentNode.resolveQualifiedClass(name);
        }
        return null;
    }
    
    /*
     * @see bluej.parser.entity.EntityResolver#resolvePackageOrClass(java.lang.String, java.lang.String)
     */
    public PackageOrClass resolvePackageOrClass(String name, String querySource)
    {
        ParsedNode cnode = classNodes.get(name);
        if (cnode != null) {
            return new TypeEntity(new ParsedReflective((ParsedTypeNode) cnode));
        }
        if (parentNode != null) {
            return parentNode.resolvePackageOrClass(name, querySource);
        }
        return null;
    }
    
    /*
     * @see bluej.parser.entity.EntityResolver#getValueEntity(java.lang.String, java.lang.String)
     */
    public JavaEntity getValueEntity(String name, String querySource)
    {
        if (parentNode != null) {
            JavaEntity rval = parentNode.getValueEntity(name, querySource);
            if (rval != null) {
                return rval;
            }
        }
        return resolvePackageOrClass(name, querySource);
    }
}
