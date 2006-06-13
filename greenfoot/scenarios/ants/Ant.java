import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.util.Random;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

/**
 * An ant that collects food.
 * 
 * @author Michael Kolling
 * @version 1.0.1
 */
public class Ant extends Actor
{
    /** Random number generator. */
    private static final Random randomizer = AntWorld.getRandomizer();

    /** Every how many steps can we place a pheromone drop. */
    private static final int MAX_PH_LEVEL = 18;

    /** How long to we keep direction after finding pheromones. */
    private static final int PH_TIME = 30;

    /** the speed the ant moves with - in pizels per update. */
    private static final int SPEED = 3;

    // current movement
    private int deltaX = 0;
    private int deltaY = 0;

    /** Location of home ant hill */
    private AntHill homeHill;

    /** Indicate whether we have any food with us */
    private boolean carryingFood = false;

    /** how much pheromone do we have right now */
    private int pheromoneLevel = MAX_PH_LEVEL;

    /** how well do we remember the last pheromone - larger number: more recent */
    private int foundLastPheromone = 0;

    public Ant()
    {
    }

    public Ant(AntHill home)
    {
        homeHill = home;
    }

    /**
     * Do what an ant's gotta do.
     */
    public void act()
    {
        if (haveFood()) {
            headHome();
        }
        else {
            walk();
        }
    }

    /**
     * Walk around in search of food.
     */
    private void walk()
    {
        if (foundLastPheromone > 0) { // if we can still remember...
            foundLastPheromone--;
            headAway();
        }
        else if (smellPheromone()) {
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
        if (randomChance(50)) {
            deltaX = adjustSpeed(deltaX);
            deltaY = adjustSpeed(deltaY);
        }
        move();
    }

    /**
     * Try to walk home.
     */
    private void headHome()
    {
        if(homeHill == null) {
            //if we do not have a home, we can not go there.
            return;
        }
        if (randomChance(2)) {
            randomWalk(); // cannot always walk straight...
        }
        else {
            int distanceX = Math.abs(getX() - homeHill.getX());
            int distanceY = Math.abs(getY() - homeHill.getY());
            boolean moveX = (distanceX > 0) && (randomizer.nextInt(distanceX + distanceY) < distanceX);
            boolean moveY = (distanceY > 0) && (randomizer.nextInt(distanceX + distanceY) < distanceY);

            deltaX = computeHomeDelta(moveX, getX(), homeHill.getX());
            deltaY = computeHomeDelta(moveY, getY(), homeHill.getY());
            move();

            if (pheromoneLevel == MAX_PH_LEVEL) {
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
        if(homeHill == null) {
            //if we do not have a home, we can not head away from it.
            return;
        }
        if (randomChance(2)) {
            randomWalk(); // cannot always walk straight...
        }
        else {
            int distanceX = Math.abs(getX() - homeHill.getX());
            int distanceY = Math.abs(getY() - homeHill.getY());
            boolean moveX = (distanceX > 0) && (randomizer.nextInt(distanceX + distanceY) < distanceX);
            boolean moveY = (distanceY > 0) && (randomizer.nextInt(distanceX + distanceY) < distanceY);

            deltaX = computeHomeDelta(moveX, getX(), homeHill.getX()) * -1;
            deltaY = computeHomeDelta(moveY, getY(), homeHill.getY()) * -1;
            move();
        }
    }

    /**
     * Compute and return the direction (delta) that we should steer in when
     * we're on our way home.
     */
    private int computeHomeDelta(boolean move, int current, int home)
    {
        if (move) {
            if (current > home)
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
        if (homeHill != null && intersects(homeHill)) {
            dropFood();
            // move one step to where we came from so that we set out back in
            // the
            // right direction
            deltaX = -deltaX;
            deltaY = -deltaY;
            move();
            move();
        }
    }

    /**
     * Is there any food here where we are? If so, take some!.
     */
    public void checkFood()
    {
        Food food = (Food) getOneIntersectingObject(Food.class);
        if (food != null) {
            takeFood(food);
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
        Actor ph = getOneIntersectingObject(Pheromone.class);
        if (ph != null) {
            deltaX = capSpeed(ph.getX() - getX());
            deltaY = capSpeed(ph.getY() - getY());
            if (deltaX == 0 && deltaY == 0) {
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
        // otherwise drop a new one
        Pheromone ph = new Pheromone();
        getWorld().addObject(ph, getX(), getY());
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
     * Adjust the speed randomly (start moving, continue or slow down). The
     * speed returned is in the range [-SPEED .. SPEED].
     */
    private int adjustSpeed(int speed)
    {
        speed = speed + randomizer.nextInt(2 * SPEED - 1) - SPEED + 1;
        return capSpeed(speed);
    }

    /**
     * The speed returned is in the range [-SPEED .. SPEED].
     */
    private int capSpeed(int speed)
    {
        if (speed < -SPEED)
            return -SPEED;
        else if (speed > SPEED)
            return SPEED;
        else
            return speed;
    }

    /**
     * Move forward according to the current delta values.
     */
    private void move()
    {
        try {
            setLocation(getX() + deltaX, getY() + deltaY);
        }
        catch (IndexOutOfBoundsException e) {
            // We don't care - just leave it
        }
        setRotation((int) (180 * Math.atan2(deltaY, deltaX) / Math.PI));
    }

    /**
     * Return 'true' in exactly 'percent' number of calls. That is: a call
     * randomChance(25) has a 25% chance to return true.
     */
    private boolean randomChance(int percent)
    {
        return randomizer.nextInt(100) < percent;
    }
}