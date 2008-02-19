import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * The MineSweeper world itself.
 * This creates all the mines and places them all randomly
 * into the world. The empty spaces are then padded out by empty
 * block spaces.
 * 
 * When the game ends the simulation will stop running.
 * It also records when the game ends incase the simulation
 * is restarted.
 * 
 * @author Joseph Lenton - JL235@Kent.ac.uk
 * @version 28/01/08
 */
public class MineSweeper extends World
{
    /* these three variables can be altered to customize
     * the minesweeper game */
    // the width of the world
    private static final int WIDTH = 9;
    // the height of the world
    private static final int HEIGHT = 9;
    // the number of mines in the world
    private static final int MINES = 10;
    
    // do not alter, this is the number of free squares in the game
    private static final int FREE_SQUARES = WIDTH*HEIGHT - MINES;
    
    // the number of free squares the player has found
    private int freeSquares;
    
    private boolean isGameOver;
    
    /**
     * Generates the minesweeper world.
     */
    public MineSweeper()
    {
        super(WIDTH, HEIGHT, 32);
        
        // there must be free squares for the player to be able to complete the game
        if (FREE_SQUARES <= 0) {
            throw new IllegalStateException(
                "This mine sweeper is impossible, no free squares!");
        }
        
        freeSquares = 0;
        int mineCount = 0;
        // repeatedly try looking for random empty places to place a mine
        while (mineCount < MINES) {
            int x = Greenfoot.getRandomNumber(WIDTH);
            int y = Greenfoot.getRandomNumber(HEIGHT);
            
            if (getObjectsAt(x, y, MineBlock.class).size() == 0) {
                addObject(new MineBlock(), x, y);
                mineCount++;
            }
        }
        
        // fill the rest of the world with empty blocks
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getWidth(); y++) {
                if (getObjectsAt(x, y, MineBlock.class).size() == 0) {
                    addObject(new EmptyBlock(), x, y);
                }
            }
        }
        
        isGameOver = false;
        
        setPaintOrder(ScreenOverlay.class, Block.class);
    }
    
    /**
     * Tells the World that an empty block has been found.
     * Needed for informing the World about the players progress
     * (i.e. have they found all the empty blocks)
     * @param block The EmptyBlock that was found.
     */
    public void foundBlock(EmptyBlock block) {
        // if the block hasn't already been changed
        if (!block.hasChanged()) {
            // count it
            freeSquares++;
            // show all blocks if the game is completed
            if (freeSquares == FREE_SQUARES) {
                showAll();
                addObject(new YouWinOverlay(), 0, 0);
                
                endGame();
            }
        }
    }
    
    /**
     * Tells the World that a mine block has been found.
     * Needed for informing the World if the player has
     * stepped on a mine.
     * @param block The MineBlock that was found.
     */
    public void foundBlock(MineBlock block) {
        showAll();
        addObject(new GameOverOverlay(), 0, 0);
        
        endGame();
    }
    
    /**
     * Changes the World so that it is now the
     * end of the game. It stops the simulation from running.
     */
    private void endGame() {
        isGameOver = true;
        Greenfoot.stopSimulation();
    }
    
    /**
     * Changes all the Empty and Mine blocks so that
     * they are all revealed.
     */
    private void showAll() {
        for (Object b : getObjects(EmptyBlock.class)) {
            ((EmptyBlock)b).changeImage();
        }
        
        for (Object m : getObjects(MineBlock.class)) {
            ((MineBlock)m).changeImage();
        }
    }
    
    /**
     * @return True if the game has already ended, false if not.
     */
    public boolean isGameOver() {
        return isGameOver;
    }
}
