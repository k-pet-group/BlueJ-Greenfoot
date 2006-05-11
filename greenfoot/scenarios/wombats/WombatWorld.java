import greenfoot.World;
import greenfoot.Actor;
import greenfoot.GreenfootImage;

import java.util.Random;

public class WombatWorld extends World
{
    private Random randomizer = new Random();
    
    /**
     * Create a new world with 8x8 cells and
     * with a cell size of 60x60 pixels
     */
    public WombatWorld() 
    {
        super(8, 8, 60);        
        setBackground("cell.jpg");
    }

    /**
     * Populate the world with a fixed scenario of wombats and leaves.
     */    
    public void populate()
    {
        Wombat w1 = new Wombat();
        addObject(w1, 3, 3);
        
        Wombat w2 = new Wombat();
        addObject(w2, 1, 7);

        Leaf l1 = new Leaf();
        addObject(l1, 5, 3);

        Leaf l2 = new Leaf();
        addObject(l2, 0, 2);

        Leaf l3 = new Leaf();
        addObject(l3, 7, 5);

        Leaf l4 = new Leaf();
        addObject(l4, 2, 6);

        Leaf l5 = new Leaf();
        addObject(l5, 5, 0);
        
        Leaf l6 = new Leaf();
        addObject(l6, 4, 7);
    }
    
    /**
     * Place a number of leaves into the world at random places.
     * The number of leaves can be specified.
     */
    public void randomLeaves(int howMany)
    {
        for(int i=0; i<howMany; i++) {
            Leaf leaf = new Leaf();
            int x = getRandomNumber(getWidth());
            int y = getRandomNumber(getHeight());
            addObject(leaf, x, y);
        }
    }
    
    /**
     * Return a random number between 0 (inclusive) and limit (exclusive).
     */
    public int getRandomNumber(int limit)
    {
        return randomizer.nextInt(limit);
    }
}