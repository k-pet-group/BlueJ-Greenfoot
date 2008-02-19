import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * Block class implements the basic aspects of a MineSweeper block.
 * This is three things, to check for input (i.e. have you clicked
 * on the block) to monitor their state (are they flagged or have they changed)
 * and to call the appropriate state change when necessary (are they a bomb or empty?).
 * These are implemented further in the EmptyBlock and MineBlock subclasses.
 * 
 * A Block can be 'changed' where they have been clicked on and have changed
 * to their appropriate state (change to a bomb or an empty square).
 * They can also be 'flagged' which stops them from being changed and the
 * Block image will change to one with a little flag on it.
 * 
 * @author Joseph Lenton - JL235@Kent.ac.uk
 * @version 28/01/08
 */
public abstract class Block extends Actor
{
    // standard block image
    private static final GreenfootImage BLOCK = new GreenfootImage("block.png");
    // standard image if they have been flagged
    private static final GreenfootImage FLAGGED_BLOCK = new GreenfootImage("flagged_block.png");
    
    // have they changed into their shown state
    private boolean changed;
    // record if they have been flagged
    private boolean flagged;
    
    /**
     * Trivial constructor setting up the Block.
     */
    public Block() {
        changed = false;
        flagged = false;
    }
    
    /**
     * The Block will listen for mouse input (right and left mouse button click)
     * and then take the appropriate action.
     */
    public void act() 
    {
        if (Greenfoot.mousePressed(this)) {
            MouseInfo mouse = Greenfoot.getMouseInfo();
            
            // left mouse button click and it's not flagged
            if (mouse.getButton() == 1 && !isFlagged()) {
                onChange();
            // right mouse button click
            } else if (mouse.getButton() == 3) {
                setFlagged();
            }
        }
    }
    
    /**
     * @return True if it has been flagged, false if not.
     */
    public boolean isFlagged() {
        return flagged;
    }
    
    /**
     * @return True if it has changed into it's true form (bomb or empty).
     */
    public boolean hasChanged() {
        return changed;
    }
    
    /**
     * Changes the Block into it's true form.
     */
    protected void onChange() {
        changed = true;
        changeImage();
    }
    
    /**
     * If the Block hasn't changed, it will be 'flagged' and
     * can no longer be clicked on.
     */
    protected void setFlagged() {
        // if it hasn't changed yet
        if (!hasChanged()) {
            // invert the flag
            flagged = !flagged;
            // update to appropriate Block image
            if (isFlagged()) {
                setImage(FLAGGED_BLOCK);
            } else {
                setImage(BLOCK);
            }
        }
    }
    
    /**
     * Called when the Block changes state.
     * It updates the image to it's new appropriate image
     * for the changed state.
     */
    public abstract void changeImage();
}
