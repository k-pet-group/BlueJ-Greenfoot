package bluej.editor;

import java.util.Vector;

/**
 ** @version $Id: EditorManager.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** Interface between the editor manager and the rest of BlueJ
 **/
public interface EditorManager
{
	/**
	 ** Open an editor to display a class. The filename may be "null"
	 ** to open an empty editor (e.g. for displaying a view). The editor 
	 ** is initially hidden. A call to "Editor::show" is needed to make 
	 ** is visible after opening it.
	 **
	 ** @arg filename	name of the source file to open (may be null)
	 ** @arg windowTitle	title of window (usually class name)
	 ** @arg watcher	an object interested in editing events
	 ** @arg compiled	true, if the class has been compiled
	 ** @arg breakpoints	vector of Integers: line numbers where bpts are
	 ** @returns		the new editor, or null if there was a problem
	 **/
	public Editor openClass(String filename, String windowTitle, 
				  EditorWatcher watcher, boolean compiled,
				  Vector breakpoints);


	/**
	 ** Open an editor to display a text document. The difference to 
	 ** "openClass" is that code specific functions (such as compile, 
	 ** debug, view) are disabled in the editor. The filename may be
	 ** "null" to open an empty editor. The editor is initially hidden. 
	 ** A call to "Editor::show" is needed to make is visible after 
	 ** opening it.
	 **
	 ** @arg filename	name of the source file to open (may be null)
	 ** @arg windowTitle	title of window (usually class name)
	 ** @arg watcher	an object interested in editing events
	 ** @returns		the new editor, or null if there was a problem
	 **/
	public Editor openText(String filename, String windowTitle, 
				  EditorWatcher watcher);

} // end interface EditorManager
