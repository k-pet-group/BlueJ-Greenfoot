/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.debugger.gentype.TestReflective;

public class JavaUtilTests extends TestCase
{
    @Override
    protected void setUp()
    {
        // nothing to do
    }
    
    @Override
    protected void tearDown()
    {
        // nothing to do
    }

    /**
     * Test that types with infinite recursion don't cause us to bomb out.
     * In this case we use Enum, as Enum&lt;E&gt; has E extend Enum&lt;E&gt;.
     */
    public void testEnumInfiniteRecursion()
    {
        JavaUtils ju = JavaUtils.getJavaUtils();
        try {
            Class<?> enumClass = getClass().getClassLoader().loadClass("java.lang.Enum");
            ju.getTypeParams(enumClass);
        }
        catch (ClassNotFoundException cnfe) {}
        // ok test passed
    }
    
    // This is just a method for testing signature creation
    //   see testSignatures()
    public void sampleMethod(int arg1, int arg2)
    {
    }
    
    // This is just a method for testing signature creation
    //   see testSignatures()
    public void sampleMethod2(String [] args)
    {
    }
    
    /**
     * Test that method/constructor signatures are constructed correctly.
     */
    public void testSignatures()
    {
        boolean onjava5 = true;
        // String majorVersion = System.getProperty("java.specification.version");        
        // boolean onjava6 = majorVersion.compareTo("1.6") >= 0;
        
        Method sampleMeth = null;
        
        Class<? extends JavaUtilTests> thisClass = getClass();
        try {
            sampleMeth = thisClass.getMethod("sampleMethod", new Class [] {int.class, int.class});
        }
        catch (NoSuchMethodException nsme) {
            fail();
        }
        
        String sig = JavaUtils.getSignature(sampleMeth);
        assertEquals(sig, "void sampleMethod(int, int)");
        
        if (onjava5) {
            // test a varargs method
            Class<?> clazz = Class.class;
            try {
                sampleMeth = clazz.getMethod("getConstructor", new Class [] {Class [].class});
            }
            catch (NoSuchMethodException nsme) {
                fail();
            }
            
            sig = JavaUtils.getSignature(sampleMeth);
            assertEquals("java.lang.reflect.Constructor getConstructor(java.lang.Class[])", sig);
        }

        try {
            sampleMeth = thisClass.getMethod("sampleMethod2", new Class [] {String[].class});
        }
        catch (NoSuchMethodException nsme) {
            fail();
        }

        sig = JavaUtils.getSignature(sampleMeth);
        assertEquals("void sampleMethod2(java.lang.String[])", sig);
    }
    
    public void testTparReturn() throws Exception
    {
        Class<?> colClass = java.util.Collections.class;
        Method minMethod = colClass.getMethod("min", Collection.class);
        JavaUtils ju = JavaUtils.getJavaUtils();
        
        JavaType type = JavaUtils.genTypeFromClass(minMethod.getReturnType());
        assertNotNull(type.asClass());
        
        type = ju.getReturnType(minMethod);
        assertEquals("T", type.toString());
    }
    
    public void testRecursiveTpar() throws Exception
    {
        Class<?> colClass = java.util.Collections.class;
        Method minMethod = colClass.getMethod("min", Collection.class);
        JavaUtils ju = JavaUtils.getJavaUtils();
        
        List<GenTypeDeclTpar> rlist = ju.getTypeParams(minMethod);
        assertEquals(1, rlist.size());
        
        GenTypeDeclTpar tvar = rlist.get(0);
        GenTypeSolid bound = tvar.getBound();
        assertEquals("java.lang.Comparable<? super T>", bound.toString());
        
        GenTypeSolid [] ubounds = bound.getUpperBounds();
        assertEquals(2, ubounds.length); // should be Object and Comparable
        GenTypeClass boundClass =  bound.getUpperBounds()[1].asClass();
        
        List<? extends GenTypeParameter> tpars = boundClass.getTypeParamList();
        assertEquals(1, tpars.size());
        
        GenTypeParameter tparOne = tpars.get(0);
        GenTypeSolid shouldBeT = tparOne.getLowerBound();
        assertNotNull(shouldBeT);
        Set<Reflective> s = new HashSet<Reflective>();
        shouldBeT.erasedSuperTypes(s);
        
        assertTrue(s.size() > 0);
        boolean foundComparable = false;
        for (Reflective r: s) {
            if (r.getName().equals("java.lang.Comparable")) {
                foundComparable = true;
                break;
            }
        }
        
        assertTrue(foundComparable);
    }
    
    /**
     * Test that a class can access protected members of its superclass, and of its
     * superclass' superclass and so on.
     */
    public void testAccessCheck()
    {
        TestReflective baseR = new TestReflective("Base");
        TestReflective subR = new TestReflective("Sub", baseR);
        TestReflective subsubR = new TestReflective("SubSub", subR);
        
        assertTrue(JavaUtils.checkMemberAccess(subR, new GenTypeClass(subsubR), subsubR, Modifier.PROTECTED, false));
        assertFalse(JavaUtils.checkMemberAccess(subR, new GenTypeClass(subsubR), subsubR, Modifier.PRIVATE, false));
        
        assertTrue(JavaUtils.checkMemberAccess(subR, new GenTypeClass(baseR), baseR, Modifier.PROTECTED, false));
        assertFalse(JavaUtils.checkMemberAccess(subR, new GenTypeClass(baseR), baseR, Modifier.PRIVATE, false));
    }
    
    /**
     * Test that we can't access a package-private member from a class which isn't
     * a superclass, even if a superclass is in the same package.
     */
    public void testAccessCheck2()
    {
        TestReflective baseR = new TestReflective("Base");
        TestReflective subR = new TestReflective("somepkg.Sub", baseR);
        TestReflective otherR = new TestReflective("Other");
        
        assertFalse(JavaUtils.checkMemberAccess(otherR, new GenTypeClass(subR), subR, 0, false));
    }
    
    /**
     * Test that a subclass can only access a protected member from the superclass via an expression
     * of subclass type.
     */
    public void testAccessCheck3()
    {
        TestReflective baseR = new TestReflective("Base");
        TestReflective subR = new TestReflective("somepkg.Sub", baseR);
        TestReflective subsubR = new TestReflective("somepkg.SubSub", subR);
        
        assertFalse(JavaUtils.checkMemberAccess(baseR, new GenTypeClass(baseR),
                subR, Modifier.PROTECTED, false));
        
        assertTrue(JavaUtils.checkMemberAccess(baseR, new GenTypeClass(subR),
                subR, Modifier.PROTECTED, false));

        assertTrue(JavaUtils.checkMemberAccess(baseR, new GenTypeClass(subsubR),
                subR, Modifier.PROTECTED, false));
    }
}
