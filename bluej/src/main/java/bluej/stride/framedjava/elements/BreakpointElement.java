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
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.frames.BreakpointFrame;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class BreakpointElement extends CodeElement implements JavaSingleLineDebugHandler
{
    public static final String ELEMENT = "breakpoint";
    private BreakpointFrame frame;

    public BreakpointElement(BreakpointFrame frame, boolean enabled)
    {
        this.frame = frame;
        this.enable = enabled;
    }
    
    @Override
    public JavaSource toJavaSource()
    {
        return JavaSource.createBreakpoint(frame, this, this);
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement breakpointEl = new LocatableElement(this, ELEMENT);
        addEnableAttribute(breakpointEl);
        return breakpointEl;
    }
    
    public BreakpointElement(Element el)
    {
        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new BreakpointFrame(editor, isEnable());
        return frame;
    }

    @Override
    public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
    {
        return frame.showDebugBefore(debug);
    }
        
    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }

    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.empty();
    }  
}
