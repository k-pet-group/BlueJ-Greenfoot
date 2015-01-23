/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2012,2013,2015  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.collision.ibsp;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.collision.*;

import java.awt.Color;
import java.awt.Graphics;
import java.util.*;

/**
 * A collision checker using a Binary Space Partition tree.
 * 
 * <p>Each node of the tree represents a rectangular area, and potentially has
 * two non-overlapping child nodes which together cover the same area as their
 * parent.
 * 
 * @author Davin McCall
 */
public class IBSPColChecker implements CollisionChecker
{
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    
    public static final int PARENT_LEFT = 0;
    public static final int PARENT_RIGHT = 1;
    public static final int PARENT_NONE = 3; // no particular side
    
    public static final int REBALANCE_THRESHOLD = 20;
    
    private GOCollisionQuery actorQuery = new GOCollisionQuery();
    private NeighbourCollisionQuery neighbourQuery = new NeighbourCollisionQuery();
    private PointCollisionQuery pointQuery = new PointCollisionQuery();
    private InRangeQuery inRangeQuery = new InRangeQuery();
    
    private int cellSize;
    
    private BSPNode bspTree;
    
    public static boolean debugging = false;
    
    /* (non-Javadoc)
     * @see greenfoot.collision.CollisionChecker#initialize(int, int, int, boolean)
     */
    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        this.cellSize = cellSize;
    }

    /*
     * @see greenfoot.collision.CollisionChecker#addObject(greenfoot.Actor)
     */
    public void addObject(Actor actor)
    {
        // checkConsistency(true);
        Rect bounds = getActorBounds(actor);
        if (bspTree == null) {
            // The tree is currently empty; just create a new node containing only the one actor
            int splitAxis;
            int splitPos;
            if (bounds.getWidth() > bounds.getHeight()) {
                splitAxis = X_AXIS;
                splitPos = bounds.getMiddleX();
            }
            else {
                splitAxis = Y_AXIS;
                splitPos = bounds.getMiddleY();
            }
            bspTree = BSPNodeCache.getBSPNode();
            bspTree.getArea().copyFrom(bounds);
            bspTree.setSplitAxis(splitAxis);
            bspTree.setSplitPos(splitPos);
            bspTree.addActor(actor);
        }
        else {
            Rect treeArea = bspTree.getArea();
            while (! treeArea.contains(bounds)) {
                // We increase the tree area in up to four directions:
                if (bounds.getX() < treeArea.getX()) {
                    // double the width out to the left
                    int bx = treeArea.getX() - treeArea.getWidth();
                    Rect newArea = new Rect(bx, treeArea.getY(),
                            treeArea.getRight() - bx, treeArea.getHeight());
                    BSPNode newTop = BSPNodeCache.getBSPNode();
                    newTop.getArea().copyFrom(newArea);
                    newTop.setSplitAxis(X_AXIS);
                    newTop.setSplitPos(treeArea.getX());
                    newTop.setChild(PARENT_RIGHT, bspTree);
                    bspTree = newTop;
                    treeArea = newArea;
                }
                if (bounds.getRight() > treeArea.getRight()) {
                    // double the width out to the right
                    int bx = treeArea.getRight() + treeArea.getWidth();
                    Rect newArea = new Rect(treeArea.getX(), treeArea.getY(),
                            bx - treeArea.getX(), treeArea.getHeight());
                    BSPNode newTop = BSPNodeCache.getBSPNode();
                    newTop.getArea().copyFrom(newArea);
                    newTop.setSplitAxis(X_AXIS);
                    newTop.setSplitPos(treeArea.getRight());
                    newTop.setChild(PARENT_LEFT, bspTree);
                    bspTree = newTop;
                    treeArea = newArea;
                }
                if (bounds.getY() < treeArea.getY()) {
                    // double the height out the top
                    int by = treeArea.getY() - treeArea.getHeight();
                    Rect newArea = new Rect(treeArea.getX(), by,
                            treeArea.getWidth(), treeArea.getTop() - by);
                    BSPNode newTop = BSPNodeCache.getBSPNode();
                    newTop.getArea().copyFrom(newArea);
                    newTop.setSplitAxis(Y_AXIS);
                    newTop.setSplitPos(treeArea.getY());
                    newTop.setChild(PARENT_RIGHT, bspTree);
                    bspTree = newTop;
                    treeArea = newArea;
                }
                if (bounds.getTop() > treeArea.getTop()) {
                    // double the height out the bottom
                    int by = treeArea.getTop() + treeArea.getHeight();
                    Rect newArea = new Rect(treeArea.getX(), treeArea.getY(),
                            treeArea.getWidth(), by - treeArea.getY());
                    BSPNode newTop = BSPNodeCache.getBSPNode();
                    newTop.getArea().copyFrom(newArea);
                    newTop.setSplitAxis(Y_AXIS);
                    newTop.setSplitPos(treeArea.getTop());
                    newTop.setChild(PARENT_LEFT, bspTree);
                    bspTree = newTop;
                    treeArea = newArea;
                }
            }
            
            insertObject(actor, bounds, bounds, treeArea, bspTree);
        }
        // checkConsistency(true);
    }
    
    /**
     * Check the consistency of the tree, useful for debugging.
     */
    public void checkConsistency(boolean checkActorBounds)
    {
        if (! debugging) {
            return;
        }
        
        LinkedList<BSPNode> stack = new LinkedList<BSPNode>();
        
        stack.add(bspTree);
        while(! stack.isEmpty()) {
            BSPNode node = stack.removeLast();
            if (node != null) {
                Rect nodeArea = node.getArea();
                List<Actor> actors = node.getActorsList();
                
                for (Actor actor : actors) {
                    // The actor is really in this node
                    Rect actorBounds = getActorBounds(actor);
                    if (checkActorBounds && ! nodeArea.intersects(actorBounds)) {
                        throw new IllegalStateException("Actor not contained within region bounds?");
                    }
                }
                
                // check the same actor doesn't occur further up tree // TODO
                
                if (node.getLeft() != null) {
                    Rect leftArea = node.getLeft().getArea();
                    if (! Rect.equals(leftArea, node.getLeftArea())) {
                        throw new IllegalStateException("Areas wrong!");
                    }
                }
                
                if (node.getRight() != null) {
                    Rect rightArea = node.getRight().getArea();
                    if (! Rect.equals(rightArea, node.getRightArea())) {
                        throw new IllegalStateException("Areas wrong!");
                    }
                }
                
                stack.add(node.getLeft());
                stack.add(node.getRight());
            }
        }
    }
    
    /**
     * Insert an actor into the tree at the given position
     * 
     * @param actor   The actor to insert
     * @param actorBounds  The total bounds of the actor
     * @param bounds  The bounds of the actor (limited to the present area)
     * @param area    The total area represented by the current search node
     * @param node    The current search node (null, if the search has reached its end!)
     */
    private void insertObject(Actor actor, Rect actorBounds, Rect bounds, Rect area, BSPNode node)
    {
        // the current search node might already contain the
        // actor...
        if (node.containsActor(actor)) {
            return;
        }

        // If there's no actor at all in the node yet, then we can stop here.
        // Also, if the area is sufficiently small, there's no point subdividing it.
        if (node.isEmpty() || (area.getWidth() <= actorBounds.getWidth()
                && area.getHeight() <= actorBounds.getHeight())) {
            node.addActor(actor);
            return;
        }

        // The search continues...
        Rect leftArea = node.getLeftArea();
        Rect rightArea = node.getRightArea();

        Rect leftIntersects = Rect.getIntersection(leftArea, bounds);
        Rect rightIntersects = Rect.getIntersection(rightArea, bounds);

        if (leftIntersects != null) {
            if (node.getLeft() == null) {
                BSPNode newLeft = createNewNode(leftArea);
                newLeft.addActor(actor);
                node.setChild(PARENT_LEFT, newLeft);
            }
            else {
                insertObject(actor, actorBounds, leftIntersects, leftArea, node.getLeft());
            }
        }

        if (rightIntersects != null) {
            if (node.getRight() == null) {
                BSPNode newRight = createNewNode(rightArea);
                newRight.addActor(actor);
                node.setChild(PARENT_RIGHT, newRight);
            }
            else {
                insertObject(actor, actorBounds, rightIntersects, rightArea, node.getRight());
            }
        }
    }
    
    /**
     * Create a new node for the given area.
     */
    private BSPNode createNewNode(Rect area)
    {
        int splitAxis, splitPos;
        if (area.getWidth() > area.getHeight()) {
            splitAxis = X_AXIS;
            splitPos = area.getMiddleX();
        }
        else {
            splitAxis = Y_AXIS;
            splitPos = area.getMiddleY();
        }
        BSPNode newNode = BSPNodeCache.getBSPNode();
        newNode.setArea(area);
        newNode.setSplitAxis(splitAxis);
        newNode.setSplitPos(splitPos);
        return newNode;
    }
    
    public final Rect getActorBounds(Actor actor)
    {
        Rect r = ActorVisitor.getBoundingRect(actor);      
        return r;
    }
    
    public static void printTree(BSPNode node, String indent, String lead)
    {
        if (node == null) {
            return;
        }
        
        String xx = lead;
        xx += node + ": ";
        xx += node.getArea();
        println(xx);
        
        BSPNode left = node.getLeft();
        BSPNode right = node.getRight();
        
        if (left != null) {
            String newIndent;
            if (right != null) {
                newIndent = indent + " |";
            }
            else {
                newIndent = indent + "  ";
            }
            printTree(left, newIndent, indent + " \\L-");
        }
        
        if (right != null) {
            printTree(node.getRight(), indent + "  ", indent + " \\R-");
        }
    }
    
    public void printTree()
    {
        printTree(bspTree, "", "");
    }
    
    public void removeObject(Actor object)
    {
        // checkConsistency(true);
        ActorNode node = getNodeForActor(object);
        
        while (node != null) {
            BSPNode bspNode = node.getBSPNode();
            node.remove();
            checkRemoveNode(bspNode);
            node = getNodeForActor(object);
        }
        // checkConsistency(true);
    }
    
    /**
     * Check whether a node can be removed, and remove it if so, traversing up the
     * tree and so on. Returns the highest node which wasn't removed.
     */
    private BSPNode checkRemoveNode(BSPNode node)
    {
        // checkConsistency(false); // may be due to actor moving
        while (node != null && node.isEmpty()) {
            BSPNode parent = node.getParent();
            int side = (parent != null) ? parent.getChildSide(node) : PARENT_NONE;
            BSPNode left = node.getLeft();
            BSPNode right = node.getRight();
            if (left == null) {
                if (parent != null) {
                    if (right != null) {
                        right.getArea().copyFrom(node.getArea());
                        right.areaChanged();
                    }
                    parent.setChild(side, right);
                }
                else {
                    bspTree = right;
                    if (right != null) {
                        right.setParent(null);
                    }
                }
                node.setChild(PARENT_RIGHT, null);
                BSPNodeCache.returnNode(node);
                node = parent;
            }
            else if (right == null) {
                if (parent != null) {
                    if (left != null) {
                        left.getArea().copyFrom(node.getArea());
                        left.areaChanged();
                    }
                    parent.setChild(side, left);
                }
                else {
                    bspTree = left;
                    if (left != null) {
                        left.setParent(null);
                    }
                }
                node.setChild(PARENT_LEFT, null);
                BSPNodeCache.returnNode(node);
                node = parent;
            }
            else {
                break;
            }
        }
        
        // checkConsistency(false);
        return node;
    }
    
    private static int dbgCounter = 0;
    
    private static void println(String s)
    {
        if (dbgCounter < 3000) {
            System.out.println(s);
            // dbgCounter++;
        }
    }
        
    public static ActorNode getNodeForActor(Actor object)
    {
        return (ActorNode) ActorVisitor.getData(object);
    }
    
    public static void setNodeForActor(Actor object, ActorNode node)
    {
        ActorVisitor.setData(object, node);
    }
    
    /**
     * An actors position or size has changed - update the tree.
     */
    private void updateObject(Actor object)
    {
        // checkConsistency(false); // can't check actor bounds - actor has moved, may be inconsistent
        ActorNode node = getNodeForActor(object);
        if (node == null) {
            // It seems that this can get called before the actor is added to the
            // checker...
            return;
        }
        
        Rect newBounds = getActorBounds(object);
        if (! bspTree.getArea().contains(newBounds)) {
            // The actor has moved out of the existing tree area
            while (node != null) {
                BSPNode rNode = node.getBSPNode();
                node.remove();
                checkRemoveNode(rNode);
                node = node.getNext();
            }
            addObject(object);
            return;
        }
        
        // First process all existing actor nodes. We cull nodes which
        // no longer contain any part of the actor; also, if we find a
        // BSPNode which completely contains the actor, we just throw
        // all the other actor nodes away.
        while (node != null) {
            //updateNodeForMovedObject(object, newBounds, bspNode);
            BSPNode bspNode = node.getBSPNode();
            Rect bspArea = bspNode.getArea();
            if (bspArea.contains(newBounds)) {
                // Ok, we found a BSPNode which completely contains the
                // actor - we can throw all other actor nodes away
                ActorNode iter = getNodeForActor(object);
                while (iter != null) {
                    if (iter != node) {
                        BSPNode rNode = iter.getBSPNode();
                        iter.remove();
                        checkRemoveNode(rNode);
                    }
                    iter = iter.getNext();
                }
                return;
            }
            else if (! bspArea.intersects(newBounds)) {
                // This actor node is no longer needed
                BSPNode rNode = node.getBSPNode();
                node.remove();
                checkRemoveNode(rNode);
                
                // It's possible, when there is only one actor, that the tree is now empty:
                if (bspTree == null) {
                    addObject(object);
                    return;
                }
            }
            node.clearMark();
            node = node.getNext();
        }
        
        // If we got here, there was no single node which contained the whole
        // actor (and we have culled any nodes which no longer contain any
        // part of the actor). We now need to find a suitable BSPNode
        // and do a re-insertion.
        node = getNodeForActor(object);
        BSPNode bspNode;
        Rect bspArea;
        if (node != null) {
            bspNode = node.getBSPNode();
            while (bspNode != null && ! bspNode.getArea().contains(newBounds)) {
                bspNode = bspNode.getParent();
            }
            if (bspNode == null) {
                // No node contains the whole actor; we need to expand the tree size
                // First: remove old actor nodes
                while (node != null) {
                    bspNode = node.getBSPNode();
                    node.remove();
                    checkRemoveNode(bspNode);
                    node = node.getNext();
                }
                // Now: expand the tree
                addObject(object);
                return;
            }
        }
        else {
            bspNode = bspTree;
        }
                
        // Note, we can pass null as the parent because bspNode is guaranteed not to be null.
        bspArea = bspNode.getArea();
        insertObject(object, newBounds, newBounds, bspArea, bspNode);
        
        // Finally, it's possible the object changed size and therefore has been stored
        // in higher nodes than previously. This means there are duplicate actor nodes.
        // The insertObject call will mark all the nodes it touches, so we need remove
        // any unmarked nodes.
        node = getNodeForActor(object);
        while (node != null) {
            if (! node.checkMark()) {
                bspNode = node.getBSPNode();
                node.remove();
                checkRemoveNode(bspNode);
            }
            node = node.getNext();
        }
        
        // checkConsistency(true);
    }
    
    public void updateObjectLocation(Actor object, int oldX, int oldY)
    {
        updateObject(object);
    }

    public void updateObjectSize(Actor object)
    {
        updateObject(object);
    }

    private List<Actor> getIntersectingObjects(Rect r, CollisionQuery query)
    {
        Set<Actor> set = new HashSet<Actor>();
        getIntersectingObjects(r, query, set, bspTree);
        List<Actor> l = new ArrayList<Actor>(set);
        return l;
    }
    
    private void getIntersectingObjects(Rect r, CollisionQuery query, Set<Actor> resultSet, BSPNode startNode)
    {
        LinkedList<BSPNode> nodeStack = new LinkedList<BSPNode>();
        
        if (startNode != null) {
            nodeStack.add(startNode);
        }
        
        while (! nodeStack.isEmpty()) {
            BSPNode node = nodeStack.removeLast();
            if (node.getArea().intersects(r)) {
                Iterator<Actor> i = node.getActorsIterator();
                while (i.hasNext()) {
                    Actor actor = i.next();
                    if (query.checkCollision(actor)) {
                        if (! resultSet.contains(actor)) {
                            resultSet.add(actor);
                        }
                    }
                }
                
                BSPNode left = node.getLeft();
                BSPNode right = node.getRight();
                if (left != null) {
                    nodeStack.add(left);
                }
                if (right != null) {
                    nodeStack.add(right);
                }
            }
        }
    }
    
    /**
     * Check if there is at least one actor in the given BSPNode which matches
     * the given collision query, and return it if so.
     */
    private Actor checkForOneCollision(Actor ignore, BSPNode node, CollisionQuery query)
    {
        Iterator<Actor> i = node.getActorsIterator();
        
        while (i.hasNext()) {
            Actor candidate = i.next();
            if (ignore != candidate && query.checkCollision(candidate)) {
                return candidate;
            }
        }
        
        return null;
    }
    
    /**
     * Search for a single object which matches the given collision
     * query, starting from the given tree node and searching only
     * down the tree.
     * 
     * @param ignore - do not return this actor
     * @param r  Bounds - do not search nodes which don't intersect this
     * @param query  The query to check objects against
     * @param startNode  The node to begin the search from
     * @return  The actor found, or null
     */
    private Actor getOneObjectDownTree(Actor ignore, Rect r, CollisionQuery query, BSPNode startNode)
    {
        if (startNode == null) {
            return null;
        }
        
        LinkedList<BSPNode> nodeStack = new LinkedList<BSPNode>();
        nodeStack.add(startNode);
        
        while (! nodeStack.isEmpty()) {
            BSPNode node = nodeStack.removeLast();
            if (node.getArea().intersects(r)) {
                Actor res = checkForOneCollision(ignore, node, query);
                if (res != null) {
                    return res;
                }
                
                BSPNode left = node.getLeft();
                BSPNode right = node.getRight();
                if (left != null) {
                    nodeStack.add(left);
                }
                if (right != null) {
                    nodeStack.add(right);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Search down the tree, but only so far as the last node which fully contains the area.
     * @param r
     * @param query
     * @param actor
     * @return
     */
    private Actor getOneIntersectingDown(Rect r, CollisionQuery query, Actor actor)
    {
        if (bspTree == null) {
            return null;
        }
        
        LinkedList<BSPNode> nodeStack = new LinkedList<BSPNode>();
        nodeStack.add(bspTree);
        
        while (! nodeStack.isEmpty()) {
            BSPNode node = nodeStack.removeLast();
            if (node.getArea().contains(r)) {
                Actor res = checkForOneCollision(actor, node, query);
                if (res != null) {
                    return res;
                }
                
                BSPNode left = node.getLeft();
                BSPNode right = node.getRight();
                if (left != null) {
                    nodeStack.add(left);
                }
                if (right != null) {
                    nodeStack.add(right);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Search up the tree, up to (not including) the node which fully contains the area.
     * @param r
     * @param query
     * @param actor
     * @param start
     */
    public Actor getOneIntersectingUp(Rect r, CollisionQuery query, Actor actor, BSPNode start)
    {
        while (start != null && ! start.getArea().contains(r)) {
            Actor res = checkForOneCollision(actor, start, query);
            if (res != null) {
                return res;
            }
            start = start.getParent();
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls)
    {
        synchronized (pointQuery) {
            int px = x * cellSize + cellSize / 2;
            int py = y * cellSize + cellSize / 2;
            pointQuery.init(px, py, cls);
            return (List<T>) getIntersectingObjects(new Rect(px, py, 1, 1), pointQuery);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getIntersectingObjects(Actor actor,
            Class<T> cls)
    {
        Rect r = getActorBounds(actor);
        
        synchronized (actorQuery) {
            actorQuery.init(cls, actor);
            return (List<T>) getIntersectingObjects(r, actorQuery);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r,
            Class<T> cls)
    {
        int halfCell = cellSize / 2;
        int size = 2 * r * cellSize;
        
        Rect rect = new Rect((x - r) * cellSize + halfCell,
                (y - r) * cellSize + halfCell,
                size,
                size);
        
        List<T> result;
        synchronized (actorQuery) {
            actorQuery.init(cls, null);
            result = (List<T>) getIntersectingObjects(rect, actorQuery);
        }
        
        Iterator<T> i = result.iterator();
        synchronized (inRangeQuery) {
            inRangeQuery.init(x * cellSize + halfCell , y * cellSize + halfCell, r * cellSize);
            while (i.hasNext()) {
                if (! inRangeQuery.checkCollision(i.next())) {
                    i.remove();
                }
            }
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getNeighbours(Actor actor, int distance,
            boolean diag, Class<T> cls)
    {
        int x = ActorVisitor.getX(actor);
        int y = ActorVisitor.getY(actor);
        int xPixel = x * cellSize;
        int yPixel = y * cellSize;
        int dPixel = distance * cellSize;
        
        Rect r = new Rect(xPixel - dPixel, yPixel - dPixel, dPixel * 2 + 1, dPixel * 2 + 1);
        
        synchronized (neighbourQuery) {
            neighbourQuery.init(x, y, distance, diag, cls);
            List<T> res = (List<T>) getIntersectingObjects(r, neighbourQuery);
            return res;
        }
    }

    public <T extends Actor> List<T> getObjectsInDirection(int x, int y,
            int angle, int length, Class<T> cls)
    {
        // non-functional
        return new ArrayList<T>();
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjects(Class<T> cls)
    {
        Set<T> set = new HashSet<T>();
        LinkedList<BSPNode> nodeStack = new LinkedList<BSPNode>();
        
        if (bspTree != null) {
            nodeStack.add(bspTree);
        }
        
        while (! nodeStack.isEmpty()) {
            BSPNode node = nodeStack.removeLast();
            Iterator<Actor> i = node.getActorsIterator();
            while (i.hasNext()) {
                Actor actor = i.next();
                if (cls == null || cls.isInstance(actor)) {
                    set.add((T) actor);
                }
            }
            BSPNode left = node.getLeft();
            BSPNode right = node.getRight();
            if (left != null) {
                nodeStack.add(left);
            }
            if (right != null) {
                nodeStack.add(right);
            }
        }
        
        List<T> list = new ArrayList<T>(set);
        return list;
    }

    public List<Actor> getObjectsList()
    {
        return getObjects(null);
    }

    public final void startSequence()
    {
        // Nothing necessary.
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> T getOneObjectAt(Actor object, int dx, int dy,
            Class<T> cls)
    {
        synchronized (pointQuery) {
            int px = dx * cellSize + cellSize / 2;
            int py = dy * cellSize + cellSize / 2;
            pointQuery.init(px, py, cls);
            CollisionQuery query = pointQuery;
            if (cls != null) {
                query = new ClassQuery(cls, pointQuery);
            }
            // Use of getOneIntersectingDown is ok, because the area is only 1x1 pixel
            // in size - it will be contained by all nodes.
            return (T) getOneIntersectingDown(new Rect(px, py, 1, 1), query, object);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> T getOneIntersectingObject(Actor actor, Class<T> cls)
    {
        Rect r = getActorBounds(actor);
        synchronized (actorQuery) {
            actorQuery.init(cls, actor);
            
            ActorNode node = getNodeForActor(actor);
            do {
                BSPNode bspNode = node.getBSPNode();
                T ret = (T) getOneObjectDownTree(actor, r, actorQuery, bspNode);
                if (ret != null) {
                    return ret;
                }
                ret = (T) getOneIntersectingUp(r, actorQuery, actor, bspNode.getParent());
                if (ret != null) {
                    return ret;
                }
                node = node.getNext();
            }
            while (node != null);
            return (T) getOneIntersectingDown(r, actorQuery, actor);
        }
    }

    public void paintDebug(Graphics g)
    {
        LinkedList<BSPNode> nodeStack = new LinkedList<BSPNode>();
        nodeStack.add(bspTree);
        
        Color oldColor = g.getColor();
        //g.setColor(Color.YELLOW);
        //g.setColor(Color.WHITE);
        g.setColor(Color.RED);
        
        while (! nodeStack.isEmpty()) {
            BSPNode node = nodeStack.removeLast();
            if (node != null) {
                Rect area = node.getArea();
                g.drawRect(area.getX(), area.getY(), area.getWidth(), area.getHeight());
                nodeStack.add(node.getLeft());
                nodeStack.add(node.getRight());
            }
        }
        
        g.setColor(oldColor);
    }

}
