import greenfoot.World;
import greenfoot.Actor;
import greenfoot.GreenfootImage;
import greenfoot.Greenfoot;

import java.awt.Color;

public class Paddle extends Actor
{
    public Paddle()
    {
        GreenfootImage pic = new GreenfootImage(60, 10);
        pic.fill(Color.GREEN);
        setImage(pic);
    }
    
    public void setLocation(int x, int y)
    {
        y = BrickWorld.SIZEY - 20;
        super.setLocation(x,y);
    }

    public void act()
    {
        // The paddle can be dragged with the mouse, but it can also be
        // controlled by the directional arrow keys
        
        int xdir = 0;
        if (Greenfoot.isKeyDown("left")) {
            xdir = -3;
        }
        if (Greenfoot.isKeyDown("right")) {
            xdir = 3;
        }
        
        int newx = getX() + xdir;
        if (newx >= 0 && newx < getWorld().getWidth()) {
            super.setLocation(newx,getY());
        }
    }
}
