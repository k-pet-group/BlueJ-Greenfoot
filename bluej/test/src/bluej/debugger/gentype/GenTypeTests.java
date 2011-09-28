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

import java.util.*;

import junit.framework.TestCase;
import bluej.utility.JavaReflective;

/**
 * Tests for the GenType classes.
 * 
 * @author Davin McCall
 */
public class GenTypeTests extends TestCase
{
    protected void setUp()
    {
        // nothing to do
    }
    
    protected void tearDown()
    {
        // nothing to do
    }
    
    /**
     * Test "map to derived" functionality for a non-immediate derived class.
     *
     * fails in 2.0.2.
     */
    public void test1()
    {
        TestReflective baseReflective = new TestReflective("base");
        TestReflective derivedR = new TestReflective("derived1");
        TestReflective derivedR2 = new TestReflective("derived2");
        
        // Create genType for java.lang.Object
        Class<?> c = Object.class;
        JavaReflective objectR = new JavaReflective(c);
        GenTypeSolid oBound = new GenTypeClass(objectR);

        // each of the three classes will have a type parameter T
        derivedR.typeParams.add(new GenTypeDeclTpar("T", new GenTypeSolid [] {oBound}));
        derivedR2.typeParams.add(new GenTypeDeclTpar("T", new GenTypeSolid [] {oBound}));
        baseReflective.typeParams.add(new GenTypeDeclTpar("T", new GenTypeSolid[] {oBound}));
        
        List<GenTypeParameter> tpars = new ArrayList<GenTypeParameter>();
        tpars.add(new GenTypeTpar("T"));
        derivedR.superTypes.add(new GenTypeClass(baseReflective, tpars));
        derivedR2.superTypes.add(new GenTypeClass(derivedR, tpars));
        
        List<GenTypeParameter> basePars = new ArrayList<GenTypeParameter>();
        basePars.add(oBound);
        GenTypeClass baseClass = new GenTypeClass(baseReflective, basePars);
        //Map m = baseClass.mapToDerived(derivedR2);
        GenTypeClass mapped = (GenTypeClass) baseClass.mapToDerived(derivedR2);
        
        //assertTrue(m.get("T").equals(oBound));
        assertEquals("derived2<java.lang.Object>", mapped.toString());
    }
    
    /**
     * Must be able to map tpars of a wildcard to a wildcard. The result is
     * not a legal java type, but its string representation must be a legal
     * type.
     */
    public void test2()
    {
        // Create genType for java.lang.Object
        JavaReflective objectR = new JavaReflective(Object.class);
        GenTypeSolid object = new GenTypeClass(objectR);
        
        // Create a wildcard ('wildcard1') "? extends T"
        GenTypeExtends wildcard1 = new GenTypeExtends(new GenTypeTpar("T"));
        
        // create a wildcard ('wildcard2') "? extends Object"
        GenTypeExtends wildcard2 = new GenTypeExtends(object);
        
        // create a mapping "T -> wildcard2"
        Map<String,GenTypeParameter> m = new HashMap<String,GenTypeParameter>();
        m.put("T", wildcard2);
        
        // Apply mapping to wildcard1
        String st = wildcard1.mapTparsToTypes(m).toString(true);
        
        // check that result is a legal java type (when as a string)
        assertEquals("?", st);
    }
    
    /**
     * Test we can map successfully from a raw type to a non-generic base type.
     */
    public void test3()
    {
        TestReflective baseReflective = new TestReflective("base");
        TestReflective derivedR = new TestReflective("derived1");

        // Create genType for java.lang.Object
        Class<?> c = Object.class;
        JavaReflective objectR = new JavaReflective(c);
        GenTypeSolid oBound = new GenTypeClass(objectR);

        // derived class has a type parameter "T"
        derivedR.typeParams.add(new GenTypeDeclTpar("T", new GenTypeSolid [] {oBound}));

        // derived inherits from base
        List<GenTypeParameter> noTpars = new ArrayList<GenTypeParameter>();
        derivedR.superTypes.add(new GenTypeClass(baseReflective, noTpars));

        // Make a raw version of the derived type
        GenTypeClass derived = new GenTypeClass(derivedR, noTpars);
        GenTypeClass mapped = derived.mapToSuper("base");
        
        assertEquals(mapped.toString(), "base");
    }
    
    /**
     * For a type A which inherits the raw version of a generic type B which
     * inherits a generic base C, mapping A -> C should yield the raw type C.
     */
    public void test4()
    {
        TestReflective aReflective = new TestReflective("AClass");
        TestReflective bReflective = new TestReflective("BClass");
        TestReflective cReflective = new TestReflective("CClass");
        TestReflective objReflective = new TestReflective("java.lang.Object");
   
        // BClass and CClass have a type parameter "T"
        GenTypeDeclTpar tparT = new GenTypeDeclTpar("T", new GenTypeSolid[] {new GenTypeClass(objReflective)});
        bReflective.typeParams.add(tparT);
        cReflective.typeParams.add(tparT);
        
        // AClass derives from raw BClass
        aReflective.superTypes.add(new GenTypeClass(bReflective));
        
        // BClass<T> derives from CClass<T>
        List<GenTypeParameter> l = new ArrayList<GenTypeParameter>();
        l.add(tparT);
        bReflective.superTypes.add(new GenTypeClass(cReflective, l));
        
        // test!
        GenTypeClass instanceAClass = new GenTypeClass(aReflective);
        GenTypeClass mapped = instanceAClass.mapToSuper("CClass");
        assertEquals("CClass", mapped.toString());
    }
    
    /**
     * Test GenTypeClass constructor taking a Reflective and Map; no type parameters, empty map
     */
    public void test5()
    {
        TestReflective aReflective = new TestReflective("AClass");
        GenTypeClass aClass = new GenTypeClass(aReflective, new HashMap<String,GenTypeParameter>());
        assertEquals("AClass", aClass.toString());
    }
    
    /**
     * Test GenTypeClass constructor taking a Reflective and Map; type parameters, empty map.
     */
    public void test6()
    {
        TestReflective aReflective = new TestReflective("AClass");
        TestReflective bReflective = new TestReflective("BClass");
        aReflective.typeParams.add(new GenTypeDeclTpar("T", new GenTypeClass(bReflective)));
        
        // effective declaration:   class AClass<T extends BClass>
        
        GenTypeClass aClass = new GenTypeClass(aReflective, new HashMap<String,GenTypeParameter>());
        assertEquals("AClass<? extends BClass>", aClass.toString());
    }
}
