import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.List;

/**
 * An EmptyBlock represents an empty space in MineSweeper.
 * When changed it will display how many mines are adjacent to itself.
 * 
 * @author Joseph Lenton - JL235@Kent.ac.uk
 * @version 13/02/08
 */
public class EmptyBlock extends Block
{
    // images for the changed state
    // either it's an empty block or an empty block with 1 to 8 written in it
    private static final GreenfootImage EMPTY = new GreenfootImage("empty.png");
    private static final GreenfootImage EMPTY_1 = new GreenfootImage("empty_1.png");
    private static final GreenfootImage EMPTY_2 = new GreenfootImage("empty_2.png");
    private static final GreenfootImage EMPTY_3 = new GreenfootImage("empty_3.png");
    private static final GreenfootImage EMPTY_4 = new GreenfootImage("empty_4.png");
    private static final GreenfootImage EMPTY_5 = new GreenfootImage("empty_5.png");
    private static final GreenfootImage EMPTY_6 = new GreenfootImage("empty_6.png");
    private static final GreenfootImage EMPTY_7 = new GreenfootImage("empty_7.png");
    private static final GreenfootImage EMPTY_8 = new GreenfootImage("empty_8.png");
    
    // how many surrounding blocks are surrounding this block
    private int surroundingBlocks;
    
    public void addedToWorld(World world) {
        // get how many blocks are surrounding this one
        surroundingBlocks = getNeighbours(1, true, MineBlock.class).size();
    }
    
    /**
     * Is this empty block next to a mine?
     * This takes into account mines that are horizontally, vertically
     * or diagonally adjacent to this one.
     * @return True if it is adjacent to a mine, False if not.
     */
    public boolean isNextToMine() {
        return surroundingBlocks > 0;
    }
    
    /**
     * Check for mouse input and act accordingly.
     * See Block class.
     */
    public void act() {
        super.act();
    }
    
    /**
     * Change from a Block into an Empty block.
     */
    protected void onChange() {
        // check if the game has not ended
        if (!((MineSweeper)getWorld()).isGameOver()) {
            // inform the world that this block has been changed
            ((MineSweeper)getWorld()).foundBlock(this);
            // inform the super class of it's own change
            super.onChange();
            
            // get all the neighbours that are also empty blocks
            List neighbours = getNeighbours(1, true, EmptyBlock.class);
            for (Object obj : neighbours) {
                EmptyBlock b = (EmptyBlock)obj;
                // and change their image too, unless they have changed
                if (!b.hasChanged()) {
                    /* recursively calls the neighbour to change,
                     * who changes their neighbour, and so on.
                     * 
                     * The If condition is to ensure that recursively you
                     * change as many blocks as possible, but stop when you
                     * reach a block next to a mine.
                     * 
                     * This causes the cleared area to be a group of empty blocks
                     * on their own surrounded by blocks next to mines. It looks nice!
                     */
                    if (!isNextToMine() || (isNextToMine() && !b.isNextToMine())) {
                        b.onChange();
                    }
                }
            }
        }
    }
    
    /**
     * Update the image to that of either an empty block,
     * or an empty block saying how many mines are next to it.
     */
    public void changeImage() {
        /* trivial switch statement deciding which image to draw
         * based on the number of mines surrounding this block. */
        switch (surroundingBlocks) {
            case 0:
                setImage(EMPTY);
                break;
            case 1:
                setImage(EMPTY_1);
                break;
            case 2:
                setImage(EMPTY_2);
                break;
            case 3:
                setImage(EMPTY_3);
                break;
            case 4:
                setImage(EMPTY_4);
                break;
            case 5:
                setImage(EMPTY_5);
                break;
            case 6:
                setImage(EMPTY_6);
                break;
            case 7:
                setImage(EMPTY_7);
                break;
            case 8:
                setImage(EMPTY_8);
                break;
            }
    }
}
