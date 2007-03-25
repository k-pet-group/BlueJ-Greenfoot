import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * Write a description of class Game here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Game extends World
{
    public static final int OBJECT_HALF_SIZE = 24;
    
    private ColorHandler colorHandler;
    
    /**
     * Constructor for objects of class Game.
     * 
     */
    public Game()
    {
        super(OBJECT_HALF_SIZE*4, OBJECT_HALF_SIZE*6, 1);
        colorHandler = new ColorHandler();
        addObject( colorHandler, getWidth()/2, OBJECT_HALF_SIZE );
    }
    
    /**
     * 
     */
    public ColorHandler getColorHandler()
    {
        return colorHandler;
    }
}