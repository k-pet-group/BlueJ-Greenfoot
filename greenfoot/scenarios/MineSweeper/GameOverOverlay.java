import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A simple class which draws the game over image to the centre of the overlay.
 * By extending the overlay class, we can guarantee this is always
 * drawn to the centre of the world.
 * 
 * Simply draws the GameOver image centred in the world.
 * 
 * @author Joseph Lenton - JL235@Kent.ac.uk
 * @version 13/02/08
 */
public class GameOverOverlay extends ScreenOverlay
{
    // the game over image
    private static final GreenfootImage GAME_OVER = new GreenfootImage("game_over.png");
    
    public void addedToWorld(World world) {
        super.addedToWorld(world);
        // draw the game over image to the actors image
        getImage().drawImage(
            GAME_OVER,
            getWidth()/2 - GAME_OVER.getWidth()/2,
            getHeight()/2 - GAME_OVER.getHeight()/2);
    }
}
