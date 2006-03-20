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
        super(SIZE/RESOLUTION,SIZE/RESOLUTION,RESOLUTION);       
        GreenfootImage background = new GreenfootImage("images/sand.jpg");
        background.setTiled(true);
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