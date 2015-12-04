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
package bluej.stride.framedjava.slots;

import bluej.editor.stride.FrameCatalogue.Hint;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FilledExpressionSlot extends ExpressionSlot<FilledExpressionSlotFragment>
{
    public static final List<Hint> CONDITION_HINTS = Arrays.asList(
        new Hint("isAtEdge()", "Is at the edge of the world"),
        new Hint("isTouching(Crab.class)", "Is touching a Crab actor"),
        new Hint("Greenfoot.isKeyDown(\"left\")", "Is the left arrow key pressed")
    );

    public static final List<Hint> EACH_HINTS = Arrays.asList(
        new Hint("1..10", "Numbers 1 to 10 (inclusive)"),
        new Hint("getIntersectingObjects(Crab.class)", "Touching Crab actors")
    );

    public static final List<Hint> SRC_HINTS = Arrays.asList(
        new Hint("getX()", "X coordinate"),
        new Hint("Greenfoot.ask(\"How many?\")", "Get number from user")
    );

    public FilledExpressionSlot(InteractionManager editor,
                                Frame parentFrame, CodeFrame<?> parentCodeFrame,
                                FrameContentRow row,
                                String stylePrefix)
    {
        this(editor, parentFrame, parentCodeFrame, row, stylePrefix, Collections.emptyList());
    }

    public FilledExpressionSlot(InteractionManager editor,
            Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row,
            String stylePrefix, List<Hint> hints)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, hints);
    }

    @Override
    protected FilledExpressionSlotFragment makeSlotFragment(String content, String javaCode)
    {
        return new FilledExpressionSlotFragment(content, javaCode, this);
    }

    
}
