/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014,2015,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.debugger.gentype.ConstructorReflective;
import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.utility.JavaReflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This class acts as purely as an access source in TextAnalyzer. It
 * is not a "real" reflective; it has a name, which can be used for
 * access checks, but that's all.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class DummyReflective extends Reflective
{
    private String name;
    
    public DummyReflective(String name)
    {
        this.name = name;
    }
    
    @Override
    public Reflective getArrayOf()
    {
        return null;
    }

    @Override
    public Map<String, FieldReflective> getDeclaredFields()
    {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Set<MethodReflective>> getDeclaredMethods()
    {
        return Collections.emptyMap();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Reflective getRelativeClass(String name)
    {
        return null;
    }

    @Override
    public List<GenTypeClass> getSuperTypes()
    {
        List<GenTypeClass> l = new ArrayList<GenTypeClass>(1);
        l.add(new GenTypeClass(new JavaReflective(Object.class)));
        return l;
    }

    @Override
    public List<Reflective> getSuperTypesR()
    {
        List<Reflective> l = new ArrayList<Reflective>(1);
        l.add(new JavaReflective(Object.class));
        return l;
    }

    @Override
    public List<GenTypeDeclTpar> getTypeParams()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isAssignableFrom(Reflective r)
    {
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
        return false;
    }

    @Override
    public boolean isStatic()
    {
        return false;
    }
    
    @Override
    public boolean isFinal()
    {
        return false;
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

    @Override
    public List<ConstructorReflective> getDeclaredConstructors()
    {
        return Collections.emptyList();
    }
}
