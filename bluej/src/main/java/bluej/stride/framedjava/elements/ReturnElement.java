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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import nu.xom.Attribute;
import nu.xom.Element;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.frames.ReturnFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class ReturnElement extends CodeElement implements JavaSingleLineDebugHandler
{
    public static final String ELEMENT = "return";
    private final OptionalExpressionSlotFragment val;
    private ReturnFrame frame;
    
    // val is optional and can be null
    public ReturnElement(ReturnFrame frame, OptionalExpressionSlotFragment val, boolean enabled)
    {
        this.frame = frame;
        this.val = val;
        this.enable = enabled;
    }

    @Override
    public JavaSource toJavaSource()
    {
        List<JavaFragment> fragments = new ArrayList<JavaFragment>();
        fragments.add(f(frame, "return"));
        if (val != null) {
            fragments.addAll(Arrays.asList(space(), val));
        }        
        fragments.add(f(frame, ";"));
        return new JavaSource(this, fragments);
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement retEl = new LocatableElement(this, ELEMENT);
        if (val != null) {
            retEl.addAttributeStructured("value", val);
        }
        addEnableAttribute(retEl);
        return retEl;
    }
    
    public ReturnElement(Element el)
    {
        Attribute valueAttribute = el.getAttribute("value");
        val = (valueAttribute == null) ? null : new OptionalExpressionSlotFragment(valueAttribute.getValue(), el.getAttributeValue("value-java"));
        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new ReturnFrame(editor, val, isEnable());
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
        return val == null ? Stream.empty() : Stream.of(val);
    }
}
