/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
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
package bluej.editor;

import java.awt.Rectangle;
import java.nio.charset.Charset;

import bluej.editor.moe.MoeEditorManager;
import bluej.parser.entity.EntityResolver;
import bluej.pkgmgr.JavadocResolver;

/**
 * Interface between the editor manager and the rest of BlueJ.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Bruce Quig
 * @version $Id: EditorManager.java 8887 2011-04-21 03:29:25Z davmac $
 */
public abstract class EditorManager
{

    private static EditorManager theEditorManager = new MoeEditorManager();

    /**
     * Singleton factory method to return an EditorManager instance;
     *
     * @returns the singleton EditorManager instance
     */
    public static EditorManager getEditorManager()
    {
        return theEditorManager;
    }

    /**
     * Open an editor to display a class. The filename may be "null"
     * to open an empty editor (e.g. for displaying a view). The editor
     * is initially hidden; a call to "Editor.show()" is needed to make
     * it visible after opening it.
     *
     * @param filename     name of the source file to open (may be null)
     * @param docFilename  name of the corresponding javadoc file
     * @param charset      the character set of the file contents 
     * @param windowTitle  title of window (usually class name)
     * @param watcher      an watcher to be notified of edit events
     * @param compiled     true, if the class has been compiled
     * @param bounds       the bounds of the window to appear on screen
     * @param projectResolver   A resolver for external symbols
     * @param javadocResolver   A resolver for javadoc on external methods
     * 
     * @return          the new editor, or null if there was a problem
     */
    public abstract Editor openClass(String filename, 
        String docFilename,
        Charset charset,
        String windowTitle, 
        EditorWatcher watcher, 
        boolean compiled, 
        Rectangle bounds,
        EntityResolver projectResolver,
        JavadocResolver javadocResolver);


    /**
     * Open an editor to display a text document. The difference to
     * "openClass" is that code specific functions (such as compile,
     * debug, view) are disabled in the editor. The filename may be
     * "null" to open an empty editor. The editor is initially hidden.
     * A call to "Editor::show" is needed to make is visible after
     * opening it.
     *
     * @param filename          name of the source file to open (may be null)
     * @param windowTitle       title of window (usually class name)
     * @param watcher           an object interested in editing events
     * @returns                 the new editor, or null if there was a problem
     */
    public abstract Editor openText(String filename, Charset charset, String windowTitle,
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
