package bluej.editor;

/**
 ** @version $Id: EditorWatcher.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Interface between the editor and the rest of BlueJ
 ** The editor uses this class
 **/
public interface EditorWatcher
{
	/**
	 ** Called by Editor when a file is changed
	 **/
	public void modificationEvent(Editor editor);

	/**
	 ** Called by Editor when a file is saved
	 **/
	public void saveEvent(Editor editor);

	/**
	 ** Called by Editor when it is closed
	 **/
	public void closeEvent(Editor editor);

	/**
	 ** Called by Editor when a breakpoint is been set/cleared
	 ** @arg lineNo		the line number of the breakpoint
	 ** @arg set		whether the breakpoint is set (true) or cleared
	 ** @returns            An error message or null or "".
	 **/
	public String breakpointToggleEvent(Editor editor, int lineNo, 
					    boolean set);

	/**
	 ** Called by Editor to change the view displayed by an editor
	 ** @arg viewType	the view to display, should be
	 **			one of bluej.editor.Editor.PUBLIC, etc.
	 ** @returns a boolean indicating if the change was allowed
	 **/
	public boolean changeView(Editor editor, int viewType);

	/**
	 ** Called by Editor when a file is to be compiled
	 **/
	public void compile(Editor editor);
} // end class EditorWatcher
