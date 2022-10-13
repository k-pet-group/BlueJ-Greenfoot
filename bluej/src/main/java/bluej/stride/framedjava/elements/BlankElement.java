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
package bluej.stride.framedjava.elements;

import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import nu.xom.Element;
import bluej.stride.framedjava.ast.FrameFragment;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.frames.BlankFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class BlankElement extends CodeElement
{
    public static final String ELEMENT = "blank";

    public BlankElement()
    {
    }
    
    public BlankElement(Element el)
    {
    }

    @Override
    public JavaSource toJavaSource()
    {
        return new JavaSource(null, new FrameFragment(null, this, ""));
    }

    @Override
    public LocatableElement toXML()
    {
        return new LocatableElement(this, ELEMENT);
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        return new BlankFrame(editor);
    }

    @Override
    public void show(ShowReason reason)
    {
        // Should not be called on BlankElement
        throw new IllegalStateException();
    }

    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.empty();
    }
}
