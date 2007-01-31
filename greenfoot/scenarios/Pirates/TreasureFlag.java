import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;
public class TreasureFlag extends DirectionFlag
{
    private static final int ImageSize = 50;
    private static final int StartingLife = 200;
    private int life;
    
    public TreasureFlag(int direction)
    {
        super.direction = direction-4;
        super.random = 6;
        life = StartingLife;
        
        setImage(new GreenfootImage(1, 1));
    }

    public void act()
    {
        life--;
        if (life == 0) {
            getWorld().removeObject(this);
        }
        else {
            updateImage();
        }
    }
    
    private void updateImage()
    {
        GreenfootImage image = getImage();
        if (image.getWidth() != ImageSize) {
            image.scale(ImageSize, ImageSize);
        }
        image.clear();
        int alpha = (int) (255*(life/(StartingLife*1.2)));
        image.setColor(new Color(255, 255, 255, alpha));
        image.fillOval(0, 0, ImageSize, ImageSize);
    }
}