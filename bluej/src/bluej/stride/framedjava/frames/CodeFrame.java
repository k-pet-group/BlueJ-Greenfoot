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
package bluej.stride.framedjava.frames;

import bluej.stride.framedjava.elements.CodeElement;

public interface CodeFrame<T extends CodeElement>
{   
    /**
     * Regenerates the AST.  The next return of getCode() may well be different afterwards.
     */
    void regenerateCode();
    
    /**
     * Gets the latest code.  Should not change unless regenerateCode is called.
     */
    T getCode();
    
    default BreakFrame.BreakEncloser asBreakEncloser()
    {
        return null;
    }

    default void setElementEnabled(boolean enabled)
    {
        T el = getCode();
        if (el != null)
            el.setEnable(enabled);
    }
}
