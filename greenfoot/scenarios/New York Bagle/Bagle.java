import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;

/**
 * The Bagle Class is for when the bagle is in the air.
 * I moves across the x and y axis, through it's momentum,
 * which are affected by the world's gravity and wind.
 * 
 * When it hits the floor it will create an instance of
 * the BagleFloor class. If it goes outside the world,
 * it will be removed.
 * 
 * @author Joseph Lenton
 * @version 13/03/07
 */
public class Bagle extends Actor
{
    // the minimum and maximum rotation speeds
    private static final int ROTATION_MINIMUM_SPEED = 5;
    private static final int ROTATION_MAXIMUM_SPEED = 15;
    
    // the rotation speed of this instance
    private int rotationSpeed;
    
    // a double version of x and y,
    // so I can have more accurate movement
    private double x;
    private double y;
    
    // the momentum along the x and y axis, for each act
    private double deltaX;
    private double deltaY;
    
    // if the Bagle should remove itself from the world
    private boolean removeSelf;
    
    /**
     * Sets the Bagles angle,
     * and calculates it's speed across x and y axis.
     */
    public Bagle(int angle, double speed, int x, int y)
    {
        super();
        
        this.x = x;
        this.y = y;
        
        removeSelf = false;
        
        deltaX = speed*Math.cos(Math.toRadians(angle));
        deltaY = speed*Math.sin(Math.toRadians(angle));
        
        setRotation(angle);
        rotationSpeed = Greenfoot.getRandomNumber(ROTATION_MAXIMUM_SPEED-ROTATION_MINIMUM_SPEED)+ROTATION_MINIMUM_SPEED;
    }
    
    /**
     * Moves the bagle and checks if there have been any
     * collisions. Then it will check if it should be removed
     * from the world.
     */
    public void act() 
    {
        move();
        
        checkTouchingBasket();
        checkTouchingGround();
        
        if (removeSelf) {
            getWorld().removeObject(this);
        }
    }
    
    /**
     * Checks if the Bagle has touched a basket.
     * If so, tells the basket that a Bagle has been added,
     * and states to remove its self.
     */
    private void checkTouchingBasket()
    {
        Basket basket = (Basket) getOneIntersectingObject(Basket.class);
        
        if (basket != null) {
            basket.addBagle();
            // state to be removed
            removeSelf();
        }
    }
    
    /**
     * Checks if the Bagle has hit the ground.
     * If so, creates a new BagleFloor instance at that
     * position, and states to remove its self.
     */
    private void checkTouchingGround()
    {
        City city = (City) getWorld();
        Color pixelColor = city.getBackground().getColorAt(getX(), getY());
        
        if (city.isLandColor(pixelColor)) {
            city.addObject(new BagleFloor(getRotation()), getX(), getY());
            // state to be removed
            removeSelf();
        }
    }
    
    /**
     * Moves the Bagle, and states to be removed from
     * the world if it goes off screen.
     */
    public void move()
    {
        City city = (City) getWorld();
        
        // update the bagle's momentum
        deltaX += city.getWindStrength();
        deltaY += city.GRAVITY;
        
        // update it's x and y co-ordinates accordingly
        x += deltaX;
        y += deltaY;
        
        // check if it's off-screen
        if ( offScreen() ) {
            // state to be removed
            removeSelf();
        }
        // otherwise update it's position
        else {
            setLocation((int) x, (int) y);
            turnRotation(rotationSpeed);
        }
    }
    
    /**
     * Checks if the Bagle is off-screen.
     * 
     * @return true if the Bagle is off-screen, false if not.
     */
    private boolean offScreen()
    {
        World city = getWorld();
        return ( x < 0 || x >= city.getWidth() || y < 0 || y >= city.getHeight() );
    }
    
    /**
     * States for the Bagle to be removed from the world.
     */
    private void removeSelf()
    {
        removeSelf = true;
    }
    
    /**
     * Turns the Bagle.
     * 
     * @param angle the angle, in degrees, to turn.
     */
    private void turnRotation(int angle)
    {
        setRotation(getRotation()+angle);
    }
}
