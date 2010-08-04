/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger;

import java.util.List;
import java.util.Map;

/**
 * A class for representing classes in the debugged VM.
 *
 * @author     Michael Kolling
 */
public abstract class DebuggerClass
{
    /**
     *  Return the name of this class (fully qualified).
     *
     *@return    The class name
     */
    public abstract String getName();

    /**
     *  Return the number of static fields.
     *
     *@return    The StaticFieldCount value
     */
    public abstract int getStaticFieldCount();

    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public abstract String getStaticFieldName(int slot);
    
    /**
     * Return the type of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The type of the static field
     */
    public abstract String getStaticFieldType(int slot);

    /**
     *  Return the object in static field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       The StaticFieldObject value
     */
    public abstract DebuggerObject getStaticFieldObject(int slot);

    /**
     *  Return a list of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Whether to include modifiers (private,etc.)
     *@return                   The StaticFields value
     */
    public abstract List<String> getStaticFields(boolean includeModifiers, Map<String, List<String>> restrictedClasses);
    
    public final List<String> getStaticFields(boolean includeModifiers)
    {
        return getStaticFields(includeModifiers, null);
    }

    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean staticFieldIsPublic(int slot);

    /**
     *  Return true if the static field 'slot' is an object (and not
     *  a simple type, or null).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean staticFieldIsObject(int slot);
    
    /**
     * Returns true if this represents a Java interface
     * 
     */
    public abstract boolean isInterface();

    /**
     * Returns true if this represents an enum
     * 
     */
    public abstract boolean isEnum();

}
