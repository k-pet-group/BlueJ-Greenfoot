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

import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.FocusParent;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotValueListener;
import bluej.stride.slots.TypeTextSlot;
import bluej.stride.slots.VariableNameDefTextSlot;
import bluej.utility.javafx.SharedTransition;

import java.util.Optional;

public class EachExpressionSlot extends FilledExpressionSlot
{
    private final TypeTextSlot loopVarTypeSlot;
    private final VariableNameDefTextSlot loopVarNameSlot;

    public EachExpressionSlot(InteractionManager editor, Frame parentFrame,
            CodeFrame<?> parentCodeFrame, FrameContentRow row, TypeTextSlot loopVarTypeSlot, VariableNameDefTextSlot loopVarNameSlot, String stylePrefix)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, FilledExpressionSlot.EACH_HINTS);
        this.loopVarTypeSlot = loopVarTypeSlot;
        this.loopVarNameSlot = loopVarNameSlot;
        
        // When the type is modified, we are effectively modified too, as we will generate
        // different code afterwards:
        loopVarTypeSlot.addValueListener(new SlotValueListener() {
            
            @Override
            public boolean valueChanged(HeaderItem slot, String oldValue,
                    String newValue, FocusParent<HeaderItem> parent)
            {
                EachExpressionSlot.this.modified();
                return true;
            }
            
            @Override
            public void deletePressedAtEnd(HeaderItem slot)
            {                
            }
            
            @Override
            public void backSpacePressedAtStart(HeaderItem slot)
            {
            }
        });
    }
    
    @Override
    public String getCurlyLiteralPrefix()
    {
        return "new " + loopVarTypeSlot.getText() + " []";
    }

    @Override
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        getTopLevel().setView(oldView, newView, animate, Optional.of(loopVarNameSlot.getText()));
    }
}
