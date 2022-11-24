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
package bluej.stride.slots;

import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;

import java.util.Collections;
import java.util.List;

public class MethodNameDefTextSlot extends TextSlot<NameDefSlotFragment>
{
    public <T extends Frame & CodeFrame<? extends CodeElement>>
    MethodNameDefTextSlot(InteractionManager editor, T frameParent, FrameContentRow row,
            CompletionCalculator completion, String stylePrefix)
    {
        super(editor, frameParent, frameParent, row, completion, stylePrefix, Collections.emptyList());
        
        addValueListener((slot, oldValue, newValue, parent) -> 
            // We don't differentiate start and part, as that just gets too fiddly during editing:
            newValue.chars().allMatch(Character::isJavaIdentifierPart)
        );
    }
    
    @Override
    public NameDefSlotFragment createFragment(String content)
    {
        return new NameDefSlotFragment(content, this);
    }

    @Override
    public void valueChangedLostFocus(String oldValue, String newValue)
    {
        // Nothing to do        
    }


    @Override
    public List<? extends PossibleLink> findLinks()
    {
        // The name is defined here, so there's never any links to go somewhere else
        return Collections.emptyList();
    }
}
