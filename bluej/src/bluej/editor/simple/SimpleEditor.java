package bluej.editor;

import java.util.Hashtable;
import java.util.Vector;

/**
 ** @version $Id: SimpleEditor.java 111 1999-06-04 06:16:57Z mik $
 ** @author Michael Cahill
 ** Interface between the editor and the rest of BlueJ
 **/

public class SimpleEditor extends Editor
{
	private Hashtable editors = new Hashtable();
	
	/**
	 ** Interface helper function
	 ** @param filename	the name of the file to open
	 ** @returns the editor frame object for that filename, if it exists
	 **/
	protected SimpleFrame getEditor(String filename)
	{
		return (SimpleFrame)editors.get(filename);
	}

	// Start of the BlueJ interface
	/**
	 ** Open a named file.
	 ** If the file is already open then bring its window to front.
	 ** @param filename	the name of the file to open
	 ** @param isCode	indicates whether the file contains source code
	 **/
	public void open(String filename, boolean isCode)
	{
		SimpleFrame editor = getEditor(filename);
		
		if(editor == null)
		{
			editor = new SimpleFrame(filename);
			editors.put(filename, editor);
		}
		
		editor.setVisible(true);
	}

	/**
	 ** Revert to the contents of the named file.
	 ** @param filename	the name of the file to revert to
	 **/
	public void reopen(String filename)
	{
		SimpleFrame editor = getEditor(filename);
		
		if(editor != null)
			; // XXX
	}
	
	/**
	 ** Save the buffer to disk.
	 ** @param filename	the name of the file for the operation
	 **      if <tt>filename</tt> is null then apply to all open editors
	 **/
	public void save(String filename)
	{
		SimpleFrame editor = getEditor(filename);
		
		if(editor != null)
			; // XXX
	}
	
	/**
	 ** Try to close (allows the editor to prompt the user to save
	 ** modified files).
	 ** @param filename	the name of the file to try to close
	 **      if <tt>filename</tt> is null then apply to all open editors
	 ** @returns	a boolean indicating whether the editor was closed
	 **/
	public boolean canClose(String filename)
	{
		SimpleFrame editor = getEditor(filename);
		
		if(editor != null)
			return editor.canClose();
		
		return true;
	}

	/**
	 ** Close regardless of whether the file is modified.
	 ** @param filename the name of the file to close
	 **      if <tt>filename</tt> is null then apply to all open editors
	 **/
	public void doClose(String filename)
	{
		SimpleFrame editor = getEditor(filename);
		
		if(editor != null)
			editor.doClose();
	}
	
	/**
	 ** Set the breakpoints of the named file.
	 ** @param filename	the name of the file for the operation
	 ** @param breakpoints	a list of breakpoint line numbers
	 **/
	public void setBreakpoints(String filename, Vector breakpoints)
	{
		// XXX: not implemented
	}
	
	/**
	 ** Set the views of the named file.
	 ** @param filename	the name of the file for the operation
	 ** @param inherited	a string representing the inherited view
	 ** @param exported	a string representing the exported view
	 **/
	public void setViews(String filename, String inherited, String exported)
	{
		// XXX: not implemented
	}
	
	/**
	 ** Highlight a line or region of a file
	 ** @param filename	the name of the file to try to close
	 ** @param startLine	the first line to highlight
	 ** @param endLine	the last line to highlight
	 **/
	public void select(String filename, int startLine, int endLine)
	{
		// XXX: not implemented
	}

	/**
	 ** Go to a particular line number - move the cursor to the
	 ** specified line and ensure it is displayed.
	 ** Note: need not change the current selection.
	 ** @param filename	the name of the file for the operation
	 ** @param lineNo		the line number to display
	 **/
	public void goToLine(String filename, int lineNo)
	{
		// XXX: not implemented
	}
	
	/**
	 ** Display a message (used for compile errors)
	 ** @param filename	the name of the file for the operation
	 ** @param message	the message to be displayed
	 **/
	public void showMessage(String filename, String message)
	{
		SimpleFrame editor = getEditor(filename);
		
		if(editor != null)
			editor.showMessage(message);
	}

	/**
	 ** Determine whether a file has been modified from the version
	 ** on disk
	 ** @param filename	the name of the file for the operation
	 ** @returns	a boolean indicating whether the file is modified
	 **/
	public boolean isModified(String filename)
	{
		SimpleFrame editor = getEditor(filename);
		
		if(editor != null)
			return editor.isModified();
			
		return false;
	}
} // end class
