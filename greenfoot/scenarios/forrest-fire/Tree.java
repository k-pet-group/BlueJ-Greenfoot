import greenfoot.World;
import greenfoot.Actor;

import greenfoot.GreenfootImage;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.util.*;

public class Tree extends Actor
{
    private final static int SIZE = 5;
    private final static int BURN_RADIUS = 1;

    private static final Color green = new Color(12, 130, 2);
    private static final Color black = new Color(12, 0, 2);
    private static final Color red = Color.RED;
    
    private static GreenfootImage tree = new GreenfootImage(SIZE, SIZE);
    private static GreenfootImage burntTree = new GreenfootImage(SIZE, SIZE);
    private static GreenfootImage burningTree = new GreenfootImage(SIZE, SIZE);
    private boolean burnt;
    private boolean burning;

    private boolean inPreAct = true;
    
    private boolean doBurnNeighbors = true;
    
   
    
    static {
        tree.setColor(green);
        tree.fillOval(0, 0, SIZE-1, SIZE-1);
        
        burntTree.setColor(black);
        burntTree.fillOval(0, 0, SIZE-1, SIZE-1);

        burningTree.setColor(red);
        burningTree.fillOval(0, 0, SIZE-1, SIZE-1);
        
    }
    
    public Tree()
    {        
        burnt = false;
        burning = false;
        draw();
    }
    
    /**
     * Draw the tree
     */
    public void draw() 
    {
     //   Image im = getImage();
        if(burnt) {
            setImage(burntTree);
          //  im.setColor(black);
        }
        else if(burning) {
            
            setImage(burningTree);
            //im.setColor(Color.RED);
        }
        else {            
            setImage(tree);

        //    im.setColor(green);
        }
       // im.fillOval(0, 0, SIZE-1, SIZE-1);
       // setImage(im);
     //   Utilities.repaint();
     }

    public Collection getNeighbors() {
        return getNeighbours(2,true,Tree.class);
    }
 
 
    private void preAct()
    {
        doBurnNeighbors = false;
            
        if(burning) {
            doBurnNeighbors = true;
        }
    }
    
    private void doAct() 
    { 
        if(doBurnNeighbors) {
            Collection neighbors = getNeighbors();
            for(Iterator i = neighbors.iterator(); i.hasNext(); ) {
                Tree tree = (Tree) i.next();
                tree.burn();
            }
            
            burnt = true;
            burning = false;
            draw();
        }
    }
    
    public void act()
    {
        if(inPreAct) {
            preAct();
        } else {
            doAct();
        }
        inPreAct = ! inPreAct;
    }
    
    /**
     * Burn this tree.
     */
    public void burn()
    {
        if(!burning && !burnt) {
            burning = true;
            draw();
        }
        //to be deleted when gf is fixed:
        //Utilities.repaint();
    }
}