/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2011,2014,2019,2020  Michael Kolling and John Rosenberg
 
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
import bluej.parser.Token;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.entity.JavaEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Iterator;

/**
 * A "parsed node" represents a node in a limited parse tree. The tree is limited because
 * it contains only a subset of elements that might normally be found in a full parse tree.
 * A ParsedNode tree does however contain information to precisely map nodes to source code
 * document positions.<p>
 * <p>
 * Also included is basic infrastructure for incremental parsing.
 *
 * @author Davin McCall
 */
public abstract class ParsedNode extends RBTreeNode<ParsedNode>
{
    public static final int NODETYPE_NONE = 0;
    public static final int NODETYPE_TYPEDEF = 1;
    public static final int NODETYPE_METHODDEF = 2;
    public static final int NODETYPE_ITERATION = 3; // for, while, etc
    public static final int NODETYPE_SELECTION = 4; // if/then, try/catch

    public static final int NODETYPE_FIELD = 5;  // field declaration
    public static final int NODETYPE_EXPRESSION = 6; // expression

    public static final int NODETYPE_COMMENT = 7;

    /**
     * The NodeTree containing the child nodes of this node
     */
    private final NodeTree<ParsedNode> nodeTree;
    /**
     * The parent ParsedNode which contains us
     */
    private final ParsedNode parentNode;

    private boolean hasAttachedComment;

    /**
     * Specifies whether this node is complete, in that its end is properly marked with an
     * appropriate token.
     */
    protected boolean complete;

    private boolean isInner = false;

    ParsedNode(ParsedNode parentNode)
    {
        nodeTree = new NodeTree<ParsedNode>();
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
     * <li>NODETYPE_FIELD     - a field or variable declaration
     * <li>NODETYPE_EXPRESSION - an expression
     * <li>NODETYPE_COMMENT   - a code comment
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

    /**
     * Returns true if this node marks its own end, that is, the token signifying
     * the end of this node is contained within this node itself, rather than in
     * the parent node.
     */
    protected abstract boolean marksOwnEnd();

    /**
     * Insert a new child node (without affecting position of other children).
     */
    public void insertNode(ParsedNode child, int position, int size, NodeStructureListener nodeStructureListener)
    {
        getNodeTree().insertNode(child, position, size);
        nodeStructureListener.nodeAdded(new NodeAndPosition<>(child, position, size));
    }

    public void childChangedName(ParsedNode child, String oldName)
    {
        // Do nothing.
    }

    /**
     * Find the child node (if any) overlapping (including starting or ending at) the given position.
     *
     * @param position The position of the child node to find
     * @param startpos The position of this node
     * @return the "leftmost" child which overlaps the position
     */
    public final NodeAndPosition<ParsedNode> findNodeAt(int position, int startpos)
    {
        return nodeTree.findNode(position, startpos);
    }

    /**
     * Find the child node (if any) at or after the given position
     *
     * @param position The position of the child node to find
     * @param startpos The position of this node
     */
    public final NodeAndPosition<ParsedNode> findNodeAtOrAfter(int position, int startpos)
    {
        return nodeTree.findNodeAtOrAfter(position, startpos);
    }

    /**
     * Set the size of this node. Following nodes shift position according to the change in
     * size; this should normally be used when inserting or removing text from the node.
     *
     * @param newSize The new node size
     */
    public void resize(int newSize)
    {
        getContainingNodeTree().resize(newSize);
    }

    /**
     * Set the size of this node, without moving following nodes. It is the caller's
     * responsibility to ensure that setting the new size does not cause this node
     * to overlap following nodes.
     *
     * @param newSize The new size of this node.
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
        if (getContainingNodeTree() == null)
        {
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
     *
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
     * Insert the given text.
     *
     * <p>The result should be one of ALL_OK, NODE_GREW, NODE_SHRUNK, or REMOVE_NODE.
     * The latter indicates that the caller should remove the node. Except in the
     * case of ALL_OK, the parent node must generally be re-parsed.
     *
     * <p>Note, it is expected that the node grows by the amount of text inserted -
     * the return should be ALL_OK rather than NODE_GREW if that is the case.
     *
     * @param document The document
     * @param nodePos  The position of "this" node (relative to the document).
     * @param insPos   The position of the insert (relative to the document).
     * @param length   The length of the insert
     * @param listener The listener for node structural changes
     */
    public abstract int textInserted(ReparseableDocument document, int nodePos, int insPos,
                                     int length, NodeStructureListener listener);

    /**
     * The specified portion of text within the node has been removed.<p>
     *
     * <p>The result should be one of ALL_OK, NODE_GREW, NODE_SHRUNK, or REMOVE_NODE.
     * The latter indicates that the caller should remove the node. Except in the
     * case of ALL_OK, the parent node must generally be re-parsed.
     *
     * <p>It is expected that the node shrink by the amount of text removed - the
     * return should be ALL_OK rather than NODE_SHRUNK if that is the case.
     *
     * @param document The document
     * @param nodePos  The position of "this" node (relative to the document).
     * @param delPos   The position of the removal (relative to the document).
     * @param length   The length of the removal
     * @param listener The listener for node structural changes
     */
    public abstract int textRemoved(ReparseableDocument document, int nodePos, int delPos,
                                    int length, NodeStructureListener listener);

    /**
     * This node should be re-parsed from the specified point. The node position
     * and offset are relative to the document beginning.<p>
     * <p>
     * The result should be one of ALL_OK, NODE_GREW, NODE_SHRUNK, or REMOVE_NODE.
     * The latter indicates that the caller should remove the node. Except in the
     * case of ALL_OK, the parent node must generally also be re-parsed.<p>
     * <p>
     * This method should always mark which range it parsed in the document.
     */
    protected int reparseNode(ReparseableDocument document, int nodePos, int offset, int maxParse, NodeStructureListener listener)
    {
        return ALL_OK;
    }

    /**
     * Perform a re-parse of the document at a given point. The parse may be partial; a certain amount of
     * parsing will be performed and further re-parses will be queued as necessary against the document.
     *
     * @param document The document
     * @param nodePos  The position of this node
     * @param offset   The position within the document of the re-parse
     * @param maxParse The (advisory) maximum amount of document to re-parse in one hit
     * @param listener The structure listener to be notified of structural changes
     */
    public void reparse(ReparseableDocument document, int nodePos, int offset, int maxParse, NodeStructureListener listener)
    {
        int size = getSize();
        int r = reparseNode(document, nodePos, offset, maxParse, listener);
        if (r == REMOVE_NODE)
        {
            ParsedNode parent = getParentNode();
            parent.removeChild(new NodeAndPosition<ParsedNode>(this,
                nodePos, getSize()), listener);
            document.scheduleReparse(nodePos + size - 1, 0);
        }
        else if (r == NODE_GREW || r == NODE_SHRUNK)
        {
            int nsize = getSize();
            ParsedNode parent = getParentNode();
            if (parent != null)
            {
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
     *
     * @param pos      The position of the text to tokenize (document relative). Must be on a
     *                 line or token boundary.
     * @param length   The length of the text to tokenize. Must be on a line or token boundary.
     * @param nodePos  The position of the node
     * @param document The source document
     * @return A linked list of Token objects
     */
    public abstract Token getMarkTokensFor(int pos, int length, int nodePos, ReparseableDocument document);

    public ParsedNode getParentNode()
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
    protected void nodeShortened(int newLength)
    {
    }

    /**
     * This node has become incomplete (needs to be extended).
     */
    protected void nodeIncomplete()
    {
    }

    /**
     * growChild() is called by a child node when, during incremental parsing, it determines
     * that it needs to grow in size. The response must be to increase the size of the child
     * and return true, or (if increasing size is really not possible) to return false, or to
     * return false and assume responsibility for re-parsing.<p>
     * <p>
     * It is the responsibility of this method to notify the listener of the child's change
     * in size, if it occurs.
     */
    @OnThread(Tag.FXPlatform)
    protected boolean growChild(ReparseableDocument document, NodeAndPosition<ParsedNode> child,
                                NodeStructureListener listener)
    {
        return false;
    }

    /**
     * Called after a child node changed size. This is just a notification
     * and should not cause a reparse to be performed or scheduled, as that
     * should be done elsewhere.
     *
     * @param document The document which parse structure is represented
     * @param nodePos  The absolute position of this node
     * @param child    The child node which has changed size
     */
    public void childResized(ReparseableDocument document, int nodePos, NodeAndPosition<ParsedNode> child)
    {

    }

    /**
     * Get code completion suggestions at a particular point. May return null.
     */
    @OnThread(Tag.FXPlatform)
    public ExpressionTypeInfo getExpressionType(int pos, ReparseableDocument document)
    {
        return getExpressionType(pos, 0, null, document);
    }

    /**
     * Get code completion suggestions at a particular point. May return null.
     *
     * @param pos         The position to suggest completions for
     * @param nodePos     The position of this node
     * @param defaultType The type to return if there is no explicit type at the given location
     * @param document    The source document
     */
    @OnThread(Tag.FXPlatform)
    protected ExpressionTypeInfo getExpressionType(int pos, int nodePos, JavaEntity defaultType, ReparseableDocument document)
    {
        NodeAndPosition<ParsedNode> child = getNodeTree().findNode(pos, nodePos);
        if (child != null)
        {
            return child.getNode().getExpressionType(pos, child.getPosition(), defaultType, document);
        }

        GenTypeClass atype = (defaultType != null) ? defaultType.getType().asClass() : null;
        if (atype == null)
        {
            return null;
        }
        boolean isStaticCtxt = (defaultType.resolveAsType() != null);
        return new ExpressionTypeInfo(atype, atype, null, isStaticCtxt, true);
    }

    public ParsedNode getContainingMethodOrClassNode(int pos)
    {
        NodeAndPosition<ParsedNode> child = getNodeTree().findNode(pos, 0);
        if (child != null)
            return child.getNode().getContainingMethodOrClassNode(pos - child.getPosition());

        // We don't need to go too deep in the tree: retrieve a node that is at deepest a method node.
        // So we get back to the last named node parent to this leaf.
        if (this.parentNode == null)
            return null;

        ParsedNode parentNode = this.parentNode;
        while (parentNode.getName() == null)
        {
            if (parentNode.parentNode != null)
            {
                parentNode = parentNode.parentNode;
            }
        }
        return parentNode;
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
        while (i.hasNext())
        {
            NodeAndPosition<ParsedNode> nap = i.next();
            listener.nodeRemoved(nap);
            removeChildren(nap, listener);
        }
    }

    /**
     * Check whether a documentary comment is attached to this node.
     */
    public boolean isCommentAttached()
    {
        return hasAttachedComment;
    }

    /**
     * Specify whether or not this node has a documentary comment attached to it.
     */
    public void setCommentAttached(boolean commentAttached)
    {
        hasAttachedComment = commentAttached;
    }

    /**
     * Gets the absolute position of this node in the editor.
     */
    public int getAbsoluteEditorPosition()
    {
        int position = getOffsetFromParent();
        ParsedNode parent = getParentNode();
        while (parent != null)
        {
            position += parent.getOffsetFromParent();
            parent = parent.getParentNode();
        }
        return position;
    }
}
