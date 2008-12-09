import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;

/**
 * A lunar lander
 *
 * @author Poul Henriksen
 * @version 1.0.1
 */
public class Lander extends Actor
{
    /** Current speed */
    private double speed = 0;
     
    /** Current speed */
    private double MAX_LANDING_SPEED = 10;  
    
    /** Power of the rocket */
    private double thrust = -3;
    
    /** The location */
    private double altitude;
    
    /** The speed is divided by this. */
    private double speedFactor = 10;
    
    /** Rocket image without thrust */
    private GreenfootImage rocket;
   
    /** Rocket image with thrust */
    private GreenfootImage rocketWithThrust;
    
    /** Moon we are trying to land on */
    private Moon moon;    
    
    /** Left foot */
    private int leftX = -13;
    /** Right foot */
    private int rightX = 15;
    /** Bottom of lander */
    private int bottom = 27;
    
    public Lander()
    {
        rocket = getImage();
        rocketWithThrust = new GreenfootImage("thrust.png");
        rocketWithThrust.drawImage(rocket, 0, 0);
    }       

    public void act()
    {
        processKeys();
        applyGravity();
        
        altitude += speed / speedFactor;
        setLocation(getX(), (int) (altitude));
        checkCollision();
    }

    /**
     * Lander has been added to the world.
     */
    public void addedToWorld(World world) {
        moon = (Moon) world;        
        altitude = getY();
    }
    
    /**
     * Handle keyboard input.
     */
    private void processKeys() {
        if(Greenfoot.isKeyDown("down")) {
            speed+=thrust;
            setImage(rocketWithThrust);
        } else {
            setImage(rocket);
        }
    }
    
    /**
     * Let the gravity change the speed.
     */
    private void applyGravity() {
        speed += moon.getGravity();
    }
    
    /**
     * Whether we have touched the landing platform yet.
     */
    private boolean isLanding() {
        Color leftColor = moon.getColorAt(getX() + leftX, getY() + bottom);
        Color rightColor = moon.getColorAt(getX() + rightX, getY() + bottom);
        return (speed <= MAX_LANDING_SPEED) && leftColor.equals(moon.getLandingColor()) && rightColor.equals(moon.getLandingColor());
    }
     
    /** 
     * Is the lander exploding?
     */
    private boolean isExploding() {
        Color leftColor = moon.getColorAt(getX() + leftX, getY() + bottom);
        Color rightColor = moon.getColorAt(getX() + rightX, getY() + bottom);
        return !(leftColor.equals(moon.getSpaceColor()) && rightColor.equals(moon.getSpaceColor()));
    }
    
    /**
     * Check if we are colliding with anything and take appropiate action.
     */
    private void checkCollision() {
        if(isLanding()) {
            setImage(rocket);           
            moon.addObject(new Flag(), getX(), getY());
            Greenfoot.stop();            
        } else if(isExploding()) {
            moon.addObject(new Explosion(), getX(), getY());
            moon.removeObject(this);
        }
    }
}