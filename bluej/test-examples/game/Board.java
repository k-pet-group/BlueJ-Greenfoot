import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;


/**
 ** Class Board - represents a board in a game simulation.
 ** 
 ** @author: Jason Lowder
 ** @author: Bruce Quig
 ** Date: 08-03-1999
 **/

public class Board extends JFrame
{
    private Hashtable rules = new Hashtable();
    private Integer[] playerPositions;
    private String[] playerLetters;
    private JPanel canvas;
    private int playerCount;
  
    final static int DEFAULT_PLAYERS = 4;
    final static int START_POSITION = 1;
    final static String[] defaultLetters = {"A", "B", "C", "D"};
    
    private String gameTitle;
    private String gameMessage;
    private int yOffset = 50;
    private int widthStep;
    private int heightStep;


    /**
     ** Constructor for objects of class Board.  Uses default number
     ** of players.
     **/
    public Board()
    {
        this(DEFAULT_PLAYERS);
    }


    /**
     ** Constructor for objects of class Board specifying number of players.  
     ** The board automatically assigns a Letter for the number of players and
     ** an index value 1st player is Letter "A" and is player index 1, 2nd 
     ** is "B" and index 2.  These letters can be altered using setPlayerLetter method.
     ** @param numPlayers number of players in game
     **/ 
    public Board(int numPlayers)
    {
	canvas = new JPanel();
	if( numPlayers >= 2 && playerCount <= 4)
		playerCount = numPlayers;
	else
		playerCount = DEFAULT_PLAYERS;

	gameTitle = "";
	gameMessage = "";
	playerPositions = new Integer[playerCount];
	for(int i = 0; i < playerCount; i++)
		playerPositions[i] = new Integer(START_POSITION);
	
	playerLetters = new String[playerCount];
	for(int i = 0; i < playerCount; i++)
		playerLetters[i] = new String(defaultLetters[i]);

	setSize(500, 500);
	getContentPane().add(canvas);

	setVisible(true);

	// Close Action when close button is pressed
	addWindowListener(new WindowAdapter() 
	{
	    public void windowClosing(WindowEvent event) 
	    {
		dispose();
		System.exit(0);
	    }
	});

    }

    /**
     ** Overloaded paint method inherited from Frame.  
     ** This method should not be explicitly called.  
     ** @param screen Graphics object for this object
     **/
    public void paint(Graphics screen)
    {
	screen.setFont(new Font(screen.getFont().getName(), Font.PLAIN, 10));
	
	super.paint(screen);
	drawGameTitle(screen);
	drawSquares(screen);
	showBoardNumbers(screen);
        showBoardRules(screen);
	drawPlayers(screen);
	drawGameMessage(screen);
    }   


   /**
     ** Draw squares on the board
     ** 
     ** @param screen Graphics object for this object
     **/
    private void drawSquares(Graphics screen)
    {
	widthStep = getSize().width / 10;
        heightStep = (getSize().height - yOffset) / 10;
	// check if there is a division remainder if screen resized
	int hGap = getSize().width - (widthStep * 10);
	int vGap = getSize().height - yOffset  - (heightStep * 10);
	
	// top squares
        for (int x = 0; (x + hGap) < getSize().width; x = x + widthStep)
        {
	  // Check its not the last 
	  if( x + (2 * widthStep) < getSize().width)
	      screen.drawRect(x, yOffset, widthStep, heightStep);
	  // if its the last square on the top line add hGap
	  // if so last square is made a little wider	  
	  else
	       screen.drawRect(x, yOffset, (widthStep + hGap), heightStep);
	}

        
        // bottom squares
        for (int x = 0; (x + hGap) < getSize().width; x = x + widthStep)
        {
	    if( x + (2 * widthStep) < getSize().width)
	        screen.drawRect(x,(heightStep*9)+ yOffset , widthStep, heightStep + vGap);
	    else
	       screen.drawRect(x, (heightStep*9) + yOffset, widthStep + hGap, heightStep + vGap);
        }
        
        // left squares
        for (int y = heightStep; (y + vGap) < getSize().height - heightStep; y = y + heightStep)
        {
	  // check its not the last square in column
	  if( y + (2 * heightStep) < (getSize().height - yOffset))
            screen.drawRect(0, y + yOffset, widthStep, heightStep);
	  // add vartical gap (vGap) to last square
	  else
	    screen.drawRect(0, y + yOffset, widthStep, heightStep + vGap);
        }
        
        // right squares
        for (int y = heightStep; (y + vGap) < getSize().height - heightStep ; y = y + heightStep)
        {
	  // check its not the last square in column
	  if( y + (2 * heightStep) < (getSize().height - yOffset))
            screen.drawRect((widthStep * 9), y + yOffset, widthStep + hGap, heightStep);
	  // add vartical gap (vGap) to last square
	  else
            screen.drawRect((widthStep * 9), y + yOffset, widthStep + hGap, heightStep +vGap);
        }

    }

    /**
     ** Sets the Letter that represents the player.  
     ** 
     ** @param playerIndex the index of the player whose letter is to be set
     ** @param name a one letter String representing the new Letter for the player
     **/
    public void setPlayerLetter(int playerIndex, String name)
    {
	// If a letter string of more than one letter given only use first letter
	//if(name.length() > 1)
	//	name = name.sub
	if( playerIndex < playerCount )
		playerLetters[playerIndex] = name;
    }


    /**
     ** Displays the numbers on the board
     ** 
     ** @param screen Graphics object for this object
     **/
    private void showBoardNumbers(Graphics screen)
    {
        int centerPos = 30;
        int marginHeight = 12;

	// top line of numbers
        for (int pos = 1; pos <= 10; pos++)
        {
            screen.drawString(Integer.toString(pos), pos * widthStep - centerPos, marginHeight + yOffset);
        }
        
	// right hand side
        int y = 1;
        for (int pos = 11; pos <= 18; pos++)
        {
            screen.drawString(Integer.toString(pos), getSize().width - centerPos, y * heightStep + marginHeight + yOffset);
            y++;
        }
        
	// bottom line
        int x = 1;
        for (int pos = 28; pos >= 19; pos--)
        {
            screen.drawString(Integer.toString(pos), x * widthStep - centerPos, (getSize().height - heightStep) + marginHeight);
            x++;
        }

	// left hand side
        y = 1;
        for (int pos = 36; pos >= 29; pos--)
        {
            screen.drawString(Integer.toString(pos), widthStep - centerPos, y * heightStep + marginHeight + yOffset);
            y++;
        }

    }
    


    /**
     ** Display board rules
     ** 
     ** @param screen Graphics object for this object
     **/
    private void showBoardRules(Graphics screen)
    {
        int centerPos = (widthStep - 5);
        int marginHeight = 22;
	
	for (int pos = 1; pos <= 10; pos++)
        {
            if (rules.containsKey(new Integer(pos)))
                screen.drawString((String) rules.get(new Integer(pos)), 
                                  (pos * widthStep) - centerPos, 
                                  marginHeight + yOffset);
        }
        
        int y = 1;
        for (int pos = 11; pos <= 18; pos++)
        {
            if (rules.containsKey(new Integer(pos)))
                screen.drawString((String) rules.get(new Integer(pos)),
                                  getSize().width - centerPos, 
                                  y * heightStep + marginHeight + yOffset);
            y++;
        }
        
        int x = 1;
        for (int pos = 28; pos >= 19; pos--)
        {
            if (rules.containsKey(new Integer(pos)))
                screen.drawString((String) rules.get(new Integer(pos)),
                                  x * widthStep - centerPos, 
                                  (getSize().height - heightStep) + marginHeight );
            x++;
        }

        y = 1;
        for (int pos = 36; pos >= 29; pos--)
        {
            if (rules.containsKey(new Integer(pos)))
                screen.drawString((String) rules.get(new Integer(pos)), 
                                  widthStep - centerPos + 2, 
                                  y * heightStep + marginHeight + yOffset);
            y++;
        }
    }


    /**
     ** Gets the index number (playing order) associated to a player
     ** Eg. "A" = 1, "B" = 2
     ** @param  playerLetterArg letter representing a player
     ** @return the index of player
     **/
    private int getPlayerIndex(String playerLetterArg)
    {
	for(int i = 0; i < playerCount; i++)
	{
		if(playerLetterArg.toUpperCase().equals(playerLetters[i].toUpperCase()))
			return (i + 1);
	}
	
	// should not get to here if input is correct
	return -1;
    }


    /**
     ** Gets the current position of the player
     ** 
     ** @param  playerLetter letter representing a player
     ** @return the position of Player if valid letter otherwise -1 is returned
     **/
    public int getPlayerPosition(String playerLetter)
    {
	int index = getPlayerIndex(playerLetter);
	if(index >= 0 && index <= playerCount)
	{
		Integer position = playerPositions[index - 1];
		return position.intValue();
	}
	// return -1 which can be tested for as an error code
	else
		return -1;
    }


    /**
     ** Sets the position of a Player's letter on the game board.
     ** This method automatically calls for a screen update of the players position.
     ** @param playerLetterArg letter representing a player on the board
     ** @param newPosition the square number that is the new position of the Player 
     **/
    public void setPlayerPosition(String playerLetterArg, int newPosition)
    {
	int index = getPlayerIndex(playerLetterArg);
	
	if(index >= 0)
		playerPositions[index - 1] = new Integer(newPosition);
	
	repaint();
	
    }    

    /**
     ** Draws players in their current positions
     ** 
     ** @param screen Graphics object for this object
     **/
    private void drawPlayers(Graphics screen)
    {
	for(int i = 0; i < playerPositions.length; i++)
		placePlayer(screen, playerLetters[i], (i + 1));
    } 

    /**
     ** Places a player in a position
     ** 
     ** @param screen Graphics object for this object
     **/
    private void placePlayer(Graphics screen, String playerLetterArg, int order)
    {
	int playerIndex = getPlayerIndex(playerLetterArg);
	int centerPos = 20;
        int marginHeight = 5;
        int xLocation = 0;
        int yLocation = 0;
        int charSpacing = 8;
	int pos = playerPositions[playerIndex - 1].intValue();

        if (pos >= 1 && pos <= 10)
        {
            xLocation = (pos - 1) * widthStep + (charSpacing * order);
            yLocation = heightStep - marginHeight + yOffset;
        }
        
        if (pos >= 11 && pos <= 18)
        {
            xLocation = (getSize().width - widthStep) + (charSpacing * order);
            yLocation = (int) ((heightStep * (pos - 9)) + yOffset - marginHeight);  
        }
        
        if (pos >= 19 && pos <= 28)
        {
            int squaresFromLeft = 28 - pos;
            xLocation = (squaresFromLeft) * widthStep + (charSpacing * order);
            yLocation = getSize().height - (marginHeight * 2);
        }
        
        if (pos >= 29 && pos <= 36)
        {
            int squaresFromTop = 36 + 2 - pos;
            xLocation = (charSpacing * order);
            yLocation = (int) ((squaresFromTop * heightStep) + yOffset - marginHeight);
        }
        screen.drawString(playerLetters[playerIndex -1], xLocation, yLocation);
    }
    
 
    /**
     ** Waits specified milliseconds as a time delay
     ** 
     ** @param milliSeconds number of milliseconds to wait
     **/
    public void waitMilliseconds(long milliseconds)
    {
        try 
        {
            Thread.currentThread().sleep(milliseconds);
        } 
        catch (InterruptedException e) 
        {}
    }
    

    /**
     ** Sets the text of a square to that of text parameter
     ** 
     ** @param text  the text to display describing rule
     ** @param position  the number of the square to place rule text
     ** @return boolean value of true if valid square specified, 
     ** or false if invalid request
     **/
    public boolean setSquareText(String text, int position)
    {
        if (position < 1 || position > 36)
            return false;
        rules.put(new Integer(position), text);
        return true;
    }


    /**
     ** Draws the title of the game
     ** 
     ** @param screen  the Graphics object that draws the title
     **/
    private void drawGameTitle(Graphics screen)
    {
	int titleSpace = (getSize().height - canvas.getSize().height); 
	int yPosition = (titleSpace / 2) + (yOffset / 2);
	int xPosition = (canvas.getWidth() / 2) - ((gameTitle.length() * 7) / 2);
	screen.drawString(gameTitle, xPosition, yPosition);
    }

    /**
     ** Sets the title of the game
     ** 
     ** @param text  the text to display as title
     **/
    public void setGameTitle(String title)
    {
	gameTitle = title;
    }



    /**
     ** Draws a message on the game board
     ** 
     ** @param screen  the Graphics object that draws the title
     **/
    private void drawGameMessage(Graphics screen)
    {
	int titleSpace = (getSize().height - canvas.getSize().height); 
	int yPosition = getSize().height / 2;
	int xPosition = (canvas.getWidth() / 2) - ((gameTitle.length() * 7) / 2);
	screen.drawString(gameMessage, xPosition, yPosition);
    }

    /**
     ** Sets a message to be displayed on the game board in a central position.
     ** 
     ** @param text  the text to display as a message
     **/
    public void setGameMessage(String message)
    {
	gameMessage = message;
    }


}


