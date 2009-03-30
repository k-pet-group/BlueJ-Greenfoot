/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr;

import bluej.debugger.gentype.JavaType;

/**
 * A named value, such as an object on the object bench or a local variable
 * in the code page.
 * 
 * @author Davin McCall
 * @version $Id: NamedValue.java 6215 2009-03-30 13:28:25Z polle $
 */
public interface NamedValue
{
    /**
     * Get the name of the named value.
     */
    public String getName();

    /**
     * Check whether the value has been initialized. This is used to
     * distinguish established values from values which are expected to be
     * initialized by the user. If it returns false, the value is not yet
     * available.
     */
    public boolean isInitialized();
    
    /**
     * Check whether the value of this named value can be modified.
     */
    public boolean isFinal();
    
    /**
     * Get the nominated type of this value.
     */
    public JavaType getGenType();
}
