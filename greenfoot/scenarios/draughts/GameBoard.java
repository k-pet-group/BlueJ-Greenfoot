import greenfoot.*;
import greenfoot.Actor;
import greenfoot.World;
import greenfoot.GreenfootImage;

import java.util.Collection;

public class GameBoard extends World
{
    private Player playerOne;
    private Player playerTwo;
    
    /**
     * Creates a new world with 20x20 cells and
     * with a cell size of 50x50 pixels
     */
    public GameBoard() 
    {
        super(8,8,50);
        GreenfootImage background = new GreenfootImage("board.gif");
        setBackground(background);
        background.setTiled(true);
        playerOne = new Player("blue", true);
        addObject(playerOne);
        playerTwo = new Player("red", false);
        addObject(playerTwo);
        playerOne.setupPieces();
        playerTwo.setupPieces();
        //player one to start...
        playerOne.setTurn(true);
    }
    
    public static void playGame()
    {
        
    }
    
    public synchronized void removeObject(Actor object)
    {
        super.removeObject(object);
        if(object instanceof Draught) {
            playerOne.removePiece(object);
            playerTwo.removePiece(object);
        }
    }
    
    
    
    public boolean isOccupied(int x, int y)
    {
        Collection coll = getObjectsAt(x, y, Draught.class);
        return !coll.isEmpty();
//        // this is a bit of a hack seeing we have 2 hidden player objects at 0,0
//        if(x != 0 || y!= 0) 
//            return !(coll.isEmpty());
//        else if(!coll.isEmpty()) {
//            Iterator it = coll.iterator();
//            while(it.hasNext()) {
//                if(it.next() instanceof Draught)
//                    return true;
//            }            
//        }
//        return false;
    }
}