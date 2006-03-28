import greenfoot.Actor;

import java.util.*;
public class Draught extends Actor
{
    private boolean isKing;
    private String draughtColour;
    private GameBoard board;
    private int direction;
    private static Random randomiser = new Random();

    public Draught()
    {        
        this(Direction.UPWARD, "red");
        //System.out.println("Default Draught!!!!");
    }
    
    public Draught(Direction dir, String colour)
    {
        draughtColour = colour;
        String image = null;
        if(colour.equals("blue"))
            image = draughtColour + "-draught.gif";
        else if(colour.equals("red"))
            image = "red-draught.gif";
        else if(colour.equals("yellow"))
            image = "yellow-draught.gif";
      
        //check that one of these has been set
        if(image != null)
            setImage(image);
        
        isKing = false;
        this.direction = dir.getOrientation();
    }

    /**
     * The piece does not act directly itself, it is controlled 
     * by its owner, who is a Player.
     */
    public void act()
    {
        //here you can create the behaviour of your object
    }
    
    /**
     * Make this a King that can then move backwards and forwards
     * @param isKing
     */
    public void setKing(boolean isKing)
    {
        this.isKing = isKing;
        if(isKing)
            setImage(draughtColour + "-draught-king.gif");
        else
            setImage(draughtColour + "-draught.gif");
    }
    
    /**
     * Check whther this is a King. i.e. it has reached the opponent's side
     * @return
     */
    public boolean isKing()
    {
        return isKing;
    }
    
    /**
     * Get a List that holds all available non-taking moves for this piece
     * @return
     */
    public List getAvailableMoves()
    {
        List moves = new ArrayList();
               
        int newX = getX();
        int newY = getY();
        
        Move[] moveArray = Move.values();
        for(int i = 0; i < moveArray.length; i++) {
            if(canMove(moveArray[i])) {
                moves.add(moveArray[i]);
                //System.out.println("Available: " + moveArray[i]);
            }
        }  
        return moves;
    }
    
    /**
     * get a List of all moves that involve taking an opponent
     * @return
     */
    public List getAvailableTakeMoves()
    {
        List moves = new ArrayList();
               
        int newX = getX();
        int newY = getY();
        
        Move[] moveArray = Move.values();
        for(int i = 0; i < moveArray.length; i++) {
            if(canTake(moveArray[i])) {
                moves.add(moveArray[i]);
            }
        }  
        return moves;
    }
    
    /**
     * Check whether a piece can move in one of four directions.
     * @param move
     * @return
     */
    public boolean canMove(Move move)
    {
        boolean occupied = false;
        
        int newX = getX();
        int newY = getY();
        //if( newX >= 0 && newY >= 0) {
            
        if(move.equals(Move.FORWARD_LEFT)) {
            newX += direction;
            newY += direction;
            // check for within bounds
            if(isSquareEmpty(newX, newY)) {
                return true;
            }    
        }
        else if(move.equals(Move.FORWARD_RIGHT)) {
            newX -= direction;
            newY += direction;
            if(isSquareEmpty(newX, newY)) {
                return true;
            }
        }
        if(isKing()) {
            if(move.equals(Move.BACK_LEFT)) {
                newX += direction;
                newY -= direction;
                if(isSquareEmpty(newX, newY)) {
                    return true;
                }
            }
            else if(move.equals(Move.BACK_RIGHT)) {
                newX -= direction;
                newY -= direction;
                if(isSquareEmpty(newX, newY)) {
                    return true;
                }
            }
        }
        return occupied;
    }
    
    /**
     * Check whether a co-ordinate pair is within the bounds of the board
     * @param x
     * @param y
     * @return
     */
    public boolean isInsideBoard(int x, int y)
    {
        if((x >=0 && x <= 7) && (y >=0 && y <= 7)) {
            return true;
        }
        else {
         //System.out.println("Outside board");
            return false;
        }
        
    }
    
    /**
     * Check whether there is a piece to take in a certain direction
     * @param move
     * @return
     */
    public boolean canTake(Move move)
    {
        boolean canTake = false;
        
        int newX = getX();
        int newY = getY();
        //if( newX >= 0 && newY >= 0) {
            
        if(move.equals(Move.FORWARD_LEFT)) {
            newX += direction;
            newY += direction;
            // check for within bounds
            if(!isSquareEmpty(newX, newY)) {
                if(containsOpponent(newX, newY)) {
                        newX += direction;
                        newY += direction;
                        if(isSquareEmpty(newX, newY))
                            return true;                  
                }
            }       
        }    
        else if(move.equals(Move.FORWARD_RIGHT)) {
            newX -= direction;
            newY += direction;
            if(!isSquareEmpty(newX, newY)) {
                if(containsOpponent(newX, newY)) {
                    newX -= direction;
                    newY += direction;
                    if(isSquareEmpty(newX, newY))
                        return true;       
                }
            }  
        }    
        // if it is a king it can also move backwards
        if(isKing()) {
            if(move.equals(Move.BACK_LEFT)) {
                newX += direction;
                newY -= direction;
                if(!isSquareEmpty(newX, newY)) {
                    if(containsOpponent(newX, newY)) {
                        newX += direction;
                        newY -= direction;
                        if(isSquareEmpty(newX, newY))
                            return true;
                        
                    }
                }                
            }                
            else if(move.equals(Move.BACK_RIGHT)) {
                newX -= direction;
                newY -= direction;
                if(!isSquareEmpty(newX, newY)) {
                    if(containsOpponent(newX, newY)) {
                        newX -= direction;
                        newY -= direction;
                        if(isSquareEmpty(newX, newY))
                            return true;
                    }
                }    
            }
        }
        return canTake;      
    }
    
    /**
     * take a piece found in a particular direction. This involves 
     * removing the opponent and moving one square past their old 
     * position.
     * @param move the direction of the piece to take 
     */
    public void take(Move move)
    {
        //boolean canTake = false;
        
        int newX = getX();
        int newY = getY();
            
        if(move.equals(Move.FORWARD_LEFT)) {
            newX += direction;
            newY += direction;
            removeOpponent(newX, newY);
            newX += direction;
            newY += direction;
                
        }    
        else if(move.equals(Move.FORWARD_RIGHT)) {
            newX -= direction;
            newY += direction;
            removeOpponent(newX, newY);
            //setLocation(newX, newY);
            newX -= direction;
            newY += direction;
        }    
        
        if(isKing()) {
            if(move.equals(Move.BACK_LEFT)) {
                newX += direction;
                newY -= direction;
                removeOpponent(newX, newY);
                //setLocation(newX, newY);
                newX += direction;
                newY -= direction;
            }                
            else if(move.equals(Move.BACK_RIGHT)) {
                newX -= direction;
                newY -= direction;
                removeOpponent(newX, newY);
                //setLocation(newX, newY);
                newX -= direction;
                newY -= direction;
            }
        }
        setLocation(newX, newY);  
        checkKingStatus();
    }
    

    /**
     * Check that a square is empty
     * @param x
     * @param y
     * @return
     */
    public boolean isSquareEmpty(int x, int y) 
    {
        if(isInsideBoard(x, y)) {
            Collection coll = getWorld().getObjectsAt(x, y, Draught.class);
            if(coll.isEmpty()) {//already occupied
                //System.out.println("Square is empty");
                return true;    
            }
        }
        
        return false;
    }
    
    /**
     * check whether this this square contains one of "them"
     * @param x
     * @param y
     * @return
     */
    public boolean containsOpponent(int x, int y)
    {
        Collection coll = getWorld().getObjectsAt(x, y, Draught.class);
        Iterator it = coll.iterator();
        if(it.hasNext()) {
            Draught d = (Draught)it.next();
            // its one of them
            if(!this.draughtColour.equals(d.draughtColour)) {
                return true;   
            }
        }
        return false;
    }
    
    /**
     * remove an opponent's piece because I have just jumped them.
     * @param x x co-ord of the piece taken
     * @param y y co-ord of the piece taken
     */
    public void removeOpponent(int x, int y)
    {
        Collection coll = getWorld().getObjectsAt(x, y, Draught.class);
        Iterator it = coll.iterator();
        if(it.hasNext()) {
            Draught d = (Draught)it.next();
            // its one of them
            if(!this.draughtColour.equals(d.draughtColour)) {
                //System.out.println("Removing the sucker...");
                getWorld().removeObject(d);   
            }
        }
    }
    
    public void moveForwardLeft()
    {
        move(Move.FORWARD_LEFT);
    }
    
    public void moveForwardRight()
    {
        move(Move.FORWARD_RIGHT);
    }
    
    public void moveBackLeft()
    {
        move(Move.BACK_LEFT);
    }
    
    public void moveBackRight()
    {
        move(Move.BACK_RIGHT);
    }
    
    /**
     * move this piece in the specified direction
     * @param move the enumerated value for the direction to move
     */
    public void move(Move move)
    {    
        int newX = getX();
        int newY = getY();
        if(move.equals(Move.FORWARD_LEFT)) {
            newX += direction;
            newY += direction;
        }
        else if(move.equals(Move.FORWARD_RIGHT)) {
            newX -= direction;
            newY += direction;
        }
        else if(move.equals(Move.BACK_LEFT)) {
            newX += direction;
            newY -= direction;
        }
        else if(move.equals(Move.BACK_RIGHT)) {
            newX -= direction;
            newY -= direction;
        }
        setLocation(newX, newY);
        checkKingStatus();
    }
    
    /**
     * Is this piece a king? If so, set its image to show that.
     * Should be called after every move where a piece may 
     * then be a king.
     *
     */
    public void checkKingStatus()
    {
        if((direction == Direction.UPWARD.getOrientation()) && (getY() == 0)) {
            setKing(true);
        }
        else if((direction == Direction.DOWNWARD.getOrientation()) && (getY() == 7)) {
            setKing(true);
        }
        
    }
    
    /**
     * get a random number
     * @param max the upper possible value for the random number
     * @return the random number
     */
    public static int getRandom(int max)
    {
        int a = randomiser.nextInt(max);
        return (a);
    }
    
    
    

}