package bluej.editor.red;                	// This file forms part of the red package

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model

/**
 ** @version $Id: PrefDialog.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 **/

public final class PrefDialog extends Dialog implements ActionListener
{
  // private variables
  private Frame frame;		// The parent frame
  private RedEditor editor;		// The editor

  // preference settings   
  private boolean show_toolbar;         // Show toolbar flag
  private boolean show_line_num;	// Show line Number
  private boolean beep_warning;         // The Beep flag
  private boolean make_backup;          // Make backup file
  private boolean append_newline;       // Append new line
  private boolean convert_dos;          // Convert_dos flag
  private String quote_string;          // Quote string
  private String comment_start_string;  // Comment start label
  private String comment_end_string;  	// Comment end Label

  // dialog components
  private Checkbox toolbar;		// The toolbar Checkbox
  private Checkbox lineNumber;		// The line number Checkbox
  private Checkbox beep;		// The beep Checkbox
  private Checkbox backup;		// The backup Checkbox
  private Checkbox newline;		// The newline Checkbox
  private Checkbox convert;		// The convert checkbox
  private TextField commentStart;	// CommentStart textfield
  private TextField commentEnd;		// CommentEnd textfield
  private TextField quote;		// Quote textfield

/**
 * CONSTRUCTOR: PrefDialog(RedEditor)
 */

public PrefDialog(RedEditor editor)
{
  // Call the super constructor making the dialog modal
  super(editor.getFrame(), "Preferences", true);

  // initialise variables
  this.frame = editor.getFrame();
  this.editor = editor;

  // initialise preferences
  show_toolbar = RedEditorManager.red.show_toolbar();
  show_line_num = RedEditorManager.red.show_line();
  beep_warning = RedEditorManager.red.beep_on();
  make_backup = RedEditorManager.red.backup_on();
  append_newline = RedEditorManager.red.append_nl_on();
  convert_dos = RedEditorManager.red.convert_dos_on();   
  quote_string = RedEditorManager.red.text_quote_string(); 
  comment_start_string = RedEditorManager.red.start_comment_string();
  comment_end_string = RedEditorManager.red.end_comment_string();

  toolbar = new Checkbox("Show Toolbar");
  toolbar.setState(show_toolbar);

  lineNumber = new Checkbox("Show cursor line number");
  lineNumber.setState(show_line_num);

  beep = new Checkbox("Beep with warnings");
  beep.setState(beep_warning);

  backup = new Checkbox("Make backup when saving");
  backup.setState(make_backup);

  newline = new Checkbox("Always end file with Newline character");
  newline.setState(append_newline);

  convert = new Checkbox("Automatically convert DOS files");
  convert.setState(convert_dos);

  // Create Panel for Checkboxes
  Panel c = new Panel();
  c.setLayout(new GridLayout(0,1));
  // Add checkboxes to panel
  c.add(toolbar);
  c.add(lineNumber);
  c.add(beep);
  c.add(backup);
  c.add(newline);
  c.add(convert);


  // Create textfields for comment labels and quote
  commentStart = new TextField(5);
  commentEnd = new TextField(5);
  commentStart.setText(comment_start_string);
  commentEnd.setText(comment_end_string);

  // Create Panel for TextFields
  Panel t = new Panel();
  // Add textfields and textfield labels to panel
  t.add(new Label("Comment start:"));
  t.add(commentStart); 
  t.add(new Label("Comment end:"));
  t.add(commentEnd);
  c.add(t);

  t = new Panel();
  quote = new TextField(5);
  quote.setText(quote_string);
  t.add(new Label("Quote string:"));
  t.add(quote);
  c.add(t);

  // Add checkbox panel to the dialog frame
  add("North", c);
	
  // Create "OK" button
  Button OK = new Button("OK");
  // Create and register action listener objects for button
  OK.setActionCommand("OK");
  OK.addActionListener(this);
  // Create "Cancel" button
  Button Cancel = new Button("Cancel");
  // Create and register action listener objects for button
  Cancel.setActionCommand("Cancel");
  Cancel.addActionListener(this);
  // Create "Help" button
  Button Help = new Button("Help");
  // Create and register action listener objects for button
  Help.setActionCommand("Help");
  Help.addActionListener(this);
 	
  // Create a Panel p to add buttons
  Panel p = new Panel();
  // Add buttons to panel
  p.add(OK);
  p.add(Cancel);
  p.add(Help);
  // Add panel to dialog frame
  add("South", p);

  pack();
  Dimension myDim = getSize();
  Dimension frameDim = frame.getSize();   
  Dimension screenSize = getToolkit().getScreenSize();
  Point loc = frame.getLocation();
  // Center the dialog w.r.t. the frame.
  loc.translate((frameDim.width-myDim.width)/2,
      (frameDim.height-myDim.height)/2);
  // Ensure that slave is withing screen bounds.
  loc.x = Math.max(0, Math.min(loc.x,screenSize.width-getSize().width));
  loc.y = Math.max(0, Math.min(loc.y,screenSize.width-getSize().height));
  setLocation(loc.x, loc.y);
  show();
}
   
/**
 * FUNCTION: actionPerformed(ActionEvent event)
 */

public void actionPerformed(ActionEvent event)
{
  // This is the ActionListener method invoked by the buttons
  // Get the "action command" of the event
  String command = event.getActionCommand();
  if(command.equals("OK")) {
    dispose();
    // update toolbar in Editor
    editor.show_toolbar(toolbar.getState());
    // update the preferences in the RedEditorManager
    RedEditorManager.red.set_show_toolbar(toolbar.getState());
    RedEditorManager.red.set_show_line(lineNumber.getState());
    RedEditorManager.red.set_beep_on(beep.getState());
    RedEditorManager.red.set_backup_on(backup.getState());
    RedEditorManager.red.set_append_nl_on(newline.getState());
    RedEditorManager.red.set_convert_dos_on(convert.getState());
    RedEditorManager.red.set_text_quote_string(quote.getText());
    RedEditorManager.red.set_start_comment_string(commentStart.getText());
    RedEditorManager.red.set_end_comment_string(commentEnd.getText());
    // save preferences to file
    RedEditorManager.red.write_prefs();
  }
  else if(command.equals("Cancel")) {
    dispose();
  }
  else if(command.equals("Help")) {
    // Display Help screen
    RedEditorManager.red.messages.show_help(frame, Messages.HlpPref);
  }
}

}  // end class PrefDialog
