import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * The BalgeShooter shoots Bagles, and is controlled
 * by the user through the keyboard.
 * 
 * A and the left arrow key will rotate the BagleShooter anti-clockwise.
 * D and the right arrow key will rotate the BagleShooter clockwise.
 * Space will fire a Bagle.
 * 
 * @author Joseph Lenton
 * @version 13/03/07
 */
public class BagleShooter extends Actor
{
    // the time to wait between shots
    private static final int SHOOT_DELAY = 8;
    
    // the x and y offset of the body to the bagle shooter
    public static final int BODY_OFFSET_X = 0;
    public static final int BODY_OFFSET_Y = 50;
    
    // the speed of rotation
    private static final int ROTATION_SPEED = 3;
    
    // the delay between shots
    private int shootDelay;
    
    /**
     * Constructor for the BagleShooter.
     */
    public BagleShooter()
    {
        shootDelay = 0;
    }
    
    /**
     * Checks the controls and increments
     * the delay between shots.
     */
    public void act() 
    {
        controls();
        
        if (shootDelay > 0)
            shootDelay--;
    }
    
    /**
     * Checks the key's that have been pressed,
     * and acts accordingly (rotating or shooting).
     */
    private void controls()
    {
        // if left arrow key or A is pressed.
        if (Greenfoot.isKeyDown("left") || Greenfoot.isKeyDown("A")) {
            setRotation(getRotation() - ROTATION_SPEED);
        }
        // if right arrow key or D is pressed
        else if (Greenfoot.isKeyDown("right") || Greenfoot.isKeyDown("D")) {
            setRotation(getRotation() + ROTATION_SPEED);
        }
        // if space has been pressed
        if (Greenfoot.isKeyDown("space")) {
            throwBagle( 7.5 );
//            throwBagle( (Greenfoot.getRandomNumber(100) / 10.0)+5 );
        }
    }
    
    /**
     * Throws a Bagle from the BagleShooter's position,
     * at the angle the BagleShooter is facing.
     * 
     * @param speed the power the Bagle is being thrown with.
     */
    public void throwBagle(double power)
    {
        // if the bagle is allowed to shoot
        if (shootDelay == 0) {
            // calculated the end of the BagleShooter's barrel
            int barrelLength = getImage().getWidth()/2;
            int x_offset = (int) ( barrelLength*Math.cos(Math.toRadians(getRotation())) );
            int y_offset = (int) ( barrelLength*Math.sin(Math.toRadians(getRotation())) );
            
            // creates a new Bagle at that position
            getWorld().addObject(
                new Bagle(getRotation(), power, getX()+x_offset, getY()+y_offset),
                getX()+x_offset,
                getY()+y_offset
            );
            
            // resets the shoot delay
            shootDelay = SHOOT_DELAY;
        }
    }
}
