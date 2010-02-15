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
package bluej.utility;

import java.lang.reflect.Method;

import junit.framework.TestCase;

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
    public void test1()
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
    public void sampleMethod(int arg1, int arg2)
    {
        
    }
    
    /**
     * Test that method/constructor signatures are constructed correctly.
     */
    @SuppressWarnings("unchecked")
    public void testSignatures()
    {
        JavaUtils jutils = JavaUtils.getJavaUtils();
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
        
        String sig = jutils.getSignature(sampleMeth);
        assertEquals(sig, "void sampleMethod(int, int)");
        
        if (onjava5) {
            // test a varargs method
            Class<Class> clazz = Class.class;
            try {
                sampleMeth = clazz.getMethod("getConstructor", new Class [] {Class [].class});
            }
            catch (NoSuchMethodException nsme) {
                fail();
            }
            
            sig = jutils.getSignature(sampleMeth);
            assertEquals("java.lang.reflect.Constructor getConstructor(java.lang.Class[])", sig);
        }
        
    }
    
}
