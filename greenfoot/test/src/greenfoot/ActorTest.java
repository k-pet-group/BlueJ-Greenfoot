package greenfoot;

import greenfoot.collision.ibsp.Rect;
import greenfoot.core.WorldHandler;
import greenfoot.platforms.WorldHandlerDelegate;
import greenfoot.util.GreenfootUtil;
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


    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());        
    }
    
    
    public void testNoImage()
    {
        world = WorldCreator.createWorld(10, 10, 10);
        TestObject o = new TestObject(11, 31);
        o.setImage((GreenfootImage) null);
        
        world.addObject(o, 0, 0);
        assertNull(o.getImage());
        assertNull(o.getBoundingRect());
    }
    
    public void testRotatedSizeSmall()
    {
        world = WorldCreator.createWorld(100, 100, 1);
        TestObject o = new TestObject(20, 20);
        world.addObject(o, 0, 0);
        o.setRotation(45);
        
        //Width and height should now be sqrt(800) = 28.2842
        Rect r = o.getBoundingRect();
        assertEquals(30, r.getWidth());
        assertEquals(30, r.getHeight());
    }
    
    
    public void testOutOfBounds()
    {
        world = WorldCreator.createWorld(10, 10, 10);
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
    
    /**
     * Method that test whether the object is rotated around the correct point.
     * 
     * Normally it should be rotated around it's own location, but if the width
     * or height is odd it is different. For instance, if both with and height
     * is odd, it should be rotated around its own location plus (0.5, 0.5)
     */
    public void testRotationCenter()
    {
        world = WorldCreator.createWorld(700, 480, 1);
        TestObject o = new TestObject(1, 1);
        o.setRotation(-173); // Using -173 because it used to give a wrong result
        world.addObject(o, 0, 0);
        o.setLocation(0, 0);
        Rect rect = o.getBoundingRect();
        System.out.println("Rect: " + rect);
        assertEquals(-1 , rect.getX());
        assertEquals(-1 , rect.getY());
        assertEquals(2 , rect.getTop());
        assertEquals(2 , rect.getRight());        
    }
    
}
  