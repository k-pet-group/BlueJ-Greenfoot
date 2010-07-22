/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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

    /* (non-Javadoc)
     * @see greenfoot.collision.CollisionChecker#addObject(greenfoot.Actor)
     */
    public void addObject(Actor actor)
    {
        // checkConsistency();
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
            Rect nbounds = new Rect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
            bspTree = new BSPNode(nbounds, splitAxis, splitPos);
            bspTree.addActor(actor);
        }
        else {
            Rect treeArea = bspTree.getArea();
            if (treeArea.contains(bounds)) {
                insertObject(actor, bounds, bounds, treeArea, null, bspTree, PARENT_NONE);
            }
            else {
                // Our current tree won't contain the actor; need to expand the tree, up to
                // four times...
                if (bounds.getX() < treeArea.getX()) {
                    Rect newArea = new Rect(bounds.getX(), treeArea.getY(),
                            treeArea.getRight() - bounds.getX(), treeArea.getHeight());
                    BSPNode newTop = new BSPNode(newArea, X_AXIS, treeArea.getX());
                    newTop.setChild(PARENT_RIGHT, bspTree);
                    bspTree = newTop;
                    treeArea = newArea;
                }
                if (bounds.getRight() > treeArea.getRight()) {
                    Rect newArea = new Rect(treeArea.getX(), treeArea.getY(),
                            bounds.getRight() - treeArea.getX(), treeArea.getHeight());
                    BSPNode newTop = new BSPNode(newArea, X_AXIS, treeArea.getRight());
                    newTop.setChild(PARENT_LEFT, bspTree);
                    bspTree = newTop;
                    treeArea = newArea;
                }
                if (bounds.getY() < treeArea.getY()) {
                    Rect newArea = new Rect(treeArea.getX(), bounds.getY(),
                            treeArea.getWidth(), treeArea.getTop() - bounds.getY());
                    BSPNode newTop = new BSPNode(newArea, Y_AXIS, treeArea.getY());
                    newTop.setChild(PARENT_RIGHT, bspTree);
                    bspTree = newTop;
                    treeArea = newArea;
                }
                if (bounds.getTop() > treeArea.getTop()) {
                    Rect newArea = new Rect(treeArea.getX(), treeArea.getY(),
                            treeArea.getWidth(), bounds.getTop() - treeArea.getY());
                    BSPNode newTop = new BSPNode(newArea, Y_AXIS, treeArea.getTop());
                    newTop.setChild(PARENT_LEFT, bspTree);
                    bspTree = newTop;
                    treeArea = newArea;
                }
                bspTree.addActor(actor);
            }
        }
        // checkConsistency();
    }
    
    /**
     * Check the consistency of the tree, useful for debugging.
     */
    /*
    public void checkConsistency()
    {
        if (! debugging) {
            return;
        }
        
        LinkedList<BSPNode> stack = new LinkedList<BSPNode>();
        
        stack.add(bspTree);
        while(! stack.isEmpty()) {
            BSPNode node = stack.removeLast();
            if (node != null) {
                //Actor actor = node.getActor();
                //Rect actorBounds = getActorBounds(actor);
                Rect nodeArea = node.getArea();
                //if (movingActor != actor && Rect.getIntersection(actorBounds, nodeArea) == null) {
                //    println("Node doesn't contain part of actor!");
                //    throw new IllegalStateException();
                //}
                
                // check the same actor doesn't occur further up tree
                BSPNode p = node.getParent();
                while (p != null) {
                    if (p.getActor() == actor) {
                        println("Actor " + actor + " occurs further up tree! node=" + node);
                        throw new IllegalStateException();
                    }
                    p = p.getParent();
                }
                
                stack.add(node.getLeft());
                stack.add(node.getRight());
            }
        }
    }
    */
    
    /**
     * Insert an actor into the tree at the given position
     * 
     * @param actor   The actor to insert
     * @param actorBounds  The total bounds of the actor
     * @param bounds  The bounds of the actor (limited to the present area)
     * @param area    The total area represented by the current search node
     * @param parent  The parent node of the current search node
     * @param node    The current search node (null, if the search has reached its end!)
     * @param parentSide  The side of the parent we should insert on
     */
    private BSPNode insertObject(Actor actor, Rect actorBounds, Rect bounds, Rect area, BSPNode parent, BSPNode node, int parentSide)
    {
        if (node == null) {
            // This is the end of the search
            if (area.getHeight() > area.getWidth()) {
                int middle = area.getMiddleY();
                //middle = Math.min(middle, bounds.getTop());
                //middle = Math.max(middle, bounds.getY());
                node = new BSPNode(area, Y_AXIS, middle);
            }
            else {
                int middle = area.getMiddleX();
                //middle = Math.min(middle, bounds.getRight());
                //middle = Math.max(middle, bounds.getX());
                node = new BSPNode(area, X_AXIS, middle);
            }
            
            node.addActor(actor);
            if (parent == null) {
                bspTree = node;
            }
            else {
                parent.setChild(parentSide, node);
            }
            return node;
        }
        else {
            // the current search node might already contain the
            // actor...
            
            if (node.containsActor(actor)) {
                return node;
            }
            
            if (node.isEmpty() || (area.getWidth() <= actorBounds.getWidth()
                    && area.getHeight() <= actorBounds.getHeight())) {
                node.addActor(actor);
                return node;
            }
            
            // The search continues...
            Rect leftArea = node.getLeftArea();
            Rect rightArea = node.getRightArea();
            
            BSPNode lnode = null;
            
            Rect leftIntersects = Rect.getIntersection(leftArea, bounds);
            Rect rightIntersects = Rect.getIntersection(rightArea, bounds);

            if (leftIntersects != null) {
                lnode = insertObject(actor, actorBounds, leftIntersects, leftArea, node, node.getLeft(), PARENT_LEFT);
            }
            
            if (rightIntersects != null) {
                BSPNode rnode = insertObject(actor, actorBounds, rightIntersects, rightArea, node, node.getRight(), PARENT_RIGHT);
                if (lnode == null) {
                    return rnode;
                }
            }
            
            return lnode;
        }
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
        // checkConsistency();
        ActorNode node = getNodeForActor(object);
        
        while (node != null) {
            BSPNode bspNode = node.getBSPNode();
            node.remove();
            checkRemoveNode(bspNode);
            node = getNodeForActor(object);
        }
        // checkConsistency();
    }
    
    /**
     * Check whether a node can be removed, and remove it if so, traversing up the
     * tree and so on. Returns the highest node which wasn't removed.
     */
    private BSPNode checkRemoveNode(BSPNode node)
    {
        while (node != null && node.isEmpty()) {
            BSPNode parent = node.getParent();
            int side;
            if (parent != null) {
                side = parent.getChildSide(node);
            }
            else {
                side = PARENT_NONE;
            }
            BSPNode left = node.getLeft();
            BSPNode right = node.getRight();
            if (left == null) {
                if (parent != null) {
                    parent.setChild(side, right);
                }
                else {
                    bspTree = right;
                }
                node = parent;
            }
            else if (right == null) {
                if (parent != null) {
                    parent.setChild(side, left);
                }
                else {
                    bspTree = left;
                }
                node = parent;
            }
            else {
                break;
            }
        }
        
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
        //checkConsistency();
        ActorNode node = getNodeForActor(object);
        if (node == null) {
            // It seems that this can get called before the actor is added to the
            // checker...
            return;
        }
        
        if (bspTree == null) {
            addObject(object);
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
            else if (Rect.getIntersection(bspArea, newBounds) == null) {
                // This actor node is no longer needed
                BSPNode rNode = node.getBSPNode();
                node.remove();
                checkRemoveNode(rNode);
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
        insertObject(object, newBounds, newBounds, bspArea, null, bspNode, PARENT_NONE);
        
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
        
        // checkConsistency();
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
            if (Rect.getIntersection(node.getArea(), r) != null) {
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
            if (Rect.getIntersection(node.getArea(), r) != null) {
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
            pointQuery.init(x, y, cls);
            return (List<T>) getIntersectingObjects(new Rect(x * cellSize, y * cellSize, cellSize, cellSize), pointQuery);
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
        int x = actor.getX();
        int y = actor.getY();
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
            pointQuery.init(dx, dy, cls);
            CollisionQuery query = pointQuery;
            if (cls != null) {
                query = new ClassQuery(cls, pointQuery);
            }
            // Use of getOneIntersectingDown is ok, because the area is only 1x1 pixel
            // in size - it will be contained by all nodes.
            return (T) getOneIntersectingDown(new Rect(dx * cellSize + cellSize / 2, dy * cellSize + cellSize / 2, 1, 1), query, object);
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
