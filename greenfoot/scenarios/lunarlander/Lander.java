import greenfoot.World;
import greenfoot.Actor;
import greenfoot.Greenfoot;
import greenfoot.GreenfootImage;
import java.awt.Color;


public class Lander extends Actor
{
    /** Curent speed */
    private double speed = 0;
     
    /** Curent speed */
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
        rocketWithThrust = new GreenfootImage("images/thrust.png");
        rocketWithThrust.drawImage(rocket, 0, 0);
        setRotation(-90);
    }       

    public void act()
    {
        if(isLanded()) {
            return;
        }
        processKeys();
        applyGravity();
        
        altitude += speed / speedFactor;
        setLocation(getX(), (int) (altitude));
        checkCollision();
    }

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
    
    private void applyGravity() {
        speed += moon.getGravity();
    }
    
    private boolean isLanded() {
        Color leftColor = getWorld().getColorAt(getX() + leftX, getY() + bottom);
        Color rightColor = getWorld().getColorAt(getX() + rightX, getY() + bottom);
        /*System.out.println("Left Color: " + leftColor);
        moon.addObject(new Explosion(), getX() + leftX, getY() + bottom);
moon.addObject(new Explosion(), getX() + rightX, getY() + bottom);*/
        
        return (speed <= MAX_LANDING_SPEED) && leftColor.equals(moon.getLandingColor()) && rightColor.equals(moon.getLandingColor());
    }
        
    private boolean isExploded() {
        Color leftColor = getWorld().getColorAt(getX() + leftX, getY() + bottom);
        Color rightColor = getWorld().getColorAt(getX() + rightX, getY() + bottom);
        return !(leftColor.equals(moon.getSpaceColor()) && rightColor.equals(moon.getSpaceColor()));
    }
    
    private void checkCollision() {
        if(isLanded()) {
         //   putFlag();
         
           System.out.println("LAND");
            setImage(rocket);
            moon.addObject(new Flag(), getX(), getY());
            Greenfoot.pauseSimulation();
            
        } else if(isExploded()) {
           // explode();
           System.out.println("EXPLODE");
            moon.addObject(new Explosion(), getX(), getY());
            moon.removeObject(this);
           
            
            Greenfoot.pauseSimulation();
            
        }
    }
}