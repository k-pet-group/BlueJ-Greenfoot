import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * A background with a path drawn on it. Two different backgrounds are available.
 * 
 * @author mik
 * @version 1.0
 */
public class Ground  extends World
{

    /**
     * Create the field.
     */
    public Ground()
    {    
        super(800, 540, 1);
        addObject( new Car(), 180, 450);
    }
    
    public void showMap1()
    {
        setBackground("ground.png");
    }
    
    public void showMap2()
    {
        setBackground("ground2.png");
    }

}
