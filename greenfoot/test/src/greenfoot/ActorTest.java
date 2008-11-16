package greenfoot;

import junit.framework.TestCase;

/**
 * Tests some of the methods in Actor. This is things related to size,
 * position and rotation.
 * 
 * @author Poul Henriksen
 */
public class ActorTest extends TestCase
{

    private World world;

    public void testSize()
    {
        world = new World(10, 10, 10) {};
        TestObject o = new TestObject(11, 31);
        world.addObject(o, 0, 0);
        assertEquals(2, o.getWidth());
        assertEquals(4, o.getHeight());

        o = new TestObject(12, 32);
        world.addObject(o, 0, 0);
        assertEquals(3, o.getWidth());
        assertEquals(5, o.getHeight());
    }
    
    public void testOutOfBounds()
    {
        world = new World(10, 10, 10) {};
        TestObject o = new TestObject(11, 31);
        
        int x = 2;
        int y = 3;
        world.addObject(o, x, y);
        IndexOutOfBoundsException exception = null;
        try {
            o.setLocation(11,11);
        } catch (IndexOutOfBoundsException e) {
            exception = e;
        }
        assertNull(exception);
        assertEquals(9, o.getX());
        assertEquals(9, o.getY());        
    }
}
