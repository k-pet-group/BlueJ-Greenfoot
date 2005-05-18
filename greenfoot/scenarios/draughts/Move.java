/*
 * 
 *
 * Typesafe enum to represent available movements in a draughts game
 * eg. FORWARD_LEFT, FORWARD_RIGHT, BACK_LEFT, BACK_RIGHT
 */


public class Move
{
    private final String name;
    public static final Move FORWARD_LEFT = new Move("FORWARD_LEFT");
    public static final Move FORWARD_RIGHT = new Move("FORWARD_RIGHT");
    public static final Move BACK_LEFT = new Move("BACK_LEFT");
    public static final Move BACK_RIGHT = new Move("BACK_RIGHT");
    private static Move[] moves = {FORWARD_LEFT, FORWARD_RIGHT, BACK_LEFT, BACK_RIGHT};
    
    private Move(String name)
    {
        this.name = name;
    }
    
    public String toString()
    {
        return name;
    }
    
    public static Move[] values()
    {
        return moves;
    }   
}
