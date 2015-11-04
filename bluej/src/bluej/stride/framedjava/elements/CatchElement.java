/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.frames.CatchFrame;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class CatchElement extends ContainerCodeElement implements JavaSingleLineDebugHandler
{
    public static final String ELEMENT = "catch";
    private final TypeSlotFragment exceptionTypeFragment;
    private final NameDefSlotFragment exceptionNameFragment;
    private final List<CodeElement> contents;
    private CatchFrame frame;
    
    public CatchElement(CatchFrame frame, TypeSlotFragment exceptionTypeFragment, NameDefSlotFragment exceptionNameFragment, List<CodeElement> contents, boolean enabled)
    {
        this.frame = frame;
        this.exceptionTypeFragment = exceptionTypeFragment;
        this.exceptionNameFragment = exceptionNameFragment;
        this.contents = contents;
        this.enable = enabled;
        contents.forEach(c -> c.setParent(this));
    }

    @Override
    public List<CodeElement> childrenUpTo(CodeElement c)
    {
        return contents.subList(0, contents.indexOf(c));
    }

    @Override
    public JavaSource toJavaSource()
    {
        return JavaSource.createCompoundStatement(frame, this, null, Arrays.asList(f(frame, "catch ("),
                exceptionTypeFragment, f(frame, " "), exceptionNameFragment, f(frame, ")")), CodeElement.toJavaCodes(contents));
    }

    @Override
    public Element toXML()
    {
        Element catchEl = new Element(ELEMENT);
        catchEl.addAttribute(new Attribute("type", exceptionTypeFragment.getContent()));
        catchEl.addAttribute(new Attribute("name", exceptionNameFragment.getContent()));
        addEnableAttribute(catchEl);
        for (CodeElement c : contents) {
            catchEl.appendChild(c.toXML());
        }
        return catchEl;
    }

    public CatchElement(Element el)
    {
        exceptionTypeFragment = new TypeSlotFragment(el.getAttributeValue("type"));
        exceptionNameFragment = new NameDefSlotFragment(el.getAttributeValue("name"));
        contents = new ArrayList<>();
        for (int i = 0; i < el.getChildElements().size(); i++) {
            final Element child = el.getChildElements().get(i);
            CodeElement member = Loader.loadElement(child);
            contents.add(member);
            member.setParent(this);
        }
        enable = new Boolean(el.getAttributeValue("enable"));
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new CatchFrame(editor, exceptionTypeFragment, exceptionNameFragment, isEnable());
        for (CodeElement c : contents) {
            frame.getCanvas().insertBlockAfter(c.createFrame(editor), null);
        }
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
    public Stream<CodeElement> streamContained()
    {
        return streamContained(contents);
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.of(exceptionTypeFragment, exceptionNameFragment);
    }
}
