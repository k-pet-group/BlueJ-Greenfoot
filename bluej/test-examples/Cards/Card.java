/**
 ** Class Card - A card for a card game with value and suit
 ** 
 ** Author: mik
 ** Date: 
 **/
public class Card
{
    // instance variables 
    private int value;
    private String suit;
    private boolean legal = true;

    /**
     ** Constructor for objects of class Card
     **/
    public Card(int val, String suit)
    {
	// initialise instance variables
	value = val;
	checkValue();
	this.suit = suit;
    }

    /**
     ** Check whether this card is higher than another one.
     **/
    public boolean isHigher(Card other)
    {
	if(value > other.value)
	    return true;
	else 
	    return false;
    }

    /**
     ** Check whether this card is equal to another one.
     **/
    public boolean isEqual(Card other)
    {
	if(value == other.value)
	    return true;
	else 
	    return false;
    }

    /**
     * Test the colour of this card. If red, return true; if black return false.
     * We assume that the suit value has been checked.
     */
    public boolean isRed()
    {
        if(suit.equals("hearts") || suit.equals("diamonds"))
	    return true;
	else
	    return false;
    }

    /**
     * Check whether this card's specification was legal. Return true if it is,
     * false otherwise.
     */
    public boolean isLegal()
    {
        return legal;
    }

    /**
     * check whether the current value is legal
     */
    private void checkValue()
    {
        if((value<2) || (value>14))
	  legal = false;
    }

}
