/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import bluej.utility.javafx.FXPlatformRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of undo and redo for the Document interface.
 * 
 * This class works on the basis of remembering the replacements, including old and new text,
 * then using these to perform the replacements (or inverse for undo).
 */
public class DocumentUndoStack implements DocumentListener
{
    private static class Change
    {
        private final int targetStartIncl;
        private final String replaced;
        private final String replacement;

        public Change(int targetStartIncl, String replaced, String replacement)
        {
            this.targetStartIncl = targetStartIncl;
            this.replaced = replaced;
            this.replacement = replacement;
        }
    }

    private final Document document;
    // The sublists are in order of application.  Each outer list member is a change that a single
    // undo/redo will perform, each inner list member is a set of overlapping or disjoint changes that
    // form a compount edit.  Various changes like auto-indent make many small changes that will all
    // be undone/redone as part of a group.
    private final ArrayList<ArrayList<Change>> rememberedChanges = new ArrayList<>();
    // The index of the next change that we would redo.
    private int changeIndex = 0;
    // Are we the ones doing the change as part of undo/redo?  Don't add it our remembered changes a second time.
    private boolean changeByUs = false;
    // Are we currently in the middle of a compound edit?
    private boolean inCompoundEdit = false;
    private FXPlatformRunnable stateListener = null;
    
    public DocumentUndoStack(Document document)
    {
        this.document = document;
        document.addListener(false, this);
    }

    @Override
    public void textReplaced(int origStartIncl, String original, String replacement, int linesRemoved, int linesAdded)
    {
        if (!changeByUs)
        {
            if (changeIndex != rememberedChanges.size())
            {
                // We have to chop off the later changes as we're making new ones from here:
                rememberedChanges.subList(changeIndex, rememberedChanges.size()).clear();
            }
            if (!inCompoundEdit)
            {
                rememberedChanges.add(new ArrayList<>());
                changeIndex += 1;
            }
            rememberedChanges.get(changeIndex - 1).add(new Change(origStartIncl, original, replacement));
            if (!inCompoundEdit && stateListener != null)
            {
                stateListener.run();
            }
        }
    }

    /**
     * How many changes can we undo at the moment?
     */
    public int canUndoCount()
    {
        return changeIndex;
    }

    /**
     * How many changes can we redo at the moment?
     */
    public int canRedoCount()
    {
        return rememberedChanges.size() - changeIndex;
    }

    /**
     * If possible, undo the most recent change.
     * 
     * @return the position at the end of the segment that was just changed (or -1 if nothing changed).
     */
    public int undo()
    {
        if (changeIndex > 0)
        {
            changeByUs = true;
            changeIndex -= 1;
            List<Change> changesToUndo = rememberedChanges.get(changeIndex);
            int latestPos = -1;
            // Undo in reverse order:
            for (int i = changesToUndo.size() - 1; i >= 0; i--)
            {
                Change change = changesToUndo.get(i);
                document.replaceText(change.targetStartIncl, change.targetStartIncl + change.replacement.length(), change.replaced);
                latestPos = change.targetStartIncl + change.replaced.length();
            }
            changeByUs = false;
            if (stateListener != null)
            {
                stateListener.run();
            }
            return latestPos;
        }
        return -1;
    }

    /**
     * If possible, redo the most recent undo.
     * 
     * @return the position at the end of the segment that was just changed (or -1 if nothing changed).
     */
    public int redo()
    {
        if (changeIndex < rememberedChanges.size())
        {
            changeByUs = true;
            List<Change> changes = rememberedChanges.get(changeIndex);
            changeIndex += 1;
            int latestPos = -1;
            for (Change change : changes)
            {
                document.replaceText(change.targetStartIncl, change.targetStartIncl + change.replaced.length(), change.replacement);
                latestPos = change.targetStartIncl + change.replacement.length();
            }
            changeByUs = false;
            if (stateListener != null)
            {
                stateListener.run();
            }
            return latestPos;
        }
        return -1;
    }
    
    /**
     * Sets a listener to be called whenever the undo/redo potential of this undo stack changes.
     */
    public void setStateListener(FXPlatformRunnable stateListener)
    {
        this.stateListener = stateListener;
    }

    /**
     * Clears the undo/redo history.
     */
    public void clear()
    {
        rememberedChanges.clear();
        changeIndex = 0;
        if (stateListener != null)
        {
            stateListener.run();
        }
    }


    
    public void compoundEdit(FXPlatformRunnable editAction)
    {
        if (changeIndex != rememberedChanges.size())
        {
            // We have to chop off the later changes as we're making new ones from here:
            rememberedChanges.subList(changeIndex, rememberedChanges.size()).clear();
        }
        
        inCompoundEdit = true;
        ArrayList<Change> compoundChanges = new ArrayList<>();
        rememberedChanges.add(compoundChanges);
        changeIndex += 1;
        editAction.run();
        inCompoundEdit = false;
        if (compoundChanges.isEmpty())
        {
            // Nothing happened!  Forget about it
            rememberedChanges.remove(rememberedChanges.size() - 1);
            changeIndex -= 1;
        }
        else if (stateListener != null)
        {
            stateListener.run();
        }
    }
}
