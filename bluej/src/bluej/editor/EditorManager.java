package bluej.editor;

import java.util.Vector;

/**
 * Interface between the editor manager and the rest of BlueJ.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: EditorManager.java 505 2000-05-24 05:44:24Z ajp $
 */
public interface EditorManager
{
    /**
     * Open an editor to display a class. The filename may be "null"
     * to open an empty editor (e.g. for displaying a view). The editor
     * is initially hidden. A call to "Editor::show" is needed to make
     * is visible after opening it.
     *
     * @param filename      name of the source file to open (may be null)
     * @param windowTitle   title of window (usually class name)
     * @param watcher       an object interested in editing events
     * @param compiled      true, if the class has been compiled
     * @param breakpoints   vector of Integers: line numbers where bpts are
     * @returns		    the new editor, or null if there was a problem
     */
    Editor openClass(String filename, String windowTitle,
                     EditorWatcher watcher, boolean compiled,
                     Vector breakpoints);


    /**
     * Open an editor to display a text document. The difference to
     * "openClass" is that code specific functions (such as compile,
     * debug, view) are disabled in the editor. The filename may be
     * "null" to open an empty editor. The editor is initially hidden.
     * A call to "Editor::show" is needed to make is visible after
     * opening it.
     *
     * @param filename	name of the source file to open (may be null)
     * @param windowTitle	title of window (usually class name)
     * @param watcher	an object interested in editing events
     * @returns		the new editor, or null if there was a problem
     */
    Editor openText(String filename, String windowTitle,
                    EditorWatcher watcher);

    /**
     * Indicate to the manager that all resources used by this editor
     * should be discarded.
     */
    void discardEditor(Editor ed);

    /**
     * Refresh the display of all showing editors (usually because
     * an editor property such as font has changed)
     */
    void refreshAll();


} // end interface EditorManager
