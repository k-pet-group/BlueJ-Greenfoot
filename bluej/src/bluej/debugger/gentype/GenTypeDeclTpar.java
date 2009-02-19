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

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * This represents a type parameter in a declaration list. It is the same
 * as a type parameter anywhere else, except that it can be bounded.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeDeclTpar.java 6163 2009-02-19 18:09:55Z polle $
 */
public class GenTypeDeclTpar extends GenTypeTpar {

    protected GenTypeSolid [] upperBounds;
    protected GenTypeSolid lBound = null;
    
    public GenTypeDeclTpar(String parname, GenTypeSolid bound) {
        super(parname);
        upperBounds = new GenTypeSolid [] { bound };
    }
    
    /**
     * Constructor for a type parameter with bounds. The array passed to this
     * constructor should not be modified afterwards.
     * 
     * @param parname  The name of this type parameter
     * @param bounds   The declared upper bounds for this type parameter
     */
    public GenTypeDeclTpar(String parname, GenTypeSolid [] bounds)
    {
        super(parname);
        upperBounds = bounds;
    }
    
    /**
     * Constructor for a type parameter with a lower bound (as well as upper bounds).
     * This can occur from capture conversion of a "? super XX" wildcard.
     */
    public GenTypeDeclTpar(String parname, GenTypeSolid [] ubounds, GenTypeSolid lbound)
    {
        super(parname);
        upperBounds = ubounds;
        lBound = lbound;
    }
    
    /**
     * Get the upper bound (possibly an intersection type).
     */
    public GenTypeSolid getBound()
    {
        return IntersectionType.getIntersection(upperBounds);
    }
    
    /**
     * Get the bounds of this type parameter, as an array of GenTypeSolid
     * (If the upper bound is an intersection type, this returns the component
     * types).<p>
     * 
     * Note, this is different from getUpperBounds! That is for getting upper
     * bounds of a parameterizable ( = {this}), this method is specific to
     * GenTypeDeclTpar and returns the bounds of the tpar itself.
     */
    public GenTypeSolid [] upperBounds()
    {
        GenTypeSolid [] r = new GenTypeSolid [upperBounds.length];
        System.arraycopy(upperBounds, 0, r, 0, upperBounds.length);
        return r;
    }
    
    public GenTypeSolid lowerBound()
    {
        return lBound;
    }
    
    public GenTypeSolid [] lowerBounds()
    {
        if (lBound == null)
            return new GenTypeSolid[0];
        else
            return new GenTypeSolid [] {lBound};
    }
    
    public JavaType mapTparsToTypes(Map tparams)
    {
        if (tparams == null)
            return new GenTypeWildcard(upperBounds(), lowerBounds());
        
        GenTypeParameterizable newType = (GenTypeParameterizable)tparams.get(getTparName());
        if( newType == null )
            return new GenTypeWildcard(upperBounds(), lowerBounds());
        else
            return newType;
    }
    
    /**
     * Returns a string describing this type parameter. This includes name and
     * bound as written in Java.<br>
     * 
     * Example: T extends Integer
     */
    public String toString(boolean stripPrefix)
    {
        //need prefix to match java.lang.Object
        String bound = getBound().toString(false);
        if (bound.equals("java.lang.Object")) {
            return getTparName();
        }
        else {
            //now we strip the prefix if needed
            return getTparName() + " extends " + getBound().toString(stripPrefix);
        }
    }
    
    public String arrayComponentName()
    {
        return getErasedType().arrayComponentName();
    }
    
    public JavaType getErasedType()
    {
        return upperBounds[0].getErasedType();
    }

    public void erasedSuperTypes(Set s)
    {
        for (int i = 0; i < upperBounds.length; i++) {
            upperBounds[i].erasedSuperTypes(s);
        }
    }
    
    public GenTypeClass [] getReferenceSupertypes()
    {
        ArrayList al = new ArrayList();
        for (int i = 0; i < upperBounds.length; i++) {
            GenTypeClass [] brs = upperBounds[i].getReferenceSupertypes();
            for (int j = 0; j < brs.length; j++) {
                al.add(brs[j]);
            }
        }
        return (GenTypeClass []) al.toArray(new GenTypeClass[0]);
    }

    public boolean isAssignableFrom(JavaType t)
    {
        if (super.isAssignableFrom(t))
            return true;
        
        if (lBound != null)
            return lBound.isAssignableFrom(t);
        
        return false;
    }

}
