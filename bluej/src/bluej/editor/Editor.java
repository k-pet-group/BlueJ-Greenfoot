package bluej.editor;

import java.util.Vector;
import java.util.Enumeration;

/**
 ** @version $Id: Editor.java 49 1999-04-28 03:01:02Z ajp $
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** Interface between an editor and the rest of BlueJ
 **/
public interface Editor
{
    // CONSTANTS:
	// views supported by the editor
	int PUBLIC = 0;
	int PACKAGE = 1;
	int INHERITED = 2;
	int IMPLEMENTATION = 3;
	
	// styles
    // int StyleNormal = 0;
    // int StyleBold = 1;
    // int StyleItalics = 2;
    // int StyleBoldItalics = 3;
    // int StyleColourRed = 4;

	/**
	 ** Read a file into the editor buffer and show the editor. If the
	 ** editor already contains text, it is cleared first.
	 **
	 ** @arg filename	the file to be read
	 ** @arg compiled	true if this is a compiled class
	 ** @arg breakpoints	Vector of Integers: list of breakpoints in 
	 **			current class (if 'compiled' is false, 
	 **			'breakpoints' must be null).
	 ** @returns		false is there was a problem, true otherwise
	 **/
	boolean showFile(String filename, boolean compiled,
				      Vector breakpoints);


	/**
	 ** Reload and display the same file that was displayed before.
	 **/
	void reloadFile();


	/**
	 ** Clear the current buffer. The editor is not redisplayed after a
	 ** call to this function. It is typically used in a sequence
	 ** "clear; [insertText]*; show".
	 **/
	void clear();


	/**
	 ** Insert a string into the buffer. The editor is not immediately 
	 ** redisplayed. This function is typically used in a sequence
	 ** "clear; [insertText]*; show".
	 **
	 ** @arg text		the text to be inserted
	 ** @arg style  	the style in which the text is to be displayed
	 **			(one of the style constants defined in this 
	 **			class)
	 **/
	void insertText(String text, boolean bold, boolean italic);


	/**
	 ** Show the editor window. This includes whatever is necessary of the
	 ** following: make visible, de-iconify, bring to front of window 
	 ** stack.
	 **
	 ** @arg view		the view to be displayed. Must be one of the 
	 **			view constants defined above
	 **/
	void show(int view);


	/**
	 ** Save the buffer to disk under the current file name. This is an 
	 ** error if the editor has not been given a file name (ie. if
	 ** readFile was not executed).
	 **/
	void save();

	
	/**
	 ** Close the editor window.
	 **/
	void close();


	/**
	 ** Display a message (used for compile/runtime errors). An editor
	 ** must support at least two lines of message text, so the message
	 ** can contain a newline character.
	 **
	 ** @arg message	the message to be displayed
	 ** @arg lineNumber	the line to move the cursor to (the line is 
	 **			also highlighted)
	 ** @arg column		the column to move the cursor to
	 ** @arg beep		if true, do a system beep
	 ** @arg setStepMark	if true, set step mark (for single stepping)
	 **/
	void displayMessage(String message, int lineNumber, 
					int column, boolean beep, 
					boolean setStepMark);


	/**
	 ** Remove the step mark (the mark that shows the current line when
	 ** single-stepping through code). If it is not currently displayed,
	 ** do nothing.
	 **/
	void removeStepMark();


	/**
	 ** Change class name.
	 **
	 ** @arg title		new window title
	 ** @arg filename	new file name
	 **/
	void changeName (String title, String filename);



	/**
	 ** Set the "compiled" status
	 **
	 ** @arg compiled	true if the class has been compiled
	 **/
	void setCompiled (boolean compiled);


	/**
	 ** Determine whether this editor has been modified from the
	 ** version on disk
	 ** @returns	a boolean indicating whether the file is modified
	 **/
	boolean isModified();


	boolean isReadOnly();
    
	void setReadOnly(boolean readOnlyStatus);

} // end interface Editor
