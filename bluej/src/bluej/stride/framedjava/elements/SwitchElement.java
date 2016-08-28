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
import bluej.utility.Utility;
import nu.xom.Element;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaContainerDebugHandler;
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.frames.SwitchFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;
import threadchecker.OnThread;
import threadchecker.Tag;

public class SwitchElement extends ContainerCodeElement implements JavaSingleLineDebugHandler
{
    public static final String ELEMENT = "switch";
    private final FilledExpressionSlotFragment expression;
    private final List<CodeElement> casesContents;
    private final List<CodeElement> defaultContents;
    private SwitchFrame frame;
    
    public SwitchElement(SwitchFrame frame, FilledExpressionSlotFragment expression, List<? extends CodeElement> casesContents,
                         List<CodeElement> defaultContents, boolean enabled)
    {
        this.frame = frame;
        this.expression = expression;
        this.casesContents = new ArrayList<>(casesContents);
        this.casesContents.forEach(c -> c.setParent(this));

        this.defaultContents = defaultContents;
        if (this.defaultContents != null) {
            this.defaultContents.forEach(c -> c.setParent(this));
        }

        this.enable = enabled;
    }

    @Override
    public List<CodeElement> childrenUpTo(CodeElement c)
    {
        if (casesContents.contains(c)) {
            return casesContents.subList(0, casesContents.indexOf(c));
        }
        else if (defaultContents != null && defaultContents.contains(c)) {
            return defaultContents.subList(0, defaultContents.indexOf(c));
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public JavaSource toJavaSource()
    {
        List<JavaSource> contentsJavaSource = CodeElement.toJavaCodes(casesContents);
        if (defaultContents != null) {
            JavaContainerDebugHandler defaultHandler = debug -> { return frame.getDefaultDebug().showDebugAtEnd(debug);};
            contentsJavaSource.add(JavaSource.createCompoundStatement(frame, this, this, defaultHandler, Arrays.asList(f(frame, "default :")),
                    CodeElement.toJavaCodes(defaultContents)));
        }

        JavaContainerDebugHandler casesHandler = debug -> { return frame.getCasesDebug().showDebugAtEnd(debug);};
        return JavaSource.createCompoundStatement(frame, this, this, casesHandler, Arrays.asList(f(frame, "switch ("),
                expression, f(frame, ")")), contentsJavaSource);
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement switchEl = new LocatableElement(this, ELEMENT);
        switchEl.addAttributeStructured("expression", expression);
        addEnableAttribute(switchEl);

        Element casesEl = new Element("cases");
        casesContents.forEach(c -> casesEl.appendChild(c.toXML()));
        switchEl.appendChild(casesEl);

        // We only want a default if there is an defaultContents; empty is different to null:
        if (defaultContents != null) {
            Element defaultEl = new Element("default");
            defaultContents.forEach(c -> defaultEl.appendChild(c.toXML()));
            switchEl.appendChild(defaultEl);
        }

        return switchEl;
    }
    
    public SwitchElement(Element el)
    {
        expression = new FilledExpressionSlotFragment(el.getAttributeValue("expression"), el.getAttributeValue("expression-java"));
        casesContents = new ArrayList<>();
        Element casesEl = el.getChildElements("cases").get(0);
        for (int i = 0; i < casesEl.getChildElements().size(); i++) {
            final Element child = casesEl.getChildElements().get(i);
            CodeElement member = Loader.loadElement(child);
            casesContents.add(member);
            member.setParent(this);
        }

        if (el.getChildElements("default").size() == 1) {
            defaultContents = new ArrayList<>();
            Element defaultEl = el.getChildElements("default").get(0);
            for (int i = 0; i < defaultEl.getChildElements().size(); i++) {
                final Element child = defaultEl.getChildElements().get(i);
                CodeElement member = Loader.loadElement(child);
                defaultContents.add(member);
                member.setParent(this);
            }
        }
        else if (el.getChildElements("default").size() == 0) {
            defaultContents = null;
        }
        else {
            throw new IllegalArgumentException();
        }

        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new SwitchFrame(editor, expression, isEnable());
        casesContents.forEach(c -> frame.getCasesCanvas().insertBlockAfter(c.createFrame(editor), null));

        if (defaultContents != null) {
            frame.addDefault();
            defaultContents.forEach(c -> frame.getDefaultCanvas().insertBlockAfter(c.createFrame(editor), null));
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
        return Utility.concat(streamContained(casesContents), streamContained(defaultContents));
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.of(expression);
    }
}
