import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.util.Random;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

public class Ant extends GreenfootObject
{
    private static long  antCount;
    
    private static final Random randomizer = AntWorld.getRandomizer();
    
    // every how many steps can we place a pheromone drop
    private static final int MAX_PH_LEVEL = 5;

    // how long to we keep direction after finding pheromones:
    private static final int PH_TIME = 15;
    
    // the speed the ant moves with - in pizels per update
    private static final int SPEED = 4; 
    
    // current movement
    private int deltaX = 0;
    private int deltaY = 0;
    
    // location of home ant hill
    private int homeX;
    private int homeY;
    private AntHill homeHill;
    
    // indicate whether we have any food with us
    private boolean carryingFood = false;
    
    // how much pheromone do we have right now
    private int pheromoneLevel = MAX_PH_LEVEL;
    
    // how well do we remember the last pheromone - larger number: more recent
    private int foundLastPheromone = 0;
    
    public Ant()
    {
        antCount++;
        setImage("ant.gif");
        homeX = getX();
        homeY = getY();
    }

    public Ant(int x, int y, AntHill home)
    {
        super(x,y);
        antCount++;
        setImage("ant.gif");
        homeX = x;
        homeY = y;
        homeHill = home;
    }

    /**
     * Do what an ant's gotta do.
     */
    public void act()
    {
        long t1 = System.currentTimeMillis();
        //randomWalk();
        if(haveFood()) {
            headHome();
        }
        else {
            walk();
        }
       // System.out.println("Act: " + (System.currentTimeMillis() - t1));
    }
    
    /**
     * Walk around in search of food.
     */
    private void walk()
    {
        if(foundLastPheromone > 0) {   // if we can still remember...
            foundLastPheromone--;
            headAway();
        }
        else if(smellPheromone()) {
            move();
        }
        else {
            randomWalk();
        }        
        checkFood();
    }
    
    /**
     * Walk around randomly.
     */
    private void randomWalk()
    {
        //System.out.println("Random walking.");
        deltaX = adjustSpeed(deltaX);
        deltaY = adjustSpeed(deltaY);
        move();
    }
    
    /**
     * Try to walk home.
     */
    private void headHome()
    {
        //System.out.println("Heading home.");
        if(randomChance(2)) {
            randomWalk();       // cannot always walk straight...
        }
        else {
            int distanceX = Math.abs(getX() - homeX);
            int distanceY = Math.abs(getY() - homeY);
            boolean moveX = (distanceX > 0) && (randomizer.nextInt(distanceX + distanceY) < distanceX);
            boolean moveY = (distanceY > 0) && (randomizer.nextInt(distanceX + distanceY) < distanceY);

            deltaX = computeHomeDelta(moveX, getX(), homeX);
            deltaY = computeHomeDelta(moveY, getY(), homeY);
            move();
            
            if(pheromoneLevel == MAX_PH_LEVEL) {
                dropPheromone();
            }
            else {
                pheromoneLevel++;
            }
        }

        checkHome();
    }
    
    /**
     * Try to walk away from home.
     */
    private void headAway()
    {
        if(randomChance(2)) {
            randomWalk();       // cannot always walk straight...
        }
        else {
            int distanceX = Math.abs(getX() - homeX);
            int distanceY = Math.abs(getY() - homeY);
            boolean moveX = (distanceX > 0) && (randomizer.nextInt(distanceX + distanceY) < distanceX);
            boolean moveY = (distanceY > 0) && (randomizer.nextInt(distanceX + distanceY) < distanceY);
    
            deltaX = computeHomeDelta(moveX, getX(), homeX) * -1;
            deltaY = computeHomeDelta(moveY, getY(), homeY) * -1;
            move();
        }
    }
    
    /**
     * Compute and return the direction (delta) that we should steer in when
     * we're on our way home.
     */
    private int computeHomeDelta(boolean move, int current, int home)
    {
        if(move) {
            if(current > home)
                return -SPEED;
            else
                return SPEED;
        }
        else
            return 0;
    }

    /**
     * Are we home? Drop the food if we are.
     */
    private void checkHome()
    {
        Collection antHills = getWorld().getObjectsAt(getX(), getY(), AntHill.class);
        Iterator it = antHills.iterator();
        if( it.hasNext()) {
            Object hill = it.next();
            if(hill == homeHill) {
                dropFood();
            
                // move one step to where we came from so that we set out back in the 
                // right direction
                deltaX = -deltaX;
                deltaY = -deltaY;
                move();
                move();
            }
        }
        //NOT WORKING - need to check home extent
        /*if((getX() == homeX) && (getY() == homeY)) {
            dropFood();
            
            // move one step to where we came from so that we set out back in the 
            // right direction
            deltaX = -deltaX;
            deltaY = -deltaY;
            move();
            move();
        }*/
    }
    
    /**
     * Is there any food here where we are? If so, take some!.
     */
    public void checkFood()
    {
       
        Collection objectsHere = getWorld().getObjectsAt(getX(), getY(), Food.class);
        Iterator it = objectsHere.iterator();
        if( it.hasNext()) {
            Object thing = it.next();
            takeFood((Food)thing);
            return;
        }
    }
    
    /**
     * Tell whether we are carrying food of not.
     */
    public boolean haveFood()
    {
        return carryingFood;
    }

    /**
     * Check whether we can smell pheromones. If we can, turn towards it and 
     * return true. Otherwise just return false.
     */
    public boolean smellPheromone()
    {
        long t1 = System.currentTimeMillis();
        Collection objectsHere = getWorld().getObjectsAt(getX(), getY(), Pheromone.class);
                   int i=0;
       for(Iterator it = objectsHere.iterator(); it.hasNext(); ) {
            Object thing = it.next();

            
                Pheromone ph = (Pheromone)thing;
                
                deltaX = capSpeed(ph.getCenterX() - getX());
                deltaY = capSpeed(ph.getCenterY() - getY());
                if(deltaX == 0 && deltaY == 0) {
                    foundLastPheromone = PH_TIME;
                } 
                return true;
            
        }
        return false;
    }

    /**
     * Drop a spot of pheromones at our current location.
     */
    private void dropPheromone()
    {
        Pheromone ph = new Pheromone();
        ph.setLocation(getX() - ph.getWidth() / 2, getY() - ph.getHeight() / 2);
       
        getWorld().addObject(ph);
        pheromoneLevel = 0;
    }
    
    /**
     * Drop a spot of pheromones at our current location.
     */
    private void takeFood(Food food)
    {
        carryingFood = true;
        food.takeSome();
        setImage("ant-with-food.gif");
    }
    
    /**
     * Drop our food in the ant hill.
     */
    private void dropFood()
    {
        carryingFood = false;     
        homeHill.countFood();
        setImage("ant.gif");
    }
    
    /**
     * Adjust the speed randomly (start moving, continue or slow down).
     * The speed returned is in the range [-1 .. 1].
     */
    private int adjustSpeed(int speed)
    {
        speed = speed + randomizer.nextInt(2*SPEED -1) - SPEED+1;
        return capSpeed(speed);
    }
    
    /**
     * The speed returned is in the range [-SPEED .. SPEED].
     */
    private int capSpeed(int speed)
    {
        if(speed < -SPEED)
            return -SPEED;
        else if(speed > SPEED)
            return SPEED;
        else
            return speed;
    }
    
  
    
    /**
     * Move forward according to the current delta values.
     */
    private void move()
    {
        setLocation(getX() + deltaX, getY() + deltaY);
        setRotation(180*Math.atan2(deltaY,deltaX)/Math.PI+90 -45);
    }
    
    /**
     * Return 'true' in exactly 'percent' number of calls.
     * That is: a call randomChance(25) has a 25% chance to return true.
     */
    private boolean randomChance(int percent)
    {
        return randomizer.nextInt(100) < percent;
    }
}