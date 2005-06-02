package greenfoot.collision;

import greenfoot.GreenfootWorld;
import greenfoot.TestObject;

import java.util.Collection;

import junit.framework.TestCase;

/**
 * Test for collisions between GreenfootObjects
 * 
 * @author Poul Henriksen
 */
public class IntersectionTest extends TestCase
{
    private GreenfootWorld world;

    public void testIntersectingSingleCell()
    {
        world = new GreenfootWorld(10, 10, 10, false);
        TestObject o1 = new TestObject(10,10);
        o1.setLocation(2,2);
        world.addObject(o1);
        
        TestObject o2 = new TestObject(10,10);
        o2.setLocation(2,2);
        world.addObject(o2);
        
        assertTrue(o1.intersects(o2));
        assertTrue(o2.intersects(o1));
        
        Collection c = o1.getIntersectingObjects(TestObject.class);
        assertTrue(c.contains(o2));
        assertEquals(1, c.size());
        
        o2.setLocation(3,2);
        assertFalse(o1.intersects(o2));
        assertFalse(o2.intersects(o1));
    }
    
    public void testIntersectingPixelLevelOdd()
    {
        world = new GreenfootWorld(70, 70, 1, false);
        TestObject o1 = new TestObject(7,7);
        o1.setLocation(0,0);
        world.addObject(o1);
        
        TestObject o2 = new TestObject(7,7);
        o2.setLocation(6,6);
        world.addObject(o2);
        
        assertTrue(o1.intersects(o2));
        assertTrue(o2.intersects(o1));
        
        Collection c = o1.getIntersectingObjects(TestObject.class);
        assertTrue(c.contains(o2));
        assertEquals(1, c.size());
        
        o2.setLocation(7,7);
        assertFalse(o1.intersects(o2));
        assertFalse(o2.intersects(o1));
    }
    
    public void testIntersectingPixelLevelEven()
    {
        world = new GreenfootWorld(80, 80, 1, false);
        TestObject o1 = new TestObject(8,8);
        o1.setLocation(0,0);
        world.addObject(o1);
        
        TestObject o2 = new TestObject(8,8);
        o2.setLocation(7,7);
        world.addObject(o2);
        
        assertTrue(o1.intersects(o2));
        assertTrue(o2.intersects(o1));
        
        Collection c = o1.getIntersectingObjects(TestObject.class);
        assertTrue(c.contains(o2));
        assertEquals(1, c.size());
        
        o2.setLocation(8,8);
        assertFalse(o1.intersects(o2));
        assertFalse(o2.intersects(o1));
    }
}
