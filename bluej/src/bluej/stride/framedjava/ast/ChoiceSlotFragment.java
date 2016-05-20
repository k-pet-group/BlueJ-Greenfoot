/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016  Michael Kolling and John Rosenberg
 
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
import bluej.stride.generic.Frame;
import bluej.stride.slots.EditableSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A fragment of program code which is stored in a choice slot.
 * 
 * Needs to be overridden by subclasses to actually store the value in question,
 * but provides an implementation of a lot of methods from SlotFragment, suitable for
 * items which were stored in a choice slot.
 */
public abstract class ChoiceSlotFragment extends SlotFragment
{
    private final Frame frame;

    protected ChoiceSlotFragment(Frame f)
    {
        this.frame = f;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void addError(CodeError codeError)
    {
        frame.addError(codeError);
    }

    @Override
    public ErrorRelation checkCompileError(int startLine, int startColumn, int endLine, int endColumn)
    {
        if (frame == null)
            return ErrorRelation.CANNOT_SHOW;
        else
            return super.checkCompileError(startLine, startColumn, endLine, endColumn);
    }

    @Override
    @OnThread(Tag.FX)
    protected JavaFragment getCompileErrorRedirect()
    {
        EditableSlot slot = frame.getErrorShowRedirect();
        if (slot != null)
            return slot.getSlotElement();
        else
            return this;
    }

    @Override
    @OnThread(Tag.FX)
    public ErrorShower getErrorShower()
    {
        EditableSlot slot = frame.getErrorShowRedirect();
        if (slot != null)
            return slot;
        else
            return frame;
    }
}
