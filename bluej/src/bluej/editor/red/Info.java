package bluej.editor.red;	// This file is part of the red editor

import java.awt.*;		// MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model
import javax.swing.*;		// all the GUI components

/**
 ** @version $Id: Info.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 *
 * DESCRIPTION: The Info class manages the info area which is
 * the square at the bottom in the editor main window. This class
 * used to display messages and warnings.
 */

public final class Info extends JComponent
{
  private String line1;		// contents of the two text lines
  private String line2;
  private boolean dirty;
  private int fontheight;
  private int fontascent;

  // -----------------------------------------------------------------------
  /**
   * initialise font
   */

  public Info(Font font, String ln1, String ln2)
  {
    setBorder(BorderFactory.createLineBorder(Color.black));
    setForeground(RedEditor.textColor);
    setBackground(RedEditor.infoColor);

//    setFont(font);
    fontheight = getFontMetrics(font).getHeight();
    fontascent = getFontMetrics(font).getAscent();
    setPreferredSize (new Dimension (0, fontheight*2+2));

    line1 = ln1;
    line2 = ln2;

    dirty = false;
  }

  // -----------------------------------------------------------------------
  /**
   * Display a message in the info area on the screen.  The message 
   * can have two lines.  Each of the parameters are displayed on a 
   * separate line. (No line breaks -- take care that the lines are not 
   * too long.)
   */

  public void message (String ln1, String ln1a, String ln2)
  {
    line1 = ln1 + ln1a;
    line2 = ln2;
    dirty = true;
    expose ();
  }

  // -----------------------------------------------------------------------
  /**
   * Like message, but prints an integer before first message line.
   */

  public void int_message (int i, String ln1, String ln2)
  {
    line1 = i + ln1;
    line2 = ln2;
    dirty = true;
    expose();
  }

  // -----------------------------------------------------------------------
  /**
   * Like message, but appends an integer to the first message line.
   */

  public void message_int (String ln1, int i, String ln2)
  {
    line1 = ln1 + i;
    line2 = ln2;
    dirty = true;
    expose ();
  }

  // -----------------------------------------------------------------------
  /**
   * A message with a beep.
   */

  public void warning (String line1, String line2)
  {
    message (line1, "", line2);
    RedEditorManager.red.beep();
  }

  // -----------------------------------------------------------------------
  /**
   * clear the info area
   */

  public void clear ()
  {
    line1 = "";
    line2 = "";
    if (dirty) {
	erase ();
	dirty = false;
    }
  }

  // -----------------------------------------------------------------------
  /**
   * expose the info area
   * Display the current contents (stored in line1 and line2) on the screen.
   */

  public void expose ()
  {
    if (dirty) {
      erase();
      getGraphics().drawString(line1, 3, fontascent+1);
      getGraphics().drawString(line2, 3, fontascent+fontheight+1);
    }
  }

  // -----------------------------------------------------------------------
  /**
   * erase the info area
   */

  private void erase ()
  {
    Graphics g = getGraphics();
    
    g.setColor (RedEditor.infoColor);
    g.fillRect(1, 1, getWidth()-20, getHeight()-2);
  }

} // end class Info
