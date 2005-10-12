import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.util.List;
import java.util.ArrayList;

public class Wombat extends GreenfootObject
{
    private static final int EAST = 0;
    private static final int WEST = 1;
    private static final int NORTH = 2;
    private static final int SOUTH = 3;

    private int direction;
    private int leavesEaten;

    public Wombat()
    {
        setImage("wombat.gif");
        setDirection(EAST);
        leavesEaten = 0;
    }

    /**
     * Do whatever the wombat likes to to just now.
     */
    public void act()
    {
        if(foundLeaf()) {
            eatLeaf();
        }
        else if(canMove()) {
            move();
        }
        else {
            turnRandom();
        }
    }

    /**
     * Check whether there is a leaf in the same cell as we are.
     */
    public boolean foundLeaf()
    {
        GreenfootObject leaf = getOneObjectAt(0, 0, Leaf.class);
        if(leaf != null) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Eat a leaf.
     */
    public void eatLeaf()
    {
        GreenfootObject leaf = getOneObjectAt(0, 0, Leaf.class);
        if(leaf != null) {
            // eat the leaf...
            getWorld().removeObject(leaf);
            leavesEaten = leavesEaten + 1; 
        }
    }
    
    /**
     * Move one cell forward in the current direction.
     */
    public void move()
    {
        if (!canMove()) {
            return;
        }
        switch(direction) {
            case SOUTH :
                setLocation(getX(), getY() + 1);
                break;
            case EAST :
                setLocation(getX() + 1, getY());
                break;
            case NORTH :
                setLocation(getX(), getY() - 1);
                break;
            case WEST :
                setLocation(getX() - 1, getY());
                break;
        }
    }

    /**
     * Test if we can move forward. Return true if we can, false otherwise.
     */
    public boolean canMove()
    {
        GreenfootWorld myWorld = getWorld();
        int x = getX();
        int y = getY();
        switch(direction) {
            case SOUTH :
                y++;
                break;
            case EAST :
                x++;
                break;
            case NORTH :
                y--;
                break;
            case WEST :
                x--;
                break;
        }
        // test for outside border
        if (x >= myWorld.getWidth() || y >= myWorld.getHeight()) {
            return false;
        }
        else if (x < 0 || y < 0) {
            return false;
        }
        List rocks = myWorld.getObjectsAt(x, y, Rock.class);
        if(rocks.isEmpty()) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Turn in a random direction.
     */
    public void turnRandom()
    {
        // get a random number between 0 and 3...
        int turns = ((WombatWorld)getWorld()).getRandomNumber(4);
        
        // ...an turn left that many times.
        for(int i=0; i<turns; i++) {
            turnLeft();
        }
    }

    /**
     * Turns towards the left.
     */
    public void turnLeft()
    {
        switch(direction) {
            case SOUTH :
                setDirection(EAST);
                break;
            case EAST :
                setDirection(NORTH);
                break;
            case NORTH :
                setDirection(WEST);
                break;
            case WEST :
                setDirection(SOUTH);
                break;
        }
    }

    /**
     * Sets the direction we're facing.
     */
    public void setDirection(int direction)
    {
        this.direction = direction;
        switch(direction) {
            case SOUTH :
                setImage("wombat.gif");
                setRotation(90);
                break;
            case EAST :
                setImage("wombat.gif");
                setRotation(0);
                break;
            case NORTH :
                setImage("wombat-left.gif");
                setRotation(90);
                break;
            case WEST :
                setImage("wombat-left.gif");
                setRotation(0);
                break;
            default :
                break;
        }
    }

    /**
     * Tell how many leaves we have eaten.
     */
    public int getLeavesEaten()
    {
        return leavesEaten;
    }
}