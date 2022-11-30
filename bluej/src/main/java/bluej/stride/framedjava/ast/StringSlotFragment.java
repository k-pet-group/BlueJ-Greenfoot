/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.ast;

import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.slots.EditableSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

public abstract class StringSlotFragment extends SlotFragment
{
    protected final String content;
    
    public StringSlotFragment(String content)
    {
        this.content = content;
    
        if (content == null) {
            throw new IllegalArgumentException("SlotElement content cannot be null");
        }
        else if (content.contains("\n")) {
            throw new IllegalStateException("SlotElement content contains newline");
        }
    }

    public abstract EditableSlot getSlot();

    @Override
    public ErrorShower getErrorShower()
    {
        return getSlot();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public final void addError(CodeError error)
    {
        if (getSlot() != null)
            getSlot().addError(error);
    }

    @Override
    protected final JavaFragment getCompileErrorRedirect()
    {
        // SlotFragments should not override:
        return null;
    }

    /**
     * Gets the content of the slot, as it should be displayed in the editor.
     */
    public String getContent()
    {
        return content;
    }

    public boolean isEmpty()
    {
        return content.isEmpty();
    }
}
