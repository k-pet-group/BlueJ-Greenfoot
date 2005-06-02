package greenfoot.collision;

import greenfoot.GreenfootWorld;
import greenfoot.TestObject;

import java.util.Collection;

import junit.framework.TestCase;

/**
 * Tests the GreenfootWorld's and GreenfootObject's getObjectsInRange method.
 * 
 * @author Poul Henriksen
 */
public class InRangeTest extends TestCase
{
    private GreenfootWorld world;

    public void testNoWrap()
    {
        world = new GreenfootWorld(10, 10, 10, false);

        TestObject go1 = new TestObject(20, 20);
        go1.setLocation(2, 2);
        world.addObject(go1);

        TestObject go2 = new TestObject(10, 10);
        world.addObject(go2);

        go2.setLocation(2, 4);
        Collection inRange = go1.getObjectsInRange(3, TestObject.class);
        assertTrue(inRange.contains(go2));

        go2.setLocation(2, 5);
        inRange = go1.getObjectsInRange(3, TestObject.class);
        assertTrue(inRange.contains(go2));

        go2.setLocation(2, 6);
        inRange = go1.getObjectsInRange(3, TestObject.class);
        assertFalse(inRange.contains(go2));

        go2.setLocation(4, 4);
        inRange = go1.getObjectsInRange(3, TestObject.class);
        assertTrue(inRange.contains(go2));

        go2.setLocation(5, 5);
        inRange = go1.getObjectsInRange(3, TestObject.class);
        assertFalse(inRange.contains(go2));

        go2.setLocation(5, 5);
        inRange = go1.getObjectsInRange(10000, TestObject.class);
        assertTrue(inRange.contains(go2));
    }
    
    
    public void testWrap()
    {
        world = new GreenfootWorld(10, 10, 10, false);

        TestObject go1 = new TestObject(20, 20);
        go1.setLocation(0, 2);
        world.addObject(go1);

        TestObject go2 = new TestObject(10, 10);
        go2.setLocation(8, 2);
        world.addObject(go2);
        
        Collection inRange = go1.getObjectsInRange(3, TestObject.class);
        assertTrue(inRange.contains(go2));
        assertEquals(1, inRange.size());
        inRange = go2.getObjectsInRange(3, TestObject.class);
        assertTrue(inRange.contains(go1));

        inRange = go1.getObjectsInRange(2, TestObject.class);
        assertTrue(inRange.contains(go2));
        inRange = go2.getObjectsInRange(2, TestObject.class);
        assertTrue(inRange.contains(go1));

        inRange = go1.getObjectsInRange(1, TestObject.class);
        assertFalse(inRange.contains(go2));
        inRange = go2.getObjectsInRange(1, TestObject.class);
        assertFalse(inRange.contains(go1));
    }
}
