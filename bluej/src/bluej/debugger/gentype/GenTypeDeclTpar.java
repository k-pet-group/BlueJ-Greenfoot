/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2020  Michael Kolling and John Rosenberg
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * This represents a type parameter in a declaration list. It is the same
 * as a type parameter anywhere else, except that it can be bounded.
 * 
 * @author Davin McCall
 */
public class GenTypeDeclTpar extends GenTypeTpar
{
    protected GenTypeSolid [] upperBounds;
    
    /**
     * Construct a GenTypeDeclTpar without specifying bounds. The bounds should then be
     * set using setBounds(). Until that occurs any intermediate method call has undefined
     * results.
     */
    public GenTypeDeclTpar(String parname)
    {
        super(parname);
    }
    
    public GenTypeDeclTpar(String parname, GenTypeSolid bound)
    {
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
    }
    
    /**
     * Set the bounds. This should only be done when first creating the instance; otherwise,
     * GenTypeDeclTpar instances should be immutable (as with other JavaTypes).
     */
    public void setBounds(GenTypeSolid [] ubounds)
    {
        upperBounds = ubounds;
    }
    
    /**
     * Get the upper bound (possibly an intersection type).
     */
    @OnThread(Tag.FXPlatform)
    public GenTypeSolid getBound()
    {
        return IntersectionType.getIntersection(upperBounds);
    }
    
    /**
     * Get the bounds of this type parameter, as an array of GenTypeSolid.
     */
    public GenTypeSolid [] upperBounds()
    {
        GenTypeSolid [] r = new GenTypeSolid [upperBounds.length];
        System.arraycopy(upperBounds, 0, r, 0, upperBounds.length);
        return r;
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenTypeTpar#mapTparsToTypes(java.util.Map)
     */
    @OnThread(Tag.FXPlatform)
    public GenTypeParameter mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams)
    {
        if (tparams == null) {
            // Map each bound also:
            GenTypeSolid [] mappedBounds = new GenTypeSolid[upperBounds.length];
            for (int i = 0; i < upperBounds.length; i++) {
                mappedBounds[i] = upperBounds[i].mapTparsToTypes(null).getUpperBound().asSolid();
            }
            return IntersectionType.getIntersection(mappedBounds);
        }
        
        GenTypeParameter newType = (GenTypeParameter)tparams.get(getTparName());
        if( newType == null )
            return this;
        else
            return newType;
    }
    
    /**
     * Returns a string describing this type parameter. This includes name and
     * bound as written in Java.<br>
     * 
     * Example: T extends Integer
     */
    @OnThread(Tag.FXPlatform)
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

    @Override
    @OnThread(Tag.FXPlatform)
    public String arrayComponentName()
    {
        return getErasedType().arrayComponentName();
    }

    @OnThread(Tag.FXPlatform)
    public JavaType getErasedType()
    {
        return upperBounds[0].getErasedType();
    }

    @OnThread(Tag.FXPlatform)
    public void erasedSuperTypes(Set<Reflective> s)
    {
        for (int i = 0; i < upperBounds.length; i++) {
            upperBounds[i].erasedSuperTypes(s);
        }
    }

    @OnThread(Tag.FXPlatform)
    public GenTypeClass [] getReferenceSupertypes()
    {
        ArrayList<GenTypeClass> al = new ArrayList<GenTypeClass>();
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
        if (super.isAssignableFrom(t)) {
            return true;
        }
        
        return false;
    }

}
