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
package bluej.debugger.gentype;

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
        return new GenTypeCapture((GenTypeWildcard) wildcard.mapTparsToTypes(tparams));
    }

    @Override
    public String arrayComponentName()
    {
        return null;
    }

    @Override
    public JavaType getErasedType()
    {
        return wildcard.getUpperBound().getErasedType();
    }

}
