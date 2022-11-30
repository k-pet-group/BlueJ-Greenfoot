/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017 Michael Kölling and John Rosenberg
 
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
package bluej.stride.operations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import bluej.stride.generic.FrameState;
import bluej.utility.javafx.FXRunnable;

/**
 * An undo/redo manager for the frame editor. A stack of farme states is maintained;
 * the "beginFrameState()" and "endFrameState()" methods can be used to 
 * create a frame state  (which is treated as a single state for undo/redo purposes).
 * 
 * @author Amjad Altadmri
 */
public class UndoRedoManager
{
    private int current;
    private boolean recording = false;
    private boolean restoring = false;
    private final List<FrameState> statesStack = new LinkedList<>();
    private final List<FXRunnable> listeners = new ArrayList<>();
    // TODO Add it to the defs file
    private static final int MAX_CAPACITY = 30;

    public UndoRedoManager(FrameState initialState)
    {
        current = 0;
        statesStack.add(initialState);
    }

    private void addState(FrameState state)
    {
        if (!restoring) {
            boolean newState = false;
            // If the new state equals the current one, don't add it again,
            // replace instead so that cursor position is updated
            if (statesStack.size() > 0 && state.equals(statesStack.get(current))) {
                statesStack.set(current, state);
            }
            else {
                newState = true;
                // Remove all old states that been reverted and can't be reached any more
                while (canRedo()) {
                    statesStack.remove(statesStack.size() - 1);
                }
                statesStack.add(state);
                current++;
                if (statesStack.size() > MAX_CAPACITY) {
                    current--;
                    statesStack.remove(0);
                }
            }
            runListeners();
        }
    }
    
    public void beginFrameState(FrameState state)
    {
        recording = true;
        addState(state);
    }

    public void endFrameState(FrameState state)
    {
        recording = false;
        addState(state);
    }

    public boolean canUndo()
    {
        return current > 0;
    }

    public boolean canRedo()
    {
        return current < statesStack.size() - 1;
    }

    public FrameState undo()
    {
        recording = false;
        if ( canUndo() ) {
            current--;
            runListeners();
            return statesStack.get(current);
        }
        return null;
    }

    private void runListeners()
    {
        // Take copy to allow removal by listeners:
        ArrayList<FXRunnable> listenersCopy = new ArrayList<>(listeners);
        listenersCopy.forEach(FXRunnable::run);
    }

    public FrameState redo()
    {
        recording = false;
        if ( canRedo() ) {
            current++;
            runListeners();
            return statesStack.get(current);
        }
        return null;
    }

    public boolean isRecording()
    {
        return recording;
    }

    public void startRestoring()
    {
        restoring = true;
    }

    public void stopRestoring()
    {
        restoring = false;
    }

    public FrameState getCurrent()
    {
        return statesStack.get(current);
    }

    public void addListener(FXRunnable listChangeListener)
    {
        listeners.add(listChangeListener);
    }

    public void removeListener(FXRunnable listChangeListener)
    {
        listeners.remove(listChangeListener);
    }

    /**
     * Matches exact reference, not just state.  Must be possible within that many undos,
     * but not be the current state.
     */
    public boolean canUndoToReference(FrameState restoreTarget, int withinNumUndos)
    {
        int index = -1;
        // Can't use indexOf because we want to match reference:
        for (int i = 0; i < statesStack.size(); i++)
        {
            if (statesStack.get(i) == restoreTarget)
            {
                index = i;
                break;
            }
        }
        if (index == -1)
            return false;
        if (index < current && current - index <= withinNumUndos)
            return true;
        return false;
    }
}
