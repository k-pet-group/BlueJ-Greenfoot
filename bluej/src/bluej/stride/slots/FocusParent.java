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

/**
 * Created by neil on 27/04/15.
 */
public interface FocusParent<T>
{
    /**
     * Focuses the previous control, or whatever is suitable for when the up key
     * is pressed on the "src" control.
     * @param toEnd If true, and the destination for focus is a slot, move the cursor to the end of the slot
     */
    void focusUp(T src, boolean cursorToEnd);

    void focusDown(T src);

    void focusRight(T src);

    void focusEnter(T src);

    void focusLeft(T src);
}
