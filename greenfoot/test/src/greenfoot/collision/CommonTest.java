package greenfoot.collision;

import junit.framework.TestCase;
import greenfoot.World;
import greenfoot.TestObject;

/**
 * Common tests for the Collision API in greenfoot.
 * 
 * @author Poul Henriksen
 *
 */
public class CommonTest extends TestCase
{
    private World world;
    private TestObject obj1;
    protected void setUp()        
    {
        world = new World(10,10,10);
        obj1 = new TestObject(10,10);
        world.addObject(obj1);
        obj1.setLocation(0, 0);
    }

    /**
     * Test that all the collision methods can take null as the parameter for
     * the class type.
     */
    public void testNullClass()
    {
        boolean gotException = false;
        try {
            world.getObjectsAt(0, 0, null);

            obj1.getIntersectingObjectsP(null);

            obj1.getNeighboursP(1, true, null);
            obj1.getNeighboursP(1, false, null);

            obj1.getObjectsAtP(5, 5, null);

            obj1.getObjectsInDirectionP(0, 1, null);

            obj1.getObjectsInRangeP(1, null);

            obj1.getOneIntersectingObjectP(null);

            obj1.getOneObjectAtP(0, 0, null);
        }
        catch (NullPointerException npe) {
            gotException = true;
            npe.printStackTrace();
        }
        assertFalse(gotException);

    }
}
