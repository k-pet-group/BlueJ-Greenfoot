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

import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.frames.IfFrame;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SandwichCanvasesFrame;
import bluej.stride.generic.Frame;

import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;

public class IfElement extends SandwichCanvasesElement
{
    public static final String ELEMENT = "if";
    private static final String ELSE_IF_LABEL = "elseIf";
    private static final String ELSE_IF_JAVA_LABEL = "else if";
    private static final String ELSE_LABEL = "else";
    private static final String CONDITION_LABEL = "condition";
    private static final String CONDITION_JAVA_LABEL = "condition-java";

    private FilledExpressionSlotFragment condition;
    private List<FilledExpressionSlotFragment> elseIfConditions = new ArrayList<>();
    private IfFrame frame;

    /**
     *
     * @param frame
     * @param condition
     * @param thenContents
     * @param elseIfContents Note that passing null here means no else if, whereas passing an empty list
     *                      indicates that there is an else if, but it is empty
     * @param elseContents Note that passing null here means no else, whereas passing an empty list
     *                      indicates that there is an else, but it is empty
     */
    public IfElement(IfFrame frame, FilledExpressionSlotFragment condition, List<CodeElement> thenContents,
             List<FilledExpressionSlotFragment> elseIfConditions, List<List<CodeElement>> elseIfContents,
             List<CodeElement> elseContents, boolean enabled)
    {
        super(frame, ELEMENT, thenContents, ELSE_IF_LABEL, ELSE_IF_JAVA_LABEL, elseIfContents, ELSE_LABEL, elseContents, enabled);

        this.condition = condition;
        this.elseIfConditions = elseIfConditions;
    }

    public IfElement(Element element)
    {
        super(ELEMENT, ELSE_IF_LABEL, ELSE_IF_JAVA_LABEL, ELSE_LABEL);
        loadElement(element);
    }

    @Override
    protected void loadMainAttributes(final Element element)
    {
        condition = new FilledExpressionSlotFragment(element.getAttributeValue(CONDITION_LABEL),
                element.getAttributeValue(CONDITION_JAVA_LABEL));
    }

    @Override
    protected void loadIntermediateAttributes(final Element element)
    {
        elseIfConditions.add(new FilledExpressionSlotFragment(element.getAttributeValue(CONDITION_LABEL),
                element.getAttributeValue(CONDITION_JAVA_LABEL)));
    }

    @Override
    protected List<JavaFragment> getFirstHeaderFragment()
    {
        List<JavaFragment> headerFragment = super.getFirstHeaderFragment();
        headerFragment.addAll(Arrays.asList(f(frame, " ("), condition, f(frame, ")")));
        return headerFragment;
    }

    @Override
    protected List<JavaFragment> getIntermediateHeaderFragment(int index)
    {
        List<JavaFragment> headerFragment = super.getIntermediateHeaderFragment(index);
        headerFragment.addAll(Arrays.asList(f(frame, " ("), elseIfConditions.get(index), f(frame, ")")));
        return headerFragment;
    }

    @Override
    protected void addMainAttributes(LocatableElement element)
    {
        element.addAttributeStructured(CONDITION_LABEL, condition);
    }

    @Override
    protected void addIntermediateAttributes(LocatableElement element, int index)
    {
        element.addAttributeStructured(CONDITION_LABEL, elseIfConditions.get(index));
    }

    @OnThread(Tag.FX)
    @Override
    protected SandwichCanvasesFrame buildFrame(InteractionManager editor, List<Frame> firstCanvasFrames,
                        List<List<Frame>> intermediateCanvasFrames, List<Frame> tailCanvasFrames, boolean enable)
    {
        frame = new IfFrame(editor, condition, firstCanvasFrames, elseIfConditions, intermediateCanvasFrames,
                                tailCanvasFrames, enable);
        return frame;
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.of(condition);
    }
}
