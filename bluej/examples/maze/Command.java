import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Command words for "Maze", a simple text based game.
 *
 * Author:  Morten Knudsen & Kent Hansen
 *          Ported from the Blue program "Maze" by Michael Kolling.
 * Version: 1.0
 * Date:    July 1998
 *
 * This class is part of Maze. Maze is a simple, text based adventure game.
 *
 * This enumeration lists all valid commands in the game.
 */

class Command implements Enumeration {
    private int index;
    private final static String validCommands[] = {"north","south","west"
        ,"east","quit","take","drop","solve","help"};

    /**
     * Compare this Command to either a String or another Command.
     *
     * @param anObject the Object to be compared with
     */
    public boolean equals (Object anObject) {
        // Is it a String?
        if (anObject instanceof String)
            return (validCommands[index].equals (anObject));
        // Or is it an other Command?
        else if (anObject instanceof Command)
            return (((Command)anObject).index == index);
        // No, it's something else. Use lowest common denominator.
        else
            return (anObject.equals (this));
    }

    /**
     * Print all valid commands to System.out.
     */
    public void showAll () {
        int i = 0;
        while (i<validCommands.length-1)
            System.out.print (validCommands[i++]+", ");
        if (i<validCommands.length)
            System.out.println (validCommands[i]);
    }

    /**
     * Needed for Enumerator. Does this list contain more elements?
     */
    public boolean hasMoreElements() {
        return (index < validCommands.length-1);
    }

    /**
     * Needed for Enumerator. Return the next element.
     */
    public Object nextElement() {
        if (!hasMoreElements ())
            throw (new NoSuchElementException ());
        return validCommands[++index];
    }

    /**
     * Reset the enumerator.
     */
    public void gotoFirstElement () {
        index = -1;
    }
}
