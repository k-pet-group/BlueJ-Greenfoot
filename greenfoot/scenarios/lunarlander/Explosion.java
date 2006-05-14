import greenfoot.World;
import greenfoot.Actor;
import greenfoot.GreenfootImage;
import greenfoot.Greenfoot;

import java.util.*;

public class Explosion extends Actor
{

    private final static int IMAGE_COUNT= 8;
    private static GreenfootImage[] images;
    private int size=0;
    private int increment=1;
    
    static {
        GreenfootImage baseImage = new GreenfootImage("images/explosion.png");
        int maxSize = baseImage.getWidth();
        int delta = maxSize / (IMAGE_COUNT+1);
        int size = 0;
        images = new GreenfootImage[IMAGE_COUNT];
        for(int i=0; i < IMAGE_COUNT; i++) {
            size = size + delta;
            images[i] = new GreenfootImage(baseImage);
            images[i].scale(size, size);
        }
    }
    
    public Explosion() {
        setImage(images[0]);
    }
    
    public void act()
    { 
        setImage(images[size]);

        size += increment;
        if(size>=IMAGE_COUNT) {
            increment = -increment;
            size += increment;
        }
        
        List explodeEm = getIntersectingObjects(null);
        
        Iterator i = explodeEm.iterator();
        while(i.hasNext()) {
            Actor a = (Actor) i.next();
            if( ! (a instanceof Explosion)) {
                int x = a.getX();
                int y = a.getY();
                getWorld().removeObject(a);
                getWorld().addObject(new Explosion(), x, y);
            }
        }
        if(size <= 0) {
            getWorld().removeObject(this);
        }
    }

}