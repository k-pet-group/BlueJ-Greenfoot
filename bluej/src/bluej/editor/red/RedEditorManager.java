package bluej.editor.red;	    // This file forms part of the red package

import bluej.utility.Debug;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;

import java.awt.*;		// Font
import java.io.*;		// Object input, ouput streams
import java.util.Vector;

/**
 ** @version $Id: RedEditorManager.java 111 1999-06-04 06:16:57Z mik $
 ** @author Michael Kolling
 ** @author Justin Tan
 **
 ** RED implements the common part of the multiple edit windows, that the
 ** user can open.  All the data structures common to all edit windows are
 ** created and stored here.  They are: the list of user functions and the
 ** key maps.  They are created here once at program startup and then
 ** passed to every editor as it is opened.  (Note: an editor in Red is
 ** the part of the system that handles editing of a single file in an
 ** own window.  So Red can have several "editors" open.)
 **/

public final class RedEditorManager implements bluej.editor.EditorManager
{
  // public static variables

    public static boolean standAlone;		// true if stand-alone version
    public static RedEditorManager red;		// the manager object itself

  // public variables

  // misc.  (use read-only!)
    public UserFunc userfunc;			// user function object
    // public Finder finder;			// the finder
    public Messages messages;			// the message manager
    public StringBuffer paste_buffer;		// the paste buffer


  // private variables

    private Vector editors;			// open editors
    private PrefDialog pref_dialog;     	// preferences dialog
    private String red_prefs_file = "red.def";	// The Red preferences file

  // preference settings
    private boolean show_toolbar;		// Show toolbar flag
    private boolean show_line_num;		// Show line Number
    private boolean beep_warning;		// The Beep flag
    private boolean make_backup;		// Make backup file
    private boolean append_newline;		// Append new line
    private boolean convert_dos;		// Convert_dos flag
    private String quote_string;		// Quote string
    private String comment_start_string;	// Comment start label
    private String comment_end_string;  	// Comment end Label
   
   
  // =========================== PUBLIC METHODS ===========================

  public RedEditorManager(boolean standAlone)
  {	
    editors = new Vector(4,4);
    messages = new Messages();
    userfunc = new UserFunc();
    // finder = new Finder();
    paste_buffer = null;

    quote_string = null;
    comment_start_string = null;
    comment_end_string = null;
    this.standAlone = standAlone;

    read_prefs();

    red = this;		// make this object publicly available
  }


  // ------------------------------------------------------------------------
  /**
   ** Open an editor to display a class. The filename may be "null"
   ** to open an empty editor (e.g. for displaying a view). The editor 
   ** is initially hidden. A call to "Editor::show" is needed to make 
   ** is visible after opening it.
   **
   ** @param filename	name of the source file to open (may be null)
   ** @param windowTitle	title of window (usually class name)
   ** @param watcher	an object interested in editing events
   ** @param compiled	true, if the class has been compiled
   ** @param breakpoints	vector of Integers: line numbers where bpts are
   ** @returns		the new editor, or null if there was a problem
   **/

  public Editor openClass(String filename, String windowTitle, 
			  EditorWatcher watcher, boolean compiled,
			  Vector breakpoints)	// inherited from EditorManager
  {
    return openEditor (filename, true, windowTitle, watcher, compiled, 
		       breakpoints);
  }
  
  // ------------------------------------------------------------------------
  /**
   ** Open an editor to display a text document. The difference to 
   ** "openClass" is that code specific functions (such as compile, 
   ** debug, view) are disabled in the editor. The filename may be
   ** "null" to open an empty editor. The editor is initially hidden. 
   ** A call to "Editor::show" is needed to make is visible after 
   ** opening it.
   **
   ** @param filename	name of the source file to open (may be null)
   ** @param windowTitle	title of window (usually class name)
   ** @param watcher	an object interested in editing events
   ** @returns		the new editor, or null if there was a problem
   **/

  public Editor openText(String filename, String windowTitle, 
			EditorWatcher watcher)	// inherited from EditorManager
  {
    return openEditor (filename, false, windowTitle, watcher, false, null);
  }

  // ------------------------------------------------------------------------
  /**
   ** The 'open' item from the menu was chosen.  Show a file 
   ** selection dialog to let the user choose a file to open.
   ** If a file name was given try to open that file.
   ** The file is open if it exists else
   ** an exception is thrown in a dialog.
   **/

  public void openRequest(RedEditor editor)
  {
    // Create a file dialog to query the user for a filename
    FileDialog fd = new FileDialog(editor.getFrame(), "Load File",
				   FileDialog.LOAD);

    fd.show();                      	// Display the dialog and block
    String filename = fd.getFile();  	// Get users response

    if(filename != null)          	// if user didn't click "Cancel"
    { 
	// See if the requested file is readable
	File file = new File(filename);
	if(file.canRead())
	    openText(filename, null, null);
	else
	{
	    // Print out error
	    String comment = new String("ERROR: File Not Found.");
	    new QuestionDialog(editor.getFrame(), "Error", comment, "OK");
	}
    }
  }

  // ------------------------------------------------------------------------
  /**
   ** An editor has issued a "save_as" (or a save with no
   ** filename specified).  It now requests of Red to initiate a "save-as"
   ** dialog.  The file selection dialog is shown. If the 
   ** OK button was clicked save the buffer under that new filename.
   **/

  public void saveAsRequest(RedEditor editor)
  {
    // Create a file dialog to query the user for a filename
    FileDialog fd = new FileDialog(editor.getFrame(), "Save File As", 
				   FileDialog.SAVE);
    fd.show();                      	// Display the dialog and block
    String filename = fd.getFile();     	// Get users response
    if(filename != null)		// if user didn't click "Cancel"
	editor.do_save_as(filename);
  }

  // ------------------------------------------------------------------------
  /**
   ** Start a "close editor" action.  That is: check whether
   ** it has been changed; if so, ask for save.  If not, go on to real
   ** close straight away.
   **/

  public void closeRequest(RedEditor editor, boolean changed)
  {
    if (changed)
    {
	messages.show_question(editor.getFrame(), Messages.QuSaveChanges);
	String response = messages.questiondialog.getCommand();
	if (response.equals("Cancel"))
	  return;
	closeEditor(editor, (response.equals("Save"))); 
    }
    else
	closeEditor(editor, false);
  }

  // ------------------------------------------------------------------------
  /**
   ** The following functions are used by the preferences dialog to set
   ** the preferences.
   **/

  public void set_show_toolbar(boolean b) { show_toolbar=b; }
  public void set_show_line(boolean b) { show_line_num=b; }
  public void set_beep_on(boolean b) { beep_warning=b; }  
  public void set_backup_on(boolean b) { make_backup=b; }
  public void set_append_nl_on(boolean b) { append_newline=b; }
  public void set_convert_dos_on(boolean b) { convert_dos=b; }
  public void set_text_quote_string(String s) { quote_string=s; }
  public void set_start_comment_string(String s) { comment_start_string=s; }
  public void set_end_comment_string(String s) { comment_end_string=s; }

  // ------------------------------------------------------------------------
  /**
   ** The following functions are used to query the preferences.
   **/

  public boolean show_toolbar ()		// True if show toolbar
	  { return show_toolbar; }
  public boolean show_line ()   	    	// line number display is on
	  { return show_line_num; }
  public boolean beep_on ()			// True if beeping is on
	  { return beep_warning; }
  public boolean backup_on ()			// True if make_backup is on
	  { return make_backup; }
  public boolean append_nl_on ()		// append newline to file
	  { return append_newline; }
  public boolean convert_dos_on ()		// True if "convert dos" is on
	  { return convert_dos; }
  public String text_quote_string ()		// The string for file quote
	  { return quote_string; }
  public String start_comment_string ()		// The start of comment string.
	  { return comment_start_string; }
  public String end_comment_string ()		// The end of comment string.
	  { return comment_end_string; }

  // ------------------------------------------------------------------------
  /**
   ** Show prefernce dialog so user can change preferences
   **/
  public void show_pref_dialog(RedEditor editor)
  {
	   pref_dialog = new PrefDialog(editor);
  }

  // ------------------------------------------------------------------------
  /**
   ** Sound a beep if the "beep with warning" option is true
   **/
  public void beep()
  {
    if(beep_warning)
	Toolkit.getDefaultToolkit().beep();
  }

  // ========================== PRIVATE METHODS ===========================

  // ------------------------------------------------------------------------
  /**
   ** Open an editor to display a class. The filename may be "null"
   ** to open an empty editor (e.g. for displaying a view). The editor 
   ** is initially hidden. A call to "Editor::show" is needed to make 
   ** is visible after opening it.
   **
   ** @param filename	name of the source file to open (may be null)
   ** @param windowTitle	title of window (usually class name)
   ** @param watcher	an object interested in editing events
   ** @param compiled	true, if the class has been compiled
   ** @param breakpoints	vector of Integers: line numbers where bpts are
   ** @returns		the new editor, or null if there was a problem
   **/

  private Editor openEditor(String filename, boolean isCode, 
			    String windowTitle, EditorWatcher watcher, 
			    boolean compiled, Vector breakpoints)
  {
    RedEditor new_ed;
    
    new_ed = new RedEditor(windowTitle, isCode, watcher, show_toolbar, 
			   show_line_num);
    editors.addElement(new_ed);
    if (watcher!=null && filename==null)	// editor for class interface
      return new_ed;
    if (new_ed.showFile (filename, compiled, null))
      return new_ed;
    else {
      closeRequest (new_ed, false);	// close if not successful
      return null;
    }
  }
  
  // ------------------------------------------------------------------------
  /**
   ** The editor is about to be closed.  If "save" is true, the
   ** buffer should be saved first.  So save it and then proceed.
   ** Really closes an editor. If this is the last editor
   ** then we quit the program.
   **/
  private void closeEditor(RedEditor editor, boolean save)
  {
    if (save)
	editor.save();
    editor.doClose();
    editors.removeElement(editor);
    if(standAlone && (editors.size() == 0))	// if no editors left
	System.exit(0);				// exit Red
  }

  // ------------------------------------------------------------------------
  /**
   ** Read the preferences file.
   **/
  public void read_prefs ()
  {
    String filename = red_prefs_file;
    String version = RedVersion.versionString();
    boolean done = false;

    try {
	FileInputStream fis = new FileInputStream(filename);
	ObjectInputStream file = new ObjectInputStream(fis);
		
	version = (String)file.readObject();
	if(!version.equals(RedVersion.versionString()))
	{
	    //// messages.show_error (getFrame(), Messages.ErrReadPrefs);
	    System.err.println("ERROR: Could not read preferences file");
	    done = false;
	    file.close ();
	}
	
	show_toolbar = file.readBoolean();
	show_line_num = file.readBoolean();
	beep_warning = file.readBoolean();
	make_backup = file.readBoolean();
	append_newline = file.readBoolean();
	convert_dos = file.readBoolean();
	quote_string = (String)file.readObject();
	comment_start_string = (String)file.readObject();
	comment_end_string = (String)file.readObject();
		
	file.close ();
	done = true;
    } catch(Exception e) {
	done = false; 			// pref file does not exist
    }
	    
    if (!done)		//  -> use defaults
    {
	show_toolbar = true;
	show_line_num = false;
	beep_warning = true;
	make_backup = true;
	append_newline = true;
	convert_dos = true;
	quote_string = ">";
	comment_start_string = "//";
	comment_end_string = "";
    }

    if (!version.equals(RedVersion.versionString())) // pref version is old...
	write_prefs ();
  }

  // ------------------------------------------------------------------------
  /**
   ** Write the preferences file.
   **/

  public void write_prefs ()
  {
    String filename = red_prefs_file;

    try {
	FileOutputStream fos = new FileOutputStream(filename);
	ObjectOutputStream file = new ObjectOutputStream(fos);
	file.writeObject(RedVersion.versionString());
	file.writeBoolean(show_toolbar);
	file.writeBoolean(show_line_num);
	file.writeBoolean(beep_warning);
	file.writeBoolean(make_backup);
	file.writeBoolean(append_newline);
	file.writeBoolean(convert_dos);
	file.writeObject(quote_string);
	file.writeObject(comment_start_string);
	file.writeObject(comment_end_string);
	file.close ();
    } catch(Exception e) {
	String msg = "ERROR: Could not write preferences file";
	// new QuestionDialog(getFrame(), "Error", msg, "OK");
	System.err.println("ERROR: Could not write preferences file");
    }
  }
  

} // end class RedEditor
