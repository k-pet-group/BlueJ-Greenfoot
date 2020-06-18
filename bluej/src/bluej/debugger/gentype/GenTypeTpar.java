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
import java.util.Set;


public class GenTypeTpar extends GenTypeSolid
{
    private String name;
    
    public GenTypeTpar(String parname)
    {
        name = parname;
    }

    public String getTparName()
    {
        return name;
    }

    @OnThread(Tag.FXPlatform)
    public String toString(boolean stripPrefix)
    {
        return name;
    }
    
    public String toString(NameTransform nt)
    {
        return name;
    }
        
    public String toTypeArgString(NameTransform nt)
    {
        return name;
    }

    @OnThread(Tag.FXPlatform)
    public String arrayComponentName()
    {
        // We don't know the erased type.
        throw new UnsupportedOperationException();
    }
    
    public boolean isInterface()
    {
        return false;
    }
    
    public boolean equals(JavaType other)
    {
        // For tpars to be equal, they must be the *same* tpar.
        return other == this;
    }

    @OnThread(Tag.FXPlatform)
    public GenTypeParameter mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams)
    {
        if (tparams == null)
            return this;
        
        GenTypeParameter newType = tparams.get(name);
        if( newType == null ) {
            return this;
        }
        else {
            return newType;
        }
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void getParamsFromTemplate(Map<String,GenTypeParameter> map, GenTypeParameter template)
    {
        // If a mapping already exists, precisify it against the template.
        // Otherwise, create a new mapping to the template.
        
        GenTypeParameter x = map.get(name);
        if (x != null)
            x = x.precisify(template);
        else
            x = template;
        map.put(name, x);
    }
    
    public GenTypeParameter precisify(GenTypeParameter other)
    {
        // shouldn't get called.
        return other;
    }
    
    public boolean isPrimitive()
    {
        return false;
    }
    
    public boolean isAssignableFrom(JavaType t)
    {
        if (t.isNull())
            return true;
        
        // If the other type has an upper bound which is this tpar, it's assignable
        GenTypeSolid ubound = t.getUpperBound().asSolid();
        if (ubound != null) {
            GenTypeSolid [] ubounds = ubound.getIntersectionTypes();
            for (int i = 0; i < ubounds.length; i++) {
                if (ubounds[i] == this) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public boolean isAssignableFromRaw(JavaType t)
    {
        // We don't know the erased type.
        throw new UnsupportedOperationException();
    }

    @OnThread(Tag.FXPlatform)
    public JavaType getErasedType()
    {
        // We don't know the erased type.
        throw new UnsupportedOperationException();
    }

    @OnThread(Tag.FXPlatform)
    public void erasedSuperTypes(Set<Reflective> s)
    {
        throw new UnsupportedOperationException();
    }

    @OnThread(Tag.FXPlatform)
    public GenTypeClass [] getReferenceSupertypes()
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public GenTypeArray getArray()
    {
        return new GenTypeArray(this);
    }
    
    @Override
    public JavaType getCapture()
    {
        return this;
    }
}
