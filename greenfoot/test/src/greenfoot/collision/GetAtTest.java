/*
 * Created on Jun 7, 2005
 */
package greenfoot.collision;

import java.util.List;

import greenfoot.World;
import greenfoot.TestObject;
import junit.framework.TestCase;

public class GetAtTest extends TestCase
{
    private World world;

    public void testPixelOdd()
    {
        world = new World(100, 100, 1);

        TestObject actor1 = new TestObject(21, 21);
        world.addObject(actor1);

        actor1.setLocation(50, 50);

        List result = world.getObjectsAt(50, 50, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(60, 60, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(40, 40, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(39, 39, TestObject.class);
        assertFalse(result.contains(actor1));

        result = world.getObjectsAt(61, 61, TestObject.class);
        assertFalse(result.contains(actor1));

    }

    public void testPixelEven()
    {
        world = new World(100, 100, 1);

        TestObject actor1 = new TestObject(20, 20);
        world.addObject(actor1);

        actor1.setLocation(50, 50);

        List result = world.getObjectsAt(50, 50, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(59, 59, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(40, 40, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(39, 39, TestObject.class);
        assertFalse(result.contains(actor1));

        result = world.getObjectsAt(60, 60, TestObject.class);
        assertFalse(result.contains(actor1));

    }
}
