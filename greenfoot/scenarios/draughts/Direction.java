/**
 * 
 *
 * Typesafe enum to represent player in a draughts game
 * eg. TOP & BOTTOM whether player is the player at the top
 */


public class Direction
{
    private final String name;
    private final int orientation;
    
    private Direction(String name, int direction)
    {
        this.name = name;
        this.orientation = direction;
    }
    
    public String toString()
    {
        return name;
    }
    
    public int getOrientation()
    {
        return orientation;
    }
       
    public static final Direction DOWNWARD = new Direction("DOWNWARD", 1);
    
    public static final Direction UPWARD = new Direction("UPWARD", -1);
    
}

