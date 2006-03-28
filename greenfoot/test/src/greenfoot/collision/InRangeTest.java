package greenfoot.collision;

import greenfoot.World;
import greenfoot.TestObject;

import java.util.Collection;

import junit.framework.TestCase;

/**
 * Tests the World's and Actor's getObjectsInRange method.
 * 
 * @author Poul Henriksen
 */
public class InRangeTest extends TestCase
{
    private World world;

    public void testNoWrap()
    {
        world = new World(10, 10, 10);

        TestObject actor1 = new TestObject(20, 20);
        actor1.setLocation(2, 2);
        world.addObject(actor1);

        TestObject actor2 = new TestObject(10, 10);
        world.addObject(actor2);
        
        actor2.setLocation(2, 4);
        Collection inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor2));

        actor2.setLocation(2, 5);
        inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor2));

        actor2.setLocation(2, 6);
        inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertFalse(inRange.contains(actor2));

        actor2.setLocation(4, 4);
        inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor2));

        actor2.setLocation(5, 5);
        inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertFalse(inRange.contains(actor2));

        actor2.setLocation(5, 5);
        inRange = actor1.getObjectsInRangeP(10000, TestObject.class);
        assertTrue(inRange.contains(actor2));
    }
    
    
    public void testWrap()
    {
        world = new World(10, 10, 10);
        world.setWrapped(true);

        TestObject actor1 = new TestObject(20, 20);
        actor1.setLocation(0, 2);
        world.addObject(actor1);

        TestObject actor2 = new TestObject(10, 10);
        actor2.setLocation(8, 2);
        world.addObject(actor2);
        
        Collection inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor2));
        assertEquals(1, inRange.size());
        inRange = actor2.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor1));

        inRange = actor1.getObjectsInRangeP(2, TestObject.class);
        assertTrue(inRange.contains(actor2));
        inRange = actor2.getObjectsInRangeP(2, TestObject.class);
        assertTrue(inRange.contains(actor1));

        inRange = actor1.getObjectsInRangeP(1, TestObject.class);
        assertFalse(inRange.contains(actor2));
        inRange = actor2.getObjectsInRangeP(1, TestObject.class);
        assertFalse(inRange.contains(actor1));
    }
}
