/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018 Michael Kölling and John Rosenberg 
 
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


import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
public interface SlotParent<T> extends FocusParent<T>
{
    /**
     * For when backspace has been pressed at the start of the slot
     * @param src The slot in which backspace was pressed (it's up to SlotParent to work out the previous one
     * that might want to act on the backspace)
     * @return True if we have focused into another slot, or deleted this slot, or somehow it is invalid to retain
     * focus in the src slot
     */
    public boolean backspaceAtStart(T src);

    public boolean deleteAtEnd(T src);

    public void escape(T src);
}
