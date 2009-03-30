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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;

/**
 * A version of Reflective which can be easily customised to suit the needs
 * of a test.
 * 
 * @author Davin McCall
 * @version $Id: TestReflective.java 6215 2009-03-30 13:28:25Z polle $
 */
public class TestReflective extends Reflective
{
    public String name;
    public List typeParams;
    public List superTypes; // list of GenTypeClass
    
    public TestReflective(String name)
    {
        this.name = name;
        typeParams = new ArrayList();
        superTypes = new ArrayList();
    }
    
    public String getName()
    {
        return name;
    }
    
    public boolean isInterface()
    {
        return false;
    }
    
    public boolean isStatic()
    {
        return false;
    }
    
    public Reflective getRelativeClass(String name)
    {
        return null;
    }
    
    public List getTypeParams()
    {
        return typeParams;
    }
    
    public List getSuperTypesR()
    {
        List n = new ArrayList();
        Iterator i = superTypes.iterator();
        while (i.hasNext()) {
            n.add(((GenTypeClass)i.next()).getReflective());
        }
        return n;
    }
    
    public List getSuperTypes()
    {
        return superTypes;
    }
    
    public Reflective getArrayOf()
    {
        return null;
    }
    
    public boolean isAssignableFrom(Reflective r)
    {
        return false;
    }
}
