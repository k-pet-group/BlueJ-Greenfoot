package greenfoot;

import junit.framework.TestCase;

/**
 * Tests some of the methods in GreenfootObject. This is things related to size,
 * position and rotation.
 * 
 * @author Poul Henriksen
 */
public class GreenfootObjectTest extends TestCase
{

    private GreenfootWorld world;

    public void testSize()
    {
        world = new GreenfootWorld(10, 10, 10);
        world.setWrapped(true);
        TestObject o = new TestObject(11, 31);
        world.addObject(o);
        assertEquals(2, o.getWidth());
        assertEquals(4, o.getHeight());

        o = new TestObject(12, 32);
        world.addObject(o);
        assertEquals(3, o.getWidth());
        assertEquals(5, o.getHeight());
    }
    
    public void testOutOfBounds()
    {
        world = new GreenfootWorld(10, 10, 10);
        TestObject o = new TestObject(11, 31);
        world.addObject(o);
        int x = 2;
        int y = 3;
        o.setLocation(x, y);
        IndexOutOfBoundsException exception = null;
        try {
            o.setLocation(11,11);
        } catch (IndexOutOfBoundsException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(x, o.getX());
        assertEquals(y, o.getY());        
    }
}
