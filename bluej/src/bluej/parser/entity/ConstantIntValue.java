/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kolling and John Rosenberg 
 
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
package bluej.parser.entity;

import bluej.debugger.gentype.JavaType;

/**
 * Represents a value entity with a known (constant) integer value.
 * 
 * @author Davin McCall
 */
public class ConstantIntValue extends ValueEntity
{
    private long value;
    
    /**
     * Construct a constant integer value entity.
     * @param name   The entity name (may be null)
     * @param type   The entity type (should be a primitive integer type)
     * @param value  The value represented
     */
    public ConstantIntValue(String name, JavaType type, long value)
    {
        super(name, type);
        this.value = value;
    }
    
    @Override
    public boolean hasConstantIntValue()
    {
        return true;
    }
    
    @Override
    public long getConstantIntValue()
    {
        return value;
    }
}
