
import java.util.Random;

/**
 ** Class Game - simple card game
 ** 
 ** Author: 
 ** Date: 
 **/
public class Game
{
    private static Random random = new Random();

    // instance variables
    private Card userCard = null;
    private Card computerCard = null;

    /**
     ** Constructor for objects of class Game
     **/
    public Game()
    {
	// initialise instance variables
    }

    /**
     * method comment here
     */
    public void play(String suit, int val)
    {
        if(! getUserCard(suit, val))
	  System.out.println("Illegal card values!");
	else if(! getComputerCard())
	  System.out.println("Error: computer card illegal!");
	
	evaluate();	  
    }

    /**
     ** Get a card for the user
     **/
    private boolean getUserCard(String suit, int val)
    {
	userCard = new Card(val, suit);
	if(userCard.isLegal())
	    return true;
	else {
	    userCard = null;
	    return false;
	}
    }

    /**
     ** Get a card for the computer
     **/
    private boolean getComputerCard()
    {
	int val = random.nextInt();
	val = (val<0 ? -val : val) % 13 + 2;
	computerCard = new Card(val, "hearts");
	if(computerCard.isLegal())
	    return true;
	else {
	    computerCard = null;
	    return false;
	}
    }

    /**
     * method comment here
     */
    private void evaluate()
    {
        if(computerCard == null || userCard == null)
	   System.out.println("One of the players does not have a valid card.");
	else {
	   if(isDraw())
		System.out.println("The game is a draw");
	   else if(computerWins())
		System.out.println("The computer wins");
	   else
		System.out.println("The player wins");
	}
    }

    /**
     * check whether both players are equal
     */
    private boolean isDraw()
    {
	if(computerCard == null || userCard == null)
	    return false;
	if(computerCard.isEqual(userCard))
	    return true;
	else
            return false;
    }

    /**
     * method comment here
     */
    private boolean computerWins()
    {
	if(computerCard.isRed() && computerCard.isHigher(userCard))
	    return true;
	else 
	    return false;
    }

}
