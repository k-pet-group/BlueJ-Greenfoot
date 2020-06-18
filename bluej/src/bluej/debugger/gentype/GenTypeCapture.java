/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2018,2020  Michael Kolling and John Rosenberg
 
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

import java.util.Map;

/**
 * Represents the capture of a type parameter.
 * 
 * <p>See capture conversion in the Java Language Specification, section 5.1.12. Instances
 * of this class represent the "fresh type variable" mentioned there. 
 * 
 * @author Davin McCall
 */
public class GenTypeCapture extends GenTypeTpar
{
    private GenTypeWildcard wildcard;
    
    public GenTypeCapture(GenTypeWildcard wildcard)
    {
        super("?capture?");
        this.wildcard = wildcard;
    }
    
    public String toString(boolean stripPrefix)
    {
        return wildcard.toString(stripPrefix);
    }
    
    public String toString(NameTransform nt)
    {
        return wildcard.toString(nt);
    }
    
    public String toTypeArgString(NameTransform nt)
    {
        return wildcard.toTypeArgString(nt);
    }
    
    @Override
    public GenTypeSolid mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams)
    {
        // This doesn't make sense; a capture is a capture. Mapping to a 'different' capture
        // breaks things.
        throw new UnsupportedOperationException("You don't want this.");
    }

    @Override
    public String arrayComponentName()
    {
        return null;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getErasedType()
    {
        if (wildcard.getUpperBound() != null) {
            // Ideally the upper bound should always be specifed (as java.lang.Object if nothing else)
            return wildcard.getUpperBound().getErasedType();
        }
        else {
            GenTypeClass[] rsts = wildcard.getLowerBound().getReferenceSupertypes();
            Reflective objRef = rsts[0].getReflective().getRelativeClass("java.lang.Object");
            return new GenTypeClass(objRef);
        }
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public GenTypeClass[] getReferenceSupertypes()
    {
        if (wildcard.getUpperBound() != null) {
            // Ideally the upper bound should always be specifed (as java.lang.Object if nothing else)
            return wildcard.getUpperBound().getReferenceSupertypes();
        }
        else {
            GenTypeClass[] rsts = wildcard.getLowerBound().getReferenceSupertypes();
            Reflective objRef = rsts[0].getReflective().getRelativeClass("java.lang.Object");
            return new GenTypeClass[] {new GenTypeClass(objRef)};
        }
    }

}
