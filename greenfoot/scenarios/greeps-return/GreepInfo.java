/**
 * Information about a Greep.
 * 
 * @author Davin McCall
 * @version 1.0
 */
public class GreepInfo
{
    // instance variables - replace the example below with your own
    private int x;
    private int y;
    private int direction;
    private boolean hasTomato;
    private int state;
    private int [] memory;
    private boolean [] flags;

    /**
     * Constructor for objects of class GreepInfo
     */
    public GreepInfo(Greep greep, int [] memory, boolean [] flags, int state)
    {
        this.x = greep.getX();
        this.y = greep.getY();
        this.direction = greep.getRotation();
        this.hasTomato = greep.carryingTomato();
        this.state = state;
        this.memory = memory;
        this.flags = flags;
    }

    /**
     * Get the X position of the greep
     */
    public int getX()
    {
        return x;
    }
    
    /**
     * Get the Y position of the greep
     */
    public int getY()
    {
        return y;
    }
    
    /**
     * Get the direction (rotation) of the greep
     */
    public int getDirection()
    {
        return direction;
    }
    
    /**
     * Check whether this greep is carrying a tomato
     */
    public boolean hasTomato()
    {
        return hasTomato;
    }
    
    /**
     * Get the greep's state. This will return one of:
     * Greep.MODE_WALKING   - This greep is in the normal state
     * Greep.MODE_BLOCKING  - This greep is blocking opponent greeps
     * Greep.MODE_FLIPPED   - This greep is currently on its back!
     */
    public int getState()
    {
        return state;
    }
    
    /**
     * Get the greep's (int) memory as an array of ints. This only works
     * on friendly greeps; for opponent greeps it will return null.
     */
    public int[] getMemory()
    {
        return memory;
    }
    
    /**
     * Get the greep's (flags) memory as an array of booleans. This only
     * works on friendly greeps; for opponent greeps it will return null.
     */
    public boolean[] getFlags()
    {
        return flags;
    }
}
