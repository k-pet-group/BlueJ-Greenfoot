/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.localdebugger;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.utility.JavaUtils;

/**
 * A DebuggerObject to represent arrays. This base class is for object arrays;
 * primitive array types are represented in seperate classes which derive from
 * this one.<p>
 * 
 * Subclasses should override:
 * <ul>
 * <li> getValueString(int)
 * <li> instanceFieldIsObject(int)
 * </ul>
 * 
 * @author Davin McCall
 */
public class LocalArray extends LocalObject
{
    private int length;
    
    protected LocalArray(Object [] object)
    {
        super(object);
        length = object.length;
    }
    
    /**
     * Subclasses use this constructor to specify the array length.
     * 
     * @param object  The array object
     * @param length  The array length
     */
    protected LocalArray(Object object, int length)
    {
        super(object);
        this.length = length;
    }

    @Override
    public int getElementCount()
    {
        return length;
    }
    
    @Override
    public DebuggerObject getElementObject(int index)
    {
        Object val = ((Object []) object)[index];
        return getLocalObject(val);
    }
    
    @Override
    public String getElementValueString(int index)
    {
        return LocalField.valueStringForObject(getElementObject(index));
    }

    @Override
    public JavaType getElementType()
    {
        return JavaUtils.genTypeFromClass(object.getClass().getComponentType());
    }
    
    @Override
    public boolean isArray()
    {
        return true;
    }
}
