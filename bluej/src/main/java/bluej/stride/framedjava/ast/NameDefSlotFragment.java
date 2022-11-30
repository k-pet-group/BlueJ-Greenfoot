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

import java.util.stream.Stream;

import bluej.stride.framedjava.errors.EmptyError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.errors.UnneededSemiColonError;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.slots.TextSlot;

public class NameDefSlotFragment extends TextSlotFragment
{
    private TextSlot<NameDefSlotFragment> slot;

    public NameDefSlotFragment(String content, TextSlot<NameDefSlotFragment> slot)
    {
        super(content);
        this.slot = slot;
    }
    
    public NameDefSlotFragment(String content)
    {
        this(content, null);
    }
    
    // Copy constructor
    public NameDefSlotFragment(StringSlotFragment f)
    {
        this(f.getContent());
    }

    @Override
    public String getJavaCode(Destination dest, ExpressionSlot<?> completing, Parser.DummyNameGenerator dummyNameGenerator)
    {
        if (!dest.substitute() || (content != null && Parser.parseableAsNameDef(content)))
            return content;
        else
        // This one may be sensitive to Java compiler implementation as to where the error is reported.
        // But at least in 8u111 it reports on the #, which is within the slot, which is what we want:
            return "invalid#";
    }
    
    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        if (content != null && content.isEmpty())
            return Stream.of(new EmptyError(this, "Name cannot be empty"));
        else if (content != null && content.endsWith(";"))
            // Must check this before general parse errors:
            return Stream.of(new UnneededSemiColonError(this, () -> getSlot().setText(content.substring(0, content.length() - 1))));
        else if (content == null || !Parser.parseableAsNameDef(content))
            return Stream.of(new SyntaxCodeError(this, "Invalid name"));
        
        // TODO look for unknown types
        return Stream.empty();
    } 

    @Override
    public TextSlot<NameDefSlotFragment> getSlot()
    {
        return slot;
    }

    @Override
    public void registerSlot(TextSlot slot)
    {
        if (this.slot == null)
            this.slot = slot;
    }
}
