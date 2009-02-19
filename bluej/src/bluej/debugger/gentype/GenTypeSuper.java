/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
 * @version $Id: GenTypeSuper.java 6163 2009-02-19 18:09:55Z polle $
 */
public class GenTypeSuper extends GenTypeWildcard
{
    public GenTypeSuper(GenTypeSolid baseType) {
        super(null, baseType);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "? super " + lowerBound.toString(stripPrefix);
    }
    
    public String toString(NameTransform nt)
    {
        return "? super " + lowerBound.toString(nt);
    }
}
