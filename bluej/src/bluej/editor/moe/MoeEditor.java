package bluej.editor.moe;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.editor.EditorWatcher;

import java.util.Hashtable;
import java.util.Vector;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.Date;
import java.text.DateFormat;
import java.io.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model    
import javax.swing.*;		// all the GUI components
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
 ** @author Michael Kolling
 **
 **/

public final class MoeEditor extends JFrame

	implements bluej.editor.Editor
{
    // -------- CONSTANTS --------

    // version number
    static final int version = 020;
    static final String versionString = "0.2";

    // colours
    static final Color textColor = new Color(0,0,0);		// normal text
    static final Color textBgColor = new Color(255,255,255);	// background
    static final Color selectionColor = Color.pink;		// selection
    static final Color cursorColor = new Color(255,0,100);	// cursor

    static final Color frameBgColor = new Color(196, 196, 196);
    static final Color infoColor = new Color(240, 240, 240);
    static final Color lightGrey = new Color(224, 224, 224);
    static final Color titleCol = Config.getItemColour("colour.text.fg");

    // Fonts
    public static Font editFont = new Font("Monospaced", Font.PLAIN, 
					   Config.editFontsize);
    public static Font printFont = new Font("Monospaced", Font.PLAIN, 
					   Config.printFontsize);

    // suffixes for resources
    static final String LabelSuffix = "Label";
    static final String ActionSuffix = "Action";
    static final String TooltipSuffix = "Tooltip";
    static final String AcceleratorSuffix = "Accelerator";

    //  width of tag area for setting breakpoints
    static final short TAG_WIDTH = 14;

    // Attributes for lines
    static final String BreakPoint = "break";

    // -------- INSTANCE VARIABLES --------

    private MoeEditor editor;		// the object itself (this)
    private EditorWatcher watcher;
    private ResourceBundle resources;
    private DefaultStyledDocument document;
    private Hashtable actions;		// user actions

    private JTextPane textPane;
    private Info info;			// the info number label
    private JPanel statusArea;		// the status area
    private LineNumberLabel lineCounter;	// the line number label
    private StatusLabel saveState;	// the status label

    private JComponent toolbar;		// The toolbar
    private JComboBox viewSelector;	// The view choice selector

    private String filename;              // name of file or null
    private String windowTitle;		// title of editor window
    private boolean firstSave;           // true if never been saved
    private boolean isCompiled;		// true when source has been compiled

    private String newline;		// the line break character used
    private boolean isCode;		// true if current buffer is code

    // undo helpers
    private UndoAction undoAction;
    private RedoAction redoAction;
    private UndoManager undoManager;

    // =========================== NESTED CLASSES ===========================

    // inner class for listening for undoable edits in text

    private class MoeUndoableEditListener implements UndoableEditListener {
  
	public void undoableEditHappened(UndoableEditEvent e)
	{
	    undoManager.addEdit(e.getEdit());
	    undoAction.update();
	    redoAction.update();
	}
    }

    /** 
     *  Inner class listening for disabling actions - if an action is disabled 
     *  (enabled), the connected button is disabled (enabled) as well.
     */
    private class ActionChangeListener implements PropertyChangeListener {
	JButton button;

	ActionChangeListener(JButton b) {
	    super();
	    button = b;
	}

	public void propertyChange(PropertyChangeEvent e)
	{
	    if (e.getPropertyName().equals("enabled")) {
		Boolean enabledState = (Boolean) e.getNewValue();
		button.setEnabled(enabledState.booleanValue());
	    }
	}
    }

    // =========================== PUBLIC METHODS ===========================

    /**
     * Constructor. Title may be null 
     */

    public MoeEditor(String title, boolean isCode, EditorWatcher watcher, 
		     boolean showToolbar, boolean showLineNum, 
		     ResourceBundle resources)
    {
	super("Moe");

	editor = this;
	this.watcher = watcher;
	this.resources = resources;
	filename = null;
	windowTitle = title;
	firstSave = true;
	isCompiled = false;
	newline = System.getProperty("line.separator");
	this.isCode = isCode;
	undoManager = new UndoManager();

	initWindow(showToolbar, showLineNum);
    }

    // --------------------------------------------------------------------
    /**
     * Load the file "fname" and show the editor window.
     */

    public boolean showFile(String filename, boolean compiled,
			    Vector breakpoints)
			    // inherited from Editor, redefined
    {
	this.filename = filename;

	boolean loaded = false;
	boolean readError = false;
 
	if (filename != null) {

	    try {
		FileReader reader = new FileReader(filename);
		textPane.read(reader, null);
		document = (DefaultStyledDocument)textPane.getDocument();
		document.addDocumentListener(saveState);
		document.addUndoableEditListener(new MoeUndoableEditListener());
		loaded = true;
	    }
	    catch (FileNotFoundException ex) {
		clear();
	    }
	    catch (IOException ex) {
		readError = true;
	    }
	}

	if (! (MoeEditorManager.standAlone || loaded))  // should exist, but didn't
	    return false;

	//     if (loaded) ## NYI
	//       if (newFile.canWrite()) {		// have write permission
	// 	  save_state = Saved;
	// 	  statusLabel.setText("saved");
	//       }
	//       else {
	// 	  save_state = ReadOnly;
	// 	  statusLabel.setText("read only");
	//       }
	//     else
	//       save_state = Saved;

	if (loaded)
	    info.message ("Moe version " + versionString);
	else if (readError)
	    info.warning ("There was a problem reading this file.",
			  "(Is it really a regular file? Do you have read access?)");
	else
	    info.message ("Moe version " + versionString, "New file");

	setWindowTitle();
	show();
	textPane.setFont(editFont);

	if (! MoeEditorManager.standAlone)
	    {
		setCompileStatus(compiled);
		if (!isCode) {
		    getActionByName("compile").setEnabled(false);
		}
	    }
	return true;

    } // showFile

    // --------------------------------------------------------------------

    public void reloadFile() // inherited from Editor, redefined
    {
	doReload();
	textPane.setFont(editFont);
    }

    // --------------------------------------------------------------------
    /**
     * Wipe out contents of the editor.  
     */

    public void clear()	// inherited from Editor, redefined
    {
	textPane.setText("");
    }

    // --------------------------------------------------------------------
    /**
     * Insert a string into the buffer. The editor is not immediately
     * redisplayed. This function is typically used in a sequence
     * "clear; [insertText]*; setVisible(true)". If the selection is on,
     * it is replaced by the new text.
     *
     * @arg text         the text to be inserted
     * @arg style         the style in which the text is to be displayed
     */

    public void insertText(String text, boolean bold, boolean italic)
    // inherited from Editor, redefined
    {
	MoeEditorKit kit = (MoeEditorKit)textPane.getEditorKit();

	MutableAttributeSet attr = kit.getInputAttributes();
	StyleConstants.setBold(attr, bold);
	StyleConstants.setItalic(attr, italic);

	//attr.addAttributes(attr);

	textPane.replaceSelection(text);
    }

    // --------------------------------------------------------------------
    /**
     * Show the editor window. This includes whatever is necessary of the
     * following: make visible, de-iconify, bring to front of window stack.
     *
     * @arg view		the view to be displayed. Must be one of the 
     *			view constants defined above
     */
    public void show(int view)	// inherited from Editor, redefined
    {
	setView(view);
	setVisible(true);		// show the window
	textPane.setFont(editFont);
	//  ## NYI: de-iconify, bring to front
    }


    // --------------------------------------------------------------------
    /**
     * Save the buffer to disk under current filename.  This is often called
     * from the outside - just in case.  Save only if really necessary, 
     * otherwise we save much too often.
     * PRE: filename != null
     */

    public void save() // inherited from Editor
    {
	if (saveState.isChanged()) {

	    Debug.assert(filename != null);

	    // missing: ## NYI
	    // check for first save -> make backup

	    try {
		FileWriter writer = new FileWriter(filename);
		textPane.write(writer);
		setSaved();
	    }
	    catch (IOException ex) {
		info.warning ("Error in saving file!");
	    }
	}
    }
    //       if (filename != null) {		// if it has a file name...
    // 	  if (first_save && RedEditorManager.red.backup_on()) {
    // 	    fname = filename.concat(".~");
    //   ////	  if (!filecopy (filename, fname))
    //   ////	    ; // cannot display warning message here - will be overwritten
    // 	    first_save = false;
    // 	  }

    // --------------------------------------------------------------------
    /**
     * The editor wants to close. Do this through the EditorManager so that
     * we can be removed from the list of open editors.
     */

    public void close()	// inherited from Editor
    {
	if (MoeEditorManager.standAlone && (saveState.isChanged())) {
	    int answer = Utility.askQuestion(this, "The text has been changed.\nSave changes?",
					     "Save", "Don't save", "Cancel");
	    if (answer == 0)	// first choice: save
		save();
	    else if (answer != 1)	// could be cancel or window close
		return;
	}
	else if (! MoeEditorManager.standAlone) {
	    save(); // temporary - should really be done by watcher from outside
	}
	doClose();
    }

    // --------------------------------------------------------------------
    /**
     * Display a message (used for compile/runtime errors). An editor
     * must support at least two lines of message text, so the message
     * can contain a newline character.
     *
     * @arg message	the message to be displayed
     * @arg line		the line to move the cursor to (the line is 
     *			also highlighted)
     * @arg column		the column to move the cursor to
     * @arg beep		if true, do a system beep
     * @arg setStepMark	if true, set step mark (for single stepping)
     */

    public void displayMessage(String message, int lineNumber, int column, 
			       boolean beep, boolean setStepMark)
			       // inherited from Editor
    {
	Element line = getLine (lineNumber);
	textPane.select(line.getStartOffset(), line.getEndOffset());

	// display the message

	if (beep)
	    info.warning (message);
	else
	    info.message (message);
    }

    // --------------------------------------------------------------------
    /**
     * Remove the step mark (the mark that shows the current line when
     * single-stepping through code). If it is not currently displayed,
     * do nothing.
     */

    public void removeStepMark()		// inherited from Editor
    {
	// ## NYI
    }

    // --------------------------------------------------------------------
    /**
     * Change class name.
     *
     * @arg title	new window title
     * @arg filename	new file name
     */
    public void changeName (String title, String filename)
    // inherited from Editor
    {
	this.filename = filename;		// error ## - need to add full path
	windowTitle = title;
	setWindowTitle();
    }

    // --------------------------------------------------------------------
    /**
     * Set the "compiled" status
     *
     * @arg compiled	true if the class has been compiled
     */
    public void setCompiled (boolean compiled)
    {
	setCompileStatus(compiled);
	if (compiled)
	    info.message("Class compiled - no syntax errors");
    }

    // --------------------------------------------------------------------
    /**
     * Determine whether this buffer has been modified.
     * @returns	a boolean indicating whether the file is modified
     */

    public boolean isModified()	// inherited from Editor, redefined
    {
	return (saveState.isChanged());
    }

    // --------------------------------------------------------------------
    // ------------ end of interface inherited from Editor ----------------
    // --------------------------------------------------------------------
    /**
     * The editor has been closed. Hide the editor window now. Never call this 
     * function
     */

    public void doClose()
    {
	setVisible(false);
	MoeEditorManager.editorManager.removeEditor(this);
	if (watcher != null)
	    watcher.closeEvent(this);
    }

    //   // --------------------------------------------------------------------
    //   /**
    //    ** sets the editors preferred size
    //    **/
    // 
    //   public Dimension getPreferredSize()
    //   {
    //     return new Dimension(width, height);
    //   }
    // 
    //   // --------------------------------------------------------------------
    //   /**
    //    ** FUNCTION: do_save_as(String)
    //    ** Save this buffer under a new file name. The new name is 
    //    ** passed as parameter.  (This is called by "save_as").
    //    **/
    // 
    //   public void do_save_as (String fname)
    //   {
    //     if (buffer.save (fname, RedEditorManager.red.append_nl_on())) {
    //       filename = new String(fname);
    //       set_title ();
    //       set_saved (true);
    //     }
    //     else
    //       info.warning ("Could not save file!",
    // 	     "The reason could be: invalid file name, or file exists" +
    //  	     " and is write protected");
    //   }
    // 
    // 
    //   // --------------------------------------------------------------------
    //   /**
    //    ** Show or hide the toolbar (depending on the parameter 'show').
    //    **/
    // 
    //   public void show_toolbar(boolean show)
    //   {
    //     if(show)
    //     {
    //       mainPanel.add("North", toolbar);
    //       frame.validate();
    //     }
    //     else
    //     {   
    //       mainPanel.remove(toolbar);
    //       frame.validate();
    //     }
    //   }
    // 
    // ===============================================
    // 
    //   // --------------------------------------------------------------------
    //   /**
    //    ** FUNCTION: do_replace_all(String, int)
    //    ** replace all
    //    **/
    // 
    //   private void do_replace_all (String pattern, int direction, 
    // 			  boolean case_sens, boolean whole_word, 
    // 			  String  rep_pattern)
    //   {
    //     BufferPos start;
    //     int cnt = 0;
    // 
    //     if (pattern.length()==0) {
    //       info.message ("Empty search string", "", "");
    //       return;
    //     }
    // 
    //     if (check_selection (false, (direction==backwd)))	// switch sel. off
    //       ;
    //     start = buffer.get_point ();
    // 
    //     while (buffer.find (pattern, case_sens, whole_word,
    // 			  (direction==backwd), false)) {
    //       delete_between_points ();
    //       insert (rep_pattern);
    //       cnt++;
    //     }
    //     if (buffer.pt.is_before (start)) {
    //       buffer.point_to (start);
    //       display ();
    //     }
    //     else {
    //       buffer.point_to (start);
    //       screen_update_from (start);
    //     }
    //     info.int_message (cnt, " instances replaced", "");
    //   }
    // 
    //   // --------------------------------------------------------------------
    //   /**
    //    ** FUNCTION: do_replace(String)
    //    ** Internal replace routine used by the replace (indirectly 
    //    ** over finder). Replaces the current selection with "pattern".
    //    ** If there is no current selection, just return false.
    //    **/
    // 
    //   private void do_replace (String pattern)
    //   {
    //     BufferPos start;
    // 
    //     if (!check_selection (true, true))		// if selection, cut it
    //       return;
    // 
    //     start = buffer.get_point ();
    //     insert (pattern);
    //     screen_update_from (start);
    //   }
    // 
    // 
    //   // --------------------------------------------------------------------
    //   /**
    //    ** FUNCTION: insert_file()
    //    ** display a file dialgo to prompt the user for a file 
    //    ** to insert then check that file exists if so 
    //    ** then insert the file
    //    **/
    // 
    //   private void insert_file ()
    //   {
    //     // Create a file dialog to query the user for a filename
    //     FileDialog fd = new FileDialog(frame,"Insert File",FileDialog.LOAD);
    //     fd.show();                            // Display the dialog and block
    //     String insertFile = fd.getFile(); 	// Get users response
    //     if(insertFile != null)
    //     {
    //       Action action;
    //       BufferPos start;
    // 
    //       try {
    // 	// See if the requested file exists
    // 	FileInputStream file = new FileInputStream(insertFile);
    // 
    // 	start = buffer.get_point ();
    // 	if (RedEditorManager.red.text_quote_string() != null) 
    // 	  buffer.insert_file (file, RedEditorManager.red.text_quote_string(), 
    // 			       RedEditorManager.red.convert_dos_on());
    // 	else
    // 	  buffer.insert_file (file, null, RedEditorManager.red.convert_dos_on());
    //       
    // 	file.close();             // Close the FileInputStream
    // 
    // 	// store information about this on undo-stack
    // 	action = undo_stack.new_action ();		// get action record
    // 	action.set_insertion (start.line_no, start.column, 
    // 			       buffer.pt.line_no, buffer.pt.column, false);
    // 
    // 	screen_update_from (start);
    // 	show_line_number (false);
    // 	set_saved (false);
    //       }
    //       catch(IOException e)
    //       {
    // 	info.warning ("Could not open file.", filename);
    //       }
    //     }
    //   }
    // 
    //   // --------------------------------------------------------------------
    //   /**
    //    ** FUNCTION: goto_line()
    //    ** Interactive. Ask for line number and set cursor to that line.
    //    ** If the number is greater than last line, goto last line.
    //    **/
    // 
    //   private void goto_line ()
    //   {
    //     int line_no;
    // 
    //     if (param_prompt != null) {		// got parameter - analyse and do it
    //       param_prompt = null;
    //       try {
    //           line_no = Integer.parseInt (parameter);
    //       }
    //       catch (NumberFormatException e) {
    // 	  RedEditorManager.red.beep ();
    // 	  return;
    //       }
    //       if (check_selection (false, true))	// make sure selection is off
    // 	;
    //       buffer.point_to_bob ();
    //       buffer.goto_pos (line_no, 0);
    //       show_point ();
    //       show_line ();
    //       show_line_number (false);
    //     }
    //     else {				// prompt for parameter
    //       param_prompt = "Goto line: ";
    //       parameter = "";
    //       info.message (param_prompt, "", "");
    //     }
    //   }
    // 

    // ============================ USER ACTIONS =============================

    abstract class MoeAbstractAction extends AbstractAction {

	private KeyStroke key;

	public MoeAbstractAction(String name, KeyStroke keyStroke) {
	    super(name);
	    key = keyStroke;
	}

	final public KeyStroke getKey() {
	    return key;
	}
    }

    // === File: ===
    // --------------------------------------------------------------------

    class NewAction extends MoeAbstractAction {

	public NewAction() {
	    super("new-file", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class OpenAction extends MoeAbstractAction {

	public OpenAction() {
	    super("open-file", KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class SaveAction extends MoeAbstractAction {

	public SaveAction() {
	    super("save", KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e) {

	    if (saveState.isSaved())  // && (last_func != UserFuncID.UFSave)) ##
		info.message ("No changes need to be saved");
	    else {

		if (filename == null) {
		    // MoeEditorManager.saveAsRequest...
		    Utility.NYI(editor);
		}
		else
		    save();
	    }
	}
    }

    // --------------------------------------------------------------------

    class SaveAsAction extends MoeAbstractAction {

	public SaveAsAction() {
	    super("save-as", null);
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    /**
     * Reload has been chosen. Ask "Really?" and call "doReload" if the answer
     * is yes.
     */
    class ReloadAction extends MoeAbstractAction {

	public ReloadAction() {
	    super("reload", null);
	}

	public void actionPerformed(ActionEvent e) {
	    if (filename == null) {
		info.warning ("Can not reload - this text was never saved!",
			      "(\"Reload\" reloads the last saved state from disk.)");
	    }
	    else if (saveState.isChanged()) {
		int answer = Utility.askQuestion(editor,
						 "Reload discards all changes since the last edit.\nAre you sure?",
						 "Reload", "Cancel", null);
		if (answer == 0)
		    doReload();
	    }
	    else
		doReload();
	}
    }

    // --------------------------------------------------------------------

    class PrintAction extends MoeAbstractAction {

	public PrintAction() {
	    super("print", KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    print();
	}
    }

    // --------------------------------------------------------------------

    class CloseAction extends MoeAbstractAction {

	public CloseAction() {
	    super("close", KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    close();
	}
    }

    // === Edit: ===
    // --------------------------------------------------------------------

    class UndoAction extends MoeAbstractAction {

	public UndoAction() 
	{
	    super("undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.ALT_MASK));
	    this.setEnabled(false);
	}
    
	public void actionPerformed(ActionEvent e) 
	{
	    try {
		undoManager.undo();
	    }
	    catch (CannotUndoException ex) {
		Debug.message("moe: cannot undo...");
	    }
	    update();
	    redoAction.update();
	}

	private void update()
	{
	    if (undoManager.canUndo()) {
		this.setEnabled(true);
		putValue(Action.NAME, undoManager.getUndoPresentationName());
	    }
	    else {
		this.setEnabled(false);
		putValue(Action.NAME, "Undo");
	    }
	}
    }

    // --------------------------------------------------------------------

    class RedoAction extends MoeAbstractAction {

	public RedoAction() 
	{
	    super("redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.ALT_MASK));
	    this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) 
	{
	    try {
		undoManager.redo();
	    }
	    catch (CannotRedoException ex) {
		Debug.message("moe: cannot redo...");
	    }
	    update();
	    undoAction.update();
	}

	private void update()
	{
	    if (undoManager.canRedo()) {
		this.setEnabled(true);
		putValue(Action.NAME, undoManager.getRedoPresentationName());
	    }
	    else {
		this.setEnabled(false);
		putValue(Action.NAME, "Redo");
	    }
	}
    }

    // --------------------------------------------------------------------

    class CommentAction extends MoeAbstractAction {

	public CommentAction() {
	    super("comment", KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class UncommentAction extends MoeAbstractAction {

	public UncommentAction() {
	    super("uncomment", KeyStroke.getKeyStroke(KeyEvent.VK_U, Event.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class InsertMethodAction extends MoeAbstractAction {

	public InsertMethodAction() {
	    super("insert-method", KeyStroke.getKeyStroke(KeyEvent.VK_M, Event.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e) {
	    int pos = textPane.getCaretPosition();
	    textPane.replaceSelection("    /**\n" +
				      "     ** An example of a method - replace this comment with your own\n" +
				      "     ** \n" +
				      "     ** @param  y   a sample parameter for a method \n" + 
				      "     ** @return     the sum of x and y \n" +
				      "     **/ \n" + 
				      "    public int sampleMethod(int y)\n" +
				      "    { \n" +
				      "        // put your code here" + 
				      "        return x + y\n;" +
				      "    }");
	    textPane.setCaretPosition(pos);
	}
    }

    // === Tools: ===
    // --------------------------------------------------------------------

    /**
     * Change the current view. This function reads the current setting of the
     * view selector and displays the selected view.
     */
    class SelectViewAction extends MoeAbstractAction {

	public SelectViewAction() {
	    super("select-view", null);
	}
 
	public void actionPerformed(ActionEvent e) {
	    int view;
    
	    switch (viewSelector.getSelectedIndex()) {
	    case (0): view = bluej.editor.Editor.IMPLEMENTATION;
		break;
	    case (1): view = bluej.editor.Editor.PUBLIC;
		break;
	    case (2): view = bluej.editor.Editor.PACKAGE;
		break;
	    case (3): view = bluej.editor.Editor.INHERITED;
		break;
	    default:  view = 0;
	    }
	    watcher.changeView(editor, view);
	}
    }

    // --------------------------------------------------------------------

    class FindAction extends MoeAbstractAction {
 
	public FindAction() {
	    super("find", KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Finder finder = MoeEditorManager.editorManager.getFinder();
	    String s = finder.getNewSearchString(editor);
	    if(s!=null)
		find(s, finder);
	}
    }

    // --------------------------------------------------------------------

    class FindBackwardAction extends MoeAbstractAction {

	public FindBackwardAction() {
	    super("find-backward", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class FindNextAction extends MoeAbstractAction {

	public FindNextAction() {
	    super("find-next", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Finder finder = MoeEditorManager.editorManager.getFinder();
	    String s = textPane.getSelectedText();
	    if (s == null) {
		s = finder.getLastSearchString();
		if (s == null) {
		    info.warning("No search string is defined.",
				 "(You never searched for anything here before.)");
		    return;
		}
	    }
	    find(s, finder);
	}
    }

    // --------------------------------------------------------------------

    class FindNextReverseAction extends MoeAbstractAction {
 
	public FindNextReverseAction() {
	    super("find-next-reverse", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class ReplaceAction extends MoeAbstractAction {

	public ReplaceAction() {
	    super("replace", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class GotoLineAction extends MoeAbstractAction {

	public GotoLineAction() {
	    super("goto-line", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Element line = getLine (2);
	    textPane.setCaretPosition(line.getStartOffset());
	}
    }

    // --------------------------------------------------------------------

    class CompileAction extends MoeAbstractAction {

	public CompileAction() {
	    super("compile", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    if (watcher == null)
		return;

	    watcher.compile(editor);
	    info.message ("Compiling...");
	}
    }
    // === Debug: ===
    // --------------------------------------------------------------------

    class SetBreakPointAction extends MoeAbstractAction {

	public SetBreakPointAction() {
	    super("set-breakpoint", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    setUnsetBreakpoint(textPane.getCaretPosition(), true);
	    info.message ("Breakpoint set");
	}
    }

    // --------------------------------------------------------------------

    class ClearBreakPointAction extends MoeAbstractAction {
 
	public ClearBreakPointAction() {
	    super("clear-breakpoint", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    setUnsetBreakpoint(textPane.getCaretPosition(), false);
	    info.message ("Breakpoint cleared");
	}
    }

    // --------------------------------------------------------------------

    class StepAction extends MoeAbstractAction {

	public StepAction() {
	    super("step", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class StepIntoAction extends MoeAbstractAction {
 
	public StepIntoAction() {
	    super("step-into", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class ContinueAction extends MoeAbstractAction {

	public ContinueAction() {
	    super("continue", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class TerminateAction extends MoeAbstractAction {

	public TerminateAction() {
	    super("terminate", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // === Options: ===
    // --------------------------------------------------------------------

    class PreferencesAction extends MoeAbstractAction {
 
	public PreferencesAction() {
	    super("preferences", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class KeyBindingsAction extends MoeAbstractAction {
 
	public KeyBindingsAction() {
	    super("key-bindings", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // === Help: ===
    // --------------------------------------------------------------------

    class AboutAction extends MoeAbstractAction {

	public AboutAction() {
	    super("help-about", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    JOptionPane.showMessageDialog(editor,
		new String[] { 
		    "Moe",
		    "",
		    "Version " + versionString,
		    "",
		    "Moe is the editor of the BlueJ programming environment.",
		    "Written by Michael K\u00F6lling (mik@csse.monash.edu.au)."
		    },
		"About Moe", JOptionPane.INFORMATION_MESSAGE);
	}
    }

    // --------------------------------------------------------------------

    class CopyrightAction extends MoeAbstractAction {
 
	public CopyrightAction() {
	    super("help-copyright", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class DescribeKeyAction extends MoeAbstractAction {

	public DescribeKeyAction() {
	    super("help-describe-key", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class HelpMouseAction extends MoeAbstractAction {

	public HelpMouseAction() {
	    super("help-mouse", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class ShowManualAction extends MoeAbstractAction {

	public ShowManualAction() {
	    super("help-show-manual", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class ReportErrorAction extends MoeAbstractAction {

	public ReportErrorAction() {
	    super("report-errors", KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.ALT_MASK));
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    class EmptyAction extends MoeAbstractAction {

	public EmptyAction() {
	    super("nothing", null);
	}
 
	public void actionPerformed(ActionEvent e) {
	    Utility.NYI(editor);
	}
    }

    // --------------------------------------------------------------------

    //     class Action extends MoeAbstractAction {
    //  
    //       public Action() {
    //  	 super("");
    //       }
    //  
    //       public void actionPerformed(ActionEvent e) {
    // 	  Utility.NYI(editor);
    //       }
    //     }


    // ========================= SUPPORT ROUTINES ==========================

    private void setCompileStatus(boolean compiled)
    {
	viewSelector.setEnabled(compiled);
	isCompiled = compiled;
	/*
	  utility->set_sensitive (break_item, compiled);
	  utility->set_sensitive (clr_break_item, compiled);
	  utility->set_sensitive (interface_item, compiled);
	  utility->set_sensitive (interface_button, compiled);
	  screen->set_tag_grey (!compiled);
	  if (showing_interface && !compiled)
	  interface_toggle ();
	*/
    }

    // --------------------------------------------------------------------
    /**
     * Set the saved/changed status of this buffer to SAVED.
     */
    private void setSaved()
    {
	info.message ("File saved");
	saveState.setState (StatusLabel.SAVED);
	if(watcher != null)
	    watcher.saveEvent(this);
    }

    // --------------------------------------------------------------------

    private void setWindowTitle()
    {
	String title = windowTitle;

	if (title == null) {
	    if (filename == null)
		title = "Moe: <no name>";
	    else
		title = "Moe: " + filename;
	}
	setTitle(title);
    }

    // --------------------------------------------------------------------
    /**
     * Buffer just went from saved to changed state (called by StatusLabel)
     */
    void setChanged()
    {
	if (! MoeEditorManager.standAlone) {
	    setCompileStatus (false);
	    if(watcher != null)
		watcher.modificationEvent(this);
	}
    }

    // --------------------------------------------------------------------
    /**
     * Sets the editor to contain a view. This is used if the view is set from
     * the outside of the editor (not by the editor function).
     *
     * @arg view	    the new view. Must be one of the defined view constants.
     */
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

    // --------------------------------------------------------------------
    /**
     * Show or hide the line number display (depending on the parameter
     * 'show').
     */

    private void showLineCounter(boolean show)
    {
	if (show)
	    statusArea.add(lineCounter);
	else
	    statusArea.remove(lineCounter);
	validate();
    }

    // --------------------------------------------------------------------
    /**
     * Implementation if the "print" user function
     * (NOTE: re-implement under jdk 1.2 with "PrinterJob"!)
     */
    private void print()
    {
	PrintJob printjob = getToolkit().getPrintJob(this, 
				"Class " + windowTitle,
				null);
	if(printjob != null) {
	    printClass(printjob);
	    printjob.end();
	}
    }

    // --------------------------------------------------------------------
    /**
     * Part of the print function. Print out the class view currently
     * visible in the editor.
     */
    private void printClass(PrintJob printjob) 
    {
  	Dimension pageSize = printjob.getPageDimension();

        Font oldFont = textPane.getFont();
        textPane.setFont(printFont);

	Rectangle printArea = getPrintArea(pageSize);
	Dimension textSize = textPane.getSize(null);
	textSize.width -= (TAG_WIDTH + 3);
	int pages = (textSize.height + printArea.height - 1) / 
			printArea.height;

	int answer;
	if(printArea.width < textSize.width-8) {
	    answer = Utility.askQuestion(editor,
			"The text is wider than the paper. Long lines\n" +
			"will be cut off. You can avoid this by resizing\n" +
			"the editor window to make it narrower. Do you\n" +
			"want to print anyway?",
			"Print", "Cancel", null);
	}
	else
	    answer = 0;

	if(answer == 0) {
	    String date = DateFormat.getDateInstance().format(new Date());

	    for(int i = 0; i < pages; i++) {
		Graphics g = printjob.getGraphics();
		printFrame(g, printArea, i + 1, date);
		
		g.translate(printArea.x - TAG_WIDTH - 2, 
			    printArea.y - i * printArea.height);
		g.setClip(TAG_WIDTH + 3, i * printArea.height, 
			  printArea.width, printArea.height);
		textPane.print(g);
		g.dispose();
	    }
	}
        textPane.setFont(oldFont);
    }

    // --------------------------------------------------------------------
    static final int PRINT_HMARGIN = 16;
    static final int PRINT_VMARGIN = 16;
    static final Font printTitleFont = new Font("SansSerif", Font.PLAIN, 
						Config.printTitleFontsize);
    static final Font printInfoFont = new Font("SansSerif", Font.ITALIC, 
					       Config.printInfoFontsize);

    // --------------------------------------------------------------------
    /**
     * Return the rectangle on the page in which to print the text.
     * The rectangle is the page minus margins minus space for header and
     * footer text.
     */
    private Rectangle getPrintArea(Dimension pageSize)
    {
	FontMetrics tfm = getFontMetrics(printTitleFont);
	FontMetrics ifm = getFontMetrics(printInfoFont);
	int fontSize = textPane.getFont().getSize();

	int printHeight = pageSize.height - 2 * PRINT_VMARGIN - 
			  tfm.getHeight() - ifm.getHeight() - 4;

	// ensure printHeight is multiple of font size
	printHeight = (printHeight / fontSize) * fontSize;

	return new Rectangle(PRINT_HMARGIN,
			     PRINT_VMARGIN + tfm.getHeight() + 4,
			     pageSize.width - 2 * PRINT_HMARGIN,
			     printHeight);
    }

    // --------------------------------------------------------------------
    /**
     * printFrame - part of the print function. Print the frame around the
     * page, including header and footer.
     */
    private void printFrame(Graphics g, Rectangle printArea, int pageNum,
			    String date) 
    {
	FontMetrics tfm = getFontMetrics(printTitleFont);
	FontMetrics ifm = getFontMetrics(printInfoFont);
	Rectangle frameArea = new Rectangle(printArea);
	frameArea.grow(1, 1);

	// frame header area
	g.setColor(lightGrey);
	g.fillRect(frameArea.x, PRINT_VMARGIN, frameArea.width, 
		   frameArea.y - PRINT_VMARGIN);

	g.setColor(titleCol);
	g.drawRect(frameArea.x, PRINT_VMARGIN, frameArea.width, 
		   frameArea.y - PRINT_VMARGIN);

	// frame print area
	g.drawRect(frameArea.x, frameArea.y, frameArea.width, 
		   frameArea.height);

	// write header
	g.setFont(printTitleFont);
	String title = "Class " + windowTitle;
	if(pageNum > 1)
	    title = title + " (continued)";
	Utility.drawCentredText(g, title, printArea.x, PRINT_VMARGIN, 
				printArea.width, tfm.getHeight()+4);

	// write footer
	g.setFont(printInfoFont);
	Utility.drawRightText(g, "printed: " + date + ",   page " + pageNum,
			      printArea.x, printArea.y + printArea.height,
			      printArea.width, ifm.getHeight()+4);
    }

    // --------------------------------------------------------------------
    /**
     * Find. Does a find with info in the info area.
     */

    private void find(String s, Finder finder)
    {
	if (s.length()==0) {
	    info.warning("Empty search string.");
	    return;
	}
	String msg;
	boolean wrap = ! finder.lastSearchFound();
	if (wrap)
	    msg = "Find forward (wrap around): " + s;
	else
	    msg = "Find forward: " + s;
	info.message(msg);
	boolean found = doFind(s, wrap);
	finder.setSearchString(s);
	finder.setSearchFound(found);
	if (! found)
	    info.warning(msg, "Not found");
    }

    // --------------------------------------------------------------------
    /**
     * doFind - do a find without visible feedback.
     */

    private boolean doFind(String s, boolean wrap)
    {
	int docLength = document.getLength();
	int startPosition = textPane.getCaretPosition();
	int endPos = docLength;

	boolean found = false;
	boolean finished = false;

	int start = startPosition;
	Element line = document.getParagraphElement(start);
	int lineEnd = Math.min(line.getEndOffset(), endPos);

	try {
	    while (!found && !finished) {
		String lineText = document.getText(start, lineEnd - start);
		if(lineText != null && lineText.length() > 0) {
		    int foundPos = lineText.indexOf(s);
		    if (foundPos != -1) {
			textPane.select(start+foundPos, start+foundPos+s.length());
			found = true;
		    }
		}
		if (lineEnd >= endPos) {
		    if (wrap) {   // do the wrapping
			endPos = startPosition;
			line = document.getParagraphElement(0);
			start = line.getStartOffset();
			lineEnd = Math.min(line.getEndOffset(), endPos);
			wrap = false;  // don't wrap again
		    }
		    else
			finished = true;
		}
		else {
		    line = document.getParagraphElement(lineEnd+1);
		    start = line.getStartOffset();
		    lineEnd = Math.min(line.getEndOffset(), endPos);
		}
	    }
	}
	catch (BadLocationException ex) {
	    Debug.message("error in editor find op");
	}
	return found;
    }

    // --------------------------------------------------------------------
    /**
     * Find and return a line in the document
     */

    private Element getLine (int lineNo)
    {
	return document.getDefaultRootElement().getElement(lineNo-1);
    }

    // --------------------------------------------------------------------
    /**
     * Return the number of the current line.
     */

    private int getCurrentLineNo ()
    {
	return document.getDefaultRootElement().getElementIndex(textPane.getCaretPosition()) + 1;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the number of the line containing position 'pos'.
     */

    private int getLineAt (int pos)
    {
	return document.getDefaultRootElement().getElementIndex(pos) + 1;
    }

    // --------------------------------------------------------------------
    /**
     * Toggle a breakpoint at a given position.
     */

    public void toggleBreakpoint(int pos)
    {
	if (hasBreakpoint(pos))
	    setUnsetBreakpoint(pos, false);  // remove
	else
	    setUnsetBreakpoint(pos, true);  // set
    }

    // --------------------------------------------------------------------
    /**
     * Check weather a position has a breakpoint set
     */

    private boolean hasBreakpoint(int pos)
    {
	Element paragraph = document.getParagraphElement(pos);
	return (Boolean.TRUE.equals(paragraph.getAttributes().getAttribute(MoeEditor.BreakPoint)));
    }

    // --------------------------------------------------------------------
    /**
     * Set or remove a breakpoint (depending on the parameter) at the given
     * position.
     */

    private void setUnsetBreakpoint(int pos, boolean set)
    {
	if (watcher != null) {
	    String result = watcher.breakpointToggleEvent(editor, 
							  getLineAt(pos), set);
	    if(result == null || result.length() == 0) {
		// no problem, go ahead
		SimpleAttributeSet a = new SimpleAttributeSet();
		a.addAttribute(BreakPoint, new Boolean(set));
		document.setParagraphAttributes(pos, 0, a, false);
	    }
	    else {
		if(result.startsWith("No code"))
		    info.warning("Cannot set breakpoint: no code in this line");
		else
		    info.warning("Cannot set breakpoint:", result);
	    }
	}
    }

    // --------------------------------------------------------------------
    /**
     * Revert the buffer contents to the last saved version.  Do
     * not ask any question - just do it. Must have a file name.
     */

    public void doReload()
    {
	Debug.assert (filename != null);

	try {
	    FileReader reader = new FileReader(filename);
	    textPane.read(reader, null);
	    document = (DefaultStyledDocument)textPane.getDocument();
	    document.addDocumentListener(saveState);
	    document.addUndoableEditListener(new MoeUndoableEditListener());
	}
	catch (FileNotFoundException ex) {
	    info.warning ("ERROR: The file seems to have disappeared!");
	}
	catch (IOException ex) {
	    info.warning ("ERROR: There was an error while trying to read this file");
	}
	setView(bluej.editor.Editor.IMPLEMENTATION);
	setSaved();
    }

    // --------------------------------------------------------------------

    private String getResource(String name)
    {
	String value;
	try {
	    value = resources.getString(name);
	}
	catch (MissingResourceException ex) {
	    value = null;
	}
	return value;
    }

    // --------------------------------------------------------------------

    private String[] tokenize(String input)
    {
	Vector v = new Vector();
	StringTokenizer t = new StringTokenizer(input);
	String tokens[];
    
	while (t.hasMoreTokens())
	    v.addElement(t.nextToken());
	tokens = new String[v.size()];
	for (int i=0; i<tokens.length; i++)
	    tokens[i] = (String) v.elementAt(i);

	return tokens;
    }

    // ======================= WINDOW INITIALISATION =======================

    // --------------------------------------------------------------------
    /**
     * Create all the Window components
     */

    private void initWindow(boolean showTool, boolean showLine)
    {
	setBackground(frameBgColor);

	// prepare the content pane

	JPanel contentPane = new JPanel(new BorderLayout(5, 5));
	contentPane.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
	setContentPane(contentPane);

	// create and add info and status areas

	JPanel bottomArea = new JPanel();		// create panel for info/status
	bottomArea.setLayout(new BorderLayout(5, 5));

	info = new Info();
	bottomArea.add (info, BorderLayout.CENTER);

	statusArea = new JPanel();
	statusArea.setLayout(new GridLayout(0, 1));	// one column, many rows
	statusArea.setBackground(infoColor);
	statusArea.setBorder(BorderFactory.createLineBorder(Color.black));

	lineCounter = new LineNumberLabel(1);
	saveState = new StatusLabel(StatusLabel.SAVED, this);
	if (showLine)				// if the line number display
	    statusArea.add(lineCounter);
	statusArea.add(saveState);
	bottomArea.add(statusArea, BorderLayout.EAST);
  
	contentPane.add(bottomArea, BorderLayout.SOUTH);

	// create the text document

	document = new DefaultStyledDocument();
	document.addDocumentListener(saveState);
	document.addUndoableEditListener(new MoeUndoableEditListener());

	// create the text pane

	MoeEditorKit kit = new MoeEditorKit();
	MutableAttributeSet attr = kit.getInputAttributes();
	//StyleConstants.setFontSize(attr, Config.editFontsize); doesn't work yet

	textPane = new JTextPane();
	textPane.setDocument(document);
	textPane.setCaretPosition(0);
	textPane.setMargin(new Insets(2,2,2,2));
	textPane.setOpaque(true);
	textPane.setEditorKit(kit);
	textPane.setCaret(new MoeCaret(this));
	//	textPane.setFont(editFont); //this does not work - don't know why
	textPane.getCaret().setBlinkRate(0);
	textPane.setSelectionColor(selectionColor);
	textPane.setCaretColor(cursorColor);

	JScrollPane scrollPane = new JScrollPane(textPane);
	scrollPane.setPreferredSize(new Dimension(598,400));

	contentPane.add(scrollPane, BorderLayout.CENTER);

	// create table of edit actions

	createActionTable(textPane);

	// create menubar and menus

	JMenuBar menubar = createMenuBar();
	setJMenuBar(menubar);

	// create toolbar

	toolbar = createToolbar();
	contentPane.add(toolbar, BorderLayout.NORTH);

	// add event listener to handle the window close requests

	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		close();
	    }
	    public void windowActivated(WindowEvent e) {
		textPane.requestFocus();
	    }
	});

	//     if (showTool)
	//        show_toolbar(true);

	setWindowTitle();
	pack();

    } // init_window


    // --------------------------------------------------------------------
    /**
     * Create the table of action supported by this editor
     */

    private void createActionTable(JTextComponent textComponent)
    {
	actions = new Hashtable();		    // will hold all user actions

	// first, create our own actions

	undoAction = new UndoAction();
	redoAction = new RedoAction();

	Action[] myActions = {

	    // class actions
	    new NewAction(),
	    new OpenAction(),
	    new SaveAction(),
	    new SaveAsAction(),
	    new ReloadAction(),
	    new PrintAction(),
	    new CloseAction(),

	    // edit actions
	    undoAction,
	    redoAction,
	    new CommentAction(),
	    new UncommentAction(),
	    new InsertMethodAction(),

	    // tool actions
	    new FindAction(),
	    new FindBackwardAction(),
	    new FindNextAction(),
	    new FindNextReverseAction(),
	    new ReplaceAction(),
	    new GotoLineAction(),
	    new CompileAction(),

	    // debug actions
	    new SetBreakPointAction(),
	    new ClearBreakPointAction(),
	    new StepAction(),
	    new StepIntoAction(),
	    new ContinueAction(),
	    new TerminateAction(),

	    // option actions
	    new PreferencesAction(),
	    new KeyBindingsAction(),

	    // help actions
	    new AboutAction(),
	    new CopyrightAction(),
	    new DescribeKeyAction(),
	    new HelpMouseAction(),
	    new ShowManualAction(),
	    new ReportErrorAction(),

	    // internal actions
	    new EmptyAction(),
	    new SelectViewAction()
	};

	// now, get the actions already defined in the editor and merge them
	// with our own actions

	Action[] allActions = TextAction.augmentList(textComponent.getActions(),
						     myActions);

	// next, enter all those actions into our hash table

	Action action;
	for (int i=0; i < allActions.length; i++) {
	    action = allActions[i];
	    actions.put(action.getValue(Action.NAME), action);
	}
    }

    // --------------------------------------------------------------------
    /**
     * Create the table of action supported by this editor
     */

    private Action getActionByName(String name)
    {
	return (Action)(actions.get(name));
    }

    // --------------------------------------------------------------------

    private JMenuBar createMenuBar()
    {
	JMenuBar menubar = new JMenuBar();
	JMenu menu = null;

	String[] menuKeys = tokenize(getResource("menubar"));
	for (int i=0; i<menuKeys.length; i++) {
	    menu = createMenu(menuKeys[i]);
	    if (menu != null)
		menubar.add(menu);
	}
	if (menu != null) {
	    // Always put help menu last
	    //menubar.setHelpMenu(menu);
	}
	return menubar;
    }

    // --------------------------------------------------------------------

    private JMenu createMenu(String key)
    {
	JMenuItem item;
	String label;
	Keymap kmap = textPane.getKeymap();

	JMenu menu = new JMenu(getResource(key + LabelSuffix));
	String itemString = getResource(key);
	if (itemString == null) {
	    Debug.message ("Moe: cannot find menu definition for " + key);
	    return null;
	}
	String[] itemKeys = tokenize(itemString);
	for (int i=0; i<itemKeys.length; i++) {
	    if (itemKeys[i].equals("-"))
		menu.addSeparator();
	    else {
		Action action = getActionByName(itemKeys[i]);
		if (action == null)
		    Debug.message ("Moe: cannot find action " + itemKeys[i]);
		else {
		    item = menu.add(action);
		    label = getResource(itemKeys[i] + LabelSuffix);
		    if (label != null)
			item.setText(label);
		    KeyStroke[] keys = kmap.getKeyStrokesForAction(action);
		    if (keys != null) { // keys.length > 0) 
			Debug.message ("key accel found");
			item.setAccelerator(keys[0]);
		    }
		}
	    }
	}
	return menu;
    }

    // --------------------------------------------------------------------

    private JComponent createToolbar()
    {
	JPanel toolbar = new JPanel();
	//    toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
	((FlowLayout)toolbar.getLayout()).setAlignment (FlowLayout.LEFT);

	String[] toolKeys = tokenize(getResource("toolbar"));
	for (int i=0; i<toolKeys.length; i++) {
	    toolbar.add(createTool(toolKeys[i]));
	}

	return toolbar;
    }

    // --------------------------------------------------------------------

    private JComponent createTool(String key)
    {
	if (key.equals("view"))
	    return createViewSelector(key);
	else
	    return createToolbarButton(key);
    }

    // --------------------------------------------------------------------

    private JButton createToolbarButton(String key)
    {
	String label = getResource(key + LabelSuffix);
	JButton button = new JButton(label);
    
	button.setRequestFocusEnabled(false);   // never get keyboard focus
	button.setMargin(new Insets(2,2,2,2));

	String actionName = getResource(key + ActionSuffix);
	if (actionName == null)
	    actionName = key;
	Action action = getActionByName(actionName);

	if (action != null) {	// should never be null...
	    button.addActionListener(action);
	    button.setActionCommand(actionName);
	    action.addPropertyChangeListener(new ActionChangeListener(button));
	}
	else {
	    button.setEnabled(false);
	    Debug.message("Moe: action not found for button " + label);
	}
	return button;
    }

    // --------------------------------------------------------------------

    private JComboBox createViewSelector(String key)
    {
	String[] viewStrings = 
	{ getResource(key + LabelSuffix + "1"),
	  getResource(key + LabelSuffix + "2"),
	  getResource(key + LabelSuffix + "3"),
	  getResource(key + LabelSuffix + "4") };
	viewSelector = new JComboBox(viewStrings);
	viewSelector.setRequestFocusEnabled(false);   // never get keyboard focus

	String actionName = getResource(key + ActionSuffix);
	if (actionName == null)
	    actionName = key;
	Action action = getActionByName(actionName);

	if (action != null) {	// should never be null...
	    viewSelector.addActionListener(action);
	    viewSelector.setActionCommand(actionName);
	}
	else {
	    viewSelector.setEnabled(false);
	    Debug.message("Moe: action not found for view selector");
	}

	return viewSelector;
    }

    public boolean isReadOnly() {
	    return !textPane.isEditable();
    }
    
    public void setReadOnly(boolean readOnlyStatus) {
	    if (readOnlyStatus)
		saveState.setState(StatusLabel.READONLY);

	    textPane.setEditable(!readOnlyStatus);
    }
		    
} // end class MoeEditor
