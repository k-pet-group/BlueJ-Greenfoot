/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.target;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import bluej.editor.*;
import bluej.pkgmgr.Package;

/**
 * A target in a package that can be edited as text
 *
 * @author  Michael Cahill
 */
public abstract class EditableTarget extends Target
    implements EditorWatcher
{
    protected Editor editor;
    protected Rectangle editorBounds;

    protected EditableTarget(Package pkg, String name)
    {
        super(pkg, name);
    }

    /**
     * @return the name of the (text) file this target corresponds to.
     */
    protected abstract File getSourceFile();

    /**
     * @return the editor object associated with this target
     */
    public abstract Editor getEditor();

    /**
     * Ensure that the source file of this target is up-to-date (i.e.
     * that any possible unsaved changes in an open editor window are 
     * saved).
     */
    public void ensureSaved() throws IOException
    {
        if(editor != null) {
            editor.save();
        }
    }
    
    /**
     * Called to open the editor for this target
     */
    public void open()
    {
        Editor editor = getEditor();

        if(editor == null)
            getPackage().showError("error-open-source");
        else
            editor.setVisible(true);
    }

    /**
     * Close the editor for this target.
     */
    protected void close()
    {
        getEditor().close();
    }

    /**
     * Return true if this editor has been opened at some point since this project was opened.
     */
    public boolean editorOpen()
    {
        return (editor!=null);
    }
    
    public void load(Properties props, String prefix)
    {
        super.load(props, prefix);
        try {
            if(props.getProperty(prefix + ".editor.x") != null) {
                editorBounds = new Rectangle(Integer.parseInt(props.getProperty(prefix + ".editor.x")),
                        Integer.parseInt(props.getProperty(prefix + ".editor.y")), 
                        Integer.parseInt(props.getProperty(prefix + ".editor.width")),
                        Integer.parseInt(props.getProperty(prefix + ".editor.height")));
            }
        }
        catch (NumberFormatException nfe) {
            // Corrupt properties file?
        }
    }

    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);
        if (editor != null) {
            editorBounds = editor.getBounds();            
        } 
        if(editorBounds!=null) {
            props.put(prefix + ".editor.x", String.valueOf((int) editorBounds.getX()));
            props.put(prefix + ".editor.y", String.valueOf((int) editorBounds.getY()));
            props.put(prefix + ".editor.width", String.valueOf((int) editorBounds.getWidth()));
            props.put(prefix + ".editor.height", String.valueOf((int) editorBounds.getHeight()));
        }
    }
    
    // --- EditorWatcher interface ---
    // (The EditorWatcher methods are typically redefined in subclasses)

    /*
     * Called by Editor when a file is changed
     */
    public void modificationEvent(Editor editor) {}

    /*
     * Called by Editor when a file is saved
     */
    public void saveEvent(Editor editor) {}

    /*
     * Called by Editor when a file is closed
     */
    public void closeEvent(Editor editor) {}

    /*
     * Called by Editor when a breakpoint is been set/cleared
     */
    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    { return null; }

    /*
     * The "compile" function was invoked in the editor
     */
    public void compile(Editor editor) {}

    // --- end of EditorWatcher interface ---
}
