/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014  Michael Kolling and John Rosenberg 
 
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

import java.lang.reflect.Modifier;
import java.util.HashMap;

import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.TestReflective;

public class EntityTests extends junit.framework.TestCase
{
    public void testValueSuperclassFieldAccess()
    {
        TestReflective base = new TestReflective("BaseClass");
        TestReflective sub = new TestReflective("SubClass", base);
        TestReflective subsub = new TestReflective("SubSubClass", sub);
        
        // Create a field call "aField" in the superclass
        base.fields = new HashMap<String,FieldReflective>();
        FieldReflective fref = new FieldReflective("aField", JavaPrimitiveType.getInt(), Modifier.PUBLIC, base);
        base.fields.put("aField", fref);
        
        // Check the fields can be accessed
        ValueEntity vent = new ValueEntity(new GenTypeClass(subsub));
        JavaEntity aFieldEnt = vent.getSubentity("aField", subsub);
        assertNotNull(aFieldEnt);
        aFieldEnt = aFieldEnt.resolveAsValue();
        assertNotNull(aFieldEnt);
        assertEquals("int", aFieldEnt.getType().toString());
    }
    
    public void testTypeSuperclassFieldAccess()
    {
        TestReflective base = new TestReflective("BaseClass");
        TestReflective sub = new TestReflective("SubClass", base);
        TestReflective subsub = new TestReflective("SubSubClass", sub);
        
        // Create some fields in the superclass
        base.fields = new HashMap<String,FieldReflective>();
        FieldReflective fref = new FieldReflective("aField", JavaPrimitiveType.getInt(), Modifier.PUBLIC, base);
        base.fields.put("aField", fref);
        fref = new FieldReflective("sField", JavaPrimitiveType.getDouble(), Modifier.PUBLIC | Modifier.STATIC, base);
        base.fields.put("sField", fref);
        
        // Check non-static field can't be accessed
        TypeEntity tent = new TypeEntity(new GenTypeClass(subsub));
        JavaEntity aFieldEnt = tent.getSubentity("aField", subsub);
        assertNull(aFieldEnt); // can't access a non-static field
        
        // Check static field can be accessed
        aFieldEnt = tent.getSubentity("sField", subsub);
        assertNotNull(aFieldEnt);
        aFieldEnt = aFieldEnt.resolveAsValue();
        assertNotNull(aFieldEnt);
        assertEquals("double", aFieldEnt.getType().toString());
    }
}
