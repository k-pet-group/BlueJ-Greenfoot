/*
 * Created on Jun 7, 2005
 */
package greenfoot.collision;

import java.util.List;

import greenfoot.GreenfootWorld;
import greenfoot.TestObject;
import junit.framework.TestCase;

public class GetAtTest extends TestCase
{
    private GreenfootWorld world;

    public void testPixelOdd()
    {
        world = new GreenfootWorld(100, 100, 1, false);

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
        world = new GreenfootWorld(100, 100, 1, false);

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
