/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2015,2018,2019  Michael Kolling and John Rosenberg
 
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.parser.entity.ParsedArrayReflective;

/**
 * A version of Reflective which can be easily customised to suit the needs
 * of a test.
 * 
 * @author Davin McCall
 */
public class TestReflective extends Reflective
{
    public String name;
    public List<GenTypeDeclTpar> typeParams;
    public List<GenTypeClass> superTypes; // list of GenTypeClass
    public Map<String,FieldReflective> fields = Collections.emptyMap();
    
    public TestReflective(String name)
    {
        this.name = name;
        typeParams = new ArrayList<GenTypeDeclTpar>();
        superTypes = new ArrayList<GenTypeClass>();
    }
    
    public TestReflective(String name, Reflective superClass)
    {
        this(name);
        superTypes.add(new GenTypeClass(superClass));
    }
    
    public String getName()
    {
        return name;
    }
    
    @Override
    public boolean isInterface()
    {
        return false;
    }
    
    @Override
    public boolean isStatic()
    {
        return false;
    }
    
    @Override
    public boolean isPublic()
    {
        return true;
    }
    
    @Override
    public boolean isFinal()
    {
        return false;
    }
    
    public Reflective getRelativeClass(String name)
    {
        return null;
    }
    
    public List<GenTypeDeclTpar> getTypeParams()
    {
        return typeParams;
    }
    
    public List<Reflective> getSuperTypesR()
    {
        List<Reflective> n = new ArrayList<Reflective>();
        Iterator<GenTypeClass> i = superTypes.iterator();
        while (i.hasNext()) {
            n.add(i.next().getReflective());
        }
        return n;
    }
    
    public List<GenTypeClass> getSuperTypes()
    {
        return superTypes;
    }
    
    public Reflective getArrayOf()
    {
        return new ParsedArrayReflective(this, "L" + getName() + ";");
    }
    
    public boolean isAssignableFrom(Reflective r)
    {
        if (r == this) {
            return true;
        }
        
        List<Reflective> supers = r.getSuperTypesR();
        for (Reflective superR : supers) {
            if (isAssignableFrom(superR)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public Map<String,FieldReflective> getDeclaredFields()
    {
        return fields;
    }
    
    @Override
    public Map<String,Set<MethodReflective>> getDeclaredMethods()
    {
        return Collections.emptyMap();
    }

    @Override
    public List<ConstructorReflective> getDeclaredConstructors()
    {
        return Collections.emptyList();
    }

    @Override
    public Reflective getInnerClass(String name)
    {
        return null;
    }

    @Override
    public String getModuleName()
    {
        return null;
    }
}
