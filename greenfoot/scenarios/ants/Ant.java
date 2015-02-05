import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * An ant that collects food.
 * 
 * @author Michael Kölling
 * @version 1.1
 */
public class Ant extends Creature
{
    /** Every how many steps can we place a pheromone drop. */
    private static final int MAX_PH_LEVEL = 18;

    /** How long do we keep direction after finding pheromones. */
    private static final int PH_TIME = 30;

    /** Indicate whether we have any food with us. */
    private boolean carryingFood = false;

    /** How much pheromone do we have right now. */
    private int pheromoneLevel = MAX_PH_LEVEL;

    /** How well do we remember the last pheromone - larger number: more recent */
    private int foundLastPheromone = 0;

    /**
     * Create an ant with a given home hill. The initial speed is zero (not moving).
     */
    public Ant(AntHill home)
    {
        setHomeHill(home);
    }

    /**
     * Do what an ant's gotta do.
     */
    public void act()
    {
        if (carryingFood) {
            walkTowardsHome();
            handlePheromoneDrop();
            checkHome();
        }
        else {
            searchForFood();
        }
    }

    /**
     * Walk around in search of food.
     */
    private void searchForFood()
    {
        if (foundLastPheromone > 0) { // if we can still remember...
            foundLastPheromone--;
            walkAwayFromHome();
        }
        else if (smellPheromone()) {
            walkTowardsPheromone();
        }
        else {
            randomWalk();
        }
        checkFood();
    }

    /**
     * Are we home? Drop the food if we are, and start heading back out.
     */
    private void checkHome()
    {
        if (atHome()) {
            dropFood();
        }
    }

    /**
     * Are we home?
     */
    private boolean atHome()
    {
        if (getHomeHill() != null) {
            return (Math.abs(getX() - getHomeHill().getX()) < 4) && (Math.abs(getY() - getHomeHill().getY()) < 4);
        }
        else {
            return false;
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
     * Take some food from a fool pile.
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
        getHomeHill().countFood();
        setImage("ant.gif");
    }

    /**
     * Check whether we can drop some pheromone yet. If we can, do it.
     */
    private void handlePheromoneDrop()
    {
        if (pheromoneLevel == MAX_PH_LEVEL) {
            Pheromone ph = new Pheromone();
            getWorld().addObject(ph, getX(), getY());
            pheromoneLevel = 0;
        }
        else {
            pheromoneLevel++;
        }
    }

    /**
     * Check whether we can smell pheromones. If we can, return true, otherwise return false.
     */
    public boolean smellPheromone()
    {
        Actor ph = getOneIntersectingObject(Pheromone.class);
        return (ph != null);
    }

    /**
     * If we can smell some pheromone, walk towards it. If not, do nothing.
     */
    public void walkTowardsPheromone()
    {
        Actor ph = getOneIntersectingObject(Pheromone.class);
        if (ph != null) {
            headTowards(ph);
            walk();
            if (ph.getX() == getX() && ph.getY() == getY()) {
                foundLastPheromone = PH_TIME;
            }
        }
    }
}