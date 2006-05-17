package greenfoot.collision;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.util.Circle;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

    static class Node
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

        }

        public Node(Circle circle, Actor actor)
        {
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
         * @param c Circle to find intersections with
         * @param checker Optional collision checker. If null it will just find
         *            collisions based on the circle.
         * @param result List to put the result in.
         */
        public void getIntersections(Circle c, CollisionQuery checker, List result)
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
        
        
        public Actor getOneIntersectingObject(BVHInsChecker.Node node, CollisionQuery checker) {
            return node.getOneIntersectingObjectUpwards(node.circle, checker);           
        }
        
        
        private Actor getOneIntersectingObjectDownwards(Circle c, CollisionQuery checker) {
            
            if (!c.intersects(this.circle)) {
                return null;
            }
            if (isLeaf() && (checker != null && checker.checkCollision(getActor()))) {
                return getActor();
            } else if (!isLeaf()) {
                //TODO maybe decide which one to actor to first based on size, distance of/between circles
                //maybe calculate the "insideness" of two circles as dist/r where dist is the distance
                //between centers of the two circles and r is the largest of the two circles' radius'. 
                //Insideness should probably be r/dist to get higher value for better.
                Actor res = left.getOneIntersectingObjectDownwards(c, checker);
                if(res!=null) {
                    return res;
                } else {
                    return right.getOneIntersectingObjectDownwards(c, checker);
                }
            }
            return null;
        }
        
        
        private Actor getOneIntersectingObjectUpwards(Circle c, CollisionQuery checker) {
            Node sibling = getSibling();
            Actor result = null;
            if(sibling != null) {
                result = sibling.getOneIntersectingObjectDownwards(sibling.circle, checker);
            }
            
            if(result == null && parent != null) {
                return parent.getOneIntersectingObjectUpwards(c, checker);
            } else  if (result != null) {
                return result;
            }
            return null;
        }

        // TODO replace with loop instead of recursion so that we can escape
        // quicker.
        public Actor getIntersection(Circle b, CollisionQuery c)
        {
            throw new RuntimeException("NOT IMPLEMENTED YET");
            /*
             * if (!b.intersects(this.circle)) { return null; } // TODO maybe
             * allow c==null if (isLeaf() && c.checkCollision(getGo())) { return
             * getGo(); } else { Actor res = left.getIntersection(b,
             * c); if(res!= null) return res; return right.getIntersection(b,
             * c); }
             */
        }

        /**
         * Resets all fields in this note and any pointers from parent or
         * children.
         */
        public void reset()
        {
            if(left != null && left.parent == this) {
                left.parent = null;
            }
            if(right != null && right.parent == this) {
                right.parent = null;
            }
            if(parent != null) {
                if(parent.left == this) {
                    parent.left = null;
                } else if(parent.right == this) {
                    parent.right = null;
                }
            }
            parent = null;
            left = null;
            right = null;
            circle = null;
            if(actor != null) {
                ActorVisitor.setData(actor, null);
        }
            actor = null;
        }

        private Node getSibling() {
            if(parent != null) {
                if(parent.left == this) {
                    return parent.right;
                } else {
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
        implements Comparable
    {

        private double ancestorExpansion;
        private Node node;
        private double volume;

        public CircleFringe(Node n, double ancestorExpansion, double volume)
        {
            this.ancestorExpansion = ancestorExpansion;
            this.volume = volume;
            this.node = n;
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

        public int compareTo(Object arg0)
        {
            CircleFringe other = (CircleFringe) arg0;
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
         * Set the volume of this fringe. That is, the size of the new parent
         * from inserting the new node as a sibling to this node.
         * 
         */
        public double getVolume()
        {
            return volume;
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
    static class CircleTree
    {
        private Node root;
        private int size;
        private CircleFringe best;

        public void addNode(Node n)
        {
            Node sibling = bestSibling(n);
            insertAtNode(n, sibling);
        }

        private void checkInvariant(Node n) {
            if(n==null) return;
            if(n.circle == null) {
                throw new RuntimeException("Invariant not true because circle==null:   Node: " + n + " #   left:" + n.left + " #   right:" +n.right);
                
            }
            if( !(n.left == null  && n.right == null) && (n.left==null || n.right==null) ) {
                throw new RuntimeException("Invariant not true:   Node: " + n + " #   left:" + n.left + " #   right:" +n.right);
            }
            checkInvariant(n.left);
            checkInvariant(n.right);
            
        }
        
        /**
         * For checking the consistency of the tree. It will throw an exception
         * if the tree has nodes without a circle of with only one child.
         */
        private void checkInvariant() {
        //   checkInvariant(root);
        }

        /**
         * Find the best place to insert the new node.
         * 
         */
        public Node bestSibling(Node newNode)
        {

            if (getRoot() == null) {
                return null;
            }

            // Priority queue ordered by ancestor expansion
            // This priority queue holds the fringe elements
            PriorityQueue fringeQueue = new PriorityQueue();

            // Initialise
            double bestCost = getRoot().circle.merge(newNode.circle).getVolume();
            best = new CircleFringe(getRoot(), 0, bestCost);
            if (!best.getNode().isLeaf()) {
                // We are not the only element, so we add the initial fringe
                CircleFringe tf = new CircleFringe(getRoot(), 0, bestCost);
                fringeQueue.add(tf);
            }

            // Search for the best location to insert
            while (!fringeQueue.isEmpty()) {
                // get best candidate
                CircleFringe tf = (CircleFringe) fringeQueue.poll();
                if (tf.getAncestorExpansion() >= (best.getVolume() + best.getAncestorExpansion())) {
                    break;
                }
                else {
                    // calculate new ancestor expansion for the children's
                    // fringes
                    double newAExp = tf.getAncestorExpansion() + tf.getVolume() - tf.getNode().circle.getVolume(); // ancestor
                    processNode(newNode, tf.getNode().left, newAExp, best, fringeQueue);
                    processNode(newNode, tf.getNode().right, newAExp, best, fringeQueue);
                }
            }
            return best.getNode();
        }

        /**
         * Looks at a node and checks if it is better than the currently best
         * result. It also creates a new fringe and inserts it into the queue.
         * 
         */
        private void processNode(Node newNode, Node childNode, double newAExp, CircleFringe best, PriorityQueue fringeQueue)
        {
            Circle enclosingCircle = childNode.circle.merge(newNode.circle);
            double enclosingVolume = enclosingCircle.getVolume();
            // have we found a better cost?
            if ((newAExp + enclosingVolume) < (best.getVolume() + best.getAncestorExpansion())) {
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
         * @param newNode The new node
         * @param sibling A node already in the tree the new node will be a sibling of.
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
                newParent.circle = newNode.circle.merge(sibling.circle);
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
                p.circle = p.left.circle.merge(p.right.circle);
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
            // TODO consider if it is possible to use the current location to
            // improve finding the best sibling.
            removeNode(n);
            addNode(n);
        }

        public void removeNode(Node n)
        {
            if (n == null) {
                return;
            }
            else if (n.parent == null) {
                setRoot(null);
            }
            else {
                Node sibling = n.getSibling();
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
                repairParents(sibling);
            }
            size--;
        }

        public List getIntersections(Circle b, CollisionQuery c)
        {
            List result = new ArrayList();
            if (getRoot() == null) {
                return result;
            }

            getRoot().getIntersections(b, c, result);

            return result;
        }

        public Actor getOneIntersection(Circle b, CollisionQuery checker)
        {
            if (getRoot() == null) {
                return null;
            }
            return getRoot().getIntersection(b, checker);
        }
        
        
        public Actor getOneIntersectingObject(Node node, CollisionQuery checker) {
             return getRoot().getOneIntersectingObject(node, checker);
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

    private CircleTree tree;
    private GOCollisionQuery actorQuery = new GOCollisionQuery();
    private NeighbourCollisionQuery neighbourQuery = new NeighbourCollisionQuery();
    private PointCollisionQuery pointQuery = new PointCollisionQuery();
    private int cellSize;
    private List objects;

    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        tree = new CircleTree();
        this.cellSize = cellSize;
        objects = new ArrayList();
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
        Node n = (Node) ActorVisitor.getData(object);
        Circle c = getCircle(object);
        if (c != null && n != null) {
            n.circle.setX(c.getX());
            n.circle.setY(c.getY());
            tree.repairNode((Node) ActorVisitor.getData(object));
        }
    }

    public synchronized void updateObjectSize(Actor object)
    {
        Node n = (Node) ActorVisitor.getData(object);
        Circle c = ActorVisitor.getBoundingCircle(object);
        if (c != null && n != null) {
            n.circle.setRadius(c.getRadius());
            tree.repairNode((Node) ActorVisitor.getData(object));
        }        
    }

    public List getObjectsAt(int x, int y, Class cls)
    {
        Circle b = new Circle(x * cellSize, y * cellSize, 0);
        synchronized (pointQuery) {
        	pointQuery.init(x, y, cls);
            return tree.getIntersections(b, pointQuery);
        }
    }

    public List getIntersectingObjects(Actor actor, Class cls)
    {
        Circle b = getCircle(actor);
        synchronized (actorQuery) {
            actorQuery.init(cls, actor);
            return tree.getIntersections(b, actorQuery);
        }
    }

    private Circle getCircle(Actor actor)
    {
        Circle c = ActorVisitor.getBoundingCircle(actor);
        if(c == null) {
            return null;
        }
        Circle b = new Circle(c.getX() * cellSize, c.getY() * cellSize, c.getRadius() * cellSize);
        return b;
    }

    public List getObjectsInRange(int x, int y, int r, Class cls)
    {   
        Circle b = new Circle(x * cellSize, y * cellSize, r * cellSize);
        synchronized (actorQuery) {
            actorQuery.init(cls, null);
            return tree.getIntersections(b, actorQuery);
        }
    }

    public List getNeighbours(int x, int y, int distance, boolean diag, Class cls)
    {
        int xPixel = x * cellSize;
        int yPixel = y * cellSize;
        int dPixel = distance * cellSize;
        int r = 0;
        if(diag) {            
            r = (int) Math.ceil(Math.sqrt(dPixel * dPixel + dPixel * dPixel));            
        } else {
            double dy = 0.5 * cellSize;
            double dx = dPixel + dy;
            r = (int) Math.sqrt(dy*dy+dx*dx);            
        }
        Circle c = new Circle(xPixel, yPixel, r);
        
        synchronized (neighbourQuery) {
            neighbourQuery.init(x, y, distance, diag, cls);
            return tree.getIntersections(c, neighbourQuery);
        }
        
    }

    public List getObjectsInDirection(int x, int y, int angle, int length, Class cls)
    {
        // TODO Auto-generated method stub

        throw new RuntimeException("NOT IMPLEMENTED YET");
    }

    public List getObjects(Class cls)
    {
        if (cls == null) {
            return new ArrayList(objects);
        }
        List l = new ArrayList();
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            Actor actor = (Actor) iter.next();
            if (cls.isInstance(actor)) {
                l.add(actor);
            }
        }
        return l;
    }
    
    public List getObjectsList()
    {
        return objects;
    }

    public void startSequence()
    {
    }

    public Actor getOneObjectAt(Actor exclude, int x, int y, Class cls)
    {
        List l = getObjectsAt(x , y , cls);
        l.remove(exclude);
        if (!l.isEmpty()) {
            return (Actor) l.get(0);
        }
        return null;
    }

    public Actor getOneIntersectingObject(Actor object, Class cls)
    {
        synchronized (actorQuery) {
            actorQuery.init(cls, object);
            
            Node node = (Node) ActorVisitor.getData(object);
            if(node != null) {
                return tree.getOneIntersectingObject(node, actorQuery);
            } else {
                List l = getIntersectingObjects(object, cls);
                if (!l.isEmpty()) {
                    return (Actor) l.get(0);
                }
            }            
        }
        return null;
        
    }

    /**
     * Paint bounding boxes used in the circletree.
     */
    public void paintDebug(Graphics g)
    {
        int missing = 0;
        synchronized (this) {
            missing = (objects.size() - tree.size());
            tree.paintDebug((Graphics2D) g);
        }
        if (missing > 0) {
            System.out.println("Objects missing: " + missing);
        }
    }
}
