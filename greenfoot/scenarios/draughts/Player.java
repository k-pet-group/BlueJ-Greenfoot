import greenfoot.GreenfootObject;

import java.util.*;

/**
 *
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Player extends GreenfootObject
{
    private static Player defaultPlayer = new Player();;
    private boolean isTop = true;
    private Direction direction;
    private String colour;
    boolean isMyTurn = false;
    
    protected List myDraughts;

    /**
     * default constructor
     */
    public Player()
    {
       this("red", true);       
    }
    
    
    /**
     * Player constructor
     * @param colour either "blue", "red" or "yellow"
     * @param isTop whetehr they are the player at the top of the board
     */
    public Player(String colour, boolean isTop)
    {
        setImage("player.gif");
        myDraughts = new ArrayList();
        this.isTop = isTop;
        if(isTop)
            direction = Direction.DOWNWARD;
        else
            direction = Direction.UPWARD;
        
        this.colour = colour;
        //setupPieces();
    }
    
    /**
     * This is a workaround for Greenfoot's requirement
     *  for a default constructor
     * @return default player
     */
    public static Player getDefaultPlayer()
    {               
        return defaultPlayer;
    }
    
    
    /**
     * remove an object (Draught) from the List of this player
     * @param piece piece to remove
     */
    public synchronized void removePiece(Object piece)
    {
        myDraughts.remove(piece);
    }

    
    /**
     * the main act method to define behaviour, i.e. what to do 
     * with your pieces when it is your turn
     * @see greenfoot.GreenfootObject#act()
     */
    public void act()
    {
        if(isMyTurn) {
            // shuffle to make the moves less predictable
            // a smarter solution would analyse the moves to 
            // work out which is the best
            Collections.shuffle(myDraughts);
           
            // keep track on whether we have made a move
            boolean moveTaken = false;            
            Iterator it = myDraughts.iterator();
            while(it.hasNext()) {
                Draught piece = (Draught)it.next();
                List takes = piece.getAvailableTakeMoves();
                // we want to take as many as we can
                while(!takes.isEmpty()) {
                    Move m = (Move)takes.get(Draught.getRandom(takes.size()));
                    piece.take(m);
                    takes = piece.getAvailableTakeMoves();
                    moveTaken = true;
                }
            }
            
            // if we found nothing to take lets try a normal move
            if(!moveTaken) {
                it = myDraughts.iterator();
                while(it.hasNext()) {
                    Draught piece = (Draught)it.next();
                    List l = piece.getAvailableMoves();
                    if(!l.isEmpty()) {
                        Move m = (Move)l.get(Draught.getRandom(l.size()));
                        piece.move(m);
                        break;                
                    }
                } 
            }
            isMyTurn = false;
        }
        else
            isMyTurn = true;
    }
    
    /**
     * A player has a Direction which represents their orientation.
     * This is needed when calculating their moves.
     * @return the direction of travel (either up or down)
     */
    public Direction getDirection()
    {
        return direction;
    }
    
    /**
     * Set up the pieces to start a game.
     *
     */
    public void setupPieces()
    {
        int start = 0;
        if(!isTop)
            start = 5;
            
        int x = 0, y = start;
        while(y < (start + 3)) {
            x = y % 2;
            while(x < 8) {
                Draught d = new Draught(direction, colour);
                d.setLocation(x, y);
                x += 2;
                getWorld().addObject(d);
                myDraughts.add(d);
            }
            y++;
        }
    }
    
    /**
     * 
     * @return whether this player at the top of the board
     */
    public boolean isTop()
    {
        return isTop;
    }
    
    /**
     * 
     * @param isTurn flag to represent whether this player's 
     *  turn is next
     */
    public void setTurn(boolean isTurn)
    {
        isMyTurn = isTurn;
    }
    
    
  
  

}