/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2014,2020  Michael Kolling and John Rosenberg
 
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

/**
 * A wildcard type with an upper and/or lower bound.
 * 
 * <p>Note that both an upper and lower bound is allowed. This type doesn't occur
 * naturally- it can't be specified in the Java language. But in some cases we
 * can deduce the type of some object to be this.
 *
 * @author Davin McCall
 */
public class GenTypeWildcard extends GenTypeParameter
{
    GenTypeSolid upperBound; // ? extends upperBound
    GenTypeSolid lowerBound; // ? super lowerBound
    
    /**
     * Constructor for a wildcard with a specific upper and lower bound, either of
     * which may be null.
     */
    public GenTypeWildcard(GenTypeSolid upper, GenTypeSolid lower)
    {
        upperBound = upper;
        lowerBound = lower;
    }
    
    @Override
    public String toString()
    {
        return toString(false);
    }
        
    @Override
    public String toString(NameTransform nt)
    {
        if (lowerBound != null) {
            return "? super " + lowerBound.toString(nt);
        }
        else if (upperBound != null) {
            String uboundStr = upperBound.toString();
            if (! uboundStr.equals("java.lang.Object"))
                return "? extends " + upperBound.toString(nt);
        }
        return "?";
    }
    
    @Override
    public String toTypeArgString(NameTransform nt)
    {
        return toString(nt);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public GenTypeWildcard mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams)
    {
        GenTypeSolid newUpper = null;
        GenTypeSolid newLower = null;
        
        // Find new upper bounds
        if (upperBound != null) {
            ArrayList<GenTypeSolid> newUppers = new ArrayList<GenTypeSolid>();
            GenTypeSolid [] upperBounds = upperBound.getUpperBounds();
            
            // find the new upper bounds
            for (int i = 0; i < upperBounds.length; i++) {
                GenTypeParameter newBound = upperBounds[i].mapTparsToTypes(tparams);
                if (newBound instanceof GenTypeWildcard) {
                    GenTypeWildcard newWcBound = (GenTypeWildcard) newBound;
                    newUppers.add(newWcBound.upperBound);
                }
                else {
                    newUppers.add((GenTypeSolid) newBound);
                }
            }
            GenTypeSolid [] newUppersA = (GenTypeSolid []) newUppers.toArray(new GenTypeSolid[newUppers.size()]);
            newUpper = IntersectionType.getIntersection(newUppersA);
        }
        
        // find the new lower bounds
        // This is easier. If the lower bound is an intersection type, it comes from
        // lub() and therefore contains no immediate type parameters.
        if (lowerBound != null) {
            GenTypeParameter newLowerP = lowerBound.mapTparsToTypes(tparams);
            newLower = newLowerP.getLowerBound();
        }
        
        return new GenTypeWildcard(newUpper, newLower);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public boolean equals(GenTypeParameter other)
    {
        if (this == other)
            return true;
        
        if (! other.isWildcard()) {
            return false;
        }
        
        GenTypeSolid otherLower = other.getLowerBound();
        JavaType otherUpper = other.getUpperBound();
        
        if (upperBound != null && ! upperBound.equals(otherUpper)) {
            return false;
        }
        if (upperBound == null && otherUpper != null) {
            return false;
        }
        if (lowerBound != null && ! lowerBound.equals(otherLower)) {
            return false;
        }
        if (lowerBound == null && otherLower != null) {
            return false;
        }
        
        return true;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getErasedType()
    {
        return upperBound.getErasedType();
    }
    
    @Override
    public boolean isWildcard()
    {
        return true;
    }
    
    @Override
    public GenTypeSolid getUpperBound()
    {
        return upperBound;
    }
    
    @Override
    public GenTypeSolid getLowerBound()
    {
        return lowerBound;
    }
        
    @Override
    public JavaType getTparCapture()
    {
        return new GenTypeCapture(this);
    }
}

