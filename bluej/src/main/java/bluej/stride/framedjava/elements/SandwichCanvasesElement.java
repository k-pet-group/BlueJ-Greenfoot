/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2018 Michael Kölling and John Rosenberg
 
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaContainerDebugHandler;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SandwichCanvasesFrame;
import bluej.utility.Utility;

import nu.xom.Element;
import nu.xom.Elements;
import threadchecker.OnThread;
import threadchecker.Tag;


public abstract class SandwichCanvasesElement extends ContainerCodeElement implements JavaSingleLineDebugHandler
{
    private SandwichCanvasesFrame frame;

    private final String frameCaption;
    private final String intermediateCanvasElement;
    private final String intermediateCanvasJavaCaption;
    private final String tailCanvasCaption;

    private List<CodeElement> firstCanvasContents;
    private List<List<CodeElement>> intermediateCanvasContents;
    private List<CodeElement> tailCanvasContents;


    protected SandwichCanvasesElement(String frameCaption, String intermediateCanvasElement,
                                      String intermediateCanvasJavaCaption, String tailCanvasCaption)
    {
        this.frameCaption = frameCaption;
        this.intermediateCanvasElement = intermediateCanvasElement;
        this.intermediateCanvasJavaCaption = intermediateCanvasJavaCaption;
        this.tailCanvasCaption = tailCanvasCaption;
    }

    /**
     *
     * @param frame
     * @param firstCanvasContents
     * @param intermediateCanvasContents
     * @param tailCanvasContents Note that passing null here means no tailCanvas, whereas passing
     *               an empty list indicates that there is a tailCanvas, but it is empty.
     */
    protected SandwichCanvasesElement(SandwichCanvasesFrame frame, String frameCaption, List<CodeElement> firstCanvasContents,
                  String intermediateCanvasElement, String intermediateCanvasJavaCaption, List<List<CodeElement>> intermediateCanvasContents,
                  String tailCanvasCaption, List<CodeElement> tailCanvasContents, boolean enabled)
    {
        this(frameCaption, intermediateCanvasElement, intermediateCanvasJavaCaption, tailCanvasCaption);
        this.frame = frame;

        this.firstCanvasContents = firstCanvasContents;
        this.firstCanvasContents.forEach(c -> c.setParent(this));

        this.intermediateCanvasContents = intermediateCanvasContents;
        this.intermediateCanvasContents.forEach(cs -> cs.forEach(c -> c.setParent(this)));

        this.tailCanvasContents = tailCanvasContents;
        if (this.tailCanvasContents != null) {
            this.tailCanvasContents.forEach(c -> c.setParent(this));
        }

        this.enable = enabled;
    }

    public void loadElement(Element el)
    {
        loadMainAttributes(el);
        firstCanvasContents = new ArrayList<>();
        Element firstStatementsEl = el.getChildElements(frameCaption + "Statements").get(0);
        for (int i = 0; i < firstStatementsEl.getChildElements().size(); i++) {
            final Element child = firstStatementsEl.getChildElements().get(i);
            CodeElement member = Loader.loadElement(child);
            firstCanvasContents.add(member);
            member.setParent(this);
        }

        intermediateCanvasContents = new ArrayList<>();
        Elements intermediateCanvasElements = el.getChildElements(intermediateCanvasElement);
        for (int i = 0; i < intermediateCanvasElements.size(); i++) {
            final Element intermediateEl = intermediateCanvasElements.get(i);
            loadIntermediateAttributes(intermediateEl);
            List<CodeElement> body = new ArrayList<>();
            for (int j = 0; j < intermediateEl.getChildElements().size(); j++)
            {
                CodeElement member = Loader.loadElement(intermediateEl.getChildElements().get(j));
                body.add(member);
                member.setParent(this);
            }
            intermediateCanvasContents.add(body);
        }

        if (el.getChildElements(tailCanvasCaption).size() == 1) {
            tailCanvasContents = new ArrayList<>();
            Element tailCanvasEl = el.getChildElements(tailCanvasCaption).get(0);
            for (int i = 0; i < tailCanvasEl.getChildElements().size(); i++) {
                final Element child = tailCanvasEl.getChildElements().get(i);
                CodeElement member = Loader.loadElement(child);
                tailCanvasContents.add(member);
                member.setParent(this);
            }
        }
        else if (el.getChildElements(tailCanvasCaption).size() == 0) {
            tailCanvasContents = null;
        }
        else {
            throw new IllegalArgumentException();
        }

        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }

    protected abstract void loadMainAttributes(final Element element);

    protected abstract void loadIntermediateAttributes(final Element element);

    @Override
    public JavaSource toJavaSource()
    {
        JavaContainerDebugHandler firstCanvasHandler = debug -> frame.getFirstCanvasDebug().showDebugAtEnd(debug);
        JavaSource src = JavaSource.createCompoundStatement(frame, this, this, firstCanvasHandler,
                getFirstHeaderFragment(),
                CodeElement.toJavaCodes(firstCanvasContents));

        for (int i = 0; i < intermediateCanvasContents.size(); i++)
        {
            final int iFinal = i;
            JavaContainerDebugHandler intermediateCanvasHandler = debug -> frame.getIntermediateCanvasDebug(iFinal).showDebugAtEnd(debug);
            src.append(JavaSource.createCompoundStatement(frame, this, this, intermediateCanvasHandler,
                    getIntermediateHeaderFragment(i),
                    CodeElement.toJavaCodes(intermediateCanvasContents.get(i))));
        }

        if (tailCanvasContents != null) {
            JavaContainerDebugHandler tailCanvasHandler = debug -> frame.getTailCanvasDebug().showDebugAtEnd(debug);
            src.append(JavaSource.createCompoundStatement(frame, this, this, tailCanvasHandler,
                    Arrays.asList(f(frame, tailCanvasCaption)),
                    CodeElement.toJavaCodes(tailCanvasContents)));
        }

        return src;
    }

    protected List<JavaFragment> getFirstHeaderFragment()
    {
        return new ArrayList<>(Arrays.asList(f(frame, frameCaption)));
    }

    protected List<JavaFragment> getIntermediateHeaderFragment(int index)
    {
        return new ArrayList<>(Arrays.asList(f(frame, intermediateCanvasJavaCaption)));
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement mainEl = new LocatableElement(this, frameCaption);
        addMainAttributes(mainEl);
        addEnableAttribute(mainEl);
        Element firstCanvasStatementsEl = new Element(frameCaption + "Statements");
        firstCanvasContents.forEach(c -> firstCanvasStatementsEl.appendChild(c.toXML()));
        mainEl.appendChild(firstCanvasStatementsEl);

        for (int i = 0; i < intermediateCanvasContents.size(); i++)
        {
            LocatableElement intermediateCanvasEl = new LocatableElement(null, intermediateCanvasElement);
            addIntermediateAttributes(intermediateCanvasEl, i);
            intermediateCanvasContents.get(i).forEach(f -> intermediateCanvasEl.appendChild(f.toXML()));
            mainEl.appendChild(intermediateCanvasEl);
        }

        // We only want a tailCanvas if there is an tailCanvasContents; empty is different to null:
        if (tailCanvasContents != null) {
            Element tailCanvasEl = new Element(tailCanvasCaption);
            tailCanvasContents.forEach(c -> tailCanvasEl.appendChild(c.toXML()));
            mainEl.appendChild(tailCanvasEl);
        }
        return mainEl;
    }

    protected abstract void addMainAttributes(LocatableElement element);

    protected abstract void addIntermediateAttributes(LocatableElement element, int index);

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        Function<CodeElement, Frame> makeFrame = e -> e.createFrame(editor);
        frame = buildFrame(editor,
                Utility.mapList(firstCanvasContents, makeFrame),
                Utility.mapList(intermediateCanvasContents, cs -> Utility.mapList(cs, makeFrame)),
                tailCanvasContents == null ? null : Utility.mapList(tailCanvasContents, makeFrame),
                isEnable()
        );
        return frame;
    }

    @OnThread(Tag.FX)
    protected abstract SandwichCanvasesFrame buildFrame(InteractionManager editor, List<Frame> firstCanvasFrames,
                            List<List<Frame>> intermediateCanvasFrames, List<Frame> tailCanvasFrames, boolean enable);


    @Override
    public List<CodeElement> childrenUpTo(CodeElement c)
    {
        if (firstCanvasContents.contains(c))
        {
            return firstCanvasContents.subList(0, firstCanvasContents.indexOf(c));
        }

        if (intermediateCanvasContents != null)
        {
            for (List<CodeElement> intermediateCanvasContent : intermediateCanvasContents)
            {
                if (intermediateCanvasContent.contains(c))
                {
                    return intermediateCanvasContent.subList(0, intermediateCanvasContent.indexOf(c));
                }
            }
        }

        if (tailCanvasContents != null && tailCanvasContents.contains(c)) {
            return tailCanvasContents.subList(0, tailCanvasContents.indexOf(c));
        }

        throw new IllegalArgumentException();
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
        Stream<CodeElement> intermediateCanvasStream = intermediateCanvasContents.stream().flatMap(c -> streamContained(c));
        return Utility.concat(streamContained(firstCanvasContents), intermediateCanvasStream, streamContained(tailCanvasContents));
    }

    /** If the child is a direct member of an intermediate canvas, return its index,
     * otherwise return empty optional
     */
    protected Optional<Integer> findDirectIntermediateChild(CodeElement child)
    {
        for (int i = 0; i < intermediateCanvasContents.size(); i++)
        {
            if (intermediateCanvasContents.get(i).stream().anyMatch(c -> c == child))
                return Optional.of(i);
        }
        return Optional.empty();
    }
}
