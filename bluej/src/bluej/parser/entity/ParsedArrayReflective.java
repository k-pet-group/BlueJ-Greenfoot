/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

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
package bluej.parser.entity;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;

public class ParsedArrayReflective extends Reflective
{
    private Reflective component;
    private String className;
    
    public ParsedArrayReflective(Reflective component, String componentName)
    {
        this.component = component;
        className = "[" + componentName;
    }
    
    @Override
    public String getName()
    {
        return "[L" + component.getName() + ";";
    }
    
    @Override
    public Reflective getArrayOf()
    {
        return new ParsedArrayReflective(this, className);
    }
    
    @Override
    public Map<String, JavaType> getDeclaredFields()
    {
        return Collections.emptyMap();
    }
    
    @Override
    public Map<String, Set<MethodReflective>> getDeclaredMethods()
    {
        return Collections.emptyMap();
    }
    
    @Override
    public List<GenTypeClass> getInners()
    {
        return Collections.emptyList();
    }
    
    @Override
    public Reflective getRelativeClass(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public List<GenTypeClass> getSuperTypes()
    {
        List<GenTypeClass> componentSupers = component.getSuperTypes();
        for (ListIterator<GenTypeClass> i = componentSupers.listIterator(); i.hasNext(); ) {
            i.set(i.next().getArray());
        }
        return componentSupers;
    }
    
    @Override
    public List<Reflective> getSuperTypesR()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public List<GenTypeDeclTpar> getTypeParams()
    {
        return Collections.emptyList();
    }
    
    @Override
    public boolean isAssignableFrom(Reflective r)
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean isInterface()
    {
        return false;
    }
    
    @Override
    public boolean isPublic()
    {
        return component.isPublic();
    }
    
    @Override
    public boolean isStatic()
    {
        return component.isStatic();
    }
}
