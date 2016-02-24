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
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.frames.ConstructorFrame;
import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.slots.ChoiceSlot;

public class SuperThisFragment extends ChoiceSlotFragment
{
    private final SuperThis value;
    private final ExpressionSlot paramsSlot;
    private ChoiceSlot<SuperThis> slot;
    
    public SuperThisFragment(SuperThis a)
    {
        super(null);
        this.value = a;
        this.paramsSlot = null;
    }
    
    @OnThread(Tag.FX)
    public SuperThisFragment(ConstructorFrame f, ChoiceSlot<SuperThis> s, ExpressionSlot superThisParamsSlot)
    {
        super(f);
        this.value = s.getValue(SuperThis.EMPTY);
        this.paramsSlot = superThisParamsSlot;
        this.slot = s;
    }

    public SuperThis getValue()
    {
        return value;
    }

    @Override
    public String getJavaCode(Destination dest, ExpressionSlot<?> completing, Parser.DummyNameGenerator dummyNameGenerator)
    {
        return value.getJavaCode();
    }

    public void registerSlot(ChoiceSlot<SuperThis> slot)
    {
        this.slot = slot;
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        if (value == null || value == SuperThis.EMPTY)
        {
            return Stream.of(new SyntaxCodeError(this, "Cannot be empty; must be super or this"));
        }
        else
        {
            return Stream.empty();
        }
    }

    public Future<List<CodeError>> findLateErrors(InteractionManager editor, CodeElement parent)
    {
        return null;
    }

    @Override
    protected @OnThread(Tag.FX) JavaFragment getCompileErrorRedirect()
    {
        if (paramsSlot != null)
            return paramsSlot.getSlotElement();
        else
            return super.getCompileErrorRedirect();
    }

    @Override
    public @OnThread(Tag.FX) ErrorShower getErrorShower()
    {
        if (paramsSlot != null)
            return paramsSlot;
        else
            return super.getErrorShower();
    }
}
