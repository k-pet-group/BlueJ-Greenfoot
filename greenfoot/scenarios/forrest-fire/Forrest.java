import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.util.Random;

public class Forrest extends GreenfootWorld
{
    private final static int WIDTH = 100;
    private final static int HEIGHT = 100;
    
    private Random random;
    
    /**
     * Creates a new world with 20x20 cells and
     * with a cell size of 50x50 pixels
     */
    public Forrest() {
        super(WIDTH, HEIGHT, 5, true);
        random = new Random();
    }
     
    /**
     * Populate the forrest with some trees.
     */
    public void populate(int density) 
    {
        int n = 0;
        int sum=0;
        for(int i=0; i < WIDTH; i++) {
            for(int j=0; j < HEIGHT; j++) {
                if(random.nextInt(100) < density) {
                    long t1 = System.currentTimeMillis();
                    Tree tree = new Tree();
                    long t2 = System.currentTimeMillis();
                    tree.setLocation(i, j);                    
                    long t3 = System.currentTimeMillis();
                    addObject(tree);                   
                    long t4 = System.currentTimeMillis();
                    n++;
                    sum+= t2-t1;
                //   System.out.println("" + (t2 -t1) + " " + (t3 -t2) + " " + (t4-t3));
                }
            }
        }
        System.out.println("Created " + n + "trees in " + sum + " ms");
        
        System.out.println("Average instantiation time: " + (double) sum/n);
    }
}