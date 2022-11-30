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

import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;

import java.util.Collections;

public class OptionalExpressionSlot extends ExpressionSlot<OptionalExpressionSlotFragment>
{

    public OptionalExpressionSlot(InteractionManager editor,
            Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row,
                                  String stylePrefix)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, Collections.emptyList());
    }

    @Override
    protected OptionalExpressionSlotFragment makeSlotFragment(String content, String javaCode)
    {
        return new OptionalExpressionSlotFragment(content, javaCode, this);
    }
}
