package greenfoot.collision;

import greenfoot.GreenfootObject;
import greenfoot.GreenfootObjectVisitor;
import greenfoot.util.Circle;
import greenfoot.util.ObjectPool;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
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
 * <li>Moderate tree construction time (could maybe be improved by using fast (but not as good) algorithm from paper)</li>
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
    
    /**
     * Node in a tree. The objects are pooled and should be created with the
     * createNode methods and deleted with the delete() method once it is not 
     * used anymore.
     */
    static class Node {
        public Node parent;
        public Node left; // child
        public Node right; // child
        public Circle circle;
        private GreenfootObject go;

        private static ObjectPool<Node> pool = new ObjectPool<Node>() {
            @Override
            protected Node createNew()
            {
                return new Node();
            }
        };
        
        public static Node createNode() {
            Node c = (Node) pool.get();
            c.init(null, null);
            return c;
        }    

        public static Node createNode(Circle circle, GreenfootObject go) {
            Node c = (Node) pool.get();
            c.reset();
            c.init(circle, go);
            return c;
        }
        
        public void delete() {
            pool.add(this);
        }
        
        public static void clearPool() {
            pool.reset();
        }
        
        private Node()
        {

        }

        public void init(Circle circle, GreenfootObject go)
        {
            this.circle = circle;
            this.go = go;
        }

        public GreenfootObject getGo()
        {
            return go;
        }

        public boolean isLeaf()
        {
            return (left == null && right == null);
        }

        /**
         * Get collisions with the circle b and then use the specific collision
         * checker for the greenfootObjects to make low-level collision check.
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
            if (isLeaf() && (checker != null && checker.checkCollision(getGo()))) {
                result.add(getGo());
            }
            else if (!isLeaf()) {
                left.getIntersections(c, checker, result);
                right.getIntersections(c, checker, result);
            }
        }
        
        
        public GreenfootObject getOneIntersectingObject(BVHInsChecker.Node node, CollisionQuery checker) {
            return node.getOneIntersectingObjectUpwards(node.circle, checker);           
        }
        
        private GreenfootObject getOneIntersectingObjectDownwards(Circle c, CollisionQuery checker) {
            
            if (!c.intersects(this.circle)) {
                return null;
            }
            if (isLeaf() && (checker != null && checker.checkCollision(getGo()))) {
                return getGo();
            } else if (!isLeaf()) {
                //TODO maybe decide which one to go to first based on size, distance of/between circles
                //maybe calculate the "insideness" of two circles as dist/r where dist is the distance
                //between centers of the two circles and r is the largest of the two circles' radius'. 
                //Insideness should probably be r/dist to get higher value for better.
                GreenfootObject res = left.getOneIntersectingObjectDownwards(c, checker);
                if(res!=null) {
                    return res;
                } else {
                    return right.getOneIntersectingObjectDownwards(c, checker);
                }
            }
            return null;
        }
        
        
        private GreenfootObject getOneIntersectingObjectUpwards(Circle c, CollisionQuery checker) {
            Node sibling = getSibling();
            GreenfootObject result = null;
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

        public GreenfootObject getIntersection(Circle b, CollisionQuery c)
        {
            throw new RuntimeException("NOT IMPLEMENTED YET");
            /*
             * if (!b.intersects(this.circle)) { return null; } // TODO maybe
             * allow c==null if (isLeaf() && c.checkCollision(getGo())) { return
             * getGo(); } else { GreenfootObject res = left.getIntersection(b,
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
            if(go != null) {
                GreenfootObjectVisitor.setData(go, null);
            }
            go = null;
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
     *<p>
     * CircleFringe objects are pooled.
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
        
        private static ObjectPool<CircleFringe> pool = new ObjectPool<CircleFringe>() {
            @Override
            protected CircleFringe createNew()
            {
                return new CircleFringe();
            }
        };
        
        public static CircleFringe createCircleFringe() {
            CircleFringe c = (CircleFringe) pool.get();
            c.init(null,0,0);
            return c;
        }    

        public static CircleFringe createCircleFringe(Node n, double ancestorExpansion, double volume)
        {
            CircleFringe c =  (CircleFringe) pool.get();
            c.init(n, ancestorExpansion, volume);
            return c;
        }
    
        public void delete() {
            pool.add(this);
        }     

        private static void delete(Collection fringeQueue) {
            pool.add(fringeQueue);
        }
                
        public static void clearPool() {
            pool.reset();
        }
        
        public void init(Node n, double ancestorExpansion, double volume) {
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
        private PriorityQueue fringeQueue = new PriorityQueue();;
        
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
        
        public synchronized   void addNode(Node n)
        {
            checkInvariant();
            Node sibling = bestSibling(n);
            insertAtNode(n, sibling);
            checkInvariant();
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
            if(!fringeQueue.isEmpty()) {
                CircleFringe.delete(fringeQueue);
                fringeQueue.clear();
            }
            // Initialise
            Circle c = getRoot().circle.merge(newNode.circle);
            double bestCost = c.getVolume();
            c.delete();
            c = null;
            CircleFringe best = CircleFringe.createCircleFringe(getRoot(), 0, bestCost);
            
            if (!best.getNode().isLeaf()) {
                if(best.getNode().left ==null || best.getNode().right == null) {
                    try {
                        throw new Exception();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // We are not the only element, so we add the initial fringe
                CircleFringe tf = CircleFringe.createCircleFringe(getRoot(), 0, bestCost);
                fringeQueue.add(tf);
            }
            
            // Search for the best location to insert
            while (!fringeQueue.isEmpty()) {
                // get best candidate
                CircleFringe tf = (CircleFringe) fringeQueue.poll();
                if (tf.getAncestorExpansion() >= (best.getVolume() + best.getAncestorExpansion())) {
                    break;
                } else {
                    // calculate new ancestor expansion for the children's
                    // fringes
                    double newAExp = tf.getAncestorExpansion() + tf.getVolume() - tf.getNode().circle.getVolume(); // ancestor                    
                    processNode(newNode, tf.getNode().left, newAExp, best, fringeQueue);
                    processNode(newNode, tf.getNode().right, newAExp, best, fringeQueue);                    
                }
                tf.delete();
                tf = null;
            }
            Node bestNode = best.getNode();
            best.delete();
            best = null;
            return bestNode;
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
            enclosingCircle.delete();
            enclosingCircle = null;
            // have we found a better cost?
            if ((newAExp + enclosingVolume) < (best.getVolume() + best.getAncestorExpansion())) {
                best.setVolume(enclosingVolume);
                best.setAncestorExpansion(enclosingVolume);
                best.setNode(childNode);
            }
            if (!childNode.isLeaf()) {
                CircleFringe newFringe = CircleFringe.createCircleFringe(childNode, newAExp, enclosingVolume);
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
            checkInvariant();           
            if (getRoot() == null) {
                setRoot(newNode);
            }
            else {
                checkInvariant();
                
                Node newParent = Node.createNode();
                newParent.parent = sibling.parent;
                
                if (sibling.parent == null) {
                    setRoot(newParent);
                } else if (sibling.parent.left == sibling) {
                    // parent is a left child
                    sibling.parent.left = newParent;
                } else {
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
                Circle c = p.circle;
                p.circle = p.left.circle.merge(p.right.circle);
                c.delete();
                p = p.parent;
            }
        }

        /**
         * Used when a node has moved or changed size.
         * 
         */
        public synchronized   void repairNode(Node n)
        {
            if (n == null)
                return;
            // TODO consider if it is possible to use the current location to
            // improve finding the best sibling.
            removeNode(n);
            addNode(n);
        }

        public synchronized  void removeNode(Node n)
        {
            checkInvariant();         
            if (n == null) {
                return;
            }
            else if (n.parent == null) {
                setRoot(null);
            }
            else {
                Node parent = n.parent;
                Node sibling = n.getSibling();
                
                checkInvariant();             
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
                parent.delete();

                parent = null;
            }
            n = null;
            size--;

            checkInvariant();
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

        public GreenfootObject getOneIntersection(Circle b, CollisionQuery checker)
        {
            if (getRoot() == null) {
                return null;
            }
            return getRoot().getIntersection(b, checker);
        }
        
        public GreenfootObject getOneIntersectingObject(Node node, CollisionQuery checker) {
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
    private GOCollisionQuery goQuery = new GOCollisionQuery();
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

    public void addObject(GreenfootObject go)
    {
        if (objects.contains(go)) {
            return;
        }

        Node n = createNode(go);
        GreenfootObjectVisitor.setData(go, n);
        tree.addNode(n);
        objects.add(go);
    }

    private Node createNode(GreenfootObject go)
    {
        Circle c = getCircle(go);
        Node n = Node.createNode(c, go);
        return n;
    }

    public void removeObject(GreenfootObject object)
    {
        Node n = (Node) GreenfootObjectVisitor.getData(object);
        tree.removeNode(n);
        n.delete();
        GreenfootObjectVisitor.setData(object, null);
        objects.remove(object);
    }

    public void updateObjectLocation(GreenfootObject object, int oldX, int oldY)
    {
        Node n = (Node) GreenfootObjectVisitor.getData(object);
        Circle c = getCircle(object);
        if (c != null && n != null) {
            n.circle.setX(c.getX());
            n.circle.setY(c.getY());
            tree.repairNode(n);
            c.delete();
        }
    }

    public void updateObjectSize(GreenfootObject object)
    {
        Node n = (Node) GreenfootObjectVisitor.getData(object);
        Circle c = getCircle(object);
        if (c != null && n != null) {
            n.circle.setRadius(c.getRadius());
            tree.repairNode(n);
            c.delete();
        }     
    }

    public List getObjectsAt(int x, int y, Class cls)
    {
        Circle b = Circle.createCircle(x * cellSize, y * cellSize, 0);
        synchronized (pointQuery) {
            pointQuery.init(x, y);
            List l = tree.getIntersections(b, pointQuery);
            b.delete();
            b=null;
            return l;
        }
    }

    public List getIntersectingObjects(GreenfootObject go, Class cls)
    {
        Circle b = getCircle(go);
        synchronized (goQuery) {
            goQuery.init(cls, go);
            return tree.getIntersections(b, goQuery);
        }
    }

    private Circle getCircle(GreenfootObject go)
    {
        Circle c = GreenfootObjectVisitor.getBoundingCircle(go);
        if(c == null) {
            return null;
        }
        Circle b = Circle.createCircle(c.getX() * cellSize, c.getY() * cellSize, c.getRadius() * cellSize);
        return b;
    }

    public List getObjectsInRange(int x, int y, int r, Class cls)
    {   
        Circle b = Circle.createCircle(x * cellSize, y * cellSize, r * cellSize);
        synchronized (goQuery) {
            goQuery.init(cls, null);
            List l = tree.getIntersections(b, goQuery);
            b.delete();
            b = null;
            return l;
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
        Circle c = Circle.createCircle(xPixel, yPixel, r);
        
        synchronized (neighbourQuery) {
            neighbourQuery.init(x, y, distance, diag, cls);
            List l = tree.getIntersections(c, neighbourQuery);
            c.delete();
            return l;
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
            return objects;
        }
        List l = new ArrayList();
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            GreenfootObject go = (GreenfootObject) iter.next();
            if (cls.isInstance(go)) {
                l.add(go);
            }
        }
        return l;
    }

    public void startSequence()
    {
    }

    public GreenfootObject getOneObjectAt(int x, int y, Class cls)
    {
        List l = getObjectsAt(x , y , cls);
        if (!l.isEmpty()) {
            return (GreenfootObject) l.get(0);
        }
        return null;
    }

    public GreenfootObject getOneIntersectingObject(GreenfootObject object, Class cls)
    {
        synchronized (goQuery) {
            goQuery.init(cls, object);
            
            Node node = (Node) GreenfootObjectVisitor.getData(object);
            if(node != null) {
                return tree.getOneIntersectingObject(node, goQuery);
            } else {
                List l = getIntersectingObjects(object, cls);
                if (!l.isEmpty()) {
                    return (GreenfootObject) l.get(0);
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
