import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;
/**
 * Level Class
 * @author Joseph Lenton
 * @date 06/02/07
 * 
 * The Level Class sets up the level of Pac-Man.
 * It uses an array storing the setup of the level,
 * and reads it sets up the world on the arrays information.
 * This level array contains information of every object
 * to be created in world.
 * 
 * It will also store the GhostHealer object it finds in
 * the level array, and return it when asked.
 * 
 * Although the world has each cell the size of one pixel,
 * it uses it's own cell size to position the level into cells
 * for placing all the objects perfectly alligned, to each other.
 */
public class Level extends World
{
    // the space between each world object when being placed
    private static final int CELL_SIZE = 20;
    
    // the levels ghostHealer
    private GhostHealer ghostHealer;
    // the level
    private int[] level = {
     1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
     1,2,2,2,2,2,2,2,2,2,2,2,2,1,1,2,2,2,2,2,2,2,2,2,2,2,2,1,
     1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1,
     1,3,1,0,0,1,2,1,0,0,0,1,2,1,1,2,1,0,0,0,1,2,1,0,0,1,3,1,
     1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1,
     1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
     1,2,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,2,1,
     1,2,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,2,1,
     1,2,2,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,2,2,1,
     1,1,1,1,1,1,2,1,1,1,1,1,0,1,1,0,1,1,1,1,1,2,1,1,1,1,1,1,
     0,0,0,0,0,1,2,1,1,1,1,1,0,1,1,0,1,1,1,1,1,2,1,0,0,0,0,0,
     0,0,0,0,0,1,2,1,1,0,0,0,0,0,0,0,0,0,0,1,1,2,1,0,0,0,0,0,
     0,0,0,0,0,1,2,1,1,0,1,1,1,6,6,1,1,1,0,1,1,2,1,0,0,0,0,0,
     1,1,1,1,1,1,2,1,1,0,1,0,0,0,0,0,0,1,0,1,1,2,1,1,1,1,1,1,
     0,0,0,0,0,0,2,0,0,0,1,5,5,5,5,5,0,1,0,0,0,2,0,0,0,0,0,0,
     1,1,1,1,1,1,2,1,1,0,1,0,0,7,0,0,0,1,0,1,1,2,1,1,1,1,1,1,
     0,0,0,0,0,1,2,1,1,0,1,1,1,1,1,1,1,1,0,1,1,2,1,0,0,0,0,0,
     0,0,0,0,0,1,2,1,1,0,0,0,0,0,0,0,0,0,0,1,1,2,1,0,0,0,0,0,
     0,0,0,0,0,1,2,1,1,0,1,1,1,1,1,1,1,1,0,1,1,2,1,0,0,0,0,0,
     1,1,1,1,1,1,2,1,1,0,1,1,1,1,1,1,1,1,0,1,1,2,1,1,1,1,1,1,
     1,2,2,2,2,2,2,2,2,2,2,2,2,1,1,2,2,2,2,2,2,2,2,2,2,2,2,1,
     1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1,
     1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1,
     1,3,2,2,1,1,2,2,2,2,2,2,2,4,0,2,2,2,2,2,2,2,1,1,2,2,3,1,
     1,1,1,2,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,2,1,1,1,
     1,1,1,2,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,2,1,1,1,
     1,2,2,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,2,2,1,
     1,2,1,1,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,1,1,2,1,
     1,2,1,1,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,1,1,2,1,
     1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
     1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1
    };
    
    /**
     * Creates and sets up the Pac-Man level.
     */
    public Level()
    {
        // creates the world
        super(28*CELL_SIZE, 31*CELL_SIZE, 1);
        // creates a background image, colours it black
        // and then sets it as the background of the level.
        GreenfootImage background = new GreenfootImage(28*CELL_SIZE, 31*CELL_SIZE);
        background.setColor(Color.black);
        background.fill();
        setBackground(background);
        
        // read the array and generate the level
        generateLevel();
    }
    
    /**
     * Reads through the level array and creates
     * objects to be placed in the world depending on
     * what integers it finds.
     * The objects it creates are also aligned by the levels
     * own cell size.
     */
    private void generateLevel()
    {
        int i = 0;
        for (int y = CELL_SIZE/2; y < 31*CELL_SIZE; y += CELL_SIZE) {
            for (int x = CELL_SIZE/2; x < 28*CELL_SIZE; x += CELL_SIZE) {
                // Wall
                if (level[i] == 1) {
                   addObject( new Wall(), x, y );
                }
                // Food
                else if (level[i] == 2) {
                    addObject( new Food(), x, y );
                }
                // Energizer
                else if (level[i] == 3) {
                     addObject( new Energizer(), x, y );
                }
                // Player (PacMan)
                else if (level[i] == 4) {
                    // appears half a CELL_SIZE to the right,
                    // so it is between two cells
                    addObject (new PacMan(), x+CELL_SIZE/2, y );
                }
                // Ghost
                else if (level[i] == 5) {
                    // appears half a CELL_SIZE to the right,
                    // so it is between two cells
                    addObject (new Ghost(), x+CELL_SIZE/2, y );
                }
                // Pac Man Wall
                else if (level[i] == 6) {
                    addObject( new PacManWall(), x, y );
                }
                // Ghost Healer
                else if (level[i] == 7) {
                    ghostHealer = new GhostHealer();
                    addObject( ghostHealer, x+CELL_SIZE/2, y );
                }
                i++;
            }
        }
    }
    
    /**
     * returns the ghostHealer, where Ghost's go to be healed.
     * @retuen the world's ghostHealer
     */
    public GhostHealer getGhostHealer()
    {
        return ghostHealer;
    }
}

