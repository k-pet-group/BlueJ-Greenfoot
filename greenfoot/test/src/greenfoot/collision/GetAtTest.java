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

        TestObject go1 = new TestObject(21, 21);
        world.addObject(go1);

        go1.setLocation(50, 50);

        List result = world.getObjectsAt(50, 50, TestObject.class);
        assertTrue(result.contains(go1));

        result = world.getObjectsAt(60, 60, TestObject.class);
        assertTrue(result.contains(go1));

        result = world.getObjectsAt(40, 40, TestObject.class);
        assertTrue(result.contains(go1));

        result = world.getObjectsAt(39, 39, TestObject.class);
        assertFalse(result.contains(go1));

        result = world.getObjectsAt(61, 61, TestObject.class);
        assertFalse(result.contains(go1));

    }

    public void testPixelEven()
    {
        world = new World(100, 100, 1);

        TestObject go1 = new TestObject(20, 20);
        world.addObject(go1);

        go1.setLocation(50, 50);

        List result = world.getObjectsAt(50, 50, TestObject.class);
        assertTrue(result.contains(go1));

        result = world.getObjectsAt(59, 59, TestObject.class);
        assertTrue(result.contains(go1));

        result = world.getObjectsAt(40, 40, TestObject.class);
        assertTrue(result.contains(go1));

        result = world.getObjectsAt(39, 39, TestObject.class);
        assertFalse(result.contains(go1));

        result = world.getObjectsAt(60, 60, TestObject.class);
        assertFalse(result.contains(go1));

    }
}
