/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015 Michael Kölling and John Rosenberg 
 
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

import java.security.InvalidParameterException;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.JavaFragment.Destination;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.Frame;
import bluej.stride.slots.EditableSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

public class FrameFragment extends JavaFragment
{
    private Frame frame;
    private final String content;
    
    public FrameFragment(Frame frame, String content)
    {
        this.frame = frame;
        this.content = content;
        
        if (content == null)
        {
            throw new InvalidParameterException("FrameFragment content cannot be null");
        }
        else if (content.contains("\n"))
        {
            throw new IllegalStateException("FrameFragment content contains newline");
        }
    }
    
    @Override
    protected String getJavaCode(Destination dest, ExpressionSlot<?> completing)
    {
        return content;
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        return Stream.empty();
    }

    @Override
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

    public void setFrame(Frame frame)
    {
        this.frame = frame;
    }
}
