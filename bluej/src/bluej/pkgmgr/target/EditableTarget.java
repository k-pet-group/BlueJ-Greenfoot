/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2016,2017,2018,2020  Michael Kolling and John Rosenberg
 
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

import java.io.File;
import java.io.IOException;

import bluej.Config;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.actions.EditableTargetOperation;
import bluej.prefmgr.PrefMgrDialog;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A target in a package that can be edited as text
 *
 * @author  Michael Cahill
 */
public abstract class EditableTarget extends Target
    implements EditorWatcher
{
    public static final String MENU_STYLE_INBUILT = "class-action-inbuilt";
    public final static String editStr = Config.getString("pkgmgr.classmenu.edit");
    public final static String removeStr = Config.getString("pkgmgr.classmenu.remove");
    protected Editor editor;

    protected EditableTarget(Package pkg, String name, String accessibleTargetType)
    {
        super(pkg, name, accessibleTargetType);
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
    @OnThread(Tag.FXPlatform)
    public void open()
    {
        Editor editor = getEditor();

        if(editor == null)
            getPackage().showError("error-open-source");
        else
            editor.setEditorVisible(true, false);
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
    public String breakpointToggleEvent(int lineNo, boolean set)
    { return null; }

    public void clearAllBreakpoints() { }

    @Override
    public void showPreferences(int paneIndex)
    {
        PrefMgrDialog.showDialog(getPackage().getProject(), paneIndex);
    }

    // --- end of EditorWatcher interface ---

}
