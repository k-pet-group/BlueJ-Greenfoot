package bluej.editor;

import java.util.Vector;

/**
 ** @version $Id: EditorManager.java 111 1999-06-04 06:16:57Z mik $
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
	 ** @param filename	name of the source file to open (may be null)
	 ** @param windowTitle	title of window (usually class name)
	 ** @param watcher	an object interested in editing events
	 ** @param compiled	true, if the class has been compiled
	 ** @param breakpoints	vector of Integers: line numbers where bpts are
	 ** @returns		the new editor, or null if there was a problem
	 **/
	Editor openClass(String filename, String windowTitle, 
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
	 ** @param filename	name of the source file to open (may be null)
	 ** @param windowTitle	title of window (usually class name)
	 ** @param watcher	an object interested in editing events
	 ** @returns		the new editor, or null if there was a problem
	 **/
	Editor openText(String filename, String windowTitle, 
				  EditorWatcher watcher);

} // end interface EditorManager
