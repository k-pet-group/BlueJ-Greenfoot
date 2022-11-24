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
package bluej.stride.slots;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;

public class Implements extends TypeList
{
    public Implements(Frame parentFrame, Supplier<TypeSlot> slotGenerator, FXRunnable focusOnNext, InteractionManager editor)
    {
        super(" implements ", parentFrame, slotGenerator, focusOnNext, editor);
    }

    public List<TypeSlotFragment> getTypes()
    {
        // Treat single empty slot as missing:
        if (typeSlots.size() == 1 && typeSlots.get(0).isEmpty())
            return Collections.emptyList();
        else
            return Utility.mapList(typeSlots, TypeSlot::getSlotElement);
    }

}
