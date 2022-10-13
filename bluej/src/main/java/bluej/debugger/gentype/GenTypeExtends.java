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
package bluej.debugger.gentype;

/**
 * "? extends ..." type.
 * 
 * @author Davin McCall
 */
public class GenTypeExtends extends GenTypeWildcard
{
    /**
     * Construct a "? extends ..." without specifying the upper bound at this time.
     * The upper bound should then be set using "setUpperBound()".
     */
    public GenTypeExtends()
    {
        super((GenTypeSolid) null, (GenTypeSolid) null);
    }
    
    /**
     * Construct a "? extends ..." type with the specified upper bound (which
     * may be an intersection type).
     */
    public GenTypeExtends(GenTypeSolid baseType)
    {
        super(baseType, null);
    }
}
