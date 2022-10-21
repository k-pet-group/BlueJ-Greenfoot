package bluej.parser;

import junit.framework.TestCase;
import bluej.parser.nodes.InnerNode;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

public class NodeTreeTest extends TestCase
{
    private NodeTree<ParsedNode> nt;
    private ParsedNode pn1;
    private ParsedNode pn2;
    private ParsedNode pn3;
    private ParsedNode pn4;
    private ParsedNode pn5;
    
    
    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp()
    {
        nt = new NodeTree<ParsedNode>();
        pn1 = new InnerNode(null);
        pn2 = new InnerNode(null);
        pn3 = new InnerNode(null);
        pn4 = new InnerNode(null);
        pn5 = new InnerNode(null);

        nt.insertNode(pn1, 0, 10);
        nt.insertNode(pn2, 20, 10);
        nt.insertNode(pn3, 40, 10);
        nt.insertNode(pn4, 60, 10);
        nt.insertNode(pn5, 80, 10);
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    protected void tearDown()
    {
    }

    public void testBasic()
    {        
        NodeAndPosition<ParsedNode> np = nt.findNode(5);
        assertNotNull(np);
        assertTrue(np.getNode() == pn1);
        assertTrue(np.getPosition() == 0);

        np = nt.findNode(25);
        assertNotNull(np);
        assertTrue(np.getNode() == pn2);
        assertTrue(np.getPosition() == 20);

        np = nt.findNode(45);
        assertNotNull(np);
        assertTrue(np.getNode() == pn3);
        assertTrue(np.getPosition() == 40);

        np = nt.findNode(65);
        assertNotNull(np);
        assertTrue(np.getNode() == pn4);
        assertTrue(np.getPosition() == 60);

        np = nt.findNode(85);
        assertNotNull(np);
        assertTrue(np.getNode() == pn5);
        assertTrue(np.getPosition() == 80);
    }
    
    public void testRemoval()
    {
        NodeAndPosition<ParsedNode> np = nt.findNode(65);
        np.getNode().remove();
        
        np = nt.findNode(5);
        assertNotNull(np);
        assertTrue(np.getNode() == pn1);
        assertTrue(np.getPosition() == 0);
        
        np = nt.findNode(65);
        assertNull(np);
        
        np = nt.findNode(85);
        assertNotNull(np);
        assertTrue(np.getNode() == pn5);
        assertTrue(np.getPosition() == 80);
    }
    
    public void testRemoval2()
    {
        // Remove all nodes
        NodeAndPosition<ParsedNode> np = nt.findNode(65);
        np.getNode().remove();
        np = nt.findNode(45);
        np.getNode().remove();
        np = nt.findNode(85);
        np.getNode().remove();
        np = nt.findNode(25);
        np.getNode().remove();
        np = nt.findNode(5);
        np.getNode().remove();

        np = nt.findNode(5);
        assertNull(np);
        np = nt.findNode(25);
        assertNull(np);
        np = nt.findNode(45);
        assertNull(np);
        np = nt.findNode(65);
        assertNull(np);
        np = nt.findNode(85);
        assertNull(np);
    }
    
    public void testRemoval3()
    {
        // For delete_case_4, need:
        // - node is black
        // - parent != null (has a parent)
        // parent is red, sibling is black
        
        nt.clear();
        nt.insertNode(pn1, 100, 10);
        nt.insertNode(pn2, 20, 10); // pn2 is red child of pn1
        nt.insertNode(pn3, 200, 10); // pn3 is red child of pn1
        nt.insertNode(pn4, 0, 10); // pn4 is red child of pn2
         // pn2 and pn3 become black
        nt.insertNode(pn5, 50, 10);
          // pn5 is red child of pn2
        
        ParsedNode pn6 = new InnerNode(null);
        nt.insertNode(pn6, 30, 10);
          // becomes a red child of pn5, uncle is pn4 (red)
          // pn4 is made black
          // pn5 is made black
          // pn2 is made red (parent = pn1, sibling = pn3)
        
        //So:
        //              pn1(black)
        //            /           \
        //           pn2(red)     pn3(black)
        //          /    \           /   \
        //        pn4(b)  pn5(b)    N     N
        //        N   N   /     N
        //              pn6(r)
        //              N    N
        //
        // All paths have an equal number of black nodes. (Remember N = null = black).
        //
        // pn4 and pn5 match criteria for delete case 4, however, pn5.left is red.
        
        NodeAndPosition<ParsedNode> nap = nt.findNode(35); // find pn6
        nap.getNode().remove();
        // The tree hasn't changed, except pn6 is gone.
        
        nap = nt.findNode(55); // find pn5
        nap.getNode().remove();
    }
    
    public void testRotation1()
    {
        nt.clear();
        nt.insertNode(pn1, 100, 10);
        nt.insertNode(pn2, 50, 10);
        nt.insertNode(pn3, 70, 10); // causes rotation
        
        NodeAndPosition<ParsedNode> nap = nt.findNode(105);
        assertTrue(nap.getNode() == pn1);
        nap = nt.findNode(55);
        assertTrue(nap.getNode() == pn2);
        nap = nt.findNode(75);
        assertTrue(nap.getNode() == pn3);
    }
}
