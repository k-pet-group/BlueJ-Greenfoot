/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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

import java.util.Arrays;
import java.util.List;

import bluej.Config;
import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.CallExpressionSlotFragment;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;

/**
 * Created by neil on 04/12/2015.
 */
public class CallExpressionSlot extends ExpressionSlot<CallExpressionSlotFragment>
{
    public static final List<FrameCatalogue.Hint> CALL_HINTS = Arrays.asList(
        new FrameCatalogue.Hint("move(3)", Config.getString("frame.catalogue.hint.move")),
        new FrameCatalogue.Hint("turn(5)", Config.getString("frame.catalogue.hint.turn")),
        new FrameCatalogue.Hint("removeTouching(Crab.class)", Config.getString("frame.catalogue.hint.removeobject"))
    );

    public CallExpressionSlot(InteractionManager editor, Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, String stylePrefix, List<FrameCatalogue.Hint> hints)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, hints);
    }

    @Override
    protected CallExpressionSlotFragment makeSlotFragment(String content, String javaCode)
    {
        return new CallExpressionSlotFragment(content, javaCode, this);
    }
}
