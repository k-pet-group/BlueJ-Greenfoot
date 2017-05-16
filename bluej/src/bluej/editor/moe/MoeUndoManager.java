/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015  Michael Kolling and John Rosenberg 
 
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
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.EventStream;
import org.reactfx.Guard;

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CompoundEdit;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * An undo/redo manager for the editor. A stack of compound edits is maintained;
 * the "beginCompoundEdit()" and "endCompoundEdit()" methods can be used to 
 * create a compound edit (which is treated as a single edit for undo/redo purposes).
 * 
 * @author Davin McCall
 */
public class MoeUndoManager implements UndoManagerFactory
{
    private final UndoManagerFactory delegate;
    private UndoManager undoManager;
    private final MoeEditor editor;
    private BooleanProperty canUndo;
    private BooleanProperty canRedo;

    public MoeUndoManager(MoeEditor editor)
    {
        this.editor = editor;
        delegate = UndoManagerFactory.fixedSizeHistoryFactory(100);
    }

    /**
     * Carry out the given edit and treat it as a single compound edit; listeners
     * for edits will only be informed of the final state not any intermediate states,
     * and the edit will not merge in the undo manager with other edits before or after.
     */
    public void compoundEdit(FXPlatformRunnable edit)
    {
        breakEdit();
        try(Guard currentEdit = ((EditableStyledDocument)editor.getSourcePane().getDocument()).beingUpdatedProperty().suspend())
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

    @Override
    public <C> UndoManager create(EventStream<C> eventStream, Function<? super C, ? extends C> function, Consumer<C> consumer)
    {
        undoManager = delegate.create(eventStream, function, consumer);
        return undoManager;
    }

    @Override
    public <C> UndoManager create(EventStream<C> eventStream, Function<? super C, ? extends C> function, Consumer<C> consumer, BiFunction<C, C, Optional<C>> biFunction)
    {
        undoManager = delegate.create(eventStream, function, consumer, biFunction);
        return undoManager;
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
