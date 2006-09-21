package greenfoot.collision.ibsp;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.collision.*;

import java.awt.Color;
import java.awt.Graphics;
import java.util.*;


public class IBSPColChecker implements CollisionChecker
{
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    
    public static final int PARENT_LEFT = 0;
    public static final int PARENT_RIGHT = 1;
    public static final int PARENT_NONE = 3; // no particular side
    
    private GOCollisionQuery actorQuery = new GOCollisionQuery();
    private NeighbourCollisionQuery neighbourQuery = new NeighbourCollisionQuery();
    private PointCollisionQuery pointQuery = new PointCollisionQuery();
    
    private int cellSize;
    
    private BSPNode bspTree;
    private Rect allArea = new Rect(0 - Integer.MAX_VALUE / 2, 0 - Integer.MAX_VALUE / 2, Integer.MAX_VALUE, Integer.MAX_VALUE);
    
    /** the node scheduled to be re-balanced */
    private BSPNode rebalanceNode;
    
    public static boolean debugging = false;
    
    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        // instance = this;
        this.cellSize = cellSize;
    }

    public void addObject(Actor actor)
    {
        checkConsistency();
        Rect bounds = getActorBounds(actor);
        insertObject(actor, bounds, bounds, allArea, null, bspTree, PARENT_NONE);
        checkConsistency();
    }
    
    /**
     * Check the consistency of the tree, useful for debugging.
     */
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
//                BSPNode p = node.getParent();
//                while (p != null) {
//                    if (p.getActor() == actor) {
//                        println("Actor " + actor + " occurs further up tree! node=" + node);
//                        throw new IllegalStateException();
//                    }
//                    p = p.getParent();
//                }
                
                stack.add(node.getLeft());
                stack.add(node.getRight());
            }
        }
    }
    
    /**
     * Insert an actor into the tree at the given positio
     * 
     * @param actor   The actor to insert
     * @param actorBounds  The total bounds of the actor
     * @param bounds  The bounds of the actor (limited to the present area)
     * @param area    The total area represented by the current search node
     * @param axis    The axis to divide by when we insert a new node
     * @param parent  The parent node of the current search node
     * @param node    The current search node
     * @param parentSide  The side of the parent we should insert on
     */
    private BSPNode insertObject(Actor actor, Rect actorBounds, Rect bounds, Rect area, BSPNode parent, BSPNode node, int parentSide)
    {
        if (node == null) {
            // This is the end of the search
            if (area.getHeight() > area.getWidth()) {
                int middle = area.getMiddleY();
                middle = Math.min(middle, bounds.getTop());
                middle = Math.max(middle, bounds.getY());
                node = new BSPNode(area, Y_AXIS, middle);
            }
            else {
                int middle = area.getMiddleX();
                middle = Math.min(middle, bounds.getRight());
                middle = Math.max(middle, bounds.getX());
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
            
            if (rebalanceNode == null && node.needsRebalance()) {
                rebalanceNode = node;
            }
            
            if (node.containsActor(actor)) {
                return node;
            }
            
            if (area.getWidth() <= actorBounds.getWidth() && area.getHeight() <= actorBounds.getHeight()) {
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
    
    /**
     * Re-balance a node in the tree. Should only call this if the node
     * actually needs rebalancing.
     */
    public void rebalance(BSPNode node)
    {
        List<Actor> actorsInNode = node.getActorsList();
        
        // remove actually does the rebalance...
        node.clear();
        BSPNode newNode = removeNode(node);

        for (Actor actor : actorsInNode) {
            Rect actorBounds = getActorBounds(actor);
            insertObject(actor, actorBounds, actorBounds, allArea, null, bspTree, PARENT_NONE);
        }
    }
    
    public Rect getActorBounds(Actor actor)
    {
        int halfCell = cellSize / 2;
        int xpos = actor.getX() * cellSize + halfCell;
        int ypos = actor.getY() * cellSize + halfCell;
        int width = actor.getWidth() * cellSize;
        int height = actor.getHeight() * cellSize;
        int left = xpos - width / 2;
        int top = ypos - height / 2;
        return new Rect(left, top, width, height);
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
        checkConsistency();
        ActorNode node = getNodeForActor(object);
        
        while (node != null) {
            BSPNode bspNode = node.getBSPNode();
            node.remove();
            if (bspNode.isEmpty()) {
                removeNode(bspNode, object);
            }
            node = getNodeForActor(object);
        }
        checkConsistency();
    }
    
    private static int dbgCounter = 0;
    
    private static void println(String s)
    {
        if (dbgCounter < 3000) {
            System.out.println(s);
            // dbgCounter++;
        }
    }
    
    private BSPNode removeNode(BSPNode node)
    {
        return removeNode(node, null);
    }
    
    /**
     * Remove a node from the tree, and distribute the children
     * of the node amongst the tree in an (hopefully) efficient
     * manner.
     * 
     * Only call this if the node to be removed is empty.
     */
    private BSPNode removeNode(BSPNode node, Actor removedActor)
    {
        // DEBUG
        if (! node.isEmpty()) {
            throw new IllegalArgumentException();
        }
        
        if (rebalanceNode == node) {
            rebalanceNode = null;
        }
        
        checkConsistency();
        BSPNode parent = node.getParent();
        BSPNode left = node.getLeft();
        BSPNode right = node.getRight();
        
        if (parent == null) {
            if (left == null) {
                bspTree = right;
                if (bspTree != null) {
                    bspTree.setParent(null);
                    bspTree.setArea(allArea);
                }
                checkConsistency();
            }
            else if (right == null) {
                bspTree = left;
                bspTree.setParent(null);
                bspTree.setArea(allArea);
            }
            else {
                int leftDepth = node.getLeftDepth();
                int rightDepth = node.getRightDepth();
                // we aim to re-drop the smallest number of nodes
                // (could try the other way...)
                if (leftDepth > rightDepth) {
                    bspTree = left;
                    bspTree.setParent(null);
                    bspTree.setArea(allArea);
                    dropNode(right, left, removedActor);
                }
                else {
                    bspTree = right;
                    bspTree.setParent(null);
                    bspTree.setArea(allArea);
                    dropNode(left, right, removedActor);
                }
                checkConsistency();
            }
            return bspTree;
        }
        else {
            int side = parent.getChildSide(node);
            if (left == null) {
                parent.setChild(side, right);
                checkConsistency();
                return right;
            }
            else {
                int leftDepth = node.getLeftDepth();
                int rightDepth = node.getRightDepth();
                // we aim to re-drop the smallest number of nodes
                // (could try the other way...)
                if (leftDepth > rightDepth) {
                    parent.setChild(side, left);
                    dropNode(right, left, removedActor);
                    checkConsistency();
                    return left;
                }
                else {
                    parent.setChild(side, right);
                    dropNode(left, right, removedActor);
                    checkConsistency();
                    return right;
                }
            }
        }
    }
    
    /**
     * @param node   The node to drop
     * @param parent  The new parent node
     */
    private void dropNode(BSPNode node, BSPNode parent, Actor removedActor)
    {
        if (node == null) {
            return;
        }
                
        // First go down the tree as far as possible to find
        // a more suitable parent node
        boolean sifting = true;
        while (sifting) {
            
            Iterator<Map.Entry<Actor, ActorNode>> i = node.getEntriesIterator();
            
            // Remove actors which are in the parent from the current node
            while (i.hasNext()) {
                Map.Entry<Actor, ActorNode> entry = i.next();
                if (parent.containsActor(entry.getKey())) {
                    // entry.getValue().remove();
                    i.remove();
                    entry.getValue().removed();
                }
            }
            
            if (node.isEmpty()) {
                // we can remove a node
                if (node == rebalanceNode) {
                    rebalanceNode = null;
                }
                dropNode(node.getLeft(), parent, removedActor);
                dropNode(node.getRight(), parent, removedActor);
                checkConsistency();
                return;
            }
            
            if (parent.getLeftArea().contains(node.getArea())) {
                BSPNode newParent = parent.getLeft();
                if (newParent == null) {
                    parent.setChild(PARENT_LEFT, node);
                    checkConsistency();
                    return;
                }
                parent = newParent;
            }
            else if (parent.getRightArea().contains(node.getArea())) {
                BSPNode newParent = parent.getRight();
                if (newParent == null) {
                    parent.setChild(PARENT_RIGHT, node);
                    checkConsistency();
                    return;
                }
                parent = newParent;
            }
            else {
                sifting = false;
            }
        }
        
        Rect areaBefore = node.getArea();
        
        Iterator<Map.Entry<Actor, ActorNode>> i = node.getEntriesIterator();
        
        while (i.hasNext()) {
            Map.Entry<Actor, ActorNode> entry = i.next();
            Actor actor = entry.getKey();
            
            if (actor == removedActor) {
                i.remove();
                entry.getValue().removed();
                if (node.isEmpty()) {
                    break;
                }
                continue;
            }
            
            Rect actorBounds = getActorBounds(actor);
            Rect parentTotal = parent.getArea();
            BSPNode newNode = insertObject(actor, actorBounds, actorBounds, parentTotal, parent.getParent(), parent, PARENT_NONE);
            
            if (newNode != null) {
                // Ok, we successfully moved a single actor
                Rect newArea = newNode.getArea();
                if (newArea.contains(areaBefore)) {
                    if (newNode.getLeft() == null && newNode.getRight() == null && newNode.numberActors() == 1) {
                        // Just remove the new node and replace it with the old one
                        BSPNode newNodeParent = newNode.getParent();
                        if (newNodeParent == null) {
                            bspTree = node;
                            bspTree.setArea(allArea);
                        }
                        else {
                            int side = newNodeParent.getChildSide(newNode);
                            newNode.removeActor(actor);
                            newNodeParent.setChild(side, node);
                        }
                        return;
                    }
                    // minor optimisation - as the new node area contains the
                    // old node area, it's safe to drop the children at the
                    // new node instead of its parent
                    parent = newNode;
                }
            }
            
            i.remove();
            entry.getValue().removed();
        }
        
        dropNode(node.getLeft(), parent, removedActor);
        dropNode(node.getRight(), parent, removedActor);
    }
        
    public static ActorNode getNodeForActor(Actor object)
    {
        return (ActorNode) ActorVisitor.getData(object);
    }
    
    public static void setNodeForActor(Actor object, ActorNode node)
    {
        ActorVisitor.setData(object, node);
    }
    
    private void updateObject(Actor object)
    {
        checkConsistency();
        Rect newBounds = getActorBounds(object);
        
        ActorNode node = getNodeForActor(object);
        while (node != null) {
            // make sure we process all nodes, but not removed
            // ones. The node list may be modified during this
            // process, but it's ok: new nodes will only be added
            // at the front of the list, and also, if the current
            // node is removed,the "next" pointer is still valid.
            BSPNode bspNode = node.getBSPNode();
            updateNodeForMovedObject(object, newBounds, bspNode);
            node = node.getNext();
        }
        checkConsistency();
    }
    
    public void updateObjectLocation(Actor object, int oldX, int oldY)
    {
        updateObject(object);
    }
    
    /**
     * @param actor      The actor which moved
     * @param newBounds  The actor's new bounds
     * @param node       The node to be updated
     */
    private void updateNodeForMovedObject(Actor actor, Rect newBounds, BSPNode node)
    {
        Rect nodeArea = node.getArea();
        if (nodeArea.contains(newBounds)) {
            return;
        }
        
        if (Rect.getIntersection(newBounds, nodeArea) == null) {
            BSPNode parent = node.getParent();
            checkConsistency();
            node.removeActor(actor);
            if (node.isEmpty()) {
                removeNode(node);
            }
            checkConsistency();
            
            do {
                if (parent == null) {
                    insertObject(actor, newBounds, newBounds, allArea, null, bspTree, PARENT_NONE);
                    checkConsistency();
                    return;
                }
                nodeArea = parent.getArea();
                node = parent;
                parent = parent.getParent();
            } while (! nodeArea.contains(newBounds));
            
            checkConsistency();
            insertObject(actor, newBounds, newBounds, nodeArea, parent, node, PARENT_NONE);
            checkConsistency();
            return;
        }
        else {
            // Final case. The actor remains in this area but may also have
            // entered another.
            BSPNode parent = node.getParent();
            
            do {
                if (parent == null) {
                    insertObject(actor, newBounds, newBounds, allArea, null, bspTree, PARENT_NONE);
                    checkConsistency();
                    return;
                }
                node = parent;
                nodeArea = node.getArea();
                parent = parent.getParent();
            } while (! nodeArea.contains(newBounds));
            
            insertObject(actor, newBounds, newBounds, nodeArea, parent, node, PARENT_NONE);
            checkConsistency();
            return;
        }
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
    private Actor checkForOneCollision(BSPNode node, CollisionQuery query)
    {
        Iterator<Actor> i = node.getActorsIterator();
        
        while (i.hasNext()) {
            Actor candidate = i.next();
            if (query.checkCollision(candidate)) {
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
     * @param r  Bounds - do not search nodes which don't intersect this
     * @param query  The query to check objects against
     * @param startNode  The node to begin the search from
     * @return  The actor found, or null
     */
    private Actor getOneObjectDownTree(Rect r, CollisionQuery query, BSPNode startNode)
    {
        if (startNode == null) {
            return null;
        }
        
        LinkedList<BSPNode> nodeStack = new LinkedList<BSPNode>();
        nodeStack.add(startNode);
        
        while (! nodeStack.isEmpty()) {
            BSPNode node = nodeStack.removeLast();
            if (Rect.getIntersection(node.getArea(), r) != null) {
                Actor res = checkForOneCollision(node, query);
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
     * Get one object which matches the given query, using the given
     * node as a guess for a starting point.
     * 
     * @param r    The bounding rectangle of the search
     * @param query    The query to check against
     * @param startNode   The node we guess is most likely to match
     * @return  An actor which matches the query, or null if none
     *          can be found
     */
    private Actor getOneIntersectingObject(Rect r, CollisionQuery query, BSPNode startNode)
    {
        BSPNode rootNode = startNode;
        
        if (startNode != null) {
            Actor res = getOneObjectDownTree(r, query, startNode);
            if (res != null) {
                return res;
            }
            
            int rootSide = PARENT_NONE;
            while (! rootNode.getArea().contains(r)) {
                res = checkForOneCollision(rootNode, query);
                if (res != null) {
                    return res;
                }
                BSPNode rootParent = rootNode.getParent();
                rootSide = rootParent.getChildSide(rootNode);
                rootNode = rootNode.getParent();
            }
            
            if (rootSide != PARENT_NONE) {
                res = checkForOneCollision(rootNode, query);
                if (res != null) {
                    return res;
                }
                // whichever side we came up, we want to go down the
                // other side.
                if (rootSide == PARENT_LEFT) {
                    rootNode = rootNode.getRight();
                }
                else {
                    rootNode = rootNode.getLeft();
                }
                return getOneObjectDownTree(r, query, rootNode);
            }
        }
        
        return null;
    }
    
    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls)
    {
        synchronized (pointQuery) {
            pointQuery.init(x, y, cls);
            return (List<T>) getIntersectingObjects(new Rect(x * cellSize, y * cellSize, cellSize, cellSize), pointQuery);
        }
    }

    public <T extends Actor> List<T> getIntersectingObjects(Actor actor,
            Class<T> cls)
    {
        Rect r = getActorBounds(actor);
        
        synchronized (actorQuery) {
            actorQuery.init(cls, actor);
            return (List<T>) getIntersectingObjects(r, actorQuery);
        }
    }

    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r,
            Class<T> cls)
    {
        int halfCell = cellSize / 2;
        int size = 2 * r * cellSize;
        
        Rect rect = new Rect((x - r) * cellSize + halfCell,
                (y - r) * cellSize + halfCell,
                size,
                size);
        synchronized (actorQuery) {
            actorQuery.init(cls, null);
            return (List<T>) getIntersectingObjects(rect, actorQuery);
        }
    }

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

    public void startSequence()
    {
        if (rebalanceNode != null) {
            if (rebalanceNode.needsRebalance()) {
                rebalance(rebalanceNode);
                // rebalanceNode will be cleared automatically, and
                // it might set a new node to be rebalanced.
            }
            else {
                rebalanceNode = null;
            }
        }
    }

    public <T extends Actor> T getOneObjectAt(Actor object, int dx, int dy,
            Class<T> cls)
    {
        synchronized (pointQuery) {
            pointQuery.init(dx, dy, cls);
            CollisionQuery query = pointQuery;
            if (cls != null) {
                query = new ClassQuery(cls, pointQuery);
            }
            return (T) getOneIntersectingObject(new Rect(dx, dy, 1, 1), query, bspTree);
        }
    }

    public Actor getOneIntersectingObject(Actor actor, Class cls)
    {
        Rect r = getActorBounds(actor);
        synchronized (actorQuery) {
            actorQuery.init(cls, actor);
            
            ActorNode node = getNodeForActor(actor);
            do {
                Actor ret = getOneIntersectingObject(r, actorQuery, node.getBSPNode());
                if (ret != null) {
                    return ret;
                }
                node = node.getNext();
            }
            while (node != null);
            return null;
        }
    }

    public void paintDebug(Graphics g)
    {
        LinkedList<BSPNode> nodeStack = new LinkedList<BSPNode>();
        nodeStack.add(bspTree);
        
        Color oldColor = g.getColor();
        g.setColor(Color.YELLOW);
        
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
