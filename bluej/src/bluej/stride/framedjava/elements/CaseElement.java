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
import nu.xom.Element;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.frames.CaseFrame;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class CaseElement extends ContainerCodeElement implements JavaSingleLineDebugHandler
{
    public static final String ELEMENT = "case";
    private final FilledExpressionSlotFragment expression;
    private final List<CodeElement> contents;
    private CaseFrame frame;
    
    public CaseElement(CaseFrame frame, FilledExpressionSlotFragment condition, List<CodeElement> contents, boolean enabled)
    {
        this.frame = frame;
        this.expression = condition;
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
        return JavaSource.createCompoundStatement(frame, this, this, null, Arrays.asList(f(frame, "case "),  expression, f(frame, " :")), CodeElement.toJavaCodes(contents));
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement caseEl = new LocatableElement(this, ELEMENT);
        caseEl.addAttributeStructured("expression", expression);
        addEnableAttribute(caseEl);
        for (CodeElement c : contents) {
            caseEl.appendChild(c.toXML());
        }
        return caseEl;
    }
    
    public CaseElement(Element el)
    {
        expression = new FilledExpressionSlotFragment(el.getAttributeValue("expression"), el.getAttributeValue("expression-java"));
        contents = new ArrayList<CodeElement>();
        for (int i = 0; i < el.getChildElements().size(); i++) {
            final Element child = el.getChildElements().get(i);
            CodeElement member = Loader.loadElement(child);
            contents.add(member);
            member.setParent(this);
        }
        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new CaseFrame(editor, expression, isEnable());
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
        return Stream.of(expression);
    }
}
