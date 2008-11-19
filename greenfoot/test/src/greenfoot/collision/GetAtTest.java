/*
 * Created on Jun 7, 2005
 */
package greenfoot.collision;

import java.util.List;

import greenfoot.TestUtilDelegate;
import greenfoot.World;
import greenfoot.TestObject;
import greenfoot.util.GreenfootUtil;
import junit.framework.TestCase;

public class GetAtTest extends TestCase
{
    private World world;

    
    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());        
    }
    
    public void testPixelOdd()
    {
        world = new World(100, 100, 1){};

        TestObject actor1 = new TestObject(21, 21);
        world.addObject(actor1, 50 , 50);


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
        world = new World(100, 100, 1) {};

        TestObject actor1 = new TestObject(20, 20);
        world.addObject(actor1, 50, 50);

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
    
    /**
     * Test that collision checking works when an actor is rotated.
     */
    public void testRotation()
    {
        world = new World(100, 100, 1) {};

        // add object spanning (30, 40)-(69, 59)
        TestObject actor1 = new TestObject(40, 20);
        world.addObject(actor1, 50, 50);
        List result = world.getObjectsAt(30, 40, TestObject.class);
        assertTrue(result.contains(actor1));
       
        result = world.getObjectsAt(69, 59, TestObject.class);
        assertTrue(result.contains(actor1));

        //Now, rotate it 90 degrees so it spans (40, 30)-(59, 69)
        actor1.setRotation(90);
        result = world.getObjectsAt(42, 32, TestObject.class);
        assertTrue(result.contains(actor1));
        result = world.getObjectsAt(57, 67, TestObject.class);
        assertTrue(result.contains(actor1));
        
        // TODO also try negative degress and odd degrees like 55 or something.
        // And some values outside the rotation
    
    }
    
    public void testBigCells() 
    {
        world = new World(10, 10, 50) {};

        TestObject actor1 = new TestObject(50, 50);
        world.addObject(actor1, 1, 1);

        List result = world.getObjectsAt(1, 1, TestObject.class);
        assertTrue(result.contains(actor1));
        result = world.getObjectsAt(0, 0, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(0, 1, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(1, 0, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(2, 2, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(1, 2, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(2, 1, TestObject.class);
        assertFalse(result.contains(actor1));        
    }
}
