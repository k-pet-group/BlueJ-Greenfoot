import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;

/**
 * When placed, it draws the you win image centred in the world.
 * 
 * @author Joseph Lenton
 * @version 13/02/08
 */
public class YouWinOverlay extends ScreenOverlay
{
    private static final GreenfootImage YOU_WIN = new GreenfootImage("you_win.png");
    
    public void addedToWorld(World world) {
        super.addedToWorld(world);
        getImage().drawImage(
                YOU_WIN,
                getImage().getWidth()/2 - YOU_WIN.getWidth()/2,
                getImage().getHeight()/2 - YOU_WIN.getHeight()/2);
    }
}
