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
package bluej.stride.slots;

import bluej.stride.framedjava.slots.UnderlineContainer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Gets a slot, if a slot has already been associated with this fragment, either
 * because that slot generated this fragment, or because this fragment has previously
 * had a slot registered (via registerSlot).
 */
//@OnThread(Tag.FX)
//public abstract SLOT getSlot();

//@OnThread(Tag.FX)
//public abstract void registerSlot(SLOT slot);


@OnThread(Tag.FX)
public class LinkedIdentifier implements TextSlot.Underline
{
    private final String name;
    private final int startPosition; // Within slot
    private final int endPosition; // Within slot
    private final FXPlatformRunnable onClick;
    private final UnderlineContainer slot;
    @OnThread(Tag.Any)
    public LinkedIdentifier(String name, int startPosition, int endPosition,
                            UnderlineContainer slot, FXPlatformRunnable onClick)
    {
        this.name = name;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.onClick = onClick;
        this.slot = slot;
    }

    @OnThread(Tag.FXPlatform)
    public void show() { slot.addUnderline(this); }

    @Override
    public int getStartPosition()
    {
        return startPosition;
    }

    @Override
    public int getEndPosition()
    {
        return endPosition;
    }

    @Override
    public FXPlatformRunnable getOnClick()
    {
        return onClick;
    }

    public String getName()
    {
        return name;
    }
}
