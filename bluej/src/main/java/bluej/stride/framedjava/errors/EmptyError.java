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
package bluej.stride.framedjava.errors;

import bluej.stride.framedjava.ast.SlotFragment;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An error which complains that the given slot is empty.  This is just a special
 * form of the more general syntax error.
 */
public class EmptyError extends SyntaxCodeError
{
    /**
     * Creates an error complaining a slot is empty.
     *
     * @param slot The slot this error is attached to (i.e. the empty one)
     * @param msg The message complaining about the empty slot
     */
    @OnThread(Tag.Any)
    public EmptyError(SlotFragment slot, String msg)
    {
        super(slot, msg);
    }
}
