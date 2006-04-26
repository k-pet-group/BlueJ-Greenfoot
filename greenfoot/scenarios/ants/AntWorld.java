import greenfoot.World;
import greenfoot.Actor;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.util.Random;
 
public class AntWorld extends World
{
    public static final int RESOLUTION = 1;
    public static final int SIZE = 640; 
  
    private static Random randomizer = new Random();

    public static Random getRandomizer()
    {
        return randomizer; 
    }
    

    public AntWorld() {
        super(SIZE/RESOLUTION,SIZE/RESOLUTION,RESOLUTION);       
        GreenfootImage background = new GreenfootImage("images/sand.jpg");
        setBackground(background);
    }
    
    public void scenario1()
    {
        newObject(new AntHill(70), SIZE / 2, SIZE / 2);
        newObject(new Food(), SIZE/2, SIZE/2 - 260);
        newObject(new Food(), SIZE/2 + 215, SIZE/2 - 100);
        newObject(new Food(), SIZE/2 + 215, SIZE/2 + 100);
        newObject(new Food(), SIZE/2, SIZE/2 + 260);
        newObject(new Food(), SIZE/2 - 215, SIZE/2 + 100);
        newObject(new Food(), SIZE/2 - 215, SIZE/2 - 100);
    }

    public void scenario2()
    {
        newObject(new AntHill(40), 546, 356);
        newObject(new AntHill(40), 95,267);
        
        newObject(new Food(), 80, 71);
        newObject(new Food(), 291, 56);
        newObject(new Food(), 516, 212);
        newObject(new Food(), 311, 269);
        newObject(new Food(), 318, 299);
        newObject(new Food(), 315, 331);
        newObject(new Food(), 141, 425);
        newObject(new Food(), 378, 547);
        newObject(new Food(), 566, 529);
    }

    private void newObject(Actor obj, int x, int y)
    {
        addObject(obj, x, y);
    }
    
    public int getResolution() {
        return RESOLUTION;
    }
}