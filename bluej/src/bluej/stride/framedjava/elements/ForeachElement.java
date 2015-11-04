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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import nu.xom.Attribute;
import nu.xom.Element;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaContainerDebugHandler;
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.frames.ForeachFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class ForeachElement extends ContainerCodeElement
  implements JavaSingleLineDebugHandler, JavaContainerDebugHandler
{
    public static final String ELEMENT = "foreach";
    private final TypeSlotFragment type;
    private final NameDefSlotFragment var;
    private final FilledExpressionSlotFragment collection;
    private final List<CodeElement> contents;
    private ForeachFrame frame;

    public ForeachElement(ForeachFrame frame, TypeSlotFragment type, NameDefSlotFragment var, FilledExpressionSlotFragment collection, List<CodeElement> contents, 
            boolean enabled)
    {
        this.frame = frame;
        this.type = type;
        this.var = var;
        this.collection = collection;
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
        return JavaSource.createCompoundStatement(frame, this, this, Arrays.asList(f(frame, "for (final "), type, space(), var, f(frame, " : "), collection, 
                f(frame, ")")), CodeElement.toJavaCodes(contents));
    }

    @Override
    public Element toXML()
    {
        Element loopEl = new Element(ELEMENT);
        loopEl.addAttribute(new Attribute("type", type.getContent()));
        loopEl.addAttribute(new Attribute("var", var.getContent()));
        loopEl.addAttribute(new Attribute("collection", collection.getContent()));
        loopEl.addAttribute(new Attribute("collection-java", collection.getJavaCode()));
        addEnableAttribute(loopEl);
        for (CodeElement c : contents)
        {
            loopEl.appendChild(c.toXML());
        }
        return loopEl;
    }
    
    public ForeachElement(Element el)
    {
        type = new TypeSlotFragment(el.getAttributeValue("type"));
        var = new NameDefSlotFragment(el.getAttributeValue("var"));
        collection = new FilledExpressionSlotFragment(el.getAttributeValue("collection"), el.getAttributeValue("collection-java"));
        contents = new ArrayList<CodeElement>();
        for (int i = 0; i < el.getChildElements().size(); i++)
        {
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
        frame = new ForeachFrame(editor, type, var, collection, isEnable());
        for (CodeElement c : contents)
        {
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
    public HighlightedBreakpoint showDebugAtEnd(DebugInfo debug)
    {
        return frame.showDebugAtEnd(debug);
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
        return Stream.of(type, var, collection);
    }
    
    @Override
    public List<LocalParamInfo> getDeclaredVariablesWithin()
    {
        return Collections.singletonList(new LocalParamInfo(type.getContent(), var.getContent(), false, this));
    }
}
