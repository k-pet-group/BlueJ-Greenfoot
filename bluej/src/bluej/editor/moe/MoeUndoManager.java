/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2018  Michael Kolling and John Rosenberg
 
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
package bluej.editor.moe;

import bluej.utility.javafx.FXPlatformRunnable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.fxmisc.richtext.util.UndoUtils;
import org.fxmisc.undo.UndoManager;

/**
 * An undo/redo manager for the editor. A stack of compound edits is maintained;
 * the "beginCompoundEdit()" and "endCompoundEdit()" methods can be used to 
 * create a compound edit (which is treated as a single edit for undo/redo purposes).
 * 
 * @author Davin McCall
 */
public class MoeUndoManager
{
    private UndoManager undoManager;
    private BooleanProperty canUndo;
    private BooleanProperty canRedo;

    public MoeUndoManager(MoeEditorPane editorPane)
    {
        // We uses a plain text undo manager instead of a rich text one.
        // This is to avoid making the undo manager to record the automatic
        // styling that MoeEditor performs.
        undoManager = UndoUtils.plainTextUndoManager(editorPane);
    }

    public UndoManager getUndoManager()
    {
        return undoManager;
    }


    /**
     * Runs the given edit action.  See comment within.
     */
    public void compoundEdit(FXPlatformRunnable edit)
    {
        breakEdit();
        // What we would like to do is carry out the given edit and treat it as a single compound edit; listeners
        // for edits would only be informed of the final state not any intermediate states,
        // and the edit will not merge in the undo manager with other edits before or after.

        // However, this is not what pausing the updates does.  The changes are paused, but they are then later played back
        // individually.  The document in the listener will be reflecting the final state of the document, not the
        // intermediate state at that point of change.  This means if you do an insert and delete during
        // the compound edit, the inserted change will get notified, even though the inserted text
        // is no longer there.  What we probably need is an update mechanism building on top of RichTextFX
        // (and perhaps an undo manager which can merge non-adjacent changes).  For now though, we just
        // avoid suspending the updates, and live with the fact that the updates will get played separately
        // (esp. since they wouldn't merge in the undo manager anyway)

        //try(Guard currentEdit = ((EditableStyledDocument)editor.getSourcePane().getDocument()).beingUpdatedProperty().suspend())
        {
            edit.run();
        }
        breakEdit();
    }
    
    public BooleanExpression canUndo()
    {
        if (canUndo == null)
        {
            canUndo = new SimpleBooleanProperty();
            canUndo.bind(undoManager.undoAvailableProperty());
        }
        return canUndo;
    }
    
    public BooleanExpression canRedo()
    {
        if (canRedo == null)
        {
            canRedo = new SimpleBooleanProperty();
            canRedo.bind(undoManager.redoAvailableProperty());
        }
        return canRedo;
    }
    
    public void undo()
    {
        undoManager.undo();
    }
    
    public void redo()
    {
        undoManager.redo();
    }

    public void forgetHistory()
    {
        undoManager.forgetHistory();
    }

    /**
     * Stops edits before this point merging with later edits into one single undoable item
     * (which is what happens by default).
     */
    public void breakEdit()
    {
        undoManager.preventMerge();
    }
}
