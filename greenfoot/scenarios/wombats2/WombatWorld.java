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
        
        GreenfootImage background = new GreenfootImage("images/cell.jpg");
        setBackground(background);
        background.setTiled(true);
    }

    /**
     * Populate the world with a fixed scenario of wombats and leaves.
     */    
    public void populate()
    {
        Wombat w1 = new Wombat();
        w1.setLocation(3, 3);
        addObject(w1);
        
        Wombat w2 = new Wombat();
        w2.setLocation(1, 7);
        addObject(w2);

        Leaf l1 = new Leaf();
        l1.setLocation(5, 3);
        addObject(l1);

        Leaf l2 = new Leaf();
        l2.setLocation(0, 2);
        addObject(l2);

        Leaf l3 = new Leaf();
        l3.setLocation(7, 5);
        addObject(l3);

        Leaf l4 = new Leaf();
        l4.setLocation(2, 6);
        addObject(l4);

        Leaf l5 = new Leaf();
        l5.setLocation(5, 0);
        addObject(l5);
        
        Leaf l6 = new Leaf();
        l6.setLocation(4, 7);
        addObject(l6);
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
            leaf.setLocation(x, y);
            addObject(leaf);
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