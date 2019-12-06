/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2019  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.inspector;

import java.util.Objects;

/**
 * Plain old data type for field/value pairs.
 * 
 * @author Davin McCall
 */
public class FieldInfo
{
    private String description;
    private String value;
    
    /**
     * Construct a FieldInfo object with the given field description (modifiers, type, name) and value.
     */
    public FieldInfo(String description, String value)
    {
        this.description = description;
        this.value = value;
    }
    
    /**
     * Get the field description (modifiers, type, name).
     */
    public String getDescription()
    {
        return description;
    }
    
    /**
     * Get the field value representation.
     * @return
     */
    public String getValue()
    {
        return value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo fieldInfo = (FieldInfo) o;
        return Objects.equals(description, fieldInfo.description) &&
                Objects.equals(value, fieldInfo.value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(description, value);
    }
}
