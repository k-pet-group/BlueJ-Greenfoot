package bluej.editor.moe;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model    
import javax.swing.*;		// all the GUI components

/**
 ** @author Michael Kolling
 **
 **/

public final class Info extends JPanel

{
  public static Font infoFont = new Font("SansSerif", Font.BOLD, 10);

  // -------- INSTANCE VARIABLES --------

  private JLabel line1;
  private JLabel line2;
  boolean isClear;


  // ------------- METHODS --------------

  public Info()
  {
    super();
    setLayout(new GridLayout(0, 1));	// one column, many rows
    setBackground(MoeEditor.infoColor);
    setBorder(BorderFactory.createLineBorder(Color.black));
    setFont(infoFont);

    line1 = new JLabel();
    line2 = new JLabel();
    add(line1);
    add(line2);

    isClear = true;
  }

  /**
   ** display a one line message
   **/

  public void message(String msg)
  {
    int newline = msg.indexOf('\n');
    if (newline == -1)
	message (msg, "");
    else
	message (msg.substring(0, newline), msg.substring(newline+1));
  }


  /**
   ** display a two line message
   **/

  public void message(String msg1, String msg2)
  {
    line1.setText(msg1);
    line2.setText(msg2);
    isClear = false;
  }


  /**
   ** display a one line warning (message with beep)
   **/

  public void warning(String msg)
  {
    message (msg);
    MoeEditorManager.editorManager.beep();
  }


  /**
   ** display a two line warning (message with beep)
   **/

  public void warning(String msg1, String msg2)
  {
    message (msg1, msg2);
    MoeEditorManager.editorManager.beep();
  }

 
  /**
   ** clear the display
   **/

  public void clear()
  {
    if (!isClear) {
      message ("", "");
      isClear = true;
    }
  }

 
}  // end class Info
