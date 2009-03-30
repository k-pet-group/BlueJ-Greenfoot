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
// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import bluej.Config;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;

import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import java.awt.*;		// Object input, ouput streams

/**
** @author Michael Kolling
**
**/

public final class MoeEditorManager
    extends bluej.editor.EditorManager
{
    // public static variables

    protected static MoeEditorManager editorManager;   // the manager object itself

    // private variables

    private Properties resources;
    private List<MoeEditor> editors; // open editors
    private Finder finder;           // the finder object

    // user preferences

    private boolean showLineNum;
    private boolean showToolBar;

    // =========================== PUBLIC METHODS ===========================

    public MoeEditorManager()
    {
        editors = new ArrayList<MoeEditor>(4);
        finder = new Finder();
               
        showToolBar = true;
        showLineNum = false;

        resources = Config.moeUserProps;

        editorManager = this;	// make this object publicly available
    }


    // ------------------------------------------------------------------------
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
    ** @param breakpoints	list of Integers: line numbers where bpts are
    ** @return		the new editor, or null if there was a problem
    **/

    public Editor openClass(String filename, 
                String docFilename,
                String windowTitle,
                EditorWatcher watcher, 
                boolean compiled,
                Rectangle bounds)
    {
        return openEditor (filename, docFilename, true, windowTitle, watcher, compiled,
                           bounds);
    }

    // ------------------------------------------------------------------------
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
    ** @returns		the new editor, or null if there was a problem
    **/

    public Editor openText(String filename, String windowTitle,
                           Rectangle bounds)	// inherited from EditorManager
    {
        return openEditor (filename, null, false, windowTitle, null, false, bounds);
    }

    public void refreshAll()
    {
        Iterator<MoeEditor> e = editors.iterator();

        while(e.hasNext()) {
            Editor ed = e.next();

            if(ed.isShowing()) {
                ed.refresh();
            }
       }
    }

    // ------------------------------------------------------------------------
    /**
     * Sound a beep if the "beep with warning" option is true
     */
    public void beep()
    {
        if(true) // if beepWarning option is on...
            Toolkit.getDefaultToolkit().beep();
    }

    // ------------------------------------------------------------------------
    /**
     * Discard the given editor and leave it to be collected by the garbage
     * collector.
     */
    public void discardEditor(Editor ed)
    {
        ed.close();
        editors.remove(ed);
    }

    // ========================== PACKAGE METHODS ===========================

    // ------------------------------------------------------------------------
    /**
    ** Return the shared finder object
    **/

    Finder getFinder()
    {
        return finder;
    }

       // ------------------------------------------------------------------------
 
    
    // ========================== PRIVATE METHODS ===========================

    // ------------------------------------------------------------------------
    /**
    ** Open an editor to display a class. The filename may be "null"
    ** to open an empty editor (e.g. for displaying a view). The editor
    ** is initially hidden. A call to "Editor::show" is needed to make
    ** is visible after opening it.
    **
    ** @param filename	name of the source file to open (may be null)
    ** @param docFilename	name of the documentation based on filename
    ** @param windowTitle	title of window (usually class name)
    ** @param watcher	an object interested in editing events
    ** @param compiled	true, if the class has been compiled
    ** @param bounds	bounds for the editor window
    ** @returns		the new editor, or null if there was a problem
    **/

    private Editor openEditor(String filename, String docFilename,
    							  boolean isCode, String windowTitle, 
                              EditorWatcher watcher, boolean compiled, 
                              Rectangle bounds)
    {
        MoeEditor editor;

        editor = new MoeEditor(windowTitle, isCode, watcher, showToolBar,
                               showLineNum, resources);
        editors.add(editor);
        if (editor.showFile(filename, compiled, docFilename, bounds))
            return editor;
        else {
            editor.doClose();			// editor will remove itself
            return null;
        }
    }

} // end class MoeEditorManager
