package bluej.editor.moe;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import javax.swing.*;		// all the GUI components

import bluej.utility.Debug;

/**
 ** @author Michael Kolling
 **
 **/

public final class Finder
{
    // -------- CONSTANTS --------

    // search direction for the finder
    static final int backwd = 1;
    static final int forwd = 2; 
  
  // -------- INSTANCE VARIABLES --------

    private String searchString;	// the last search string used
    private boolean searchFound;	// true if last find was successfull

  // ------------- METHODS --------------

    public Finder()
    {
	searchString = null;
	searchFound = true;
    }

    /**
     * set the search string
     */
    public void setSearchString(String s)
    {
	searchString = s;
    }

    /**
     * Ask the user for input of search details via a dialogue.
     *  Returns null if operation was cancelled.
     */
    public String getNewSearchString(JFrame parent)
    {
	String s = JOptionPane.showInputDialog(parent, "Find:", "Find", 
					       JOptionPane.PLAIN_MESSAGE);
	return s;
    }

    /**
     * return the last search string
     */
    public String getLastSearchString()
    {
	return searchString;
    }

    /**
     * set last search found
     */
    public void setSearchFound(boolean found)
    {
	searchFound = found;
    }

    /**
     * return info whether the last search was successful
     */
    public boolean lastSearchFound()
    {
	return searchFound;
    }

}  // end class Finder
