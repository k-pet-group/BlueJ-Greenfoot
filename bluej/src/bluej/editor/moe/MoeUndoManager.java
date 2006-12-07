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
    LinkedList editStack;
    UndoManager undoManager;
    CompoundEdit currentEdit;
    MoeEditor editor;
    
    public MoeUndoManager(MoeEditor editor)
    {
        this.editor = editor;
        undoManager = new UndoManager();
        currentEdit = undoManager;
        editStack = new LinkedList();
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
