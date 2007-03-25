import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * 
 * 
 * @author Joseph Lenton
 * @version (a version number or a date)
 */
public abstract class SimonColor extends Actor
{
    private AnimatedImage image;
    private String key;
    private boolean keyDown;
    private boolean keyHit;
    private String sound;
    
    private boolean running;
    
    /**
     * 
     */
    public SimonColor(String key, String animatedImageFileName, String sound)
    {
        image = new AnimatedImage(animatedImageFileName, 2, 48, 48);
        this.key = key;
        this.sound = sound;
        keyHit = false;
        keyDown = false;
        running = true;
        
        turnOff();
    }
    
    /**
     * Act - do whatever the Color wants to do. This method is called whenever
     * the 'Act' or 'Run' button gets pressed in the environment.
     */
    public void act() 
    {
        checkKeyHit();
        
        if (running) {
            if (keyHit) {
                turnOn();
                informColorHandler();
            }
            else if (!keyDown) {
                turnOff();
            }
        }
    }
    
    /**
     * To check if a key has been hit, but not if it is being
     * held down. Will return true or false accordingly.
     * 
     * @return true if the key has been hit, false if not or it is constantly held down.
     */
    private void checkKeyHit()
    {
        if (Greenfoot.isKeyDown(key)) {
            if (!keyDown) {
                keyHit = true;
                keyDown = true;
            }
            else {
                keyHit = false;
            }
        }
        else {
            keyHit = false;
            keyDown = false;
        }
    }
    
    /**
     * States for the Actor to remember that it's key
     * is down, and calls to update the image.
     */
    public void turnOn()
    {
        setImage( image.getFrame(1) );
        Greenfoot.playSound(sound);
    }
    
    /**
     * Gets and then tells the color handler that it
     * has just been pressed.
     */
    private void informColorHandler()
    {
        ((Game) getWorld()).getColorHandler().colorPressed(this);
    }
    
    /**
     * States for the Actor to remember that it's key
     * is up, and calls to update the image.
     */
    public void turnOff()
    {
        setImage( image.getFrame(0) );
    }
    
    /**
     * Enables the SimonColor, so it will now
     * respond to key presses.
     */
    public void enable()
    {
        running = true;
        // flush the key for this color
        Greenfoot.isKeyDown(key);
    }
    
    /**
     * Disables the SimonColor from responding to
     * key presses.
     */
    public void disable()
    {
        running = false;
    }
}
