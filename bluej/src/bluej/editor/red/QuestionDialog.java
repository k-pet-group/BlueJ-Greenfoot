package	bluej.editor.red;		// This file forms part of the red package

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import bluej.utility.Debug;
import bluej.utility.MultiLineLabel;

/**
 ** @version $Id: QuestionDialog.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 ** @author Justin Tan
 **
 ** This class implements a QuestionDialog with up to three
 ** buttons. A title and message must be also specified, as
 ** well as the button names.
 **/
 
public class QuestionDialog extends Dialog implements ActionListener
{
  // private variables
  private Frame frame;
  private String message, title;
  private String yesLabel, noLabel, cancelLabel;
  private String command;

  /**
   ** one button constructor
   **/
  public QuestionDialog(Frame frame, String title, String message, 
			String OkLabel)
  {
	  this(frame, title, message, OkLabel, null, null);
  }
  
  /**
   ** three button constructor
   **/
  public QuestionDialog(Frame frame, String title, String message,
		  String yesLabel, String noLabel, String cancelLabel)
  {
    // call dialog constructor
    super(frame, title, true);

    // initialise variables
    this.frame = frame;
    this.title = title;
    this.message = message;
    this.yesLabel = yesLabel;
    this.noLabel = noLabel;
    this.cancelLabel = cancelLabel;
  }
  
  /**
   ** FUNCTION: display()
   ** This method displays the dialog box
   **/
  public void display()
  {
    setLayout(new BorderLayout(15, 15));
    add("Center", new MultiLineLabel(message));

    // Create a panel for the dialog buttons and put it at the bottom
    // of the dialog.  Specify a FlowLayout layout manager for it.
    Panel buttonbox = new Panel();
    buttonbox.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 15));
    add("South", buttonbox);

    // Create each specified button, specifying the action listener
    // and action command for each, and adding them to the button box
    if((yesLabel != null) && (yesLabel.length() > 0))
    {
	    Button yes = new Button(yesLabel);		// Create	button.
	    yes.setActionCommand("yes");			// Set action	command.
	    yes.addActionListener(this);			// Set listener.
	    buttonbox.add(yes);					// Add button	to the panel.
    }
    if((noLabel != null) && (noLabel.length() > 0))
    {
	    Button no = new Button(noLabel);
	    no.setActionCommand("no");
	    no.addActionListener(this);
	    buttonbox.add(no);
    }
    if ((cancelLabel != null) && (cancelLabel.length() > 0))
    {
	    Button cancel = new Button(cancelLabel);
	    cancel.setActionCommand("cancel");
	    cancel.addActionListener(this);
	    buttonbox.add(cancel);
    }

    // Finally,	set	the	dialog to	its	preferred	size and display it.
    pack();
    Dimension myDim = getSize();
    Dimension frameDim = frame.getSize();
    Dimension screenSize = getToolkit().getScreenSize();
    Point loc = frame.getLocation();

    // Center the dialog w.r.t. the frame.
    loc.translate((frameDim.width-myDim.width)/2, (frameDim.height-myDim.height)/2);

    // Ensure	that slave is	withing	screen bounds.
    loc.x = Math.max(0, Math.min(loc.x,screenSize.width-getSize().width));
    loc.y = Math.max(0, Math.min(loc.y,screenSize.width-getSize().height));
    setLocation(loc.x, loc.y);

    show();
  }
  
  /**
   ** FUNCTION: String getCommand()
   ** returns the label of the button which was pressed where
   **/
  private void setCommand(String str)
  {
	  command = str;
  }
  
  public String getCommand()
  {
	  return command;
  }
  
  /**
   ** Action listener Method for use by the buttons of the dialog.
   ** When a button is pressed, this listener first closes the dialog box.
   **/
  public void actionPerformed(ActionEvent	e)
  {
	  setVisible(false);		 // pop down window
	  String cmd = e.getActionCommand();
	  if(cmd.equals("yes"))
		  setCommand(yesLabel);
	  else if(cmd.equals("no"))
		  setCommand(noLabel);
	  else
		  setCommand(cancelLabel);
	  // Debug.message("actionListener:command=" + getCommand());
  }
}	// end class QuestionDialog
