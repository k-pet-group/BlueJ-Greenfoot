package bluej.editor.moe;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model    
import javax.swing.*;		// all the GUI components
import javax.swing.event.*;

/**
 ** @author Michael Kolling
 **
 **/

public final class LineNumberLabel extends JLabel
{
  // ------------ INSTANCE VARIABLES ------------

  private int line;


  // -------------- CONSTRUCTORS ----------------

  public LineNumberLabel(int initialLine)
  {
    super(String.valueOf(initialLine), JLabel.CENTER);
    line = initialLine;
  }

  // ------------- PUBLIC METHODS ---------------

  public void setLine(int newLine)
  {
    line = newLine;
    setText(String.valueOf(line));
  }

  public void lineDown()
  {
    line++;
    setText(String.valueOf(line));
  }

  public void lineUp(int newLine)
  {
    line--;
    setText(String.valueOf(line));
  }

}  // end class LineNumberLabel
