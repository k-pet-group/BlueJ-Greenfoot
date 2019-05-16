/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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

import java.util.ArrayList;

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
    private final ArrayList<Change> rememberedChanges = new ArrayList<>();
    // The index of the next change that we would redo.
    private int changeIndex = 0;
    // Are we the ones doing the change as part of undo/redo?  Don't add it our remembered changes a second time.
    private boolean changeByUs = false;
    
    public DocumentUndoStack(Document document)
    {
        this.document = document;
        document.addListener(this);
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
            rememberedChanges.add(new Change(origStartIncl, original, replacement));
            changeIndex += 1;            
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
     */
    public void undo()
    {
        if (changeIndex > 0)
        {
            changeByUs = true;
            changeIndex -= 1;
            Change change = rememberedChanges.get(changeIndex);
            document.replaceText(change.targetStartIncl, change.targetStartIncl + change.replacement.length(), change.replaced);
            changeByUs = false;
        }
    }

    /**
     * If possible, redo the most recent undo.
     */
    public void redo()
    {
        if (changeIndex < rememberedChanges.size())
        {
            changeByUs = true;
            Change change = rememberedChanges.get(changeIndex);
            changeIndex += 1;
            document.replaceText(change.targetStartIncl, change.targetStartIncl + change.replaced.length(), change.replacement);
            changeByUs = false;
        }
    }
}
