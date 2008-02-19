import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * MineBlock represents a mine in MineSweeper. If it's clicked on
 * then it is the end of the game. It will also look like a Greenfoot
 * highlighted with a red ring.
 * 
 * It will also change at the end of the game to just a Greenfoot to
 * show that it was a bomb.
 * 
 * @author Joseph Lenton - JL235@Kent.ac.uk
 * @version 13/02/08
 */
public class MineBlock extends Block
{
    // the image for a bomb
    private static final GreenfootImage BOMB = new GreenfootImage("bomb.png");
    // the bomb image highlighted with a circle incase the user has clicked on it
    private static final GreenfootImage DETONATE_BOMB = new GreenfootImage("detonate_bomb.png");
    // record if it's been detonated (clicked on) or not
    private boolean detonated;
    
    /**
     * Trivial constructor setting up the MineBlock.
     */
    public MineBlock() {
        detonated = false;
    }
    
    /**
     * Check for mouse input and acts accordingly.
     * See the Block class for more information.
     */
    public void act() {
        super.act();
    }
    
    /**
     * Change from a Block to the MineBlock.
     * This also depends on if the mine was clicked on or not,
     * as this is called at the end of the game.
     */
    protected void onChange() {
        MineSweeper world = (MineSweeper)getWorld();
        
        // if the game is still running, then this must have been clicked on
        if (!world.isGameOver()) {
            // so inform the world that it's been clicked on
            world.foundBlock(this);
            // and state that it's detonated
            detonated = true;
            super.onChange();
        }
    }
    
    /**
     * Changes to either an image of the bomb highlighted
     * or to one of a bomb on it's own, depending on if
     * the user clicked on the MineBlock or not.
     */
    public void changeImage() {
        // display the appropriate image
        if (detonated) {
            setImage(DETONATE_BOMB);
        } else {
            setImage(BOMB);
        }
    }
}
