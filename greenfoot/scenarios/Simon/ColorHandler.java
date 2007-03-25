import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.ArrayList;

/**
 * The Color Handler 
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class ColorHandler extends Actor
{
    private static final int GO = 0;
    private static final int STOP = 1;
    private static final int GAME_OVER = 2;
    
    private static final int DELAY = 10;
    // the order of colors to be pressed by the user
    private ArrayList<SimonColor> colorSeries;
    private int colorSeriesCount;
    
    private ArrayList<SimonColor> colors;
    private AnimatedImage colorHandlerImages;
    
    // used to pick a color for the first time after it has compiled
    private boolean pickFirstColor;
    
    /**
     * 
     */
    public ColorHandler()
    {
        colorHandlerImages = new AnimatedImage("color_handler_img.png", 3, 80, 48); 
        setImage( colorHandlerImages.getFrame(STOP) );
        pickFirstColor = true;
        
        colorSeries = new ArrayList<SimonColor>();
        colors = new ArrayList<SimonColor>();
        colorSeriesCount = 0;
    }
    
    /**
     * 
     */
    protected void addedToWorld(World world)
    {
        createColors(world);
    }
    
    /**
     * Act - do whatever the ColorHandler wants to do. This method is called whenever
     * the 'Act' or 'Run' button gets pressed in the environment.
     */
    public void act()
    {
        if (pickFirstColor) {
            reset();
            pickFirstColor = false;
        }
    }
    
    /**
     * 
     */
    private void createColors(World world)
    {
        SimonColor red = new Red();
        SimonColor blue = new Blue();
        SimonColor green = new Green();
        SimonColor yellow = new Yellow();
        
        world.addObject( green, Game.OBJECT_HALF_SIZE, Game.OBJECT_HALF_SIZE*3 );
        world.addObject( red, Game.OBJECT_HALF_SIZE*3, Game.OBJECT_HALF_SIZE*3 );
        world.addObject( yellow, Game.OBJECT_HALF_SIZE, Game.OBJECT_HALF_SIZE*5 );
        world.addObject( blue, Game.OBJECT_HALF_SIZE*3, Game.OBJECT_HALF_SIZE*5 );
        
        colors.add(red);
        colors.add(blue);
        colors.add(green);
        colors.add(yellow);
    }
    
    /**
     * 
     */
    public void colorPressed(SimonColor color)
    {
        if (color == nextColor()) {
            correctColor();
        }
        else {
            wrongColor();
        }
    }
    
    /**
     * 
     */
    private void correctColor()
    {
        colorSeriesCount++;
        
        if (!isNextColor()) {
            setImage( colorHandlerImages.getFrame(STOP) );
            
            pause(DELAY);
            disableColors();
            pickRandomColor();
            colorSeriesCount = 0;
            
            setImage( colorHandlerImages.getFrame(GO) );
            enableColors();
        }
    }
    
    /**
     * 
     */
    private void wrongColor()
    {
        setImage( colorHandlerImages.getFrame(GAME_OVER) );
        pause(DELAY);
        disableColors();
        Greenfoot.playSound("game over.wav");
        pause(DELAY*2);
        
        reset();
        enableColors();
    }
    
    /**
     * 
     */
    private void reset()
    {
        setImage( colorHandlerImages.getFrame(STOP) );
        
        colorSeriesCount = 0;
        colorSeries = new ArrayList<SimonColor>();
        pickRandomColor();
        
        setImage( colorHandlerImages.getFrame(GO) );
    }
    
    /**
     * Enables all the SimonColors to accept input
     */
    private void enableColors()
    {
        for (SimonColor color : colors) {
            color.enable();
        }
    }
    
    /**
     * Disables all the SimonColors from accepting
     * input.
     */
    private void disableColors()
    {
        for (SimonColor color : colors) {
            color.turnOff();
            color.disable();
        }
    }
    
    /**
     * 
     */
    private SimonColor nextColor()
    {
        return colorSeries.get(colorSeriesCount);
    }
    
    /**
     * 
     */
    private boolean isNextColor()
    {
        return colorSeriesCount < colorSeries.size();
    }
    
    /**
     * 
     */
    private void pickRandomColor()
    {
        colorSeries.add( colors.get(Greenfoot.getRandomNumber(colors.size())) );
        
        for (SimonColor color : colorSeries) {
            pause(DELAY);
            color.turnOn();
            pause(DELAY);
            color.turnOff();
        }
    }
    
    /**
     * 
     */
    private void pause(int actDelay)
    {
        for (int i = 0; i < actDelay; i++) {
            Greenfoot.delay();
        }
    }
}
