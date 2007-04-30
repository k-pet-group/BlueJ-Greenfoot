import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.*;

/**
 * A boid is an object that is part of a flock. 
 * The boid follows a few simple rules that gives emergent behaviour when lots of boids are placed in the world.
 * 
 * @author Poul Henriksen 
 * @version 2.0
 */
public class Boid extends SmoothActor
{
    /** Distance the wall-force will start working from.  */
    private final static int WALL_DIST = 50;    
    /** The size of wall force when it is at max. */
    private final static int WALL_FORCE = 200;
    
    /**  Maximum speed of the boid. */
    private final static int MAX_SPEED = 200;
    /**  Minimum speed of the boid. */
    private final static int MIN_SPEED = 50;
    /** The speed is divided by this. */
    private final static int SPEED_DIVIDER = 15; 
     
    /** Distance from which repulsion from other objects start working.*/
    private final static int REPULSE_DIST = 40;   
    /** Distance from which alignment with other boids start working. */ 
    private final static int ALIGN_DIST = 150;    
    /** Distance from which attraction to other boids start working. */ 
    private final static int ATTRACT_DIST = 150;
    
    /**
     * Creates a new boid with minimum speed in a random direction.
     */
    public Boid()
    { 
        setMaximumSpeed(MAX_SPEED);
        setMinimumSpeed(MIN_SPEED);
        setSpeedDivider(SPEED_DIVIDER);
        
        Vector vel = new Vector();
        vel.setDirection((Math.random())*360);
        vel.setLength(MIN_SPEED);
        setVelocity(vel);
    }
    
    /**
     * Flock!
     */
    public void act() 
    {      
        acc();
        super.act();
    }    
    
    /**
     * Calculate accelaration by appling the boid rules
     */
    private void acc() {
        Vector acc = new Vector(0,0);
        acc.add(getFlockAttraction(ATTRACT_DIST).divide(7.5));     
        acc.add(getFlockRepulsion(REPULSE_DIST).multiply(1));
        acc.add(getFlockAlignment(ALIGN_DIST).divide(8));
        acc.add(getWallForce());
        setAccelaration(acc);
    }
    
    /**
     * Get the size of the wall force on this boid. Will make the boid avoid the world boundaries.
     */    
    public Vector getWallForce() {
        Vector location = getLocation();
        //Special border repulse rules:
        Vector wallForce = new Vector(0,0);
        if(location.getX() <= WALL_DIST) {
            double distFactor = (WALL_DIST - location.getX()) / WALL_DIST;
            wallForce.add(new Vector(WALL_FORCE * distFactor, 0));
        }
        if( (getWorld().getWidth() - location.getX()) <= WALL_DIST) {
            double distFactor = (WALL_DIST - (getWorld().getWidth() - location.getX())) / WALL_DIST;
            wallForce.subtract(new Vector(WALL_FORCE * distFactor, 0));
        }
        if(location.getY() <= WALL_DIST) {
            double distFactor = (WALL_DIST - location.getY()) / WALL_DIST;
            wallForce.add(new Vector(0, WALL_FORCE * distFactor));
        }
        if(getWorld().getHeight() - location.getY() <=  WALL_DIST) {
            double distFactor = (WALL_DIST - (getWorld().getHeight() - location.getY())) / WALL_DIST;
            wallForce.subtract(new Vector(0, WALL_FORCE * distFactor));
        }
        return wallForce;
    }
    
    /**
     * Get the other objects that are within the given distance.
     */
    private List getNeighbours(int distance, Class cls) {
        return getObjectsInRange(distance, cls);
    }
    
    /**
     * Get the center of all the boids within the given distance. 
     * That is, the average of all the positions of the other boids.
     */
    public Vector getCentreOfMass(int distance) {
        List neighbours = getNeighbours(distance, Boid.class);
        //add me
        neighbours.add(this);
        Vector centre = new Vector();
        for(Object o : neighbours) {
            Boid b = (Boid) o;
            centre.add(b.getLocation());
        }
        return centre.divide(neighbours.size()); 
    }

    /**
     * Get the attraction from the other boids within the given distance.
     */
    public Vector getFlockAttraction(int distance) {
        Vector com = getCentreOfMass(distance);
        //distance to the centre of mass
        Vector distCom = getCentreOfMass(distance).subtract(getLocation());
        return distCom;        
    }
    
    /**
     * Get the repulsion from the other boids within the given distance.
     */
    public Vector getFlockRepulsion(int distance) {
        Vector repulse = new Vector();
        List neighbours = getNeighbours(distance, SmoothActor.class);
        for(Object o : neighbours) {            
            SmoothActor other = (SmoothActor) o;
            //dist to other actor
            Vector dist = getLocation().subtract(other.getLocation());
            if(dist.getLength() > distance) {
                // Make sure we are looking at the logical distance.
                continue;
            }
            repulse.add(dist.setLength(distance - dist.getLength()));
        }
        return repulse;        
    }
    
    /**
     * Get the average velocity of all boids within the given distance.
     */
    private Vector getAverageVelocity(int distance) {
        List neighbours = getNeighbours(distance, Boid.class);
        //add me
        neighbours.add(this);
        Vector avg = new Vector();
        for(Object o : neighbours) {
            Boid b = (Boid) o;
            avg.add(b.getVelocity());
        }
        return avg.divide(neighbours.size());
    }
    
    /**
     * Get the relative direction this boid should be facing to match the average direction of the flock.
     */
    private Vector getFlockAlignment(int distance) {
        Vector avgVel = getAverageVelocity(distance);
        avgVel.subtract(getVelocity());
        return avgVel;
    }
}
