/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
//Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

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
 * @version $Id: EditorManager.java 6215 2009-03-30 13:28:25Z polle $
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
     * @return		    the new editor, or null if there was a problem
     */
    public abstract Editor openClass(String filename, 
        String docFilename, 
        String windowTitle, 
        EditorWatcher watcher, 
        boolean compiled, 
        Rectangle bounds );


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
                                    Rectangle bounds );

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
