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

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.slots.ChoiceSlot;

/**
 * Construct the class using the static helper methods
 *
 */
public class AccessPermissionFragment extends ChoiceSlotFragment
{
    private final AccessPermission value;
    private ChoiceSlot<AccessPermission> slot;
    
    public AccessPermissionFragment(AccessPermission a)
    {
        super(null);
        value = a;
    }
    
    @OnThread(Tag.FX)
    public AccessPermissionFragment(Frame f, ChoiceSlot<AccessPermission> s)
    {
        super(f);
        this.value =  s.getValue(AccessPermission.EMPTY);
        this.slot = s;
    }

    public AccessPermission getValue()
    {
        return value;
    }
    
    /**
     * Gets the content of the slot, as a String.
     */
    public String getContent()
    {
        return value.toString();
    }

    @Override
    public String getJavaCode(Destination dest, ExpressionSlot<?> completing, Parser.DummyNameGenerator dummyNameGenerator)
    {
        return value.getJavaCode();
    }

    public void registerSlot(ChoiceSlot<AccessPermission> slot)
    {
        this.slot = slot;
    }
    
    public ChoiceSlot<AccessPermission> getSlot()
    {
        return slot;
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        return Stream.empty();
    }
}
