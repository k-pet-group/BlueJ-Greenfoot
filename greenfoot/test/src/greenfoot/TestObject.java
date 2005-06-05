package greenfoot;

import java.util.Collection;

/**
 * Test object that can easily be configured to having different sizes.
 * 
 * @author Poul Henriksen
 */
public class TestObject extends GreenfootObject
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


  
}