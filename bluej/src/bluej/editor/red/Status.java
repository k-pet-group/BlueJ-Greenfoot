package bluej.editor.red;			// This file forms part of the red package

import java.awt.*;		// MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model

/**
 ** @version $Id: Status.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 **
 ** The Status class manages the status area which is
 ** the square in the bottom right corner of the editor main window.
 **/

public final class Status extends TextArea
{	
  private Font font;		// font used for display of text
  
  // ------------------------------------------------------------------------
  /**
   ** CONSTRUCTOR: Status(Font)
   ** initialise font
   **/
  public Status(Font font)
  {
    super("", 2, 10, SCROLLBARS_NONE);
    this.font = font;
    this.setFont(font);
    this.setEditable(false);
  }
  
  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: init(String)
   ** Do the initialisation.
   **/
  public void init(String txt)
  {
    setText("  " + txt);
  }
  
  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: set_label(String)
   ** sets the status label
   **/
  public void set_label(String txt)
  {
    setText("  " + txt);
  }
  
  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: clear()
   ** clear the info area
   **/
  public void clear()
  {
    setText("");
  }
  
  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: expose()                        
   ** expose the status area
   ** Display the current contents (text) on the screen.
   **/
  public void expose ()
  { }
  
} // end class Info
