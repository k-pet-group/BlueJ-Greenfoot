package bluej.terminal;

import bluej.Config;
import bluej.utility.Debug;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** A customised text area for use in the BlueJ text terminal.
 **
 ** @author Michael Kolling
 **/

public final class TermTextArea extends JTextArea
{
    /**
     * Create a new text area with given size.
     */
    public TermTextArea(int rows, int columns)
    {
	super(rows, columns);
    }

}
