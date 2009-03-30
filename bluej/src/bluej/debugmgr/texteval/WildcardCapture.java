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
package bluej.debugmgr.texteval;

import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.NameTransform;

/**
 * Represent the capture of a wildcard. This is really an unnamed type parameter.
 * 
 * @author Davin McCall
 * @version $Id$
 */
public class WildcardCapture extends GenTypeDeclTpar
{
    public WildcardCapture(GenTypeSolid [] ubounds)
    {
        super(null, ubounds);
    }
    
    public WildcardCapture(GenTypeSolid [] ubounds, GenTypeSolid lbound)
    {
        super(null, ubounds, lbound);
    }
    
    //public JavaType mapTparsToTypes(Map tparams)
    //{
    //    // We behave differently than a regular tpar - no mapping occurs.
    //    return this;
    //}

    public String toString(boolean stripPrefix)
    {
        // Need to be a little careful, as one of the bounds could be a capture itself.
        if (lBound != null) {
            return "capture of ? super " + lBound.getReferenceSupertypes()[0].toString(stripPrefix);
        }
        if (upperBounds.length != 0) {
            return "capture of ? extends " + upperBounds[0].getReferenceSupertypes()[0].toString(stripPrefix);
        }
        return "capture of ?";
    }
    
    public String toString(NameTransform nt)
    {
        // Need to be a little careful, as one of the bounds could be a capture itself.
        if (lBound != null) {
            return "? super " + lBound.getReferenceSupertypes()[0].toString(nt);
        }
        if (upperBounds.length != 0) {
            return "? extends " + upperBounds[0].getReferenceSupertypes()[0].toString(nt);
        }
        return "?";
    }

}
