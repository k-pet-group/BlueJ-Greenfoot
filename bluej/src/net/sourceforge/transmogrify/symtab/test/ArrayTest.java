/*
Copyright (C) 2001  ThoughtWorks, Inc

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
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package net.sourceforge.transmogrify.symtab.test;

// $Id: ArrayTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;
import junit.extensions.*;

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

public class ArrayTest extends DefinitionLookupTest {
  File file = new File("test/Array.java");
  File[] files = new File[] { file };

  IDefinition intArrayDef = null;
  IDefinition blankArrayDef = null;
  IDefinition otherArrayDef = null;
  IDefinition arrayArgMethod = null;
  IDefinition intArgMethod = null;
  IDefinition returnMethod = null;
  IDefinition tempDefinition = null;

  public ArrayTest(String name) {
    super(name);

  }

  public void setUp() throws Exception {
    query = createQueryEngine( files );

    intArrayDef = getDefinition(file, "intArray", 5, 9);
    blankArrayDef = getDefinition(file, "blankArray", 6, 10);
    otherArrayDef = getDefinition(file, "otherArray", 7, 9);
    tempDefinition = getDefinition(file, "temp", 8, 7);
    returnMethod = getDefinition(file, "returnMethod", 10, 16);
    arrayArgMethod = getDefinition(file, "argMethod", 14, 15);
    intArgMethod = getDefinition(file, "argMethod", 17, 15);
  }

  public void testVariableType() throws Exception {
    IClass varType = ((VariableDef)intArrayDef).getType();
    ArrayDef array = new ArrayDef(new ExternalClass(Integer.TYPE));
    assertEquals(array, varType);
  }

  public void testStupidVariableType() throws Exception {
    IClass varType = ((VariableDef)otherArrayDef).getType();
    ArrayDef array = new ArrayDef(new ExternalClass(Integer.TYPE));
    assertEquals("Array type incorrect.", array, varType);
  }

  public void testReferencesInSizeInitializer() throws Exception {
    IDefinition def = getDefinition(file, "temp", 23, 26 );
    assertNotNull(tempDefinition);
    assertEquals("Reference not found.", tempDefinition, def);
  }

  public void testReferencesInInitializer() throws Exception {
    IDefinition def = getDefinition( file, "temp", 24, 29 );
    assertNotNull( def );
    assertEquals( tempDefinition, def );
  }

  public void testReferenceInAccessor() throws Exception {
    IDefinition def = getDefinition( file, "temp", 27, 14 );
    assertNotNull( def );
    assertEquals( tempDefinition, def );
  }

  public void testReferenceInAccessorAfterMethod() throws Exception {
    IDefinition def = getDefinition( file, "temp", 28, 20 );
    assertNotNull( def );
    assertEquals( tempDefinition, def );
  }

  public void testReferenceToVariableBeingIndexed() throws Exception {
    IDefinition def = getDefinition( file, "intArray", 27, 5 );
    assertNotNull( def );
    assertEquals( intArrayDef, def );
  }

  public void testReferenceToMethodBeingIndexed() throws Exception {
    IDefinition def = getDefinition( file, "returnMethod", 28, 5 );
    assertNotNull ( def );
    assertEquals( returnMethod, def );
  }

  public void testArrayVariableMethodParam() throws Exception {
    IDefinition def = getDefinition( file, "argMethod", 31, 5 );
    assertNotNull( def );
    assertEquals( arrayArgMethod, def );
  }

  public void testArrayReturnMethodParam() throws Exception {
    IDefinition def = getDefinition( file, "argMethod", 32, 5 );
    assertNotNull( def );
    assertEquals( arrayArgMethod, def );
  }

  public void testNewArrayMethodParam() throws Exception {
    IDefinition def = getDefinition( file, "argMethod", 33, 5 );
    assertNotNull( def );
    assertEquals( arrayArgMethod, def );
  }

  public void testArrayAccessorMethodParam() throws Exception {
    IDefinition def = getDefinition( file, "argMethod", 34, 5 );
    assertNotNull( def );
    assertEquals( intArgMethod, def );
  }

  public void testArrayMethodAccessorMethodParam() throws Exception {
    IDefinition def = getDefinition( file, "argMethod", 35, 5 );
    assertNotNull( def );
    assertEquals( intArgMethod, def );
  }

  public void testPrimitiveArrayTypeCompatability() throws Exception {
    IDefinition def = getDefinition(file, "argMethod", 38, 5);
    assertNotNull("Array not upsized.", def);
    assertEquals("Array not upsized.", arrayArgMethod, def);
  }

  public void testLengthMemberDefinition() throws Exception {
    IDefinition def = getDefinition(file, "length", 42, 27);
    assertNotNull("Definition not set.", def);
  }

  public void testLengthMemberType() throws Exception {
    IDefinition def = getDefinition(file, "argMethod", 42, 5);
    assertEquals("Length member not an int.", intArgMethod, def);
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { ArrayTest.class.getName() });
  }
}
