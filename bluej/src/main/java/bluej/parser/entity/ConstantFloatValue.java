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
 * Represents a value entity with a known (constant) floating point value (single or double precision).
 * 
 * @author Davin McCall
 */
public class ConstantFloatValue extends ValueEntity
{
    private double value;
    
    /**
     * Construct a floating-point constant value entity
     * @param valueType  The type of the value (float or double)
     * @param value      The known value
     */
    public ConstantFloatValue(JavaType valueType, double value)
    {
        super(valueType);
        this.value = value;
    }
    
    @Override
    public boolean hasConstantFloatValue()
    {
        return true;
    }
    
    @Override
    public double getConstantFloatValue()
    {
        return value;
    }
}
