import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.util.Random;
 
public class AntWorld extends GreenfootWorld
{
    public static final int RESOLUTION = 1;
    public static final int SIZE = 640; 
  
    private static Random randomizer = new Random();

    public static Random getRandomizer()
    {
        return randomizer; 
    }
    

    public AntWorld() {
        super(SIZE/RESOLUTION,SIZE/RESOLUTION,RESOLUTION, false);       
        GreenfootImage background = new GreenfootImage("sand.jpg");
        background.setTiled(true);
        setBackground(background);
    }
    
    public void scenario1()
    {
        newObject(new AntHill(40), SIZE - 17, SIZE - 20);
        newObject(new Food(), 73, 21);
        newObject(new Food(), 20, 19);
        newObject(new Food(), 36, 86);
    }

    public void scenario2()
    {
        newObject(new AntHill(20), 300, 400);
        newObject(new AntHill(20), 70, 60);
        newObject(new AntHill(20), 410, 320);
        
        newObject(new Food(), 250, 70);
        newObject(new Food(), 250, 160);
        newObject(new Food(), 200, 220);
        newObject(new Food(), 150, 160);
        newObject(new Food(), 70, 270);
    }

    private void newObject(GreenfootObject obj, int x, int y)
    {
        obj.setLocation(x, y);
        addObject(obj);
    }
    
    public int getResolution() {
        return RESOLUTION;
    }
}