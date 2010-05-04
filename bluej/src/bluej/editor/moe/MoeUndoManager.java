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
package bluej.editor.moe;

import java.util.LinkedList;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * An undo/redo manager for the editor. A stack of compound edits is maintained;
 * the "beginCompoundEdit()" and "endCompoundEdit()" methods can be used to 
 * create a compound edit (which is treated as a single edit for undo/redo purposes).
 * 
 * @author Davin McCall
 */
public class MoeUndoManager implements UndoableEditListener
{
    LinkedList<CompoundEdit> editStack;
    UndoManager undoManager;
    CompoundEdit currentEdit;
    MoeEditor editor;
    
    public MoeUndoManager(MoeEditor editor)
    {
        this.editor = editor;
        undoManager = new UndoManager();
        currentEdit = undoManager;
        editStack = new LinkedList<CompoundEdit>();
    }
    
    public void undoableEditHappened(UndoableEditEvent e)
    {
        addEdit(e.getEdit());
    }
    
    public void addEdit(UndoableEdit edit)
    {
        currentEdit.addEdit(edit);
        if (currentEdit == undoManager) {
            editor.updateUndoControls();
            editor.updateRedoControls();
        }
    }
    
    public void beginCompoundEdit()
    {
        editStack.add(currentEdit);
        currentEdit = new CompoundEdit();
    }
    
    public void endCompoundEdit()
    {
        currentEdit.end();
        CompoundEdit lastEdit = (CompoundEdit) editStack.removeLast();
        lastEdit.addEdit(currentEdit);
        currentEdit = lastEdit;
        
        if (currentEdit == undoManager) {
            editor.updateUndoControls();
            editor.updateRedoControls();
        }
    }
    
    public boolean canUndo()
    {
        return undoManager.canUndo();
    }
    
    public boolean canRedo()
    {
        return undoManager.canRedo();
    }
    
    public void undo()
    {
        undoManager.undo();
    }
    
    public void redo()
    {
        undoManager.redo();
    }
}
