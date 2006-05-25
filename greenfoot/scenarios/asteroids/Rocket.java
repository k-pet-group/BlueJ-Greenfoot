import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A rocket that can be controlled by the arrowkeys: up, left, right.
 * The gun is fired by hitting the 'space' key.
 * 
 * @author Poul Henriksen
 */
public class Rocket extends MovingThing
{
    private GreenfootImage rocket = new GreenfootImage("rocket.png");    
    private GreenfootImage rocketWithThrust = new GreenfootImage("rocketWithThrust.png");

    /** The minimum delay between firing the gun. */
    private int minGunFireDelay = 5;    
    /** How long ago we fired the gun the last time. */
    private int gunFireDelay = 0;    
    /** How fast the rocket is */  
    private Vector acceleration = new Vector(0,0.3);
    
    public Rocket()
    {
    }

    public void act()
    {
        Asteroid a = (Asteroid) getOneIntersectingObject(Asteroid.class);
        if(a != null) {
            getWorld().removeObject(this);
            Greenfoot.pauseSimulation();
            return;
        }
            
        
        ignite(Greenfoot.isKeyDown("up"));
        if(Greenfoot.isKeyDown("left")) {
            setRotation(getRotation() - 5);
        }        
        if(Greenfoot.isKeyDown("right")) {
            setRotation(getRotation() + 5);
        }
        
        if(Greenfoot.isKeyDown("space")) {
            fire();
        }        
        gunFireDelay++;
        move();
    }
    
    /**
     * Should the rocket be ignited?
     */
    private void ignite(boolean b) {
        if(b) {
            setImage(rocketWithThrust);
            acceleration.setDirection(getRotation());
            increaseSpeed(acceleration);
        }
        else {
            setImage(rocket);            
        }
    }
    
    /**
     * Fire a bullet if the gun is ready.
     */
    private void fire() {     
        if(gunFireDelay >= minGunFireDelay) {
            Bullet b = new Bullet(getSpeed().copy(), getRotation());
            getWorld().addObject(b, getX(), getY());
            b.move();
            gunFireDelay = 0;
        }         
    }
}