package bluej.editor.moe;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.editor.EditorWatcher;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Date;
import java.text.DateFormat;
import java.io.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model    
import javax.swing.*;		// all the GUI components
import javax.swing.KeyStroke;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
 ** @author Michael Kolling
 **
 **/

public final class MoeEditor extends JFrame

	implements bluej.editor.Editor, ItemListener
{
    // -------- CONSTANTS --------

    // version number
    static final int version = 030;
    static final String versionString = "0.3";

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

    //  spaces for entering half tabs
    static final String spaces = "    ";

    // Attributes for lines and document
    static final String BREAKPOINT = "break";
    static final String COMPILED = "compiled";

    // -------- INSTANCE VARIABLES --------

    private EditorWatcher watcher;
    private Properties resources;
    private DefaultStyledDocument document;
    private MoeActions actions;

    private JTextPane textPane;		// the component holding the text
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

    private boolean mayHaveBreakpoints;	// true if there were BP here

    // =========================== NESTED CLASSES ===========================

    // inner class for listening for undoable edits in text

    private class MoeUndoableEditListener implements UndoableEditListener {
  
	public void undoableEditHappened(UndoableEditEvent e)
	{
	    actions.undoManager.addEdit(e.getEdit());
	    actions.undoAction.update();
	    actions.redoAction.update();
	}
    }

    /** 
     *  Inner class listening for disabling actions - if an action is
     *  disabled (enabled), the connected button is disabled (enabled) 
     *  as well.
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
     *  Constructor. Title may be null 
     */

    public MoeEditor(String title, boolean isCode, EditorWatcher watcher, 
		     boolean showToolbar, boolean showLineNum, 
		     Properties resources)
    {
	super("Moe");

	this.watcher = watcher;
	this.resources = resources;
	filename = null;
	windowTitle = title;
	firstSave = true;
	isCompiled = false;
	newline = System.getProperty("line.separator");
	this.isCode = isCode;
	mayHaveBreakpoints = false;

	initWindow(showToolbar, showLineNum);
    }

    // --------------------------------------------------------------------
    /**
     *  Load the file "fname" and show the editor window.
     */

    public boolean showFile(String filename, boolean compiled,
			    Vector breakpoints)
			    // inherited from Editor, redefined
    {
	this.filename = filename;

	boolean loaded = false;
	boolean readError = false;

	if(breakpoints != null)
	    Debug.reportError("breakpoints in showfile not supported.");

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

	if (! loaded)  // should exist, but didn't
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

	setCompileStatus(compiled);
	if (!isCode)
	    actions.compileAction.setEnabled(false);
	return true;

    } // showFile

    // --------------------------------------------------------------------

    public void reloadFile() // inherited from Editor, redefined
    {
	doReload();
    }

    // --------------------------------------------------------------------
    /**
     *  Wipe out contents of the editor.  
     */

    public void clear()	// inherited from Editor, redefined
    {
	textPane.setText("");
    }

    // --------------------------------------------------------------------
    /**
     *  Insert a string into the buffer. The editor is not immediately
     *  redisplayed. This function is typically used in a sequence
     *  "clear; [insertText]*; setVisible(true)". If the selection is on,
     *  it is replaced by the new text.
     *
     *  @param text         the text to be inserted
     *  @param style         the style in which the text is to be displayed
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
     *  Show the editor window. This includes whatever is necessary of the
     *  following: make visible, de-iconify, bring to front of window stack.
     *
     *  @param view		the view to be displayed. Must be one of the 
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
     *  Save the buffer to disk under current filename.  This is often called
     *  from the outside - just in case.  Save only if really necessary, 
     *  otherwise we save much too often.
     *  PRE: filename != null
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
     *  The editor wants to close. Do this through the EditorManager so that
     *  we can be removed from the list of open editors.
     */

    public void close()	// inherited from Editor
    {
	save(); // temporary - should really be done by watcher from outside
	doClose();
    }

    // --------------------------------------------------------------------
    /**
     *  Display a message (used for compile/runtime errors). An editor
     *  must support at least two lines of message text, so the message
     *  can contain a newline character.
     *
     *  @param message	the message to be displayed
     *  @param line		the line to move the cursor to (the line is 
     *			also highlighted)
     *  @param column		the column to move the cursor to
     *  @param beep		if true, do a system beep
     *  @param setStepMark	if true, set step mark (for single stepping)
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
     *  Remove the step mark (the mark that shows the current line when
     *  single-stepping through code). If it is not currently displayed,
     *  do nothing.
     */

    public void removeStepMark()		// inherited from Editor
    {
	// ## NYI
    }

    // --------------------------------------------------------------------
    /**
     *  Change class name.
     *
     *  @param title	new window title
     *  @param filename	new file name
     */
    public void changeName(String title, String filename)
    // inherited from Editor
    {
	this.filename = filename;		// error ## - need to add full path
	windowTitle = title;
	setWindowTitle();
    }

    // --------------------------------------------------------------------
    /**
     *  Set the "compiled" status
     *
     *  @param compiled	true if the class has been compiled
     */
    public void setCompiled(boolean compiled)
    {
	setCompileStatus(compiled);
	if (compiled)
	    info.message("Class compiled - no syntax errors");
    }

    // --------------------------------------------------------------------
    /**
     *  Remove all breakpoints in this editor.
     */
    public void removeBreakpoints()
    {
	clearAllBreakpoints();
    }

    // --------------------------------------------------------------------
    /**
     *  Determine whether this buffer has been modified.
     *  @returns	a boolean indicating whether the file is modified
     */
    public boolean isModified()	// inherited from Editor, redefined
    {
	return (saveState.isChanged());
    }

    // --------------------------------------------------------------------
    /**
     *  Set this editor to read-only.
     */
    public void setReadOnly(boolean readOnlyStatus) {
	if (readOnlyStatus)
	    saveState.setState(StatusLabel.READONLY);
	textPane.setEditable(!readOnlyStatus);
    }
		    
    // --------------------------------------------------------------------
    // ------------ end of interface inherited from Editor ----------------
    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    /**
     *  Clear the message in the info area. 
     */
    public void clearMessage()
    {
	info.clear();
    }

    // ==================== USER ACTION IMPLEMENTATIONS ===================

    // --------------------------------------------------------------------
    /**
     *  
     */
    public void userSave()
    {
	if (saveState.isSaved())
	    info.message ("No changes need to be saved");
	else 
	    save();
    }

    // --------------------------------------------------------------------
    /**
     *  
     */
    public void reload()
    {
	if (filename == null) {
	    info.warning ("Can not reload - this text was never saved!",
			  "(\"Reload\" reloads the last saved state from disk.)");
	}
	else if (saveState.isChanged()) {
	    int answer = Utility.askQuestion(this,
					     "Reload discards all changes since the last edit.\nAre you sure?",
					     "Reload", "Cancel", null);
	    if (answer == 0)
		doReload();
	}
	else
	    doReload();
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation if the "print" user function
     *  (NOTE: re-implement under jdk 1.2 with "PrinterJob"!)
     */
    public void print()
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
     *  Part of the print function. Print out the class view currently
     *  visible in the editor.
     */
    private void printClass(PrintJob printjob) 
    {
  	Dimension pageSize = printjob.getPageDimension();

        Font oldFont = textPane.getFont();
        textPane.setFont(printFont);

	Rectangle printArea = getPrintArea(pageSize);
	Dimension textSize = textPane.getSize(null);
	//Debug.message("text height: " + textSize.height);
	textSize.width -= (TAG_WIDTH + 3);
	int pages = (textSize.height + printArea.height - 1) / 
	    printArea.height;
	//Debug.message("pages: " + pages);

	int answer;
	if(printArea.width < textSize.width-8) {
	    answer = Utility.askQuestion(this,
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
     *  Return the rectangle on the page in which to print the text.
     *  The rectangle is the page minus margins minus space for header and
     *  footer text.
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
	//Debug.message("print height: " + printHeight);
	//Debug.message("lines/page " + (printHeight / fontSize));

	return new Rectangle(PRINT_HMARGIN,
			     PRINT_VMARGIN + tfm.getHeight() + 4,
			     pageSize.width - 2 * PRINT_HMARGIN,
			     printHeight);
    }

    // --------------------------------------------------------------------
    /**
     *  printFrame - part of the print function. Print the frame around the
     *  page, including header and footer.
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
     *  The editor has been closed. Hide the editor window now. 
     */
    public void doClose()
    {
	setVisible(false);
	MoeEditorManager.editorManager.removeEditor(this);
	if (watcher != null)
	    watcher.closeEvent(this);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "find" user function.
     */

    public void find()
    {
	Finder finder = MoeEditorManager.editorManager.getFinder();
	Utility.centreWindow(finder, this);
	String s = finder.getNewSearchString(this, Finder.FORWARD);
	if(s != null)
	    findString(finder, s, finder.getDirection() == Finder.BACKWARD);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "find-backward" user function.
     */

    public void findBackward()
    {
	Finder finder = MoeEditorManager.editorManager.getFinder();
	String s = finder.getNewSearchString(this, Finder.BACKWARD);
	if(s != null)
	    findString(finder, s, finder.getDirection() == Finder.BACKWARD);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "find-next" user function.
     */

    public void findNext()
    {
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
	findString(finder, s, finder.getDirection() == Finder.BACKWARD);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "find-next-reverse" user function.
     */

    public void findNextReverse()
    {
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
	findString(finder, s, finder.getDirection() == Finder.FORWARD);
    }


    // --------------------------------------------------------------------
    /**
     *   Do a find with info in the info area.
     */

    private void findString(Finder finder, String s, boolean backward)
    {
	if (s.length()==0) {
	    info.warning("Empty search string.");
	    return;
	}
	String msg;
	boolean wrap = ! finder.lastSearchFound();
	msg = "Find " + (backward ? "backward" : "forward") +
	                (wrap ? " (wrap around): " : ": ") + s;
	info.message(msg);
	boolean found;
	if(backward)
	    found = doFindBackward(s, wrap);
	else
	    found = doFind(s, wrap);
	finder.setSearchString(s);
	finder.setSearchFound(found);
	if (! found)
	    info.warning(msg, "Not found");
    }

    // --------------------------------------------------------------------
    /**
     *  doFind - do a find without visible feedback. Returns false if not found.
     */

    private boolean doFind(String s, boolean wrap)
    {
	int docLength = document.getLength();
	int startPosition = textPane.getCaretPosition();
	int endPos = docLength;

	boolean found = false;
	boolean finished = false;

	int start = startPosition;
	Element line = getLineAt(start);
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
		else {	// go to next line
		    line = document.getParagraphElement(lineEnd+1);
		    start = line.getStartOffset();
		    lineEnd = Math.min(line.getEndOffset(), endPos);
		}
	    }
	}
	catch (BadLocationException ex) {
	    Debug.message("error in editor find operation");
	}
	return found;
    }

    // --------------------------------------------------------------------
    /**
     *  doFindBackward - do a find backwards without visible feedback.
     *   Returns false if not found.
     */

    private boolean doFindBackward(String s, boolean wrap)
    {
	int docLength = document.getLength();
	int startPosition = textPane.getCaretPosition() - 1;
	if(startPosition < 0)
	    startPosition = docLength;
	int endPos = 0;		// where the search ends

	boolean found = false;
	boolean finished = false;

	int start = startPosition;	// start of next partial search
	Element line = getLineAt(start);
	int lineStart = Math.max(line.getStartOffset(), endPos);

	try {
	    while (!found && !finished) {
		String lineText = document.getText(lineStart, start-lineStart);
		if(lineText != null && lineText.length() > 0) {
		    int foundPos = lineText.lastIndexOf(s);
		    if (foundPos != -1) {
			textPane.select(lineStart+foundPos, 
					lineStart+foundPos+s.length());
			found = true;
		    }
		}
		if (lineStart <= endPos) {	// reached end of search
		    if (wrap) {   // do the wrapping around
			endPos = startPosition;
			line = document.getParagraphElement(docLength);
			start = line.getEndOffset();
			lineStart = Math.max(line.getStartOffset(), endPos);
			wrap = false;  // don't wrap again
		    }
		    else
			finished = true;
		}
		else {	// go to next line
		    line = document.getParagraphElement(lineStart-1);
		    start = line.getEndOffset();
		    lineStart = Math.max(line.getStartOffset(), endPos);
		}
	    }
	}
	catch (BadLocationException ex) {
	    Debug.message("error in editor find operation");
	}
	return found;
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "compile" user function.
     */

    public void compile()
    {
	if (watcher == null)
	    return;
	if (!isCode)
	    return;

	info.message ("Compiling...");
	watcher.compile(this);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "set-breakpoint" user function.
     */

    void setBreakpoint()
    {
	setUnsetBreakpoint(textPane.getCaretPosition(), true);
    }

    // --------------------------------------------------------------------
    /**
     *  Implementation of "clear-breakpoint" user function.
     */

    void clearBreakpoint()
    {
	setUnsetBreakpoint(textPane.getCaretPosition(), false);
    }

    // --------------------------------------------------------------------
    /**
     *  Toggle a breakpoint at a given position.
     */

    void toggleBreakpoint(int pos)
    {
	if (positionHasBreakpoint(pos))
	    setUnsetBreakpoint(pos, false);  // remove
	else
	    setUnsetBreakpoint(pos, true);  // set

    }

    // --------------------------------------------------------------------
    /**
     *   Clear all known breakpoints.
     */

    private void clearAllBreakpoints()
    {
	if(mayHaveBreakpoints) {

	    for(int i = 1; i <= numberOfLines(); i++) {
		if(lineHasBreakpoint(i))
		    doRemoveBreakpoint(getPositionInLine(i));
	    }
	    mayHaveBreakpoints = false;
	}
    }

    // --------------------------------------------------------------------
    /**
     *  Check weather a position has a breakpoint set
     */

    private boolean positionHasBreakpoint(int pos)
    {
	Element line = getLineAt(pos);
	return (Boolean.TRUE.equals(
			line.getAttributes().getAttribute(BREAKPOINT)));
    }

    // --------------------------------------------------------------------
    /**
     *  Check weather a line has a breakpoint set
     */

    private boolean lineHasBreakpoint(int lineNo)
    {
	Element line = getLine(lineNo);
	return (Boolean.TRUE.equals(
			line.getAttributes().getAttribute(BREAKPOINT)));
    }

    // --------------------------------------------------------------------
    /**
     *  Try to set or remove a breakpoint (depending on the parameter) at
     *  the given position. Informs the watcher.
     */

    private void setUnsetBreakpoint(int pos, boolean set)
    {
	if (watcher != null) {
	    int line = getLineNumberAt(pos);
	    String result = watcher.breakpointToggleEvent(this, line, set);

	    if(result == null) {
		// no problem, go ahead
		SimpleAttributeSet a = new SimpleAttributeSet();
		if(set) {
		    a.addAttribute(BREAKPOINT, Boolean.TRUE);
		    mayHaveBreakpoints = true;
		}
		else
		    a.addAttribute(BREAKPOINT, Boolean.FALSE);
		document.setParagraphAttributes(pos, 0, a, false);
	    }
	    else
		info.warning(result);
	}
	else
	    info.warning("Cannot set breakpoint:\n" +
			 "No code associated with this editor.");
	    
    }

    // --------------------------------------------------------------------
    /**
     *  Remove a breakpoint without question.
     */

    private void doRemoveBreakpoint(int pos)
    {
	SimpleAttributeSet a = new SimpleAttributeSet();
	a.addAttribute(BREAKPOINT, Boolean.FALSE);
	document.setParagraphAttributes(pos, 0, a, false);
    }

    // ========================= SUPPORT ROUTINES ==========================

    // --------------------------------------------------------------------
    /**
     *  Return the number of lines in the documant.
     */

    private int numberOfLines()
    {
	return document.getDefaultRootElement().getElementCount();
    }

    // --------------------------------------------------------------------
    /**
     *  Return the current line.
     */

    private Element getCurrentLine()
    {
	return document.getParagraphElement(textPane.getCaretPosition());
    }

    // --------------------------------------------------------------------
    /**
     *  Find and return a line by line number
     */

    private Element getLine(int lineNo)
    {
	return document.getDefaultRootElement().getElement(lineNo-1);
    }

    // --------------------------------------------------------------------
    /**
     *  Find and return a line by text position
     */

    private Element getLineAt(int pos)
    {
	return document.getParagraphElement(pos);
    }

    // --------------------------------------------------------------------
    /**
     *  Find and return a position in a line.
     */

    private int getPositionInLine(int lineNo)
    {
	return getLine(lineNo).getStartOffset();
    }

    // --------------------------------------------------------------------
    /**
     *  Return the number of the current line.
     */

    private int getCurrentLineNo()
    {
	return document.getDefaultRootElement().getElementIndex(
								textPane.getCaretPosition()) + 1;
    }

    // --------------------------------------------------------------------
    /**
     *  Return the number of the line containing position 'pos'.
     */

    private int getLineNumberAt(int pos)
    {
	return document.getDefaultRootElement().getElementIndex(pos) + 1;
    }

    // --------------------------------------------------------------------
    /**
     *  Revert the buffer contents to the last saved version.  Do
     *  not ask any question - just do it. Must have a file name.
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
    private void setCompileStatus(boolean compiled)
    {
	viewSelector.setEnabled(compiled);
	actions.getActionByName("set-breakpoint").setEnabled(compiled);
	actions.getActionByName("clear-breakpoint").setEnabled(compiled);
	isCompiled = compiled;

	if(compiled)
	    document.putProperty(COMPILED, Boolean.TRUE);
	else
	    document.putProperty(COMPILED, Boolean.FALSE);
	textPane.repaint();
    }

    // --------------------------------------------------------------------
    /**
     *  Set the saved/changed status of this buffer to SAVED.
     */
    private void setSaved()
    {
	info.message ("File saved");
	saveState.setState (StatusLabel.SAVED);
	if(watcher != null)
	    watcher.saveEvent(this);
    }

    // --------------------------------------------------------------------
    /**
     *  Buffer just went from saved to changed state (called by StatusLabel)
     */
    void setChanged()
    {
	setCompileStatus (false);
	if(watcher != null)
	    watcher.modificationEvent(this);
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
     *  Sets the editor to contain a view. This is used if the view is set from
     *  the outside of the editor (not by the editor function).
     *
     *  @param view    the new view. Must be one of the defined view constants.
     */
    private void setView(int view)
    {
	int newIndex = 0;

	if (view == bluej.editor.Editor.IMPLEMENTATION)
	    newIndex = 0;
	else if (view == bluej.editor.Editor.PUBLIC)
	    newIndex = 1;
	else if (view == bluej.editor.Editor.PACKAGE)
	    newIndex = 2;
	else if (view == bluej.editor.Editor.INHERITED)
	    newIndex = 3;

	if(newIndex != viewSelector.getSelectedIndex()) {
	    viewSelector.setSelectedIndex(newIndex);

	    isCode = (view == bluej.editor.Editor.IMPLEMENTATION);
	    if(!isCode)
		setCompileStatus(true);

	    actions.compileAction.setEnabled(isCode);
	}
    }

    // --------------------------------------------------------------------
    /**
     *  Show or hide the line number display (depending on the parameter
     *  'show').
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

    private String getResource(String name)
    {
	return resources.getProperty(name);
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

    // ---- ItemListener interface ----

    public void itemStateChanged(ItemEvent evt) 
    {
	// the only item we're listening to is the items in the view selector

	if(evt.getStateChange() == ItemEvent.DESELECTED)
	    return;  // ignore deselection events

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
	watcher.changeView(this, view);
    }

    // ======================= WINDOW INITIALISATION =======================

    // --------------------------------------------------------------------
    /**
     *  Create all the Window components
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

	// get table of edit actions

	actions = MoeActions.getActions(textPane);

	// **** temporary: disable all unimplemented actions ****

	actions.getActionByName("comment").setEnabled(false);
	actions.getActionByName("uncomment").setEnabled(false);
	actions.getActionByName("replace").setEnabled(false);
	actions.getActionByName("goto-line").setEnabled(false);
	actions.getActionByName("preferences").setEnabled(false);
	actions.getActionByName("describe-key").setEnabled(false);
	actions.getActionByName("show-manual").setEnabled(false);

	// ****

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
     *  Create the editor's menu bar.
     */
    private JMenuBar createMenuBar()
    {
	JMenuBar menubar = new JMenuBar();
	JMenu menu = null;

	String[] menuKeys = tokenize(getResource("menubar"));
	for (int i=0; i<menuKeys.length; i++) {
	    menu = createMenu(menuKeys[i]);
	    if (menu != null) {
		// Hack while "setHelpMenu" does not work...
		if(menuKeys[i].equals("help"))
		    menubar.add(Box.createHorizontalGlue());
		menubar.add(menu);
	    }
	}
	if (menu != null) {
	    // Always put help menu last
	    //menubar.setHelpMenu(menu);  // not implemented in Swing 1.1
	}
	return menubar;
    }

    // --------------------------------------------------------------------

    /**
     *  Create a single menu for the editor's menu bar. The key for the menu
     *  (as defined in moe.properties) is supplied.
     */
    private JMenu createMenu(String key)
    {
	JMenuItem item;
	String label;

	// get menu title
	JMenu menu = new JMenu(getResource(key + LabelSuffix));

	// get menu definition
	String itemString = getResource(key);
	if (itemString == null) {
	    Debug.message ("Moe: cannot find menu definition for " + key);
	    return null;
	}

	// cut menu definition into separate items
	String[] itemKeys = tokenize(itemString);

	// create menu item for each item
	for (int i=0; i<itemKeys.length; i++) {
	    if (itemKeys[i].equals("-"))
		menu.addSeparator();
	    else {
		Action action = actions.getActionByName(itemKeys[i]);
		if (action == null)
		    Debug.message ("Moe: cannot find action " + itemKeys[i]);
		else {
		    item = menu.add(action);
		    label = getResource(itemKeys[i] + LabelSuffix);
		    if (label != null)
			item.setText(label);
		    KeyStroke[] keys = actions.getKeyStrokesForAction(action);
		    if (keys != null) 
			item.setAccelerator(keys[0]);
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
	Action action = actions.getActionByName(actionName);

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
	viewSelector.setRequestFocusEnabled(false);   // never get focus
	viewSelector.addItemListener(this);
	return viewSelector;
    }

} // end class MoeEditor
