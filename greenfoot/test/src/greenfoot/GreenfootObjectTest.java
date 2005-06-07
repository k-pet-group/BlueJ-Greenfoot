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
        world = new GreenfootWorld(10, 10, 10, true);
        TestObject o = new TestObject(11, 31);
        world.addObject(o);
        assertEquals(2, o.getWidth());
        assertEquals(4, o.getHeight());

        o = new TestObject(12, 32);
        world.addObject(o);
        assertEquals(3, o.getWidth());
        assertEquals(5, o.getHeight());
    }
}
