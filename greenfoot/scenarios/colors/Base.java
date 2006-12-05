import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * This is the home of pirates. Pirates are born here, and it is where they
 * collect treasure. 
 * 
 * TODO: Create a label that show the amount of treasure collected.
 * 
 * @author Poul Henriksen
 * @version 0.5
 */
public class Base extends Actor
{
    private int treasure;
    private int agentsCreated;
    private static int MAX_AGENTS = 30;
    
    public Base()
    {
    }

    public void act()
    {
        if(agentsCreated <= MAX_AGENTS) {
            Direction direction = Direction.getDirection(Greenfoot.getRandomNumber(8) * Direction.TURN);
            getWorld().addObject(new Pirate(direction, this), getX(), getY());
            agentsCreated++;
        }
    }
    
    public void increaseTreasure() {
        treasure++;
    }

}