import greenfoot.GreenfootObject;
import greenfoot.Image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Food extends GreenfootObject
{
    private static Random randomizer = AntWorld.getRandomizer();

    private static final int SIZE = 30;
    private static final int HALFSIZE = SIZE / 2;
    private static final int color1 = new Color(160, 200, 60).getRGB();
    private static final int color2 = new Color(80, 100, 30).getRGB();
    private static final int color3 = new Color(10, 50, 0).getRGB();
    
    private int crumbs = 100;
    
    public Food()
    {
        updateImage();
    }

    public void act()
    {
        //here you can create the behaviour of your object
    }

    /**
     * 
     */
    public void takeSome()
    {
        crumbs = crumbs - 3;
        if(crumbs <= 0) {
            getWorld().removeObject(this);
        }
        else {
            updateImage();
        }
    }
    
    /**
     * Update the image
     */
    private void updateImage() 
    {
        BufferedImage image = new BufferedImage(SIZE, SIZE,
                                                BufferedImage.TYPE_INT_ARGB);
        int left = getX();
        int top = getY();
        
        for(int i=0; i<crumbs; i++) {
            int x = randomCoord();
            int y = randomCoord();
            
            image.setRGB(x, y, color1);
            image.setRGB(x+1, y, color2);
            image.setRGB(x, y+1, color2);
            image.setRGB(x+1, y+1, color3);
        }
        setImage(new Image(image));
    }
    
    private int randomCoord()
    {
        int val = HALFSIZE + (int)(randomizer.nextGaussian() * (HALFSIZE/2));
        if(val < 0)
            return 0;
        if(val > SIZE-2)
            return SIZE-2;
        else
            return val;
    }
    
}