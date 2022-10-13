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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.frames.TryFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SandwichCanvasesFrame;

import nu.xom.Attribute;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;

public class TryElement extends SandwichCanvasesElement
{
    public static final String ELEMENT = "try";
    private static final String CATCH_LABEL = "catch";
    private static final String FINALLY_LABEL = "finally";
    private static final String EXCEPTION_TYPE_LABEL = "type";
    private static final String EXCEPTION_NAME_LABEL = "name";

    private List<TypeSlotFragment> catchTypes = new ArrayList<>();
    private List<NameDefSlotFragment> catchNames = new ArrayList<>();
    private TryFrame frame;
    
    /**
     * 
     * @param frame
     * @param tryContents
     * @param catchContents
     * @param finallyContents Note that passing null here means no finally, whereas passing
     *               an empty list indicates that there is a finally, but it is empty.
     */
    public TryElement(TryFrame frame, List<CodeElement> tryContents, List<TypeSlotFragment> catchTypes,
            List<NameDefSlotFragment> catchNames, List<List<CodeElement>> catchContents,
            List<CodeElement> finallyContents, boolean enabled)
    {
        super(frame, ELEMENT, tryContents, CATCH_LABEL, CATCH_LABEL, catchContents, FINALLY_LABEL, finallyContents, enabled);

        this.catchTypes = catchTypes;
        this.catchNames = catchNames;
    }

    public TryElement(Element element) {
        super(ELEMENT, CATCH_LABEL, CATCH_LABEL, FINALLY_LABEL);
        loadElement(element);
    }

    @Override
    protected void loadMainAttributes(final Element element) {}

    @Override
    protected void loadIntermediateAttributes(final Element element)
    {
        catchTypes.add(new TypeSlotFragment(element.getAttributeValue(EXCEPTION_TYPE_LABEL), element.getAttributeValue(EXCEPTION_TYPE_LABEL + "-java")));
        catchNames.add(new NameDefSlotFragment(element.getAttributeValue(EXCEPTION_NAME_LABEL)));
    }

    @Override
    protected List<JavaFragment> getIntermediateHeaderFragment(int index)
    {
        List<JavaFragment> headerFragment = super.getIntermediateHeaderFragment(index);
        headerFragment.addAll(Arrays.asList(f(frame, " ("), catchTypes.get(index), space(), catchNames.get(index), f(frame, ")")));
        return headerFragment;
    }

    @Override
    protected void addMainAttributes(LocatableElement element) {}

    @Override
    protected void addIntermediateAttributes(LocatableElement element, int index)
    {
        element.addAttributeStructured(EXCEPTION_TYPE_LABEL, catchTypes.get(index));
        element.addAttributeCode(EXCEPTION_NAME_LABEL, catchNames.get(index));
    }

    @OnThread(Tag.FX)
    @Override
    protected SandwichCanvasesFrame buildFrame(InteractionManager editor, List<Frame> firstCanvasFrames,
                       List<List<Frame>> intermediateCanvasFrames, List<Frame> tailCanvasFrames, boolean enable)
    {
        frame = new TryFrame(editor, firstCanvasFrames, catchTypes, catchNames, intermediateCanvasFrames,
                                tailCanvasFrames, enable);
        return frame;
    }

    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.empty();
    }

    @Override
    public List<LocalParamInfo> getDeclaredVariablesWithin(CodeElement child)
    {
        Optional<Integer> subCanvas = findDirectIntermediateChild(child);
        
        return subCanvas.map(i -> Arrays.asList(new LocalParamInfo(catchTypes.get(i).getContent(), catchNames.get(i).getContent(), false, this))).orElse(Collections.emptyList());
    }
}
