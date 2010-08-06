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
 * "? super ..." type.
 * 
 * @author Davin McCall
 */
public class GenTypeSuper extends GenTypeWildcard
{
    /**
     * Construct a "? super ..." type without specifying the lower bound
     * at this time. Use "setLowerBound()" to set the lower bound before
     * using the resulting type.
     */
    public GenTypeSuper()
    {
        super((GenTypeSolid) null, (GenTypeSolid) null);
    }
    
    /**
     * Construct a "? super ..." type with the specified lower bound.
     */
    public GenTypeSuper(GenTypeSolid baseType)
    {
        super(null, baseType);
    }
    
    /*
     * @see bluej.debugger.gentype.GenTypeParameter#toString(boolean)
     */
    @Override
    public String toString(boolean stripPrefix)
    {
        return "? super " + lowerBound.toString(stripPrefix);
    }
    
    /*
     * @see bluej.debugger.gentype.GenTypeWildcard#toString(bluej.debugger.gentype.NameTransform)
     */
    @Override
    public String toString(NameTransform nt)
    {
        return "? super " + lowerBound.toString(nt);
    }
}
