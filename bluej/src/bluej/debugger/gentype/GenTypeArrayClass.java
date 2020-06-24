/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2019,2020  Michael Kolling and John Rosenberg

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

import java.util.HashMap;

/**
 * This class represents array types for which we have a reflective.
 * 
 * @author Davin McCall
 */
public class GenTypeArrayClass extends GenTypeClass
{
    private JavaType componentType;
    
    public GenTypeArrayClass(Reflective r, JavaType componentType)
    {
        super(r);
        this.componentType = componentType;
    }

    @Override
    public JavaType getArrayComponent()
    {
        return componentType;
    }
    
    @Override
    public String toString(NameTransform nt)
    {
        return componentType.toString(nt) + "[]";
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public GenTypeClass getErasedType()
    {
        JavaType newComponentType = componentType.getErasedType();
        if (newComponentType != componentType) {
            return new GenTypeArrayClass(reflective, newComponentType);
        }
        else {
            return this;
        }
    }

    @Override
    public GenTypeClass mapToSuper(String basename) {
        return this;
    }

    @Override
    public boolean isRaw()
    {
        return false;
    }

    @Override
    public HashMap<String, GenTypeParameter> getMap()
    {
        return new HashMap<>();
    }
}
