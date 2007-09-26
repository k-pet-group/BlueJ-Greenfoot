package greenfoot;

import java.util.List;

/**
 * Test object that can easily be configured to having different sizes.
 * 
 * @author Poul Henriksen
 */
public class TestObject extends Actor
{
    /**
     * A test object with an image size of 7x7. Using 7x7 gives a size just less
     * than 10x10 if rotating 45 degrees. This makes it suitable to test a scenario
     * where the gridsize is 10x10 and we do not want objects to extent to more than
     * one cell.
     */
    public TestObject()
    {
        this(7, 7);
    }
    
    public TestObject(int width, int height) {
        GreenfootImage image = new GreenfootImage(width, height);
        setImage(image);
    }

    public List getNeighboursP(int distance, boolean diagonal, Class cls)
    {
        return getNeighbours(distance, diagonal, cls);
    }

    public List getObjectsInRangeP(int distance, Class cls)
    {
        return getObjectsInRange(distance, cls);
    }

    public boolean intersectsP(Actor other)
    {
        return intersects(other);
    }

    public List getIntersectingObjectsP(Class cls)
    {
        return getIntersectingObjects(cls);
    }

    public List getObjectsAtP(int dx, int dy, Class cls)
    {
        return getObjectsAtOffset(dx, dy, cls);
    }

  /*  public List getObjectsInDirectionP(int angle, int length, Class cls)
    {
         return getObjectsInDirection(angle, length, cls);
    }*/

    public Actor getOneIntersectingObjectP(Class cls)
    {
       return getOneIntersectingObject(cls);
    }

    public Actor getOneObjectAtP(int dx, int dy, Class cls)
    {
        return getOneObjectAtOffset(dx, dy, cls);
    }

    
    


  
}