// errors:
// drag_start not initialised

package bluej.editor.red;      // This file forms part of the red package

import bluej.utility.Debug;
import bluej.editor.EditorWatcher;

import java.util.Vector;
import javax.swing.*;		// all the GUI components
import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model    
import java.io.*;               // Object input, ouput streams
import java.util.Properties;	// Store Printing properties

/**
 ** @version $Id: RedEditor.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Kolling
 ** @author Giuseppe Speranza
 **
 ** RedEditor implements an editor for a single file in a single window.
 ** It is compiled and run as part of Red, which provides editing
 ** facilities for multiple files in multiple windows by managing several
 ** instances of RedEditor.  All the resources shared by all editors are
 ** managed by Red (preferenes, key bindings), whereas all resources
 ** private to one editor are managed here.  They are mainly the screen
 ** elements belonging to this editor's window and the text buffer
 ** storing the text currenty edited.
 **/

public final class RedEditor

	implements bluej.editor.Editor, 
		   ActionListener, KeyListener, MouseListener, 
		   MouseMotionListener, ComponentListener, 
		   AdjustmentListener
		   //, FocusListener
{
  // colours
  static final Color textColor = new Color(0,0,0);		// normal text
  static final Color textBgColor = new Color(255,255,255);	// background
  static final Color selectTextColor = new Color(0, 0, 0);		// selected text
  static final Color selectBgColor = new Color(248, 128, 168); // selected background
  static final Color cursorColor = new Color(40,0,20);		// cursor

  static final Color frameBgColor = new Color(196, 196, 196);
  static final Color infoColor = new Color(240, 240, 240);

  // -------- CONSTANTS --------

  // current save state of file
  static final int ReadOnly = 0;
  static final int Saved = 1;   
  static final int Changed = 2; 
  
  // search direction for the finder
  static final int backwd = 1;
  static final int forwd = 2; 
  
  // LineTag enumerations
  static final int NoLineTag        = 0;
  static final int ErrorLineTag     = 1;
  static final int BreakLineTag     = 2;
  static final int ExecLineTag      = 3;
  static final int ExecBreakLineTag = 4;
  
  // Fonts
  public static Font editFont = new Font("Courier", Font.PLAIN, 12);
  public static Font boldFont = new Font("Courier", Font.BOLD, 12);
  public static Font italicFont = new Font("Courier", Font.ITALIC, 12);
  public static Font boldItalicFont = new Font("Courier", Font.BOLD | Font.ITALIC, 12);
  public static Font infoFont = new Font("Courier", Font.PLAIN, 12);


  // -------- INSTANCE VARIABLES --------

  private int width, height;		// the preferred size
  private EditorWatcher watcher;

  private String filename;              // name of file or null
  private String windowTitle;		// title of editor window
  private int save_state;               // indicate whether buffer is saved
  private boolean first_save;           // true if never been saved
  private boolean first_expose;		// true if never been exposed
  private boolean is_compiled;		// true when source has been compiled

  private char last_char;               // the character last typed
  private int last_func;     		// the last function executed

  private int mouse_line;               // line of last mouse click
  private int mouse_col;                // column of last mouse click
  private BufferPos drag_start;         // the position where dragging started
  private boolean button1_down;         // true while mouse button 1 is pressed
  ////Time button1_uptime;              // time of button up (for double click)
  private boolean double_click;         // True, if last click was double
  private int line_no_displayed;        // Last line number displayed

  private boolean search_found;         // true if last find was successfull
  private String param_prompt;         	// true if getting param in info area
  private String parameter;    		// the parameter read in info area   
  private int screen_column;		// the screen column aimed with up/down

  private JFrame frame;			// The frame we are within
  private JPanel mainPanel;		// The panel for all window components
  private JPanel toolbar;		// The toolbar
  private JComboBox viewSelector;	// The view choice selector
  private JPanel statusArea;		// the status area
  private JLabel statusLabel;		// the status label
  private JLabel lineNumLabel;		// the line number label

  private Screen screen;                // the edit screen
  private Scrollbar scrollbar;		// the scroll bar
  private Info info;                    // the information manager

  private Buffer buffer;                // the buffer storing the text
  private ActionStack undo_stack;   	// the stack storing actions for undo

  private BufferPos top;                // position in buffer that is at top
                                        // of screen
  private BufferPos bottom;             // position in buffer that is at
                                        // bottom of screen
  private int blanklines;               // number of screen lines below last
                                        // text line
  private final char NewLine = 10;      // the line break character used
                                        //  internally in the editor (e.g. in
                                        //  search patterns)
  private boolean isCode;		// true if current buffer is code

  // functions are organised into the following groups (in this order):
  //
  //  INTERFACE 
  //  GENERAL_SUPPORT_FUNCTIONS 
  //  EDIT_FUNCTIONS 
  //  MOVE_&_SCROLL_FUNCTIONS 
  //  DISPLAY_FUNCTIONS 
  //  USER_FUNCTIONS 
  //  EVENT_HANDLING 
  //  WINDOW_INITIALISATION 

  // =========================== PUBLIC METHODS ===========================

  /**
   ** Constructor for RedEditor class
   ** title may be null 
   **/

  public RedEditor(String title, boolean isCode, EditorWatcher watcher, 
		   boolean showToolbar, boolean showLineNum)
  {
    this.watcher = watcher;
    windowTitle = title;

    undo_stack = new ActionStack();
    buffer = null;

    top = bottom = null;

    last_func = UserFuncID.NOT_BOUND;
    search_found = true;
    param_prompt = null;
    parameter = "";

  ////  button1_down = false;
  ////  button1_uptime = CurrentTime;
  ////  double_click = false; 
    line_no_displayed = 1;
    this.isCode = isCode;

    init_window(showToolbar, showLineNum);
  
//    scrollbar.setValues(1, screen.lines, 1, buffer.lines);

    first_save = true;
    first_expose = true;
    filename = null;
  }

  // ------------------------------------------------------------------------
  /**
   ** Load the file "fname" and show the editor window.
   **/

  public boolean showFile(String filename, boolean compiled,
				      Vector breakpoints)
			  // inherited from Editor, redefined
  {
    boolean loaded = false;
    boolean goodfile = false;
    String infoline1;
    File newFile = null;

    // check whether file exists and what kind of file it is

    if(filename == null) {
      loaded = false;
      goodfile = true;
    }
    else {
      newFile = new File(filename);
      if (!newFile.exists()) {	// does not exist
	loaded = false;
	goodfile = true;
      } 
      else {
	if (newFile.isFile()) {		// normal file
          goodfile = true;
          // open file "filename"
          try {
            FileInputStream file = new FileInputStream(filename);
            clear ();
            buffer.load(file, true, null, RedEditorManager.red.convert_dos_on());
            loaded = true;
            if (breakpoints != null)
              putBreakpoints (breakpoints);
	    file.close();         // Close the FileInputStream
          }
          catch(Exception e) {
  	    loaded = false;
          }
	}
	else {				// bad file (e.g. directory)
          goodfile = false;
          filename = null;
          loaded = false;
	}
      }
    }
    if (!(RedEditorManager.standAlone || loaded)) // should exist but didn't...
      return false;

    if (!loaded)
      clear ();

    // check for read-only file

    if (loaded)
      if (newFile.canWrite()) {		// have write permission
	  save_state = Saved;
	  statusLabel.setText("saved");
      }
      else {
	  save_state = ReadOnly;
	  statusLabel.setText("read only");
      }
    else
      save_state = Saved;

    this.filename = filename;	// error ## - path must be added

    infoline1 = "Red Version " + RedVersion.versionString();

    if (loaded)
      showEditor (infoline1, "");
    else if (!goodfile) {
      showEditor (
	    "The file could not be opened because it is not a regular file.", 
	    "(It might be a directory or a device.)");
      RedEditorManager.red.beep();
    }
    else if (filename != null)
      showEditor (infoline1, "");
    else
      showEditor ("New file", "");

    if (! RedEditorManager.standAlone)
    {
      set_compiled(compiled);
      //Utility.set_sensitive (compile_button);
    }

    return true;
  } // showFile

  // ------------------------------------------------------------------------
  public void reloadFile() // inherited from Editor, redefined
  {
    Debug.assert (filename != null);
    do_revert ();
    setView (bluej.editor.Editor.IMPLEMENTATION);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: clear()
   ** Wipe out contents of the editor.  
   **/

  public void clear ()	// inherited from Editor, redefined
  {
    buffer = new Buffer();		// new empty buffer
  }

  // ------------------------------------------------------------------------
  /**
   ** Insert a string into the buffer. The string must not contains
   ** newline characters. The editor is not immediately redisplayed.
   ** This function is typically used in a sequence
   ** "clear; [insertText]*; setVisible(true)".
   **
   ** @arg text         the text to be inserted
   ** @arg newLines     number of newline characters to be inserted
   **                   after the text (newLines >= 0)
   ** @arg style         the style in which the text is to be displayed
   **/
  public void insertText(String text, int newLines, int style)
	// inherited from Editor, redefined
  {
    buffer.insert_string(text, style);
    for (; newLines>0; newLines--)
      buffer.insert_newline ();
  }

  // ------------------------------------------------------------------------
  /**
   ** Show the editor window. This includes whatever is necessary of the
   ** following: make visible, de-iconify, bring to front of window stack.
   **
   ** @arg view		the view to be displayed. Must be one of the 
   **			view constants defined above
   **/
  public void show(int view)	// inherited from Editor, redefined
  {
    frame.setVisible(true);		// show the window
    showEditor("", "");
  }


  // ------------------------------------------------------------------------
  /**
   ** Save the buffer to disk under current filename.  If it hasen't got
   ** a filename, call save_as instead.
   **/

  public void save() // inherited from Editor, redefined
  {
    String fname;

    if ((save_state != Changed) && (last_func != UserFuncID.UFSave))
      info.message ("No changes need to be saved", "", "");
    else {
      if (filename != null) {		// if it has a file name...
	  if (first_save && RedEditorManager.red.backup_on()) {
	    fname = filename.concat(".~");
  ////	  if (!filecopy (filename, fname))
  ////	    ; // cannot display warning message here - will be overwritten
	    first_save = false;
	  }
	  if (buffer.save (filename, RedEditorManager.red.append_nl_on()))
	    set_saved (true);
	  else
	    info.warning ("Error in saving file!", "");
      }
      else
	  RedEditorManager.red.saveAsRequest(this);	// ask for save-as
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** The editor wants to close. Do this through the EditorManager so that
   ** we can be removed from the list of open editors.
   **/

  public void close()	// inherited from Editor, redefined
  {
    if (RedEditorManager.standAlone)
	RedEditorManager.red.closeRequest(this, (save_state == Changed));
    else
    {
	save(); // temporary - should really be done by watcher from outside
	RedEditorManager.red.closeRequest(this, false);
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** Display a message (used for compile/runtime errors). An editor
   ** must support at least two lines of message text, so the message
   ** can contain a newline character.
   **
   ** @arg message	the message to be displayed
   ** @arg line		the line to move the cursor to (the line is 
   **			also highlighted)
   ** @arg column		the column to move the cursor to
   ** @arg beep		if true, do a system beep
   ** @arg setStepMark	if true, set step mark (for single stepping)
   **/

  public void displayMessage(String message, int line, int column, 
				boolean beep, boolean setStepMark)
	// inherited from Editor, redefined
  {
//     // select the region
// 
//     if (region.start_line != -1) {
//       if (check_selection (false, false))	// if selection on, switch it off
// 	;
//       buffer->goto_pos (region.start_line, region.start_col);
//       if (step_mark)
// 	buffer->set_step_tag ();		// set the line tag if requested
//       show_point ();
//       buffer->set_pt2 ();
//       buffer->goto_pos (region.end_line, region.end_col);
//       toggle_selection ();
//     }

    // display the message

    if (beep)
      info.warning (message, "");
    else
      info.message (message, "", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** Remove the step mark (the mark that shows the current line when
   ** single-stepping through code). If it is not currently displayed,
   ** do nothing.
   **/

  public void removeStepMark()
  {
    int cx, cy;
    int scrline, scrcol;
    boolean old_state;
    BufferPos exec_pos;
    int[] result;

    exec_pos = buffer.clear_step_tag ();		// clear in buffer

    if (exec_pos.is_invalid ())			// was not set
      return;
    if ((exec_pos.is_before (top)) || (exec_pos.is_behind (bottom)))
      return;

    result = buffer_pos_to_screen_pos (exec_pos.line, exec_pos.column);
    scrline = result[0];
    scrcol  = result[1];
  
    cx = screen.column;
    cy = screen.line;
    old_state = screen.cursor_on (false);
    screen.cursor_to (0, scrline);
    screen.set_tag (buffer.get_tag (exec_pos.line));
    screen.cursor_to (cx, cy);
    screen.cursor_on (old_state);
  }

  // ------------------------------------------------------------------------
  /**
   ** Change class name.
   **
   ** @arg title	new window title
   ** @arg filename	new file name
   **/
  public void changeName (String title, String filename)
  {
    this.filename = filename;		// error ## - need to add full path
    windowTitle = title;
    set_title();
  }

  // ------------------------------------------------------------------------
  /**
   ** Determine whether this buffer has been modified.
   ** @returns	a boolean indicating whether the file is modified
   **/

  public boolean isModified()	// inherited from Editor, redefined
  {
    return (save_state == Changed);
  }

  // ------------------ end of interface inherited from Editor --------------
  // ------------------------------------------------------------------------
  /**
   ** The editor has been closed. Hide the editor window now.
   **/

  public void doClose()
  {
    frame.setVisible(false);		// hide the window
    if (watcher != null)
	watcher.closeEvent(this);
  }

  // ------------------------------------------------------------------------
  /**
   ** Return the name of the file being edited.
   **/

  public String getFilename()
  {
	  return filename;
  }

  // ------------------------------------------------------------------------
  /**
   ** Determine whether this buffer has been modified.
   ** @returns	a boolean indicating whether the file is modified
   **/

  public Frame getFrame()
  {
    return frame;
  }

  // ------------------------------------------------------------------------
  /**
   ** sets the editors preferred size
   **/

  public Dimension getPreferredSize()
  {
    return new Dimension(width, height);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: do_save_as(String)
   ** Save this buffer under a new file name. The new name is 
   ** passed as parameter.  (This is called by "save_as").
   **/

  public void do_save_as (String fname)
  {
    if (buffer.save (fname, RedEditorManager.red.append_nl_on())) {
      filename = new String(fname);
      set_title ();
      set_saved (true);
    }
    else
      info.warning ("Could not save file!",
	     "The reason could be: invalid file name, or file exists" +
 	     " and is write protected");
  }


  // ------------------------------------------------------------------------
  /**
   ** Show or hide the toolbar (depending on the parameter 'show').
   **/

  public void show_toolbar(boolean show)
  {
    if(show)
    {
      mainPanel.add("North", toolbar);
      frame.validate();
    }
    else
    {   
      mainPanel.remove(toolbar);
      frame.validate();
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** Refresh the screen display.
   **/

  public void refreshScreen ()
  {
    display ();
  }

  // =========================== PRIVATE METHODS ===========================

  // ====================== GENERAL_SUPPORT_FUNCTIONS ======================

  // ------------------------------------------------------------------------
  /**
   ** Show the editor on screen with current contents.
   **/

  private void showEditor(String msg1, String msg2)
  {
    set_title ();

    if (! RedEditorManager.standAlone)
    {
    /*
	utility->set_toggle (interface_button, intf);
	utility->set_toggle (interface_item, intf);
    
	if (intf) {
          save_state = ReadOnly;
          statusLabel.setText ("interface\n(read only)");
	}
    */
    }

    buffer.point_to_bob ();
    top = buffer.get_point ();
    bottom = buffer.get_point ();

    // display window (realise it if it wasn't already)

    blanklines = -1;

//    expose();
    info.message (msg1, msg2, "");
  
    if (! RedEditorManager.standAlone)
    {
      // if (intf) set_compiled(true);
    }

  }

  // ------------------------------------------------------------------------
  /**
   ** Sets the editor to contain a view. This is called from outside the 
   ** editor (not by the editor function).
   **
   ** @arg view	    the new view. Must be one of the defined view constants.
   **/
  private void setView(int view)
  {
    if (view == bluej.editor.Editor.IMPLEMENTATION)
	viewSelector.setSelectedIndex(0);
    else if (view == bluej.editor.Editor.PUBLIC)
	viewSelector.setSelectedIndex(1);
    else if (view == bluej.editor.Editor.PACKAGE)
	viewSelector.setSelectedIndex(2);
    else if (view == bluej.editor.Editor.INHERITED)
	viewSelector.setSelectedIndex(3);
  }


  // ------------------------------------------------------------------------
  /**
   ** Show or hide the line number display (depending on the 
   ** parameter 'show').
   **/

  private void show_line_num(boolean show)
  {
    if (show) {
      statusArea.add(lineNumLabel);
      show_line_number (true);
      frame.validate();
    }
    else {
      statusArea.remove(lineNumLabel);
      frame.validate();
    }
  }

  // ------------------------------------------------------------------------
  //#ifndef RED_ONLY
  /*  Not required!!
  //  come_up: Try to bring this editor's window to the top, and, if it was
  //	iconified, de-iconify it.

  void EDITOR::come_up ()
  {
    utility->map_raised (edit_window);
  }

  // ------------------------------------------------------------------------
  //  come_up: Try to bring this editor's window to the top, and, if it was
  //	iconified, de-iconify it.

  void EDITOR::place_over (EDITOR* other_editor)
  {
    if (!other_editor)
      come_up ();
    else if (other_editor == this)
      return;
    else
      utility->place_over (edit_window, other_editor->edit_window);
  }
  */
  // ------------------------------------------------------------------------
  //  set_compiled: 
  //	

  private void set_compiled (boolean compiled)
  {
    /*
    utility->set_sensitive (break_item, compiled);
    utility->set_sensitive (clr_break_item, compiled);
    utility->set_sensitive (interface_item, compiled);
    utility->set_sensitive (interface_button, compiled);
    screen->set_tag_grey (!compiled);
    if (showing_interface && !compiled)
      interface_toggle ();
    is_compiled = compiled;
    */
  }

  // ------------------------------------------------------------------------
  //  set_debug: 

  private void set_debug (boolean enable)
  {
  /*
    utility->set_sensitive (step_item, enable);
    utility->set_sensitive (step_into_item, enable);
    utility->set_sensitive (cont_item, enable);
    utility->set_sensitive (term_item, enable);
  */
  }

  // ------------------------------------------------------------------------
  //  get_line_no: Get the current line number

  private int get_line_no ()
  {
    return buffer.pt.line_no;
  }

  // ------------------------------------------------------------------------
  //  clear_all_tags:  Remove all breakpoint or other tags that might exist in 
  //	the current buffer.

  private void clear_all_tags ()
  {
    buffer.clear_all_tags ();
  }

  // ------------------------------------------------------------------------
  //  replace_region:  Replace a region of text in the buffer with a new text.

//   private void replace_region (TextRegion region, String text)
//   {
//     BufferPos save_pos;
//     boolean out_of_screen;
// 
//     if (check_selection (false, false))  // if selection on, switch it off
//       ;
// 
//     save_pos = buffer.get_point ();
//     buffer.goto_pos (region.start_line, region.start_col);
//     buffer.set_pt2 ();
//     buffer.goto_pos (region.end_line, region.end_col);
//     out_of_screen = (buffer.pt.is_before (top))
// 		    || (buffer.pt2.is_behind(bottom));
//     delete_between_points ();
//     insert (text);
//     buffer.point_to (save_pos);
//     if (!out_of_screen)
//       redisplay ();
//   }
// 

  //#endif


  //			     user function execution
  //
  //  The following functions are triggered indirectly by user functions.
  //  This is typically the case when the user function popped up a dialog
  //  first and then the dialog can call one of these "do_" functions if it
  //  decides to really execute the function.

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: boolean do_find(String, int, boolean, boolean)
   ** Internal find routine used by the find, find_next and
   ** find_selection functions (partly indirectly over finder).
   ** Perform the find and, if interactive is true, report an error if not
   ** found.
   **/

  private boolean do_find (String  pattern, int direction, 
			  boolean case_sens, boolean whole_word)
  {
    String msg;

    if (pattern.length()==0) {
      info.message ("Empty search string", "", "");
      return false;
    }

    if (direction==forwd)
	if (search_found)
          msg = "Find forward: ";
	else
          msg = "Find forward (wrap around): ";
    else
	if (search_found)
          msg = "Find backward: ";
	else
          msg = "Find backward (wrap around): ";
    msg.concat (pattern);
    info.message (msg, "", "");

    if (check_selection (false, (direction==forwd)))	// switch sel. off
      ;
    search_found = buffer.find (pattern, case_sens, whole_word,
				 (direction==backwd), !search_found);
    if (search_found) {
      show_point ();
      toggle_selection ();
      return true;
    }
    else {
      info.warning (msg, "Not found");
      return false;
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: do_replace_all(String, int)
   ** replace all
   **/

  private void do_replace_all (String pattern, int direction, 
			  boolean case_sens, boolean whole_word, 
			  String  rep_pattern)
  {
    BufferPos start;
    int cnt = 0;

    if (pattern.length()==0) {
      info.message ("Empty search string", "", "");
      return;
    }

    if (check_selection (false, (direction==backwd)))	// switch sel. off
      ;
    start = buffer.get_point ();

    while (buffer.find (pattern, case_sens, whole_word,
			  (direction==backwd), false)) {
      delete_between_points ();
      insert (rep_pattern);
      cnt++;
    }
    if (buffer.pt.is_before (start)) {
      buffer.point_to (start);
      display ();
    }
    else {
      buffer.point_to (start);
      screen_update_from (start);
    }
    info.int_message (cnt, " instances replaced", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: do_replace(String)
   ** Internal replace routine used by the replace (indirectly 
   ** over finder). Replaces the current selection with "pattern".
   ** If there is no current selection, just return false.
   **/

  private void do_replace (String pattern)
  {
    BufferPos start;

    if (!check_selection (true, true))		// if selection, cut it
      return;

    start = buffer.get_point ();
    insert (pattern);
    screen_update_from (start);
  }

  // ------------------------------------------------------------------------
  /**
   ** Update the window title. If the variable "windowTitle" is set, it is 
   ** used to construct the title. If not, the file name is used.
   **/
  private void set_title ()
  {
    String title = windowTitle;

    if (title == null)
    {
	title = shortname (filename);
	if (title == null)
	  title = "<no name>";
    }
    frame.setTitle("Red: " + title);
  }


  // ------------------------------------------------------------------------
  /**
   ** 
   **/
  private void putBreakpoints (Vector breaks)
  {
//     int line;
// 
//     breaks->init_scan (ScanForward);
//     while ((line = (int)(breaks->next_element ()))) {
//       buffer->goto_pos (line, 0);
//       buffer->set_tag (BreakLineTag);
//     }
//     breaks->end_scan ();
// 
//     buffer->goto_pos (1, 0);
  }


  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: set_saved(boolean)
   ** Set the saved/changed status of this buffer. This involves: 
   ** set the internal flag and set the labal on screen. The parameter
   ** has to be true, to set it to "saved" or false to set it to "changed"
   **/
  private void set_saved (boolean sav)
  {
    if (sav) {
	info.message ("File saved", "", "");
	statusLabel.setText ("saved");
	save_state = Saved;
	if (! RedEditorManager.standAlone)
	{
	  if(watcher != null)
      	    watcher.saveEvent(this);
	}
    }
    else if (save_state != Changed) {		// first change!
	statusLabel.setText ("changed");
	save_state = Changed;
	if (! RedEditorManager.standAlone)
	{
	  if(watcher != null)
      	    watcher.modificationEvent(this);
	  set_compiled (false);
	}
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: is_saved() -> boolean
   ** Gets the saved/changed status of this buffer. 
   **/
  private boolean is_saved()
  {
	  return (save_state != Changed);
  }

  // ------------------------------------------------------------------------
  /**
   ** Display the current line number in the status area.
   ** The number is only refreshed if it has changed since last display or if 
   ** 'force' is true.
   ** @arg force	force new display even if line hasn't changed
   **/

  private void show_line_number (boolean force)
  {
    String text;

    if (!RedEditorManager.red.show_line())    // if line number display is off,
      return;				      //   don't bother

    if (force || (line_no_displayed != buffer.pt.line_no)) {

      if ((buffer.selection_on) && (buffer.pt.line_no != buffer.pt2.line_no)) {
	text = new String().valueOf(buffer.pt2.line_no);
	text.concat("-" + new String().valueOf(buffer.pt.line_no)); 
      }
      else {
	text = new String().valueOf (buffer.pt.line_no);
      }

      lineNumLabel.setText(text);
      line_no_displayed = buffer.pt.line_no;
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: String shortname()
   ** return the filename without the path from a full path name.
   **/

  private String shortname(String filename)
  {
    String fname;
    int i;

    if(filename == null) {
      fname = null;
    }     
    else if((i = filename.lastIndexOf(File.separator)) != -1) {
      fname = filename.substring(i + 1);
    }
    else {
      // filename was already a short name
      fname = filename;
    }
    return fname;	
  }


  // ======================= EDIT_FUNCTIONS =======================

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: insert_char
   ** Insert a character into the buffer at the current cursor 
   ** position and do all the display stuff.
   **/

  private void insert_char (char ch)
  {
    int orig_lines, lines_written, last_index;
    BufferPos display_pos;
    LineFragment str;
    int[] result;
    int startline, startcol;
    Action action;

    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (check_selection (true, true))	// if selection, cut it
      ;

    startline = buffer.pt.line_no;
    startcol = buffer.pt.column;

    // first check how many screen lines this line uses currently
    str = buffer.get_text_to_eol ();
    orig_lines = screen.nr_of_lines (str, screen.column);

  //Debug.message("InsertChar("+last_char+")@("+startline+","+startcol+") into ["+str.text+"]");

    buffer.insert_char (ch);
    str = buffer.get_text_to_eol();		// have to get it again!

  // Debug.message("Result["+str.text+"]");
						  // address of line might have
						  // changed
    // now write the new line onto the screen

    screen.cursor_on (false);			// hide the cursor
    str.offset--;

    str.length++;
    result = screen.write_string (str, startcol+1);
    lines_written = result[0];
    last_index = result[1];

    // if used number of lines has changed, update the rest of the screen

    if ((buffer.pt.line==bottom.line) && (last_index==bottom.line.length))
      bottom.column++;

    if (lines_written > orig_lines) {		// if we used a new screen line
      screen.clear_to_eol ();
      if (!screen.cursor_at_bottom()) {		//  then update rest of screen
	screen.next_line ();
	display_pos = buffer.get_point ();	// take point...
	buffer.next_line (display_pos);		// move to beg. of next line..
	display_from (display_pos);		// and display from there
      }
      show_point ();
    }
    else if ((str.length>1) || (screen.cursor_at_end ())) // cursor in mid-line
      show_point ();					//  or at end of screen
							  //  -> place cursor
    screen.cursor_on (true);

    // store information about this on undo-stack

    action = undo_stack.new_action ();		// get action record
    action.set_insertion (startline, startcol, 
			   buffer.pt.line_no, buffer.pt.column, false);
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: insert_new_line()
   ** Insert at newline at point position.
   **/

  private void insert_new_line (boolean indent)
  {
    Action action;
    int startline, startcol;
    BufferPos display_pos;

    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (check_selection (true, true))	// if selection, cut it
      ;

    startline = buffer.pt.line_no;
    startcol = buffer.pt.column;

    buffer.insert_newline ();
    display_pos = buffer.get_point ();
    if (indent)
      buffer.indent (true);		// discard result -- doesn't matter

    screen.cursor_on (false);		// clear rest of current line
    screen.clear_wrap_mark ();
    screen.clear_to_eol ();

    if (!screen.cursor_at_bottom ()) {	// if cursor remains on screen...
      screen.next_line ();		// ...display rest of screen
      display_from (display_pos);
      place_cursor_on_screen ();
      screen.cursor_on (true);
      update_scrollbar ();
    }
    else {
      update_scrollbar ();
      redisplay ();
    }

    // store information about this on undo-stack

    action = undo_stack.new_action ();		// get action record
    action.set_insertion (startline, startcol, 
			   buffer.pt.line_no, buffer.pt.column, false);

    show_line_number (false);
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** Insert the string in str into the buffer (no display). The string
   ** may contain line breaks marked by the C NewLine character ('\n'). 
   ** Does not redisplay.  NOTE: The selection HAS TO BE OFF before calling
   ** this -- it does not delete the selection.
   ** This insert function should be used only for interactive insertion
   ** (user editing).  There is another insert function (insert_text)
   ** for text insertion done by the program itself.
   **/

  private void insert (String str)
  {
    Action action;
    int startline, startcol;
    int index, last_index;
    String s;			// current substring
    int len;

    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    startline = buffer.pt.line_no;
    startcol = buffer.pt.column;

    // now insert the string...

    index = -1;
    while(index<str.length()) {			// while not end of str
      last_index = index + 1;
      index = str.indexOf ('\n', last_index);	// find next line break...
      if (index > 0)				//  - found
	s = str.substring (last_index, index);
      else			     		//  - not found
	s = str.substring (last_index);
      buffer.insert_string (s);
    }

    // store information about this on undo-stack

    action = undo_stack.new_action ();		// get action record
    action.set_insertion (startline, startcol, 
			   buffer.pt.line_no, buffer.pt.column, false);

    show_line_number (false);
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: delete_between_points()
   ** Delete the current selection from the buffer.  
   ** No display.  Undo information is recorded.
   **/

  private void delete_between_points ()
  {
    StringBuffer text;
    Action action;

    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    text = buffer.selection_to_string ();		// get text to be deleted
    buffer.cut ();				// delete it

    action = undo_stack.new_action ();		// get action record to store
    action.set_deletion (buffer.pt.line_no, buffer.pt.column, text, 'x', 
			  false);
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: self_insert_char()
   ** Insert the character typed to invoke this command into
   ** the text. Printable characters and TAB characters are the only ones
   ** handled properly.
   **/

  private void self_insert_char ()
  {
    insert_char (last_char);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: new_line()
   ** Insert at newline at point position.
   **/

  private void new_line ()
  {
    insert_new_line (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: open_line()
   ** Open a new line, but leave cursor on the current line.
   **/

  private void open_line ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    new_line ();
    show_point ();
    show_line_number (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: tab_to_tab_stop()
   ** Insert a Tab into the text
   **/

  private void tab_to_tab_stop ()
  {
    insert_char ('\t');
  }

  // ------------------------------------------------------------------------
  /**
   ** Insert half a tab (tab stop to multiples of 4) in the current
   ** line of in every line in the current selection, depending on whether
   ** the selection is on or not.
   **/

  private void half_tab ()
  {
    Action action;
    int startline, startcol;
    int new_chars;
    int i;
    StringBuffer spaces;

    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (check_selection (true, true))	// if selection, cut it
      ;

    startline = buffer.pt.line_no;
    startcol = buffer.pt.column;

    new_chars = buffer.insert_half_tab ();

    // store information about this on undo-stack

    if (new_chars > 0) {				// spaces inserted
      action = undo_stack.new_action ();
      action.set_insertion (startline, startcol, 
			     buffer.pt.line_no, buffer.pt.column, false);
    }
    else {
      if (new_chars < 0) {			// spaces were deleted
	spaces = new StringBuffer ("        ");
	spaces.setLength (-new_chars);
	action = undo_stack.new_action ();
	action.set_deletion (startline, startcol+new_chars, spaces, 'x', false);
	action = undo_stack.new_action ();
	action.set_insertion (buffer.pt.line_no, buffer.pt.column-1,
			       buffer.pt.line_no, buffer.pt.column, true);
      }
      else {					// just a TAB inserted
	action = undo_stack.new_action ();
	action.set_insertion (buffer.pt.line_no, buffer.pt.column-1,
			       buffer.pt.line_no, buffer.pt.column, false);
      }
    }
    screen_update_from_bol ();	// update from beginning of current line
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: indent()
   ** Indent cursor to start of next word in previous nonblank line.
   ** If there is no previous nonblank line, or it is too short, insert a
   ** TAB instead.
   **/

  private void indent ()
  {
    Action action;
    BufferPos pt_pos;	// temp store current position
    StringBuffer old_text;

    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (check_selection (false, false))	// if selection on, switch it off
      ;

    pt_pos = buffer.get_point ();		// save original text for undo
    buffer.point_to_bol ();
    buffer.set_pt2 ();  
    buffer.point_to (pt_pos);
    old_text = buffer.selection_to_string ();

    if (buffer.indent (false)) {		// is relative indent pos was found..
      screen_update_from_bol ();		// update from beginning of current line
  //    screen.cursor_to (0, screen.line);    // error on long line!!!
  //    screen_update_from (buffer.pt2);

      // Store information on undo-stack. Indent gets stored as deletion of text
      // in line before point followed by insertion of new text before point.

      action = undo_stack.new_action ();		// get action record to store
      action.set_deletion (buffer.pt.line_no, 0, old_text, 'x', false);
      action = undo_stack.new_action ();
      action.set_insertion (buffer.pt.line_no, 0, 
			     buffer.pt.line_no, buffer.pt.column, true);
      set_saved (false);
    }
    else					// otherwise insert TAB
      insert_char ('\t');
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: shift_line_right()
   ** Shift the current line one character to the right.
   ** Buffer only, no screen update.
   **/

  private void shift_line_right ()
  {
    buffer.inc_indent ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: shift_right()
   ** Shift the current line or the current selection (depending
   ** on whether the selection is on) one character to the right. Update
   ** the screen.
   **/

  private void shift_right ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (buffer.selection_on)
  ;////    block_operation (&EDITOR::shift_line_right);
    else {
      shift_line_right ();
      screen_update_from_bol ();	// update from beginning of current line
    }
    undo_stack.clear ();
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: shift_line_left()
   ** Shift the current line one character to the left.
   ** Buffer only, no screen update.
   **/

  private void shift_line_left ()
  {
    buffer.dec_indent ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: shift_left()
   ** Shift the current line or the current selection (depending
   ** on whether the selection is on) one character to the left. Update
   ** the screen.
   **/

  private void shift_left ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (buffer.selection_on)
  ;////    block_operation (&EDITOR::shift_line_left);
    else {
      shift_line_left ();
      screen_update_from_bol ();	// update from beginning of current line
    }
    undo_stack.clear ();
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: comment_line()
   ** insert comment around one line
   **/

  private void comment_line ()
  {
    buffer.comment (RedEditorManager.red.start_comment_string (), RedEditorManager.red.end_comment_string ());
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: insert_comment()
   ** insert comment around selection
   **/

  private void insert_comment ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (buffer.selection_on)
  ;////    block_operation (&EDITOR::comment_line);
    else {
      comment_line ();
      screen_update_from_bol ();	// update from beginning of current line
    }
    undo_stack.clear ();
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: uncomment_line()
   ** remove comment around one line
   **/

  private void uncomment_line ()
  {
    buffer.uncomment (RedEditorManager.red.start_comment_string (), RedEditorManager.red.end_comment_string ());
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: remove_comment()
   ** remove comment around selection
   **/

  private void remove_comment ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (buffer.selection_on)
  ;////    block_operation (&EDITOR::uncomment_line);
    else {
      uncomment_line ();
      screen_update_from_bol ();	// update from beginning of current line
    }
    undo_stack.clear ();
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: new_line_and_indent()
   ** Insert a new line and indent as line above.
   **/

  private void new_line_and_indent ()
  {
    insert_new_line (true);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: delete_char()
   ** Delete the character at point. If point is at end of line,
   ** the line is joined with the next one.
   **/

  private void delete_char ()
  {
    int orig_lines, lines_written, last_index, screen_x, screen_y;
    char del_result;
    BufferPos display_pos;
    LineFragment str;
    int[] result;
    Action action;

    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (check_selection (true, true))	// if selection, cut it
      return;

  ////  buffer.get_text_to_eol (str, length);
    str = buffer.get_text_to_eol();

    orig_lines = screen.nr_of_lines (str, screen.column);

    del_result = buffer.delete_char();

    if (del_result == '\0') {		// end of buffer
      info.warning ("End of text", "");
      return;
    }

    screen.cursor_on (false);		// hide the cursor
    screen_x = screen.column;
    screen_y = screen.line;
    str = buffer.get_text_to_eol();

    if ((str.length == 0) && (screen_x == 0) && (buffer.pt.column != 0)) {
      // wrapping line has shrunk to fit on one line now
      screen_y--;
      screen_x = screen.columns;
      display_pos = buffer.get_point ();	// take point...
      buffer.next_line (display_pos);	// move to beg. of next line..
      display_from (display_pos);		// and display from there
    }
    else {
      if ((buffer.pt.line==bottom.line) && (bottom.column>bottom.line.length))
	bottom.column--;

      result = screen.write_string (str, buffer.pt.column);
      lines_written = result[0];
      last_index = result[1];
      screen.clear_to_eol ();

      if ((del_result == NewLine) || (lines_written < orig_lines)) {
	// line now needs one screen line less than before
	screen.clear_wrap_mark ();
	if (!screen.cursor_at_bottom()) {
          screen.next_line ();
	  if (buffer.is_last (buffer.pt.line)) {
	    screen.clear_to_eos ();
	    bottom.line = buffer.pt.line;
	    bottom.line_no = buffer.pt.line_no;
	    bottom.column = buffer.pt.line.length;
	  }
	  else {
	    display_pos = buffer.get_point ();	// take point...
	    buffer.next_line (display_pos);	// move to beg. of next line..
	    display_from (display_pos);		// and display from there
	  }
	}
	update_scrollbar ();
      }
    }

    screen.cursor_to (screen_x, screen_y);
    screen.cursor_on (true);

    // record on undo stack

    action = undo_stack.new_action ();		// get action record to store
    action.set_deletion (buffer.pt.line_no, buffer.pt.column, null, 
		          del_result, false);
    if (del_result == NewLine)
      show_line_number (false);
    set_saved (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: backward_delete_char()
   ** Delete the character left of point. Includes wrap
   ** around if point is at beginning of line.
   **/

  private void backward_delete_char ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (check_selection (true, true))	// if selection, cut it
      return;

    if (buffer.point_backward()) {
      show_point ();
      delete_char();
    }
    else
      info.warning ("Beginning of text", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: backward_delete_untab()
   ** Like backward_delete_char, but if character to be
   ** deleted is a Tab, turn it into spaces first.
   **/

  private void backward_delete_untab ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    buffer.untabify ();
    backward_delete_char ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: cut_word()
   ** Cut the word under the cursor.
   **/

  private void cut_word ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (buffer.selection_on) {
      info.message ("cut-word: ", "Not possible while selection is on", 
		     "(Place cursor before calling this function)");
      return;
    }

    buffer.point_backward_word ();
    buffer.set_pt2 ();
    buffer.point_forward_word ();
    make_selection ();				// copy to paste buffer
    delete_selection ();				// delete from text
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: cut_to_end_of_word()
   ** Cut from the cursot position to the end of the word.
   **/

  private void cut_to_end_of_word ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (buffer.selection_on) {
      info.message ("cut-to-end-of-word: ", "Not possible while selection is on", 
		     "(Place cursor before calling this function)");
      return;
    }

    buffer.set_pt2 ();
    buffer.point_forward_word ();
    make_selection ();				// copy to paste buffer
    delete_selection ();				// delete from text
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: cut_line()
   ** Cut the complete current line to the paste buffer.
   ** If there is a selection, cut the selection instead.
   **/

  private void cut_line ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (buffer.selection_on)
      cut ();
    else {
      buffer.point_to_bol ();
      buffer.set_pt2 ();
      buffer.point_down (0);
      make_selection ();				// copy to paste buffer
      delete_selection ();			// delete from text
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: cut_to_end_of_line()
   ** Cut to the end of the current line to the paste 
   ** buffer. If there is a selection,
   ** cut_to_end_of_line can not be   
   ** executed. Print a warning instead.
   **/

  private void cut_to_end_of_line ()
  {
    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    if (buffer.selection_on) {
      info.message ("cut-to-end-of-line: ", "Not possible while selection is on", 
		     "(Place cursor before calling this function)");
      return;
    }

    buffer.set_pt2 ();
    if (buffer.pt.at_eoln ())
      buffer.point_forward ();    
    else
      buffer.point_to_eol ();
    make_selection ();				// copy to paste buffer
    delete_selection ();				// delete from text
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: cut_region()
   ** Cut the current region to the paste buffer.
   **/

  private void cut_region ()
  {
      info.warning ("cut-region: Not yet implemented", 
		     "(sorry)");
  }

  // ------------------------------------------------------------------------
  /**
  //				selection functions
  */

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: select_all()
   ** Select the whole buffer.
   **/

  private void select_all ()
  {
    if (buffer.selection_on)
      buffer.unselect ();

    buffer.point_to_bob ();
    buffer.set_pt2 ();
    buffer.point_to_eob ();
    toggle_selection ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: select_region()
   ** Select or unselect the region between mark and 
   ** point.
   **/

  private void select_region ()
  {
    if (buffer.selection_on) {
      toggle_selection ();
      screen.cursor_on (true);
      show_point ();
    }
    else {
      if (!buffer.set_pt2_to_mark ()) {
	info.warning ("select-region: No mark has been set.", "");
	return;
      }
      toggle_selection ();
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: select_word()
   ** Select the whole current word.
   **/

  private void select_word ()
  {
    if (buffer.selection_on) {		// add next word to selection
      buffer.unselect ();
      buffer.point_forward_word ();
      toggle_selection ();
    }
    else					// select current word
      if (buffer.point_forward()) {
	buffer.point_backward_word ();
	buffer.set_pt2 ();
	buffer.point_forward_word ();
	toggle_selection ();
      }
  }

  // ------------------------------------------------------------------------
  /**
   ** Select the whole current line.
   **/

  private void select_line ()
  {
    if (buffer.selection_on) {		// add next line to selection
      buffer.unselect ();
      buffer.point_to_bol ();
      if (buffer.pt.is_before (buffer.pt2))
	buffer.set_pt2 ();
      buffer.point_down (0);
      toggle_selection ();
    }
    else {				// select current line
      buffer.point_to_bol ();
      buffer.set_pt2 ();
      if (!buffer.point_down (0))		// in last line...
	buffer.point_to_eol ();		// ...select to end
      toggle_selection ();
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: select_line_by_number()
   ** Select the line 'line'
   **/

  private void select_line_by_number(int line)
  {
    buffer.unselect();
    buffer.goto_pos(line, 0);
    buffer.set_pt2();
    if(!buffer.point_down(0))		// in last line...
	    buffer.point_to_eol();	// ...select to end
    toggle_selection();
  }


  // ------------------------------------------------------------------------
  /**
   ** Switches the selection on or off, depending on its 
   ** previous state.  This is done both internally and on screen.  The
   ** selection is set to the region between point and mark. After
   ** completion of this function, the selection has been shown or removed
   ** on screen, point is set internally, but NOT displayed on screen.
   **/

  private void toggle_selection ()
  {
    BufferPos start_pos;
    int screenline, col;
    int[] result;

    if (buffer.selection_on) {
      buffer.unselect ();
    }
    else {
      buffer.select_point_point ();	// switch selection on
      make_selection ();		// copy selection to paste buffer
      if (!buffer.selection_on)		// if there was no selection...
	return;
    }

    // select/unselect has been finished internally, now update the screen

    if ((buffer.pt.is_before (top)) || (buffer.pt2.is_behind (bottom)))
      return;
      // nothing else to do -- selection is not on screen

    screen.cursor_on (false);

    if (buffer.pt2.is_before (top))		// adjust start position
      start_pos = new BufferPos (top);
    else
      start_pos = buffer.get_point2 ();

    result = buffer_pos_to_screen_pos (start_pos.line, start_pos.column);
    screenline = result[0];
    col = result[1];
    screen.cursor_to (col, screenline);
    display_from (start_pos);
    show_line_number (true);
  }

  // ------------------------------------------------------------------------
  /**
   ** extend (or shrink) the current highlight to the region
   ** from the current beginning to screen_x, screen_y.  If the highlight
   ** is on, pt2 is expected to be at the beginning of it.  If it is not
   ** yet on, we turn it on here and set pt2 to the beginning.
   **/

  private void extend_highlight (int screen_x, int screen_y)
  {
    BufferPos minpos;
    int screenline, col;
    int[] result;

    if (buffer.selection_on)
      buffer.unselect ();		// if selected, unselect, pt2 already at beg
    else {
      buffer.set_pt2 ();		// else set pt2 to beginning of selection
      screen.cursor_on (false);
    }

    set_point_to_square (screen_x, screen_y);
    buffer.select_point_point ();

    if (buffer.pt2.is_before (top))
      minpos = new BufferPos (top);
    else
      minpos = buffer.get_point2 ();
  
    result = buffer_pos_to_screen_pos (minpos.line, minpos.column);
    screenline = result[0];
    col = result[1];
    screen.cursor_to (col, screenline);
    display_from (minpos);
    show_line_number (true);
  }

  // ------------------------------------------------------------------------
  /**
   ** extend (or shrink) the current highlight to the region
   ** from drag_start to to screen_x, screen_y.  (screen_x and screen_y
   ** are column and row and must within the screen bounds.)
   ** The highlight has to be on.
   **/

  private void drag_highlight (int screen_x, int screen_y)
  {
    BufferPos minpos;
    int screenline, col;
    int[] result;

    buffer.unselect ();

    if (drag_start.equal (buffer.pt2)) {		// dragging forward
      minpos = buffer.get_point ();		// current end of selection
      set_point_to_square (screen_x, screen_y);
      buffer.select_point_point ();

      if (buffer.pt.is_before (minpos))
	minpos = buffer.get_point ();
    }
    else {					// dragging backwards
      minpos = buffer.get_point2 ();		// current start of selection
      set_point_to_square (screen_x, screen_y);
      buffer.set_pt2 ();
      buffer.point_to (drag_start);
      buffer.select_point_point ();

      if (buffer.pt2.is_before (minpos))
	minpos = buffer.get_point2 ();
    }  

    if (minpos.is_before (top))
      minpos = new BufferPos (top);
  
    result = buffer_pos_to_screen_pos (minpos.line, minpos.column);
    screenline = result[0];
    col = result[1];
    screen.cursor_to (col, screenline);
    display_from (minpos);
    show_line_number (true);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: make_selection()
   ** Copy the current selection (pt2 to pt) into the paste 
   ** buffer. The selection does not necessarily have to be turned on, as
   ** long as pt2 and pt are set properly (pt2 must be <= pt!).
   **/

  private void make_selection ()
  {
    StringBuffer sel_text;

    // XXX: mjc - not yet
    return;
  /*
    if (buffer.pt.equal (buffer.pt2)) {		// empty selection
      if (buffer.selection_on)
	buffer.unselect ();
      screen.cursor_on (true);
      show_point ();
      return;
    }

    sel_text = buffer.selection_to_string ();

    // now, if the prevoius function was a cut function, append this text,
    // otherwise discard previous text

    if ((last_func >= UserFuncID.UFCutWord) && (last_func <= UserFuncID.UFCut) 
	  && (RedEditorManager.red.paste_buffer != null))
      RedEditorManager.red.paste_buffer.append (new String (sel_text));

    else
      RedEditorManager.red.paste_buffer = sel_text;

  ////  utility->own_selection (da, (BLConvertSelectionProc) x_tell_selection_cb, 
  ////			  (BLLoseSelectionProc )x_selection_lost_cb, 
  ////			  CurrentTime);
  */
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: check_selection(boolean, boolean)
   ** If the selection is currently on, turn it off.
   ** If cut_it is true, delete the selection.
   ** If forwd is true, leave the cursor at the end of the selection,
   ** otherwise at the beginning. Return true.  If the selection is not
   ** on, return false.
   **/

  private boolean check_selection (boolean cut_it, boolean forwd)
  {
    if (buffer.selection_on) {
      if (cut_it) {
	delete_selection ();
	return true;
      }

      toggle_selection ();
      if (!forwd) {
	buffer.point_to (buffer.pt2);
	show_line_number (false);
      }
      screen.cursor_on (true);
      show_point ();
      return true;
    }
    else
      return false;
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: delete_selection()
   ** Delete the current selection and redisplay. The 
   ** selection does not have to be switched on -- this function just 
   ** deletes between pt and pt2. After this function is executed,
   ** the selection is off.
   **/

  private void delete_selection ()
  {
    boolean out_of_screen;

    if (save_state == ReadOnly) {
      change_read_only ();
      return;
    }

    out_of_screen = (buffer.pt2.is_before (top))
		    || (buffer.pt2.is_behind (bottom));

    delete_between_points ();		// deletes region between pt2 and pt

    if (out_of_screen)			// full redisplay
      redisplay ();
    else {				// display from cursor position
      screen.cursor_on (false);
      place_cursor_on_screen ();		// put screen cursor according to point
      display_from (buffer.pt);		// display from cursor
      place_cursor_on_screen ();		// and put cursor back up
    }

    screen.cursor_on (true);
    update_scrollbar ();
    show_line_number (true);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: block_operation()
   ** apply a function to every line in the selection.
   **/
  // ------------------------------------------------------------------------
  /*************************
  private void block_operation (CodePtr func)
  {
    BufferPos pt_pos;

    if ((buffer.pt2.column != 0) || (buffer.pt.column != 0)) {
      buffer.unselect ();
      buffer.pt2.column = 0;			// extend selection to beg.
      if (!buffer.is_last (buffer.pt.line))
	buffer.next_line (buffer.pt);		//  of first line and end of
      buffer.select_point_point ();		//  last line
    }

    pt_pos = buffer.get_point ();

    for (;;) {
      buffer.prev_line (buffer.pt);
      (this->*func)();			// call user function on each line
      if (buffer.pt.line == buffer.pt2.line)
	break;
    }
    buffer.point_to (buffer.pt2);
    show_point ();
    buffer.point_to (pt_pos);
    display_from (buffer.pt2);
  }
  ***********************/

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: change_read_only()
   ** attempt to change a read_only buffer
   **/

  private void change_read_only ()
  {
    info.warning ("You do not have access permission to change this file.", 
		   "(Use \"Save As\" to save it to another file to edit it.)");
  }

  // =================== MOVE_&_SCROLL_FUNCTIONS ==================

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: forward_char()
   ** Move cursor one character forward. Check for end of text.
   **/

  private void forward_char ()
  {
    if (check_selection (false, true)) {	// if selection on, swith it off
      show_point ();
      show_line_number (false);
      return;
    }
    if (buffer.point_forward()) {
      show_point ();
      show_line_number (false);
    }
    else
      info.warning ("End of text", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: backward_char()
   ** Move cursor one character bachward. Check for beginning
   ** of text.
   **/

  private void backward_char ()
  {
    if (check_selection (false, false)) {	// if selection on, swith it off
      show_point ();
      show_line_number (false);
      return;
    }
    if (buffer.point_backward()) {
      show_point ();
      show_line_number (false);
    }
    else
      info.warning ("Beginning of text", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: forward_word()
   ** Move cursor one word forward. Check for end of text.
   **/

  private void forward_word ()
  {
    if (check_selection (false, true))	// if selection on, swith it off
      ;
    buffer.point_forward_word ();
    show_line_number (false);
    show_point ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: backward_word()
   ** Move cursor one word bachward. Check for beginning
   ** of text.
   **/

  private void backward_word ()
  {
    if (check_selection (false, false))	// if selection on, swith it off
      ;
    buffer.point_backward_word ();
    show_line_number (false);
    show_point ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: next_line()
   ** Move cursor one line down (on screen and in buffer).
   **/

  private void next_line ()
  {
    if (check_selection (false, true))	// if selection on, switch it off
      show_point ();

    if ((last_func != UserFuncID.UFNextLine) && (last_func != UserFuncID.UFPrevLine))
      screen_column = screen.column;

    if (buffer.point_down(screen_column)) {
      show_point ();
      show_line_number (false);
    }
    else {
      end_of_line ();
      info.warning ("End of text", "");
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: previous_line()
   ** Move cursor one line up (on screen and in buffer).
   **/

  private void previous_line ()
  {
    if (check_selection (false, false))	// if selection on, swith it off
      show_point ();

    if ((last_func != UserFuncID.UFNextLine) && (last_func != UserFuncID.UFPrevLine))
      screen_column = screen.column;

    if (buffer.point_up(screen_column)) {
      show_point ();
      show_line_number (false);
    }
    else
      info.warning ("Beginning of text", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: scroll_line_down()
   ** Scroll view window on line down.
   **/

  private void scroll_line_down ()
  {
    if (blanklines > 0) {
      info.warning ("End of text","");
      return;
    }

    forward_screenline (top);	// move display down one screen line
    display ();
    show_line_number (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: scroll_line_up() 
   ** Scroll view window on line up.
   **/

  private void scroll_line_up ()
  {
    if (!backward_screenline (top))	// move display up one screen line
      info.warning ("Beginning of text","");
    else {
      display ();
      show_line_number (false);
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: scroll_half_page_down()
   ** scroll half page down
   **/

  private void scroll_half_page_down ()
  {
    int lines = screen.lines / 2;

    if (blanklines > 0) {
      info.warning ("End of text","");
      return;
    }

    while (lines-- > 0) {
      if (!forward_screenline (top))
	break;
      display ();
      show_line_number (false);
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: scroll_half_page_up()
   ** scroll half page up
   **/

  private void scroll_half_page_up ()
  {
    int lines = screen.lines / 2;

    if ((top.line_no == 1) && (top.column == 0)) {
      info.warning ("Beginning of text","");
      return;
    }

    while (lines-- > 0) {
      if (!backward_screenline (top))
	break;
      display ();
      show_line_number (false);
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: next_page()
   ** Move view window one page down.  Keep cursor on screen.
   **/

  private void next_page ()
  {
    int lines;

    if (blanklines > 0) {
      info.warning ("End of text","");
      return;
    }

    lines = screen.lines-2;

    while (lines-- > 0)
      forward_screenline (top);
    display ();
    show_line_number (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: previous_page()
   ** Move view window one page up.
   **  Keep cursor on screen.
   **/

  private void previous_page ()
  {
    int lines;

    if ((top.line_no == 1) && (top.column == 0)) {
      info.warning ("Beginning of text","");
      return;
    }

    lines = screen.lines-2;

    while ( lines-- > 0)
      if (!backward_screenline (top))
	break;
    display ();
    show_line_number (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: scroll_drag()
   ** Called when scollbar slider is draged to a new value. Set
   ** top line to the according line number and display the text from 
   ** there.
   **/

  private void scroll_drag (int value)
  {
    while (value > top.line_no)
      buffer.next_line (top);
    while (value < top.line_no)
      buffer.prev_line (top);
    display ();
    show_line_number (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: beginning_of_line()
   ** Set cursor to the beginning of the current line.
   **/

  private void beginning_of_line ()
  {
    if (check_selection (false, false))	// if selection on, swith it off
      ;
    buffer.point_to_bol ();
    show_point ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: end_of_line()
   ** Set cursor to the end of the current line.
   **/

  private void end_of_line ()
  {
    if (check_selection (false, true))	// if selection on, swith it off
      ;
    buffer.point_to_eol ();
    show_point ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: beginning_of_text()
   ** Set cursor to the beginning of the text.
   **/

  private void beginning_of_text ()
  {
    if (check_selection (false,false))	// make sure selection is off
      ;
    set_mark ();
    buffer.point_to_bob ();
    show_point ();
    show_line_number (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: end_of_text()
   ** Set cursor to the end of the text.
   **/

  private void end_of_text ()
  {
    if (check_selection (false,true))	// make sure selection is off
      ;
    set_mark ();
    buffer.point_to_eob ();
    show_point ();
    show_line_number (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: cursor_to()
   ** set cursor to coordinates on screen
   **/

  private void cursor_to (int screen_x, int screen_y)
  {
    if (buffer.selection_on)
      toggle_selection ();
 
    screen.cursor_on (true);
    set_point_to_square (screen_x, screen_y);
    place_cursor_on_screen ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: forward_screenline()
   ** Move the buffer position 'pos' forward, so that it
   ** is at the text position that corosponds to the beginning of the next
   ** line on screen.
   **/

  private boolean forward_screenline (BufferPos pos)
  {
    int ln=0, col;
    LineFragment str;
    int[] result;

    str = buffer.get_text_in_line (pos.line, pos.column);
    result = screen.square_to_index (str, ln, 0, 1);
    ln = result[0];
    col = result[1];

    if (col == -1) {			// next screenline not in line
      if (buffer.is_last (pos.line))
	return false;
      else {
	buffer.next_line (pos);
      }
    }
    else
      pos.column += col;
    return true;
  }

  // ------------------------------------------------------------------------
  /**
   ** Set pos to the position that is one line above it 
   ** on the screen. For short lines that means: set it to the beginning
   ** of the previous line. For long lines the position that appears in
   ** column 0 on the screen has to be calculated.
   ** Pos has to be a position that is in screen column 0. (This function
   ** is normally used for 'top'.)
   **/

  private boolean backward_screenline (BufferPos pos)
  {
    int lines, stln=0, ln;
    LineFragment str;
    int[] result;

    if (pos.column == 0)			// we are at a beginning of a line, so
      if (buffer.is_top (pos.line))	//  find last screen line in prev line
	return false;
      else {
	buffer.prev_line (pos);		// to beginning of prev. line
	str = buffer.get_text_in_line (pos.line, 0);
	lines = screen.nr_of_lines (str, 0);
	if (lines != 1) {
	  result = screen.square_to_index (str, stln, 0, lines-1);
	  stln = result[0];
	  pos.column = result[1];
	}
      }
    else {
      str = buffer.get_text_in_line (pos.line, 0);
      str.length = pos.column;
      result = screen.index_to_square (str, false);
      ln = result[0];
      result = screen.square_to_index (str, stln, 0, ln-1);
      stln = result[0];
      pos.column = result[1];
    }
    return true;
  }

  // ====================== DISPLAY_FUNCTIONS =====================

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: redisplay()
   ** Display the buffer on screen.  That is: clear the whole
   ** screen, calculate topline (the line at the top of the screen) so
   ** that point is in the middle of the screen, and redisplay the then
   ** visible buffer contents on the screen.
   ** This is done by calculating topline and then calling "display".
   **/

  private void redisplay ()
  {
    int ln;
    LineFragment str;
    int[] result;

    top = buffer.get_point ();
    top.column = 0;
    ln = (screen.lines-1)/2;

    str = buffer.get_text_in_line (buffer.pt.line, 0);
    str.length = buffer.pt.column;
    ln -= screen.nr_of_lines (str, 0);

    while (ln > 0) {
      if (buffer.is_top (top.line))
	break;
      buffer.prev_line (top);
      str = buffer.get_text_in_line (top.line, 0);
      ln -= screen.nr_of_lines (str, 0);
    }

    if (ln < 0) {		// screenline not beginning of buffer line
      result = screen.square_to_index (str, ln, 0, 0);
      ln = result[0];
      top.column = result[1];
    }
    display ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: display()
   ** Display the current screen (according to top.line), place the
   ** cursor and set the scrollbar accordingly.
   ** It is assumed that topline points to the line to be displayed at the
   ** top of the screen. If the cursor is not on the screen, place it on
   ** the screen. The whole screen is then redisplayed.
   **/

  private void display ()
  {
    int oldblanklines = blanklines;
  
    screen.cursor_on (false);
    screen.cursor_to (0,0);
    display_from (top);

    if (buffer.selection_on) {
      if (buffer.pt.is_before (top)) {
	buffer.unselect ();
	buffer.point_to (top);
	place_cursor_on_screen ();
	screen.cursor_on (true);
      }
      else if (buffer.pt2.is_behind (bottom)) {
	buffer.unselect ();
	buffer.point_to (bottom);
	place_cursor_on_screen ();
	screen.cursor_on (true);
      }
    }
    else {
      if (buffer.pt.is_before (top))
	buffer.point_to (top);
      else if (buffer.pt.is_behind (bottom))
	buffer.point_to (bottom);
      place_cursor_on_screen ();
      screen.cursor_on (true);
    }
    update_scrollbar ();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: screen_update_from_bol()
   ** Update the screen from the beginning of the 
   ** current line.
   ** Display from:	buffer pos: beg. of pt.line
   **			screen pos: screen pos of (beg. of pt.line)
   ** The selection must be off.
   ** Positions cursor to point position.
   **/

  private void screen_update_from_bol ()
  {
    BufferPos pos;
    BufferPos pt_pos;

    pt_pos = buffer.get_point ();		// store point position

    if (buffer.pt.column <= screen.columns) {
      buffer.point_to_bol ();
      screen.cursor_to (0, screen.line);
    }
    else {
      buffer.point_to_bol ();
      show_point ();
    }
    pos = buffer.get_point ();
    buffer.point_to (pt_pos);		// restore point position
    screen_update_from (pos);
  }

  // ------------------------------------------------------------------------
  /**
   ** Update the screen from a certain point. That is:
   ** Display the screen from that point and update the scroll bar.
   ** Display from:	buffer pos: 'pos'
   **			screen pos: current position
   ** The selection must be off. The cursor must be at the right position.
   ** Positions cursor to point position.
   **/

  private void screen_update_from (BufferPos pos)
  {
    screen.cursor_on (false);			// hide the cursor
    display_from (pos);
    show_point ();
    update_scrollbar ();
    screen.cursor_on (true);			// show the cursor
  }

  // ------------------------------------------------------------------------
  /**
   ** Display the buffer from the position indicated in pos
   ** on the screen starting at the current screen cursor position.
   ** So this redisplays a part of the screen from an arbitrary position
   ** to the bottom of the screen.
   ** 'pos' maybe at end (the line marking the end of the text). In that
   ** case nothing is displayed.
   ** If the text ends before the end of the screen, the rest of the 
   ** screen is cleared.
   ** This routine should be called with the screen cursor off.
   ** Updates bottom position.
   **/

  private synchronized void display_from (BufferPos pos)
  {
    int parts=0;	    // number of parts in line to be displayed separately
    int lines_written;	    // screen lines used with display of some text
    int current_index=0;    // current index in current buffer line

    int length1=0, length2=0, length3=0, lines2=0;
    boolean first = true;
    int[] result;
    LineFragment str;
    int screenline = screen.line;
  
    // This is ugly but necessary, given the current implementation
    pos = new BufferPos(pos);
  
    if ((pos.line.select == Buffer.InSelection) 
	|| (pos.line.select == Buffer.SelectionEnd))
      screen.set_selection (true);

    while (!buffer.is_end (pos.line)) {		// while not AFTER text
      if (first) {
	current_index = pos.column;
	first = false;
      }
      else {
	screen.next_line ();
	current_index = 0;
	screenline = screen.line;
      }
      str = buffer.get_text_in_line (pos.line, current_index);

      screen.clear_wrap_mark ();

      switch (pos.line.select) {		// look at selection status of line
	case Buffer.NoSelection:		// no selection here
	case Buffer.InSelection:		// line is within selection
	  parts = 1;
          length1 = str.length;
          break;
	case Buffer.SelectionStart:		// selection starts in this line
	  parts = 2;
          length1 = buffer.pt2.column - pos.column;
	  if (length1 < 0) length1 = 0;
	  length2 = str.length-length1;
          break;
	case Buffer.SelectionEnd:		// selection ends in this line
	  parts = 2;
          length1 = buffer.pt.column - pos.column;
	  if (length1 < 0) length1 = 0;
	  length2 = str.length-length1;
          break;
	case Buffer.ContainsSelection:	    // selection lies entirely in this line
	  parts = 3;
          length1 = buffer.pt2.column - pos.column;
	  if (length1 < 0) length1 = 0;
	  if (pos.column > buffer.pt2.column)
	    length2 = buffer.pt.column - pos.column;
	  else
	    length2 = buffer.pt.column - buffer.pt2.column;
	  if (length2 < 0) length2 = 0;
	  length3 = str.length-length2-length1;
          break;
      }
      
      if(pos.line.style != StyleNormal)
          screen.set_style(pos.line.style);

      str.length = length1;
      result = screen.write_string (str, current_index);
      lines_written = result[0];
      current_index = result[1];

      if (parts>1) {
          screen.toggle_selection ();
	  str.offset = str.offset + length1;
	  str.length = length2;
          result = screen.write_string (str, current_index);
	  lines2 = result[0];
	  current_index = result[1];
	  lines_written = lines_written + lines2 - 1;

	  if (parts>2) {
	      screen.toggle_selection ();
	      str.offset = str.offset + length2;
	      str.length = length3;
              result = screen.write_string (str, current_index);
	      lines2 = result[0];
	      current_index = result[1];
	      lines_written = lines_written + lines2 - 1;
	  }
      }
      if (pos.line.style != StyleNormal)
            screen.set_style(StyleNormal);
      screen.clear_to_eol ();
      buffer.next_line (pos);
    
      if (screen.cursor_at_bottom ()) break;
    }
    screen.set_selection (false);

    // Now line points behind last displayed line and screen cursor is in last
    // used screen line behind last character. Update bottom and blanklines.

    buffer.prev_line (pos);
    bottom.line = pos.line;
    bottom.line_no = pos.line_no;
    bottom.column = current_index;
    blanklines = screen.lines - screen.line - 1;

    if (blanklines>0)
      screen.clear_to_eos ();
    
    //Debug.message("Editor.display_from ended for " + this);
  }


  // ------------------------------------------------------------------------
  /**
   ** Display point on the screen (just placing the cursor, if the 
   ** location is currently visible, or redisplaying in case the location
   ** is out of screen).
   **/

  private void show_point ()
  {
    if (buffer.pt.is_before (top))
    {
      redisplay ();		// point above screen - redisplay
      return;
    }

    if (buffer.pt.is_behind (bottom))
    {
      redisplay ();		// point below screen - redisplay
      return;
    }

    // Now we know: point is on screen. Find screen position and place cursor.

    place_cursor_on_screen ();
  }

  // ------------------------------------------------------------------------
  /**
    * FUNCTION: place_cursor_on_screen()
    * Place the cursor on the screen according to 
    * point.  This function assumes that point and top line are up to date
    * and that point is on screen.
    * PRE: cursor position is on screen
   **/

  private void place_cursor_on_screen ()
  {
    int screenline=0, col=0;
    int[] result;

    result = buffer_pos_to_screen_pos (buffer.pt.line, buffer.pt.column);
    screenline = result[0];
    col = result[1];
    screen.cursor_to (col, screenline);
  }

  // ------------------------------------------------------------------------
  /**
   ** Given a position in the buffer (bufline, 
   ** bufcol) the function returns the position on the screen where that
   ** buffer position is displayed (scrline, scrcol).
   ** It is assumed that the position is visible on the screen and that
   ** top is set correctly.
   **
   ** returns: result[0] = scrline
   **	     result[1] = scrcol
   **/

  private int[] buffer_pos_to_screen_pos (Line bufline, int bufcol)
  {
    BufferPos pos = new BufferPos (top);
    int ln, column;
    LineFragment str;
    int scrline;
    int scrcol;
    int[] result;

    // find offset of top line of screen top (0 if normal, negativ, if top in
    //  long line)

    if (pos.column==0)
      scrline = 0;
    else {
      str = buffer.get_text_in_line (pos.line, 0);
      str.length = pos.column;
      scrline =  -(screen.nr_of_lines (str, 0));
    }

    while (pos.line != bufline) {
      str = buffer.get_text_in_line (pos.line, 0);
      scrline += screen.nr_of_lines (str, 0);
      buffer.next_line (pos);
    }

    // now we found bufline

    str = buffer.get_text_in_line (pos.line, 0);
    if (bufcol < str.length)
      column = bufcol;
    else
      column = str.length;
    str.length = column;
    result = screen.index_to_square (str, true);
    ln = result[0];
    scrcol = result[1];

    scrline += ln;
    if (scrline<0)			// check: (-1,maxcol) can happen for
      scrline = scrcol = 0;		//  long lines; make it (0,0)

    result[0] = scrline;
    result[1] = scrcol;
    return result;
  }

  // ------------------------------------------------------------------------
  /**
   ** Find out what buffer position is at a square on the
   ** screen and set point to it.
   **/

  private void set_point_to_square (int x, int y)
  {
    BufferPos pos = new BufferPos (top);
    int screenline = 0;
    int col;
    LineFragment str;
    int[] result;

    while (true) {
      str = buffer.get_text_in_line (pos.line, pos.column);
      result = screen.square_to_index (str, screenline, x, y);
      screenline = result[0];
      col = result[1];
      if (col != -1)
	break;			// found buffer position on square
      screenline++;
      if (buffer.is_last (pos.line)) {
	col = pos.line.length;
	break;
      }
      buffer.next_line (pos);
    }
    pos.column += col;
    buffer.point_to (pos);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: update_scrollbar()
   ** Update the scrollbar to reflect current state
   **/

  private void update_scrollbar ()
  {
/*
    if (bottom.column == bottom.line.length)
	scrollbar.setValues(top.line_no, bottom.line_no - top.line_no + 1,
			    0, buffer.lines);
    else
	scrollbar.setValues(top.line_no, bottom.line_no - top.line_no + 1,
			    0, buffer.lines + 1);
			      
     scrollbar.setBlockIncrement(bottom.line_no - top.line_no + 1);
*/
    if (scrollbar.getVisibleAmount() != screen.lines)
    {
	scrollbar.setValues(top.line_no, screen.lines, 1, buffer.lines+1);
  //Debug.message("update_scrollbar.setValue("+top.line_no+","+ screen.lines+","+0+","+buffer.lines+")");
    }
    else
    {
	scrollbar.setValue(top.line_no);
  //Debug.message("update_scrollbar.setValue("+top.line_no+")");
    }
  }

  // ======================= USER_FUNCTIONS =======================

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: new_window()
   ** Open new, empty editor.
   **/

  private void new_window()
  {
    RedEditorManager.red.openText(null, null, null);
  }

  // ------------------------------------------------------------------------
  /**
   ** Open another editor showing some file. (That is: show a file
   ** selection dialog first.)  This is done by calling RedEditorManager::
   ** openRequest -- RedEditorManager handles the file selection dialog.
   **/

  private void open_other ()
  {
    RedEditorManager.red.openRequest(this);
  }

  // ------------------------------------------------------------------------
  /**
   ** Save buffer under new name. Again a file selection dialog is 
   ** shown first such that the user may select a new filename.
   **/

  private void save_as ()
  {
    RedEditorManager.red.saveAsRequest(this);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: revert()
   ** Revert has been chosen. Ask "Really?" and get "do_revert" 
   ** called if the answer is yes.  NOTE: In the interface this function
   ** has been renamed "Reload".
   **/

  private void revert ()
  {
    if(filename == null) {
      info.warning ("Can not reload - this text was never saved!",
		     "(\"Reload\" reloads the last saved state from disk.)");
    }
    else if (save_state == Changed) {
      RedEditorManager.red.messages.show_question (frame, Messages.QuRevert);
      String response = RedEditorManager.red.messages.questiondialog.getCommand();
      if(response.equals("Reload")) { do_revert(); }
      else { return; }
    }
    else {
      do_revert ();
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: do_revert()
   ** Revert the buffer contents to the last saved version.  Do
   ** not ask any question - just do it.
   **/

  public void do_revert ()
  {
    FileInputStream file = null;

    try {
      // open file "filename"
      file = new FileInputStream(filename);
      buffer.load (file, true, null, RedEditorManager.red.convert_dos_on());
      set_saved (true);
      show_line_number (false);
      redisplay ();
    }
    catch(FileNotFoundException e) {
      info.warning ("ERROR: The file seems to have disappeared!", "");
    }

    try {
      file.close();
    }
    catch(IOException e) {
      ;
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: print()
   ** Displays a print dialog and prints the
   ** specifid file.
   **/

  private void print()
  {
  /*  -- NYI - mik

    // Obtain a PrintJob object.  This posts a Print dialog.
    // printprefs (created below) stores user printing preferences.
    Toolkit toolkit = this.getToolkit();
    PrintJob job = toolkit.getPrintJob(frame,filename,printprefs);
            
    // If the user clicked Cancel in the print dialog, then do nothing.
    if(job == null) return;
            
    // Get a Graphics object for the first page of output.
    Graphics page = job.getGraphics();
           
    // Check the size of the textarea component and of the page.
    Dimension size = screen.getSize();
    //Dimension pagesize = job.getPageDimension();
                
    // Set a clipping region so our text doesn't go outside the page.
    // On-screen this clipping happens automatically, but not on paper.
    page.setClip(0, 0, size.width, size.height);
         
    // Draw the textarea onto the page to be printed
    page.setFont(new Font("Helvetica", Font.PLAIN, 12));
  ////  String text = new String(screen.getText());
  String text = new String("TEST");
    page.drawString(text, 50, 50);
     
    // Print this screen. By default this will just call paint().
    this.print(page);

    // Finish up printing.
    page.dispose();   // End the page--send it to the printer.
    job.end();        // End the print job.
  */
  }


  // ------------------------------------------------------------------------
  /** This Properties object stores the user print dialog settings. */

  private static Properties printprefs = new Properties();


 // ------------------------------------------------------------------------
  /**
   ** FUNCTION: undo()
   ** Undo the last action (insertion or deletion).
   **/

  private void undo ()
  {
    char ch;
    StringBuffer text;
    Action action;
    BufferPos start;

    // if the previous function was not an undo, reset the undo stack
    if (last_func != UserFuncID.UFUndo)
      undo_stack.reset ();

    do {
      action = undo_stack.get_action ();
      if (action == null) {
	info.warning ("No further undo information available.", "");
	return;
      }

      if (check_selection (false, true))	// if selection, switch it off
	;

      if (action.get_type() == Action.InsAction) { // action was insertion
	buffer.goto_pos (action.get_start_line(), action.get_start_col());
	show_point ();
	buffer.set_pt2 ();
	buffer.goto_pos (action.get_end_line(), action.get_end_col());
	delete_between_points ();
	screen_update_from (buffer.pt);
      }
      else {					// action was deletion
	buffer.goto_pos (action.get_start_line(), action.get_start_col());
	show_point ();
	text = action.get_text ();
	if (text == null) {
          ch = action.get_character ();
	  if (ch == NewLine)
	    new_line ();
	  else
	    insert_char (ch);
	}
	else {
	  start = buffer.get_point ();
	  insert (new String (text));
	  screen_update_from (start);
	}
      }
    } while (action.is_linked ());
    show_line_number (false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: cut()
   ** Cut out the current selection and put it into the paste buffer.
   **/

  private void cut()
  {
    int lines;

    if (buffer.selection_on) {	// if selection was on...
  /////    make_selection ();
      lines = buffer.lines;
  ////    delete_selection ();
      if (lines-1 > buffer.lines)
	info.int_message (lines - buffer.lines, " lines cut to paste buffer.","");
    }
    else
      info.warning ("Cut: No selection!",
		     "(You need to select text first before you can cut it.)");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: paste()
   ** User function paste - request the text of the X selection.  When
   ** the text is recieved, 'do_paste' will be called.
   **/

  private void paste ()
  {
  notYet();
  // ------------------------------------------------------------------------
  /***********************
    utility->get_selection (this, da, (BLSelectionCallbackProc) x_paste_cb, 
			    CurrentTime);
  ********************/
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: do_paste(String)
   ** Paste 'text' into the buffer at the current position.
   **/

  private void do_paste (String text)
  {
    BufferPos start;
    int lines;

    if (text == null)
      info.warning ("Paste buffer is empty.", "");
    else {
      if (check_selection (true, true))	// if selection, cut it
	;
      buffer.set_mark ();
      lines = buffer.lines;

      start = buffer.get_point ();
      insert (text);
      screen_update_from (start);
      if (lines+1 < buffer.lines)
	info.int_message (buffer.lines - lines, " lines pasted.",
			   "(Mark set at beginning of pasted text.)");
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: insert_file()
   ** display a file dialgo to prompt the user for a file 
   ** to insert then check that file exists if so 
   ** then insert the file
   **/

  private void insert_file ()
  {
    // Create a file dialog to query the user for a filename
    FileDialog fd = new FileDialog(frame,"Insert File",FileDialog.LOAD);
    fd.show();                            // Display the dialog and block
    String insertFile = fd.getFile(); 	// Get users response
    if(insertFile != null)
    {
      Action action;
      BufferPos start;

      try {
	// See if the requested file exists
	FileInputStream file = new FileInputStream(insertFile);

	start = buffer.get_point ();
	if (RedEditorManager.red.text_quote_string() != null) 
	  buffer.insert_file (file, RedEditorManager.red.text_quote_string(), 
			       RedEditorManager.red.convert_dos_on());
	else
	  buffer.insert_file (file, null, RedEditorManager.red.convert_dos_on());
      
	file.close();             // Close the FileInputStream

	// store information about this on undo-stack
	action = undo_stack.new_action ();		// get action record
	action.set_insertion (start.line_no, start.column, 
			       buffer.pt.line_no, buffer.pt.column, false);

	screen_update_from (start);
	show_line_number (false);
	set_saved (false);
      }
      catch(IOException e)
      {
	info.warning ("Could not open file.", filename);
      }
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: find()
   ** The find function has been called. Call the finder to start the 
   ** find dialog.  The finder will pop up the dialog and, when called 
   ** back, call the "do_find" function below.
   **/

  private void find ()
  {
  notYet();
  ////  RedEditorManager.red.finder.show_dialog(status_area, false, this, false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: find_backward()
   ** The find function has been called. Call the finder to start the 
   ** find dialog.  The finder will pop up the dialog and, when called 
   ** back, call the "do_find" function below.
   **/

  private void find_backward ()
  {
  notYet();
  ////  RedEditorManager.red.finder.show_dialog (status_area, false, this, true);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: find_next()
   ** Find either the text selected or, if there is no selection,
   ** the same pattern as in the last find operation. 
   ** Call the finder to get information about the last find
   ** operation and then perform the operation again. This is done by 
   ** calling do_find below.
   **/

  private void find_next ()
  {
  notYet();
  // ------------------------------------------------------------------------
  /***************
    char* pattern;
    SearchDir direction;
    boolean case_matters, whole_word;

    red->finder->get_find_info (pattern, direction, case_matters, whole_word);
    if (buffer->selection_on) {
      pattern = buffer->selection_to_string ();
      red->finder->set_find_string (pattern);
    }
    if (pattern == null)
      info->warning ("find-next: No search pattern has been defined yet",
		     "(Use \"find\" first or select some text to search for)");
    else
      do_find (pattern, direction, case_matters, whole_word); 
      // discard result
  *********************/
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: find_next_reverse()
   ** Like "find_next", but reverse direction of search.
   **/

  private void find_next_reverse ()
  {
  notYet();
  // ------------------------------------------------------------------------
  /******************
    char* pattern;
    SearchDir direction;
    boolean case_matters, whole_word;

    red->finder->get_find_info (pattern, direction, case_matters, whole_word);
    if (buffer->selection_on) {
      pattern = buffer->selection_to_string ();
      red->finder->set_find_string (pattern);
    }
    if (pattern == null)
      info->warning ("find-next-reverse: No search pattern has been defined yet",
		     "(Use \"find\" first or select some text to search for)");
    else
      if (direction==forwd)
	do_find (pattern, backwd, case_matters, whole_word);
      else
	do_find (pattern, forwd, case_matters, whole_word);
      // discard result
  ***********************/
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: replace()
   ** Pop up the replace dialog. The actual replacing (when the 
   ** "replace" or "replace all" button in the dialog is pressed) is done
   ** in the do_replace function.
   **/

  private void replace ()
  {
  notYet();
  ////  RedEditorManager.red.finder.show_dialog (status_area, true, this, false);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: goto_line()
   ** Interactive. Ask for line number and set cursor to that line.
   ** If the number is greater than last line, goto last line.
   **/

  private void goto_line ()
  {
    int line_no;

    if (param_prompt != null) {		// got parameter - analyse and do it
      param_prompt = null;
      try {
          line_no = Integer.parseInt (parameter);
      }
      catch (NumberFormatException e) {
	  RedEditorManager.red.beep ();
	  return;
      }
      if (check_selection (false, true))	// make sure selection is off
	;
      buffer.point_to_bob ();
      buffer.goto_pos (line_no, 0);
      show_point ();
      show_line ();
      show_line_number (false);
    }
    else {				// prompt for parameter
      param_prompt = "Goto line: ";
      parameter = "";
      info.message (param_prompt, "", "");
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** Display the current line number.
   **/

  private void show_line ()
  {
    info.message_int ("Line ", buffer.pt.line_no, "");
  }

  // ------------------------------------------------------------------------
  /**
   ** Compile this class (assuming we have a class here)
   **/

  private void compile ()
  {
    if (watcher == null)
      return;

    save();
    watcher.compile(this);
//    info.message ("Class saved and compiled - no syntax errors", "", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** Change the current view. This function reads the current setting of the
   ** view selector and displays the selected view.
   **/
  private void changeView()
  {
    int view;
    
    switch (viewSelector.getSelectedIndex()) {
      case (0):	view = bluej.editor.Editor.IMPLEMENTATION;
		break;
      case (1):	view = bluej.editor.Editor.PUBLIC;
		break;
      case (2):	view = bluej.editor.Editor.PACKAGE;
		break;
      case (3):	view = bluej.editor.Editor.INHERITED;
		break;
      default:  view = 0;
    }
    watcher.changeView(this, view);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: preferences()
   ** show prefernce dialog so user can change preferences
   **/

  private void preferences ()
  {
    RedEditorManager.red.show_pref_dialog(this);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: key_bindings()
   ** show key bindings dialog so user can change preferences
   **/

  private void key_bindings ()
  {
    RedEditorManager.red.userfunc.show_dialog(frame);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: describe_key()
   ** describes a key binding
   **/

  private void describe_key()
  {
    param_prompt =  "Describe key: ";
    info.message (param_prompt, "", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: do_describe() 
   ** do the describe key 
   **/

  private void do_describe (int func)
  {
    String name;

    if (func == UserFuncID.NOT_BOUND)		// no function on this key
      info.message ("This key is undefined","","");
    else {
      name = RedEditorManager.red.userfunc.funcname (func);
      info.message ("This key calls the function ", name, "");
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: set_mark()
   ** Set mark at cursor position.
   **/

  private void set_mark ()
  {
    buffer.set_mark ();
    info.message ("Mark set", "", "");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: swap_point_mark()
   ** Swap mark and cursor position.
   **/

  private void swap_point_mark ()
  {
    if (check_selection (false,true))	// make sure selection is off
      ;
    if (buffer.swap_point_mark ()) {
      show_point ();
      show_line_number (false);
    }
    else
      info.warning ("No mark has been set", 
		     "(You tried to swap point and mark)");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: about()
   ** diplay about dialog
   **/

  private void about()
  {
    RedEditorManager.red.messages.show_help(frame, Messages.HlpAboutRed);
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: copyright()
   ** diplay copyright dialog
   **/

  private void copyright()
  {  
    RedEditorManager.red.messages.show_help(frame, Messages.HlpCopyright);
  }  


  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: mouse_buttons()
   ** diplay mouse buttons dialog
   **/

  private void mouse_buttons()
  {  
    RedEditorManager.red.messages.show_help(frame, Messages.HlpMouse);
  }  
  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: report_bugs()
   ** diplay reporting bugs dialog
   **/

  private void report_bugs()
  {  
    RedEditorManager.red.messages.show_help(frame, Messages.HlpErrors);
  }  

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: manual()
   ** Start up a WWW browser defined in the application resources to
   ** display the user manual.
   **/

  private void manual ()
  {
  notYet();
  // ------------------------------------------------------------------------
  /****************************
    char command[128];
    int error;

    info->message ("Opening web browser to display manual...", "", "");
    utility->flush ();
    strncpy (command, rsrc->red_man_command, 115);
    strcat (command, " 2>/dev/null");
    error = system (command);
    if (error)						// if command failed...
      error = system (rsrc->red_man_command2);		// try second command
    if (error)
      info->message ("Failed to execute command to display manual", "",
	    "(Check the \"redManCmd\" resource in your X resource file)");
    else
      info->message ("Opening web browser to display manual... done", "", "");
  *************************/
  }

  // ------------------------------------------------------------------------
  /**
   ** Dummy used for key bindings that call functions in one
   ** version of red (integrated or stand-alone) but not in the other.
   **/

  private void empty_function ()
  {
    info.warning ("undefined key","");
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: notYet()
   ** Shows a dialog informing the user that
   ** this function has NOT YET been implemented
   **/

  private void notYet() 
  {
    String comment = "Function not yet implemented!";
    QuestionDialog qd = new QuestionDialog(frame, "Not Yet", comment, "OK");
    qd.display();
  }


  // ======================= EVENT_HANDLING =======================

  // ------------------------------------------------------------------------
  /**
   ** Handle GUI actions. This is the ActionListener method invoked by the
   ** Swing component actions. Actions are generated by menu items, buttons
   ** and the ComboBox.
   **/
  public void actionPerformed(ActionEvent event)
  {
    String command = event.getActionCommand();

    if(command.equals("New")) { new_window(); }
    else if(command.equals("Open")) { open_other(); }
    else if(command.equals("Save")) { save(); }
    else if(command.equals("SaveAs")) { save_as(); }
    else if(command.equals("Reload")) { revert(); }
    else if(command.equals("Print")) { print(); }
    else if(command.equals("Close")) { close(); }
    else if(command.equals("Undo")) { undo(); }
    else if(command.equals("Cut")) { cut(); }
    else if(command.equals("Paste")) { paste(); }
    else if(command.equals("SelectAll")) { select_all(); }
    else if(command.equals("Comment")) { insert_comment(); }
    else if(command.equals("Uncomment")) { remove_comment(); }
    else if(command.equals("InsertFile")) { insert_file(); }
    else if(command.equals("Find")) { find(); }
    else if(command.equals("FindNext")) { find_next(); }
    else if(command.equals("Replace")) { replace(); }
    else if(command.equals("GotoLine")) { goto_line(); }
    else if(command.equals("LineNo")) { show_line(); }
    else if(command.equals("Compile")) { compile(); }
    else if(command.equals("ChangeView")) { changeView(); }
    else if(command.equals("Preferences")) { preferences(); }
    else if(command.equals("KeyBindings")) { key_bindings(); }
    else if(command.equals("About")) { about(); }
    else if(command.equals("Copyright")) { copyright(); }
    else if(command.equals("Functions")) { key_bindings(); }
    else if(command.equals("DescribeKey")) { describe_key(); }
    else if(command.equals("MouseButtons")) { mouse_buttons(); }
    else if(command.equals("Manual")) { manual(); }
    else if(command.equals("ReportErrors")) { report_bugs(); }
  }

  // ------------------------------------------------------------------------
  /********** NOTYET **************
  //  scroll_event: The scroll bar was clicked somewhere. Call the appropriate
  //      scroll function depending on the part that was clicked (reason).
  //      value is the new scroll value.

//   private void scroll_event (int reason, int value)
//   {
//     last_func = UserFuncID.NOT_BOUND;
//     switch (reason) {
// 
//       case BLXmCR_DECREMENT:
// 	scroll_line_up ();
// 	break;
// 
//       case BLXmCR_INCREMENT:
// 	scroll_line_down ();
// 	break;
// 
//       case BLXmCR_PAGE_DECREMENT:
// 	previous_page ();
// 	break;  
// 
//       case BLXmCR_PAGE_INCREMENT:
// 	next_page ();
// 	break;
// 
//       case BLXmCR_DRAG:
// 	scroll_drag (value);
// 	break;
// 
//       default:
// 	assertion (false);
//     }
//   }
  ****************************************/

  // ------------------------------------------------------------------------
  /**
   ** keyTyped - called when a key is pressed and then released
   **/

  public void keyTyped (KeyEvent e) {}

    // ATTENTION: in a keyTyped event, only the keyChar is set; the keyCode
    //   is invalid!
    // There is an annoying bug with key presses in Java: Usually we would like
    // to use "KeyPressed", but when "Shift" is pressed, the key does not 
    // generate a "KeyPressed" event (BUG!). It just generates a "KeyTyped"
    // event. On the other hand, there is no "KeyTyped" event for non-printable
    // keys. Moreover, the modifier flag in a "KeyTyped" event is wrong (always
    // 0).

  // ------------------------------------------------------------------------
  /**
   ** A key has been pressed in the edit window.  Use
   ** the keymaps to map the key to an editor function, then call that
   ** function.
   **/

  public void keyPressed (KeyEvent e) {}

  // ------------------------------------------------------------------------
  /**
   ** keyReleased - called when a key is released
   **/

  public void keyReleased (KeyEvent e) 
  {
    int keysym;
    int modifiers;
    int func;

    keysym = e.getKeyCode ();
    modifiers = e.getModifiers ();

    func = RedEditorManager.red.userfunc.translate_key(keysym, modifiers);
    //Debug.message("keyPressed: func == " + func + "  modifiers == " + modifiers);

    if (func == UserFuncID.UNKNOWN_KEY)   // do nothing. probably modifier key,
      return;                             // or something we don't want to handle

    last_char = e.getKeyChar();
    call_userfunction (func);
  }

  // ------------------------------------------------------------------------
  /**
   ** componentHidden (from ComponentListener) - called when the frame has 
   ** been hidden - IGNORED
   **/

  public void componentHidden(ComponentEvent e) {}

  // ------------------------------------------------------------------------
  /**
   ** componentMoved (from ComponentListener) - called when the frame has 
   ** been moved - IGNORED
   **/

  public void componentMoved(ComponentEvent e) {}

  // ------------------------------------------------------------------------
  /**
   ** componentResized (from ComponentListener) - called when the frame has 
   ** been resized
   **/

  public void componentResized(ComponentEvent e)
  {
    screen.resize();
    Debug.message("component resized - resizing screen");
  }

  // ------------------------------------------------------------------------
  /**
   ** componentShown (from ComponentListener) - called when the frame has 
   ** been shown
   **/

  public void componentShown(ComponentEvent e)
  {
    screen.requestFocus();
    Debug.message("component shown");
  }

  // ------------------------------------------------------------------------
  /**
   ** focusGained - called when something gains the focus
   **/
//   public void focusGained(FocusEvent e) 
//   {
// 	  screen.requestFocus();
//   }

  // ------------------------------------------------------------------------
  /**
   ** focusLost - called when something loses the focus
   **/
//   public void focusLost(FocusEvent e) {}

  // ------------------------------------------------------------------------
  /**
   ** Mouse Events
   **/
  public void mouseClicked(MouseEvent e) 
  { 
    boolean inTag = false;

	  screen.requestFocus();

	  if (e.getSource() != screen)	// Do screen point operations only if src is screen
		  return;
		
	  Point pt = e.getPoint();
		
	  info.clear();
	  last_func = UserFuncID.NOT_BOUND;
	  int[/* 2 */] mpos = screen.point_to_square(pt.x, pt.y);
	  mouse_line = mpos[0];
	  mouse_col = mpos[1];
	  if (mouse_col == -1)
	    inTag = true;
	
	  boolean button2 = e.isAltDown();
	  boolean button3 = e.isMetaDown();
	  boolean button1 = !(button2 || button3);
	
	  if(button1)
	  {
		  if(e.getClickCount() == 2)	// double click
			  select_word();
		  else if(e.getClickCount() > 2)	// triple+ click
		  {
			  if(e.isShiftDown())
				  select_word();
			  else
				  select_line();
		  }
		  else		// single click
		  {
			  cursor_to(mouse_col, mouse_line);
			  show_line_number(false);
		  }
		
		  screen.addMouseMotionListener(this);
		  drag_start = buffer.pt;
	  }
	  else if(button2)
	  {
		  cursor_to(mouse_col, mouse_line);
		  paste();
	  }
	  else		// Button 3
	  {
		  extend_highlight(mouse_col, mouse_line);
		  make_selection();
	  }
  }

  // ------------------------------------------------------------------------
  public void mouseReleased(MouseEvent e)
  {
	  screen.requestFocus();

	  if (e.getSource() != screen)	// Do screen point operations only if src is screen
		  return;

	  boolean button2 = e.isAltDown();
	  boolean button3 = e.isMetaDown();
	  boolean button1 = !(button2 || button3);
	
	  if(button1)
	  {
		  make_selection();		// copy to paste buffer
		  screen.removeMouseMotionListener(this);
	  }
  }

  // ------------------------------------------------------------------------
  public void mouseDragged(MouseEvent e)
  {
	  Point pt = e.getPoint();
		
	  int[/* 2 */] mpos = screen.point_to_square(pt.x, pt.y);
	  int line = mpos[0];
	  int col = mpos[1];
	
	  if((col != mouse_col) || (line != mouse_line))
	  {
		  mouse_col = col;
		  mouse_line = line;
		
		  drag_highlight(mouse_col, mouse_line);
	  }
  }

  public void mousePressed(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  public void mouseMoved(MouseEvent e) {}

  // ------------------------------------------------------------------------
  /**
   ** Adjustment Events
   **/
  public void adjustmentValueChanged(AdjustmentEvent e)
  {
  //Debug.message("Editor:adjustmentValueChanged to " + e.getValue() );
  //	top.line_no = e.getValue();
  //	forward_screenline (top);	// move display down one screen line
  //	display ();
  //	show_line_number (false);

  //	buffer.goto_pos(e.getValue(),0);
  //	show_point();

  //	top.line_no = e.getValue();
  //	display();

	  scroll_drag(e.getValue());
  }

  // ------------------------------------------------------------------------
  /**
   ** Expose event - this is not a real Java event, but I handle it as if it
   ** were... It is actually cause by the "paint" method of the screen or info
   ** being called.
   **/
  public void expose()
  {
    Debug.message("expose");

    screen.expose();
    info.expose();

    if(first_expose) {
      redisplay();
      first_expose = false;
    }
    else
      display();
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: call_userfunction(int)
   ** Call the user function
   **/
    
  private void call_userfunction (int func)
  {
    if (param_prompt != null)		// currently getting param in info
      get_param (func);
    else {
      if (func == UserFuncID.NOT_BOUND)		// no function on this key
	info.warning ("undefined key","");
      else {                                      // call the function
	info.clear();
	call_func (func);	
      }
      if (func != UserFuncID.UFClose) 	// if we haven't just closed this editor
	last_func = func;                 // save the last function executed
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** FUNCTION: call_func(int)
   ** call the function
   **/

  private void call_func(int funcID)
  {
    switch(funcID) 
    {
      case UserFuncID.UFNewLine:		new_line(); break;
      case UserFuncID.UFOpenLine:		open_line(); break;
      case UserFuncID.UFDeleteChar:	delete_char(); break;
      case UserFuncID.UFBackDeleteChar:	backward_delete_char(); break;
      case UserFuncID.UFBackDeleteUntab: 	backward_delete_untab(); break;
      case UserFuncID.UFTabToTabStop:	tab_to_tab_stop(); break;
      case UserFuncID.UFHalfTab:		half_tab(); break;
      case UserFuncID.UFIndent:		indent(); break;
      case UserFuncID.UFNewLineIndent:	new_line_and_indent(); break;
      case UserFuncID.UFCutWord:		cut_word(); break;
      case UserFuncID.UFCutToEOWord:	cut_to_end_of_word(); break;
      case UserFuncID.UFCutLine:		cut_line(); break;
      case UserFuncID.UFCutToEOLine:	cut_to_end_of_line(); break;
      case UserFuncID.UFCutRegion:	cut_region(); break;
      case UserFuncID.UFCut:		cut();	break;
      case UserFuncID.UFPaste:		paste(); break;
      case UserFuncID.UFSelectWord:	select_word(); break;
      case UserFuncID.UFSelectLine:	select_line(); break;
      case UserFuncID.UFSelectRegion:	select_region(); break;
      case UserFuncID.UFSelectAll:	select_all(); break;
      case UserFuncID.UFShiftLeft:	shift_left(); break;
      case UserFuncID.UFShiftRight:	shift_right(); break;
      case UserFuncID.UFInsertFile:	insert_file(); break;
      case UserFuncID.UFInsertComment:	insert_comment(); break;
      case UserFuncID.UFRemoveComment:	remove_comment(); break;
      case UserFuncID.UF_Unused1:		empty_function(); break;
      case UserFuncID.UF_Unused2:		empty_function(); break;
      case UserFuncID.UFForwardChar:	forward_char(); break;
      case UserFuncID.UFBackwardChar:	backward_char(); break;
      case UserFuncID.UFForwardWord:	forward_word(); break;
      case UserFuncID.UFBackwardWord:	backward_word(); break;
      case UserFuncID.UFEndOfLine:	end_of_line(); break;
      case UserFuncID.UFBegOfLine:	beginning_of_line(); break;
      case UserFuncID.UFNextLine:		next_line(); break;
      case UserFuncID.UFPrevLine:		previous_line(); break;
      case UserFuncID.UFScrollLineDown:	scroll_line_down(); break;
      case UserFuncID.UFScrollLineUp:	scroll_line_up(); break;
      case UserFuncID.UFScrollHPDown:	scroll_half_page_down(); break;
      case UserFuncID.UFScrollHPUp:	scroll_half_page_up(); break;
      case UserFuncID.UFPrevPage:		previous_page(); break;
      case UserFuncID.UFNextPage:		next_page(); break;
      case UserFuncID.UFBegOfText:	beginning_of_text(); break;
      case UserFuncID.UFEndOfText:	end_of_text(); break;
      case UserFuncID.UFSwapCursorMark:	swap_point_mark(); break;
  //    case UserFuncID.UFNextFlag:	blue_next_flag(); break;
      case UserFuncID.UFNew:		new_window(); break;
      case UserFuncID.UFOpen:		open_other(); break;
  //    case UserFuncID.UFOpenSel:	open_selection(); break;
      case UserFuncID.UFSave:		save(); break;
      case UserFuncID.UFSaveAs:		save_as(); break;
      case UserFuncID.UFRevert:		revert(); break;
      case UserFuncID.UFClose:		close(); break;
      case UserFuncID.UFPrint:		print(); break;
      case UserFuncID.UFPreferences:	preferences(); break;
      case UserFuncID.UFKeyBindings:	key_bindings(); break;
  //    case UserFuncID.UFEditToolb:	edit_toolbar(); break;
  //    case UserFuncID.UFSetFonts:	fonts(); break;
  //    case UserFuncID.UFSetColours:	colors(); break;
      case UserFuncID.UFDescribeKey:	describe_key(); break;
      case UserFuncID.UFShowManual:	manual(); break;
      case UserFuncID.UFUndo:		undo(); break;
      case UserFuncID.UFFind:		find(); break;
      case UserFuncID.UFFindBackward:	find_backward(); break;
      case UserFuncID.UFFindNext:		find_next(); break;
      case UserFuncID.UFFindNextRev:	find_next_reverse(); break;
      case UserFuncID.UFReplace:		replace(); break;
      case UserFuncID.UFSetMark:		set_mark(); break;
      case UserFuncID.UFGotoLine:		goto_line(); break;
      case UserFuncID.UFShowLine:		show_line(); break;
  //    case UserFuncID.UFDefMacro:	define_macro(); break;
  //    case UserFuncID.UFEndMacro:	end_macro(); break;
  //    case UserFuncID.UFRunMacro:	run_macro(); break;
  //    case UserFuncID.UFInterface:	interface_toggle(); break;
      case UserFuncID.UFRedisplay:	redisplay(); break;
  //    case UserFuncID.UFBlueNewRout:	blue_new_routine(); break;
  //    case UserFuncID.UFBlueExpand:	blue_expand(); break;
  //    case UserFuncID.UFStatus:		edit_debug(); break;
      case UserFuncID.UFCompile:	compile(); break;
      case UserFuncID.UF_UNUSED:		empty_function(); break;
  //    case UserFuncID.UFSetBreak:	set_breakpoint(); break;
  //    case UserFuncID.UFClearBreak:	clear_breakpoint(); break;
  //    case UserFuncID.UFStep:		step(); break;
  //    case UserFuncID.UFStepInto:	step_into(); break;
  //    case UserFuncID.UFContinue:	continue_exec(); break;
  //    case UserFuncID.UFTerminate:	terminate_exec(); break;
      case UserFuncID.UFSelfInsert:	self_insert_char(); break;
      default:
	  empty_function();
	  Debug.message("Empty Function Called: func = " + funcID);
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** We are currently reading a parameter in the info area.
   **/

  private void get_param (int func)
  {
    if (last_func == UserFuncID.UFDescribeKey) {
      do_describe (func);
      param_prompt = null;
    }
    else {
	switch (func) {
	  case UserFuncID.UFSelfInsert:
	    parameter = parameter + last_char;
	    info.message (param_prompt, parameter, "");
	    break;
	  case UserFuncID.UFBackDeleteChar:
	    if (parameter.length() > 0)
		  parameter = parameter.substring (0, parameter.length()-1);
	    info.message (param_prompt, parameter, "");
	    break;
	  case UserFuncID.UFNewLine:
	  case UserFuncID.UFNewLineIndent:
	    goto_line ();
	    break;
	  default:
	    RedEditorManager.red.beep ();
	}
    }
  }

  // ======================= WINDOW INITIALISATION =======================

  // ------------------------------------------------------------------------
  /**
   ** Create all the Window components
   **/

  private void init_window(boolean showTool, boolean showLine)
  {
    frame = new JFrame("Red");
    frame.setBackground(frameBgColor);
    frame.addComponentListener(this);

    width = 500;
    height = 500;

    mainPanel = new JPanel(false);
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 6, 5));
    mainPanel.setLayout(new BorderLayout(5, 5));

    // * create menubar *

    JMenuBar menubar = new JMenuBar();
    menubar.setMargin(new Insets (2, 2, 2, 2));

    // * create menus *

    String[] labels = null;
    String[] commands = null;


    // * create "File" menu *

    JMenu fileMenu = new JMenu("File");
    menubar.add(fileMenu);
    if (RedEditorManager.standAlone)
    {
      labels = new String[]   { "New", "Open...", "Save", "Save as...", 
				"Reload", "--", "Print...", "--", "Close"};
      commands = new String[] { "New", "Open", "Save", "SaveAs",
				"Reload", "--", "Print", "--", "Close"};
    }
    else
    {
      labels = new String[]   { "Save", "Reload", "--", "Print...", "--", 
				"Close"};
      commands = new String[] { "Save", "Reload", "--", "Print", "--", "Close"};
    }
    buildMenu(fileMenu, labels, commands);


    // * create "Edit" menu *  

    JMenu editMenu = new JMenu("Edit");
    menubar.add(editMenu);
    if (RedEditorManager.standAlone)
    {
      labels = new String[]   { "Undo", "--", "Cut", "Paste", "Select All", 
				"--", "Comment", "Uncomment", "--", 
				"Insert File..."};
      commands = new String[] { "Undo", "--", "Cut", "Paste", "SelectAll", 
				"--", "Comment", "Uncomment", "--", 
				"InsertFile"};
    }
    else
    {
      labels = new String[]   { "Undo", "--", "Cut", "Paste", "Select All", 
				"--", "Comment", "Uncomment"};
      commands = new String[] { "Undo", "--", "Cut", "Paste", "SelectAll", 
				"--", "Comment", "Uncomment"};
    }
    buildMenu(editMenu, labels, commands);


    // * create "Tools" menu *

    JMenu toolsMenu = new JMenu("Tools");
	toolsMenu.setMargin(new Insets (2, 2, 2, 2));
    menubar.add(toolsMenu);
    if (RedEditorManager.standAlone)
    {
      labels = new String[]   { "Find", "Find Next", "Replace...", 
				"Goto Line...", "Show Line Number" };
      commands = new String[] {	"Find", "FindNext", "Replace", 
				"GotoLine", "LineNo"};
    }
    else
    {
      labels = new String[]   { "Find", "Find Next", "Replace...", 
				"Goto Line...", "Show Line Number", "--", 
				"Compile" };
      commands = new String[] {	"Find", "FindNext", "Replace", 
				"GotoLine", "LineNo", "--", "Compile"};
    }
    buildMenu(toolsMenu, labels, commands);


    // * create "Debug" menu *

    if (!RedEditorManager.standAlone)
    {
      JMenu debugMenu = new JMenu("Debug");
      menubar.add(debugMenu);
      labels = new String[]   { "Set Breakpoint", "Clear Breakpoint", "--", 
				"Step", "Step Into", "Continue", "Terminate" };
      commands = new String[] { "SetBreak", "ClearBreak",  "--", 
			        "Step", "StepInto", "Continue", "Terminate" };
      buildMenu(debugMenu, labels, commands);
      set_debug(false);
    }


    // * create "Options" menu *

    JMenu optionsMenu = new JMenu("Options");
    menubar.add(optionsMenu);

    labels = new String[]   { "Preferences...", "Key Bindings..." };
			  //    "Edit Toolbar...", "Fonts...", "Colours..."};
    commands = new String[] { "Preferences", "KeyBindings" };
			  //    "EditToolbar", "Fonts", "Colours"};
    buildMenu(optionsMenu, labels, commands);


    // * create "Help" menu *

    JMenu helpMenu = new JMenu("Help");
//    menubar.setHelpMenu(helpMenu);	// NYI in JMenuBar
    menubar.add(helpMenu);
    labels = new String[]   { "About Red...", "Copyright Notice...", "--",
			      "Functions and Keys...", "Describe Key...",
			      "Mouse Buttons...", "Manual...", "--", 
			      "Reporting Errors..." };
    commands = new String[] { "About", "Copyright", "--", "Functions", 
			      "DescribeKey", "MouseButtons", "Manual", "--", 
			      "ReportErrors"};
    buildMenu(helpMenu, labels, commands);

    frame.setJMenuBar(menubar);
      

    // * create the Toolbar panel *

    toolbar = new JPanel();
    ((FlowLayout)toolbar.getLayout()).setAlignment (FlowLayout.LEFT);
//    toolbar.addFocusListener(this);

    if (RedEditorManager.standAlone)
    {
      labels = new String[]   { "Save", "Open...", "Undo", "Cut", "Paste", 
			        "Find...", "Find Next", "Replace...", "Close" };
      commands = new String[] { "Save", "Open", "Undo", "Cut", "Paste", 
			        "Find", "FindNext", "Replace", "Close" };
    }
    else
    {
      labels = new String[]   { "Compile", "Undo", "Cut", "Paste", 
			        "Find...", "Find Next", "Replace...", "Close" };
      commands = new String[] { "Compile", "Undo", "Cut", "Paste", 
			        "Find", "FindNext", "Replace", "Close" };

      // add view selector
      String[] viewStrings = 
        { "Implementation", "Public View", "Package View", "Protected View" };
      viewSelector = new JComboBox(viewStrings);
      viewSelector.setActionCommand("ChangeView");
      viewSelector.addActionListener(this);
      toolbar.add(viewSelector);
    }

    JButton[] buttonArray = new JButton[labels.length];
    
    for(int i=0; i<labels.length; i++) {
      buttonArray[i]  = new JButton(labels[i]);
      buttonArray[i].setActionCommand(commands[i]);
      buttonArray[i].addActionListener(this);
      buttonArray[i].setFocusPainted(false);
      buttonArray[i].setMargin(new Insets (2, 2, 2, 2));
      toolbar.add(buttonArray[i]);
    }


    // * create and add screen to main panel *

    JPanel screenArea = new JPanel();	// create panel for screen w/ scrollbar
    screenArea.setLayout(new BorderLayout());

      if (isCode)
          screen = new Screen(this, 16);
      else
          screen = new Screen(this, 0);
      screen.addKeyListener(this);
      screen.addMouseListener(this);
      screenArea.add("Center", screen);

      // Create and add the scrollbar

      scrollbar = new Scrollbar(Scrollbar.VERTICAL);
      scrollbar.addAdjustmentListener(this);
      screenArea.add("East", scrollbar);

    mainPanel.add("Center", screenArea);

    // * create and add info and status areas *

    JPanel bottomArea = new JPanel();		// create panel for info/status
    bottomArea.setLayout(new BorderLayout(5, 5));

      info = new Info(RedEditor.infoFont, "","");
//      info.addFocusListener(this);
      bottomArea.add ("Center", info);

      statusArea = new JPanel();
      statusArea.setLayout(new GridLayout(0, 1));	// one column, many rows
      statusArea.setBackground(infoColor);
      statusArea.setBorder(BorderFactory.createLineBorder(Color.black));

      lineNumLabel = new JLabel("1", JLabel.CENTER);
      statusArea.add(lineNumLabel);
      statusLabel = new JLabel("saved", JLabel.CENTER);
      statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
      statusArea.add(statusLabel);
      bottomArea.add("East", statusArea);
  
    mainPanel.add("South",bottomArea);  	// Add panel to main panel


    // * add event listener to handle the window close requests *

    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	close();
      }
    });

    if (showTool)
       show_toolbar(true);

    if (!showLine)				// if the line number display
      show_line_num (false);			//   is not on, hide it

    frame.getContentPane().add (mainPanel, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);

  } // init_window


  /**
   ** Build a menu from string arrays defining items
   **/

  private void buildMenu (JMenu menu, String[] labels, String[] commands)
  {
    for(int i=0; i<labels.length; i++) {

      if (labels[i].equals("--"))

	menu.addSeparator();

      else
      {
	JMenuItem item = new JMenuItem(labels[i]);
	item.setActionCommand(commands[i]);
	item.addActionListener(this);
	menu.add(item);
      }
    }
  }


} // end class RedEditor
