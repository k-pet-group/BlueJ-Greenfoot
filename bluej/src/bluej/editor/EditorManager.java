// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

package bluej.editor;

import java.awt.Rectangle;
import java.util.List;
import bluej.editor.moe.MoeEditorManager;

/**
 * Interface between the editor manager and the rest of BlueJ.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Bruce Quig
 * @version $Id: EditorManager.java 2642 2004-06-21 14:53:23Z polle $
 */
public abstract class EditorManager
{

    private static EditorManager theEditorManager = new MoeEditorManager();

    /**
     * Singleton factory method to return an EditorManager instance;
     *
     * @returns	the singleton EditorManager instance
     */
    public static EditorManager getEditorManager()
    {
        return theEditorManager;
    }


    /**
     * Open an editor to display a class. The filename may be "null"
     * to open an empty editor (e.g. for displaying a view). The editor
     * is initially hidden. A call to "Editor::show" is needed to make
     * is visible after opening it.
     *
     * @param filename      name of the source file to open (may be null)
     * @param docFilename	name of the documentation based on filename
     * @param windowTitle   title of window (usually class name)
     * @param watcher       an object interested in editing events
     * @param compiled      true, if the class has been compiled
     * @param breakpoints   vector of Integers: line numbers where bpts are
     * @returns		    the new editor, or null if there was a problem
     */
    public abstract Editor openClass(String filename, 
        String docFilename, 
        String windowTitle, 
        EditorWatcher watcher, 
        boolean compiled, 
        List breakpoints, 
        ClassLoader projectClassLoader, Rectangle bounds );


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
    public abstract Editor openText(String filename, String windowTitle,
                    EditorWatcher watcher, Rectangle bounds );

    /**
     * Indicate to the manager that all resources used by this editor
     * should be discarded.
     */
    protected abstract void discardEditor(Editor ed);

    /**
     * Refresh the display of all showing editors (usually because
     * an editor property such as font has changed)
     */
    public abstract void refreshAll();


} // end interface EditorManager
