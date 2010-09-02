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
package greenfoot.collision;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.util.Circle;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * This collision checker is based on a Bounding Volume Hierarchy formed by
 * circles. The tree is build by insertion.
 * <p>
 * 
 * Some of the good properties of this:
 * <ul>
 * <li>On-line, which means it is cheap to insert and remove objects.</li>
 * <li>Good for many kinds of object distributions.</li>
 * <li>Moderate tree construction time</li>
 * <li> </li>
 * </ul>
 * 
 * Some of the bad properties of this:
 * <ul>
 * <li>Only decent for strictly cell based scenarios.</li>
 * </ul>
 * 
 * <p>
 * 
 * This implementation is based on the Balltree On-line Insertion algorithm
 * described in: <a
 * href="http://www.icsi.berkeley.edu/ftp/global/pub/techreports/1989/tr-89-063.pdf">Five
 * Balltree Construction Algorithms by Stephen M. Omohundro</a>
 * 
 * @author Poul Henriksen
 * 
 */
public class BVHInsChecker
    implements CollisionChecker
{

    class Node
    {
        public Node parent;
        public Node left; // child
        public Node right; // child
        public Circle circle;
        private Actor actor;

        public Node(Circle circle)
        {
            this.circle = circle;
        }

        public Node()
        {
            circle = new Circle();
        }

        public Node(Circle circle, Actor actor)
        {
            if(actor == null) {
                throw new NullPointerException("Actor may not be null.");
            }
            this.circle = circle;
            this.actor = actor;
        }

        public Actor getActor()
        {
            return actor;
        }

        public boolean isLeaf()
        {
            return (left == null && right == null);
        }

        /**
         * Get collisions with the circle b and then use the specific collision
         * checker for the Actors to make low-level collision check.
         * 
         * @param c
         *            Circle to find intersections with
         * @param checker
         *            Optional collision checker. If null it will just find
         *            collisions based on the circle.
         * @param result
         *            List to put the result in.
         */
        public void getIntersections(Circle c, CollisionQuery checker, List<Actor> result)
        {
            if (!c.intersects(this.circle)) {
                return;
            }
                if (isLeaf() && (checker != null && checker.checkCollision(getActor()))) {
                    result.add(getActor());
                }
                else if (!isLeaf()) {
                    left.getIntersections(c, checker, result);
                    right.getIntersections(c, checker, result);
                }
        }

        private Actor getOneIntersectingObject(Circle c, CollisionQuery checker)
        {
            return getOneIntersectingObjectUpwards(c, checker);
        }

        private Actor getOneIntersectingObjectDownwards(Circle c, CollisionQuery checker)
        {

            if (!c.intersects(this.circle)) {
                return null;
            }
            if (isLeaf() && (checker != null && checker.checkCollision(getActor()))) {
                return getActor();
            }
            else if (!isLeaf()) {
                // TODO maybe decide which one to traverse first based on size,
                // distance of/between circles
                // maybe calculate the "insideness" of two circles as dist/r
                // where dist is the distance
                // between centers of the two circles and r is the largest of
                // the two circles' radius'.
                // Insideness should probably be r/dist to get higher value for
                // better.
                Actor res = left.getOneIntersectingObjectDownwards(c, checker);
                if (res != null) {
                    return res;
                }
                else {
                    return right.getOneIntersectingObjectDownwards(c, checker);
                }
            }
            return null;
        }

        /**
         * Searches for intersections with the circle c, starting from this node and going upwards in the tree.
         * @param c Circle to check collision against
         * @param checker Query to do fine grained checks
         * @return
         */
        private Actor getOneIntersectingObjectUpwards(Circle c, CollisionQuery checker)
        {
            Node sibling = getSibling();
            Actor result = null;
            if (sibling != null) {
                result = sibling.getOneIntersectingObjectDownwards(sibling.circle, checker);
            }

            if (result == null && parent != null) {
                return parent.getOneIntersectingObjectUpwards(c, checker);
            }
            else if (result != null) {
                return result;
            }
            return null;
        }

        /**
         * Removes this node from the tree it is in by clearing all pointers to
         * and from this node. Resets all fields in this node and any pointers
         * from parent or children. The node will have the same state as when it
         * was created.
         */
        public void reset()
        {
            if (left != null && left.parent == this) {
                left.parent = null;
            }
            if (right != null && right.parent == this) {
                right.parent = null;
            }
            if (parent != null) {
                if (parent.left == this) {
                    parent.left = null;
                }
                else if (parent.right == this) {
                    parent.right = null;
                }
            }
            parent = null;
            left = null;
            right = null;
        }

        private Node getSibling()
        {
            if (parent != null) {
                if (parent.left == this) {
                    return parent.right;
                }
                else {
                    return parent.left;
                }
            }
            return null;
        }

    }

    /**
     * A circle fringe represents extra information calculated about a node in a
     * tree. In particular the "ancestor expansion" which is the total increase
     * in the size of the ancestor when insert a new node as a sibling to this
     * node. 
     * 
     * @author Poul Henriksen
     * 
     */
    static class CircleFringe
        implements Comparable<CircleFringe>
    {
        /** The sibling node for this circle fring e*/
        private Node node;
        /** Total expansion of all the ancestors of node */
        private double ancestorExpansion;
        /** Volume of the new parent for the sibling node and the new node */         
        private double volume;

        public CircleFringe(Node n, double ancestorExpansion, double volume)
        {
            this.ancestorExpansion = ancestorExpansion;
            this.volume = volume;
            this.node = n;
        }

        /**
         * Will create a new fringe with the same contents as the other fringe.
         * 
         * @param other
         */
        public CircleFringe(CircleFringe other)
        {
            copyValuesFrom(other);
        }

        public void copyValuesFrom(CircleFringe other) {
            node = other.getNode();
            ancestorExpansion = other.getAncestorExpansion();
            volume = other.getVolume();
        }
        
        /**
         * Get total increase in the size of the ancestor when insert a new node
         * as a sibling to this node.
         */
        public double getAncestorExpansion()
        {
            return ancestorExpansion;
        }

        /**
         * Set total increase in the size of the ancestor when insert a new node
         * as a sibling to this node.
         */
        public void setAncestorExpansion(double ancestorExpansion)
        {
            this.ancestorExpansion = ancestorExpansion;
        }

        public int compareTo(CircleFringe other)
        {
            return (int) (this.ancestorExpansion - other.getAncestorExpansion());
        }

        /**
         * The node that this fringe relates to.
         */
        public Node getNode()
        {
            return node;
        }

        /**
         * The node that this fringe relates to.
         */
        public void setNode(Node node)
        {
            this.node = node;
        }

        /**
         * Set the volume of this fringe. That is, the size of the new parent
         * from inserting the new node as a sibling to this node.
         * 
         */
        public void setVolume(double volume)
        {
            this.volume = volume;
        }

        /**
         * Get the volume of this fringe. That is, the size of the new parent
         * from inserting the new node as a sibling to this node.
         * 
         */
        public double getVolume()
        {
            return volume;
        }
        
        /**
         * Return the cost of this fringe. That is: volume + ancestor expansion
         * @return
         */
        public double getCost() {
            return ancestorExpansion + volume;
        }

    }

    /**
     * Tree of circles. The leaf nodes contains the objects. Each objects has a
     * bounding circle. The parent of the two nodes has a bounding circle which
     * is the bounding circle of the two child circles.
     * 
     * @author Poul Henriksen
     * 
     */
    class CircleTree
    {
        private Node root;
        private int size;
        // where did we insert a node the last time? This can speed up insertion
        // when iterating thorugh the world from one end to the other. Have to
        // make sure that this node is not deleted from the tree in the
        // meantime though.
        private Node lastInsertionPoint; 
        
        public void addNode(Node n, Node bestGuess)
        {
            Node sibling = bestSibling(n, bestGuess);
            insertAtNode(n, sibling);
        }

        public void addNode(Node n)
        {            
            //The last insertion point could have been removed
            if (! contains(lastInsertionPoint)) {
                lastInsertionPoint = null;
            }           
            
            Node sibling = bestSibling(n, lastInsertionPoint);            
            insertAtNode(n, sibling);
            lastInsertionPoint = n.getSibling();            
        }

        private boolean contains(Node n)
        {
            if (n == null) {
                return false;
            }            
            if (root == n || n.parent != null) {
                return true;
            }
            return false;
        }

        /**
         * Find the best place to insert the new node.
         */
        public Node bestSibling(Node newNode, Node bestGuess)
        {
            if (getRoot() == null) {
                return null;
            }

            if(root.isLeaf()) {
                return root;
            }            
            
            CircleFringe rootFringe = createFringe(newNode, root);            

            // Lets set the initial best one to be the root
            final CircleFringe best = new CircleFringe(rootFringe);   
            
            // There is a good chance that bestGuess will have a better cost
            // than the root, so we use this to get an initial good value for
            // the best cost to quickly disregard branches that we do not need
            // to traverse.
            if (bestGuess != null) {
                CircleFringe newFringe = createFringe(newNode, bestGuess);
                if(newFringe.getCost() < best.getCost()) {
                    best.copyValuesFrom(newFringe);               
                }
            } 

            // Priority queue ordered by ancestor expansion
            // This priority queue holds the fringe elements
            PriorityQueue<CircleFringe> fringeQueue = new PriorityQueue<CircleFringe>();
            
            // Add the fringe for the root
            fringeQueue.add(rootFringe);

            bestSiblingSearch(newNode, best, fringeQueue);

            return best.getNode();
        }
  
        /**
         * Searches through the tree for the best sibling.
         */
        private void bestSiblingSearch(Node newNode, final CircleFringe best,
                PriorityQueue<CircleFringe> fringeQueue)
        {
            // Search for the best location to insert
            while (!fringeQueue.isEmpty()) {
                // get best candidate
                CircleFringe tf = fringeQueue.poll();
                if (tf.getAncestorExpansion() >= (best.getCost())) {
                    // If the ancestorExpansion of the current fringe is larger
                    // than the best cost found so far, then we are done.
                    break;
                }
                else {
                    // calculate new ancestor expansion for the children's fringes
                    double newAExp = tf.getCost() - tf.getNode().circle.getVolume();
                    processNode(newNode, tf.getNode().left, newAExp, best, fringeQueue);
                    processNode(newNode, tf.getNode().right, newAExp, best, fringeQueue);
                }
            }
        }
        
        /**
         * Creates the fringe for newNode at currentNode.
         * 
         * TODO: Find the best fringe from the currentNode and up to the root instead of only the fringe at the current node.
         * @param newNode The new node that is to be inserted
         * @param currentNode The node for which to create the fringe
         * @return
         */
        private CircleFringe createFringe(Node newNode, Node currentNode)
        {
            Circle bestCircle;
            double bestCost;
            bestCircle = new Circle();
            bestCircle.merge(currentNode.circle, newNode.circle);

            bestCost = bestCircle.getVolume();
            double ae = 0;
            Node n = currentNode;
            while (n.parent != null) {
                Circle enclosingCircle = new Circle();
                enclosingCircle.merge(n.circle, newNode.circle);
                double delta = enclosingCircle.getVolume() - n.parent.circle.getVolume();
                if (delta == 0) {
                    break;
                }
                ae += delta;
                n = n.parent;
            }
            CircleFringe newFringe = new CircleFringe(currentNode, ae, bestCost);

            return newFringe;
        }

        /**
         * Looks at a node and checks if it is better than the currently best
         * result. It also creates a new fringe and inserts it into the queue.
         */
        private void processNode(Node newNode, Node childNode, double newAExp, final CircleFringe best,
                PriorityQueue<CircleFringe> fringeQueue)
        {
            Circle enclosingCircle = new Circle();
            enclosingCircle.merge(childNode.circle, newNode.circle);
            double enclosingVolume = enclosingCircle.getVolume();
            
            // have we found a better cost?
            if ( (newAExp + enclosingVolume) < best.getCost()) {
                best.setVolume(enclosingVolume);
                best.setAncestorExpansion(enclosingVolume);
                best.setNode(childNode);
            }
            if (!childNode.isLeaf()) {
                CircleFringe newFringe = new CircleFringe(childNode, newAExp, enclosingVolume);
                fringeQueue.add(newFringe);
            }
        }

        /**
         * Insert new node as a sibling of a node in the tree.
         * 
         * @param newNode
         *            The new node
         * @param sibling
         *            A node already in the tree the new node will be a sibling
         *            of.
         */
        public void insertAtNode(Node newNode, Node sibling)
        {
            if (getRoot() == null) {
                setRoot(newNode);
            }
            else {
                Node newParent = new Node();
                newParent.parent = sibling.parent;
                if (sibling.parent == null) {
                    setRoot(newParent);
                }
                else if (sibling.parent.left == sibling) {
                    // parent is a left child
                    sibling.parent.left = newParent;
                }
                else {
                    // parent is a right child
                    sibling.parent.right = newParent;
                }

                newParent.left = sibling;
                newParent.right = newNode;

                newNode.parent = newParent;
                sibling.parent = newParent;
                newParent.circle.merge(newNode.circle, sibling.circle);
                repairParents(newParent);

            }
            size++;
        }

        /**
         * Adjust the parents bounding volumes to fit with this node.
         * 
         * @param newParent
         */
        private void repairParents(Node newParent)
        {
            Node p = newParent.parent;
            while (p != null) {
                int radius = p.circle.getRadius();
                p.circle.merge(p.left.circle, p.right.circle);
                // break this loop if the radius no longer changes.
                if (p.circle.getRadius() == radius) {
                    break;
                }
                p = p.parent;
            }
        }

        /**
         * Used when a node has moved or changed size.
         * 
         */
        public void repairNode(Node n)
        {
            if (n == null)
                return;
            Node sibling = removeNode(n);
            addNode(n, sibling);
        }

        /**
         * @return The sibling from which it was removed.
         */
        public Node removeNode(Node n)
        {
            Node sibling = null;
            if (n == null) {
                return null;
            }
            else if (n == root) {
                //WTF ? I can't.just delete the root! I can because the node will be a leaf and hence is the only element if it is also the root
                setRoot(null);
            }
            else {
                sibling = n.getSibling();
                Node parent = n.parent;

                sibling.parent = parent.parent;
                if (parent.parent == null) {
                    setRoot(sibling);
                }
                else if (parent.parent.left == parent) {
                    parent.parent.left = sibling;
                }
                else {
                    parent.parent.right = sibling;
                }
                parent.reset();
                repairParents(sibling);
            }
            n.reset();
            size--;
            return sibling;
        }

        public List<Actor> getIntersections(Circle b, CollisionQuery c)
        {
            List<Actor> result = new ArrayList<Actor>();
            if (getRoot() == null) {
                return result;
            }

            getRoot().getIntersections(b, c, result);

            return result;
        }

        public Actor getOneIntersectingObject(Node node, Circle circle, CollisionQuery checker)
        {
            if(node != null) {
                return node.getOneIntersectingObject(circle, checker);
            }
            else if(root != null) {
                return root.getOneIntersectingObject(circle, checker);
            } 
            else {
                return null;
            }
        }
        
        public Actor getOneIntersectingObject(Node node, CollisionQuery checker)
        {
            return node.getOneIntersectingObject(node.circle, checker);
        }

        public void paintDebug(Graphics g)
        {
            if (getRoot() != null) {
                paintNode(getRoot(), g);
            }
        }

        private void paintNode(Node n, Graphics g)
        {
            paintCircle(n.circle, g);
            if (n.left != null) {
                g.setColor(Color.BLUE);
                paintLine(n, n.left, g);
                paintNode(n.left, g);
            }
            if (n.right != null) {
                g.setColor(Color.GREEN);
                paintLine(n, n.right, g);
                paintNode(n.right, g);
            }
        }

        private void paintLine(Node from, Node to, Graphics g)
        {
            if (from == null || to == null) {
                return;
            }
            g.drawLine(from.circle.getX(), from.circle.getY(), to.circle.getX(), to.circle.getY());
        }

        private void paintCircle(Circle b, Graphics g)
        {
            if (b != null) {
                g.setColor(Color.RED);
                g.drawOval(b.getX() - b.getRadius(), b.getY() - b.getRadius(), b.getRadius() * 2, b.getRadius() * 2);
            }
        }

        public int size()
        {
            return size;
        }

        public void setRoot(Node root)
        {
            this.root = root;
        }

        public Node getRoot()
        {
            return root;
        }

    }

    public CircleTree tree;
    private GOCollisionQuery actorQuery = new GOCollisionQuery();
    private NeighbourCollisionQuery neighbourQuery = new NeighbourCollisionQuery();
    private PointCollisionQuery pointQuery = new PointCollisionQuery();
    private int cellSize;
    private List<Actor> objects;

    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        tree = new CircleTree();
        this.cellSize = cellSize;
        objects = new ArrayList<Actor>();
    }

    public synchronized void addObject(Actor actor)
    {   
        if (objects.contains(actor)) {
            return;
        }

        Node n = createNode(actor);
        ActorVisitor.setData(actor, n);
        tree.addNode(n);
        objects.add(actor);
    }

    private Node createNode(Actor actor)
    {
        Circle c = getCircle(actor);
        Node n = new Node(c, actor);
        return n;
    }

    public synchronized void removeObject(Actor object)
    {
        tree.removeNode((Node) ActorVisitor.getData(object));       
        
        ActorVisitor.setData(object, null);
        objects.remove(object);
    }

    public synchronized void updateObjectLocation(Actor object, int oldX, int oldY)
    {
        int ax = ActorVisitor.getX(object);
        int ay = ActorVisitor.getY(object);
        if(ax == oldX && ay == oldY) {
            return;
        }
        Node n = (Node) ActorVisitor.getData(object);
        Circle c = getCircle(object);
        if (c != null && n != null) {
            n.circle.setX(c.getX());
            n.circle.setY(c.getY());
            tree.repairNode(n);
        }
    }

    public synchronized void updateObjectSize(Actor object)
    {
        // Node n = (Node) ActorVisitor.getData(object);
        throw new RuntimeException("No longer working because of missing bounding circle");
        /*Circle c = ActorVisitor.getBoundingCircle(object);
        if (c != null && n != null) {
            n.circle.setRadius(c.getRadius() * cellSize);
            tree.repairNode(n);
        }*/
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls)
    {
        int halfCell = cellSize / 2;
        Circle b = new Circle(x * cellSize + halfCell, y * cellSize + halfCell, 0);
        synchronized (pointQuery) {
            pointQuery.init(x, y, cls);
            return (List<T>) tree.getIntersections(b, pointQuery);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getIntersectingObjects(Actor actor, Class<T> cls)
    {
        Circle b = getCircle(actor);
        synchronized (actorQuery) {
            actorQuery.init(cls, actor);
            return (List<T>) tree.getIntersections(b, actorQuery);
        }
    }

    private Circle getCircle(Actor actor)
    {
        throw new RuntimeException("No longer working because of missing bounding circle");
       /* Circle c = ActorVisitor.getBoundingCircle(actor);
        if (c == null) {
            return null;
        }
        Circle b = new Circle(c.getX() * cellSize, c.getY() * cellSize, c.getRadius() * cellSize);
        return b;*/
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r, Class<T> cls)
    {
        Circle b = new Circle(x * cellSize, y * cellSize, r * cellSize);
        synchronized (actorQuery) {
            actorQuery.init(cls, null);
            return (List<T>) tree.getIntersections(b, actorQuery);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getNeighbours(Actor a, int distance, boolean diag, Class<T> cls)
    {
        int x = ActorVisitor.getX(a);
        int y = ActorVisitor.getY(a);
        int xPixel = x * cellSize;
        int yPixel = y * cellSize;
        int dPixel = distance * cellSize;
        int r = 0;
        if (diag) {
            r = (int) Math.ceil(Math.sqrt(dPixel * dPixel + dPixel * dPixel));
        }
        else {
            double dy = 0.5 * cellSize;
            double dx = dPixel + dy;
            r = (int) Math.sqrt(dy * dy + dx * dx);
        }
        Circle c = new Circle(xPixel, yPixel, r);

        synchronized (neighbourQuery) {
            neighbourQuery.init(x, y, distance, diag, cls);
            return (List<T>) tree.getIntersections(c, neighbourQuery);
        }

    }

    public <T extends Actor> List<T> getObjectsInDirection(int x, int y, int angle, int length, Class<T> cls)
    {
        // TODO complete
        throw new RuntimeException("NOT IMPLEMENTED YET");
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjects(Class<T> cls)
    {
        if (cls == null) {
            return (List<T>) new ArrayList<Actor>(objects);
        }
        List<T> l = new ArrayList<T>();
        for (Iterator<Actor> iter = objects.iterator(); iter.hasNext();) {
            Actor actor = iter.next();
            if (cls.isInstance(actor)) {
                l.add((T) actor);
            }
        }
        return l;
    }

    public List<Actor> getObjectsList()
    {
        return objects;
    }

    public void startSequence()
    {}

    @SuppressWarnings("unchecked")
    public <T extends Actor> T getOneObjectAt(Actor actor, int x, int y, Class<T> cls)
    {		
        int halfCell = cellSize / 2;
        Circle b = new Circle(x * cellSize + halfCell, y * cellSize + halfCell, 0);
        synchronized (pointQuery) {
            pointQuery.init(x, y, cls);
            Node node = (Node) ActorVisitor.getData(actor);
            return (T) tree.getOneIntersectingObject(node, b, pointQuery);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> T getOneIntersectingObject(Actor object, Class<T> cls)
    {
        synchronized (actorQuery) {
            actorQuery.init(cls, object);

            Node node = (Node) ActorVisitor.getData(object);
            if(node == null) {
                return null;
            }
            return (T) tree.getOneIntersectingObject(node, actorQuery);            
        }

    }

    /**
     * Paint bounding boxes used in the circletree.
     */
    public void paintDebug(Graphics g)
    {
        int missing = 0;
        synchronized (this) {
            missing = (objects.size() - tree.size());
            tree.paintDebug(g);
        }
        if (missing > 0) {
            System.out.println("Objects missing: " + missing);
        }
    }
}
