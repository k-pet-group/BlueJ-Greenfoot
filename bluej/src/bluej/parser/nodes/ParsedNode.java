/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 
 
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
import java.util.Iterator;
import java.util.Map;

import javax.swing.text.Document;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.editor.moe.Token;
import bluej.parser.CodeSuggestions;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.utility.GeneralCache;

/**
 * A "parsed node" represents a node in a limited parse tree. The tree is limited because
 * it contains only a subset of elements that might normally be found in a full parse tree.
 * A ParsedNode tree does however contain information to precisely map nodes to source code
 * document positions.<p>
 * 
 * Also included is basic infrastructure for incremental parsing.
 * 
 * @author Davin McCall
 */
public abstract class ParsedNode extends RBTreeNode implements EntityResolver
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
    private NodeTree<ParsedNode> nodeTree;
    /** The parent ParsedNode which contains us */
    private ParsedNode parentNode;
    
    /**
     * Specifies whether this node is complete, in that its end is properly marked with an
     * appropriate token. 
     */
    protected boolean complete;
    
    private Map<String,ParsedNode> classNodes = new HashMap<String,ParsedNode>();
    
    private GeneralCache<String,JavaEntity> valueEntityCache =
        new GeneralCache<String,JavaEntity>(10);
    private GeneralCache<String,PackageOrClass> pocEntityCache =
        new GeneralCache<String,PackageOrClass>(10);
    
    private boolean isInner = false;
	
    public ParsedNode()
    {
        nodeTree = new NodeTree<ParsedNode>();
    }
	
    ParsedNode(ParsedNode parentNode)
    {
        this();
        this.parentNode = parentNode;
    }

    public Iterator<NodeAndPosition<ParsedNode>> getChildren(int offset)
    {
        return nodeTree.iterator(offset);
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
     * Specify whether this node is complete - that is, it is ended by an appropriate token.
     */
    public void setComplete(boolean complete)
    {
        this.complete = complete;
    }
    
    /**
     * Check whether this node is known to be complete.
     */
    public boolean isComplete()
    {
        return complete;
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
    
    /**
     * If text is inserted immediately before this node, should it be made part of this
     * node? For instance in a method inner body, this would be true, seeing as anything
     * after the '{' (which is part of the outer body) must by definition be part of the
     * inner body.
     */
    public boolean growsForward()
    {
        return false;
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
     * Find the child node (if any) at the given position
     * @param position   The position of the child node to find
     * @param startpos   The position of this node
     */
    public final NodeAndPosition<ParsedNode> findNodeAt(int position, int startpos)
    {
        return nodeTree.findNode(position, startpos);
    }

    /**
     * Find the child node (if any) at or after the given position
     * @param position   The position of the child node to find
     * @param startpos   The position of this node
     */
    public final NodeAndPosition<ParsedNode> findNodeAtOrAfter(int position, int startpos)
    {
        return nodeTree.findNodeAtOrAfter(position, startpos);
    }    
    
    /**
     * Set the size of this node. Following nodes shift position according to the change in
     * size; this should normally be used when inserting or removing text from the node.
     * @param newSize  The new node size
     */
    public void resize(int newSize)
    {
        getContainingNodeTree().resize(newSize);
    }
    
    /**
     * Set the size of this node, without moving following nodes. It is the caller's
     * responsibility to ensure that setting the new size does not cause this node
     * to overlap following nodes.
     * @param newSize  The new size of this node.
     */
    public void setSize(int newSize)
    {
        getContainingNodeTree().setSize(newSize);
    }
    
    /**
     * Get the offset of this node from its parent node.
     */
    public int getOffsetFromParent()
    {
        if (getContainingNodeTree() == null) {
            return 0;
        }
        return getContainingNodeTree().getPosition();
    }
    
    /**
     * Remove this node from the parent, without disturbing the position of any sibling nodes.
     * Probably, you don't want to call this directly; call removeChild() on the parent instead.
     */
    public void remove()
    {
        getContainingNodeTree().remove();
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

    /* Constants for status of various methods defined below */
    protected final static int ALL_OK = 0;
    protected final static int NODE_GREW = 1;
    protected final static int NODE_SHRUNK = 2;
    protected final static int REMOVE_NODE = 3; // (and reparse parent)
    
    /**
     * Insert the given text.<p>
     * 
     * The result should be one of ALL_OK, NODE_GREW, NODE_SHRUNK, or REMOVE_NODE.
     * The latter indicates that the caller should remove the node. Except in the
     * case of ALL_OK, the parent node must generally be re-parsed.
     * 
     * @param document   The document
     * @param nodePos    The position of "this" node (relative to the document).
     * @param insPos     The position of the insert (relative to the document).
     * @param length     The length of the insert
     * @param listener   The listener for node structural changes
     */
    public abstract int textInserted(Document document, int nodePos, int insPos, int length, NodeStructureListener listener);

    /**
     * The specified portion of text within the node has been removed.<p>
     * 
     * The result should be one of ALL_OK, NODE_GREW, NODE_SHRUNK, or REMOVE_NODE.
     * The latter indicates that the caller should remove the node. Except in the
     * case of ALL_OK, the parent node must generally be re-parsed.
     * 
     * @param document   The document
     * @param nodePos    The position of "this" node (relative to the document).
     * @param insPos     The position of the removal (relative to the document).
     * @param length     The length of the removal
     * @param listener   The listener for node structural changes
     */
    public abstract int textRemoved(Document document, int nodePos, int delPos, int length, NodeStructureListener listener);

    /**
     * This node should be re-parsed from the specified point. The node position
     * and offset are relative to the document beginning.<p>
     * 
     * The result should be one of ALL_OK, NODE_GREW, NODE_SHRUNK, or REMOVE_NODE.
     * The latter indicates that the caller should remove the node. Except in the
     * case of ALL_OK, the parent node must generally also be re-parsed.<p>
     * 
     * This method should always mark which range it parsed in the document.
     */
    protected int reparseNode(Document document, int nodePos, int offset, NodeStructureListener listener)
    {
        return ALL_OK;
    }
    
    /**
     * Perform a reparse of the document at a given point
     * 
     * @param document  The document
     * @param nodePos   The position of this node
     * @param offset    The position within the document of the reparse
     * @param listener  The structure listener to be notified of structural changes
     */
    public void reparse(MoeSyntaxDocument document, int nodePos, int offset, NodeStructureListener listener)
    {
        int size = getSize();
        int r = reparseNode(document, nodePos, offset, listener);
        if (r == REMOVE_NODE) {
            ParsedNode parent = getParentNode();
            parent.removeChild(new NodeAndPosition<ParsedNode>(this,
                    nodePos, getSize()), listener);
            document.scheduleReparse(nodePos + size - 1, 0);
        }
        else if (r == NODE_GREW || r == NODE_SHRUNK) {
            int nsize = getSize();
            ParsedNode parent = getParentNode();
            if (parent != null) {
                int ppos = nodePos - getOffsetFromParent();
                parent.childResized(document, ppos,
                        new NodeAndPosition<ParsedNode>(this, nodePos, nsize));
            }
            document.scheduleReparse(nodePos + nsize, Math.max(size - nsize, 0));
        }
    }
    
    /**
     * Get a sequence of "tokens" which indicate the colour and position/size of various tokens
     * in a line of source code text. 
     * @param pos       The position of the text to tokenize (document relative). Must be on a
     *                  line or token boundary.
     * @param length    The length of the text to tokenize. Must be on a line or token boundary.
     * @param nodePos   The position of the node
     * @param document  The source document
     * @return  A linked list of Token objects
     */
    public abstract Token getMarkTokensFor(int pos, int length, int nodePos, Document document);

    protected ParsedNode getParentNode()
    {
        return parentNode;
    }

    protected NodeTree<ParsedNode> getNodeTree()
    {
        return nodeTree;
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
     * growChild() is called by a child node when, during incremental parsing, it determines
     * that it needs to grow in size. The response must be to increase the size of the child
     * and return true, or (if increasing size is really not possible) to return false, or to
     * return false and assume responsibility for re-parsing.<p>
     * 
     * It is the responsibility of this method to notify the listener of the child's change
     * in size, if it occurs.
     */
    protected boolean growChild(Document document, NodeAndPosition<ParsedNode> child,
            NodeStructureListener listener)
    {
        return false;
    }
    
    /**
     * Called after a child node changed size. This is just a notification
     * and should not cause a reparse to be performed or scheduled, as that
     * should be done elsewhere.
     * 
     * @param document   The document which parse structure is represented
     * @param nodePos    The absolute position of this node
     * @param child      The child node which has changed size
     */
    public void childResized(MoeSyntaxDocument document, int nodePos, NodeAndPosition<ParsedNode> child)
    {
        
    }
        
    /**
     * Get code completion suggestions at a particular point. May return null.
     */
    public CodeSuggestions getExpressionType(int pos, Document document)
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
     * Get code completion suggestions at a particular point. May return null.
     *
     * @param pos     The position to suggest completions for
     * @param nodePos   The position of this node
     * @param defaultType  The type to return if there is no explicit type at the given location 
     * @param document  The source document
     */
    protected CodeSuggestions getExpressionType(int pos, int nodePos, TypeEntity defaultType, Document document)
    {
        NodeAndPosition<ParsedNode> child = getNodeTree().findNode(Math.max(pos - 1, 0), nodePos);
        if (child != null) {
            return child.getNode().getExpressionType(pos, child.getPosition(), defaultType, document);
        }
        GenTypeClass atype = (defaultType != null) ? defaultType.getType().asClass() : null;
        if (atype == null) {
            return null;
        }
        return new CodeSuggestions(atype, atype, null);
    }
    
    protected Map<String,ParsedNode> getClassNodes()
    {
        return classNodes;
    }
    
    /**
     * Remove a child node, and notify the NodeStructureListener that the child and
     * its descendants have been removed.  Won't disturb the position of subsequent
     * children. The "child" NodeAndPosition parameter specifies the absolute location
     * of the child node, not its position relative to "this" node.
     */
    protected final void removeChild(NodeAndPosition<ParsedNode> child, NodeStructureListener listener)
    {
        child.getNode().remove();
        childRemoved(child, listener);
    }
    
    protected void childRemoved(NodeAndPosition<ParsedNode> child, NodeStructureListener listener)
    {
        listener.nodeRemoved(child);
        removeChildren(child, listener);
    }
    
    /**
     * Notify the NodeStructureListener that all descendants of a particular node
     * are removed, due to the node itself having been removed. (Note this does not actually
     * remove the children from the parent node).
     */
    protected static void removeChildren(NodeAndPosition<ParsedNode> node, NodeStructureListener listener)
    {
        Iterator<NodeAndPosition<ParsedNode>> i = node.getNode().getChildren(node.getPosition());
        while (i.hasNext()) {
            NodeAndPosition<ParsedNode> nap = i.next();
            listener.nodeRemoved(nap);
            removeChildren(nap, listener);
        }
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
}
