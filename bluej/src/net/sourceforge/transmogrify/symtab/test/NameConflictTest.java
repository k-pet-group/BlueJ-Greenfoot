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

// $Id: NameConflictTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

public class NameConflictTest extends DefinitionLookupTest {

  File file = new File( "test/NameConflicts.java" );

  IDefinition classFoo = null;
  IDefinition varFoo = null;
  IDefinition labelFoo = null;

  IDefinition classClass = null;
  IDefinition classMethod = null;
  IDefinition classVar = null;

  IDefinition blockClass = null;
  IDefinition blockVar = null;
  IDefinition blockLabel = null;

  public NameConflictTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    createQueryEngine( new File[] {file} );

    classFoo = getDefinition( "Foo", 4, 11 );
    varFoo = getDefinition( "Foo", 16, 9);
    labelFoo = getDefinition( "Foo", 18, 5);

    classClass = getDefinition( "aMethod", 30, 9 );
    classMethod = getDefinition( "aMethod", 3, 15 );
    classVar = getDefinition( "aMethod", 32, 7 );

    blockClass = getDefinition( "blockName", 43, 13 );
    blockVar = getDefinition( "blockName", 44, 11 );
    blockLabel = getDefinition( "blockName", 49, 7 );
  }

  private IDefinition getDefinition( String name, int line, int column ) {
    return getDefinition( file, name, line, column );
  }

  public void tearDown() {}

  public void testMethodClass() {
    IDefinition reference = getDefinition("Foo", 26, 5);
    assertNotNull(reference);
    assertEquals(classFoo, reference);
  }

  public void testMethodVar() {
    IDefinition reference = getDefinition( "Foo", 27, 13 );
    assertNotNull( reference );
    assertEquals( varFoo, reference );
  }

  public void testMethodLabel() {
    IDefinition reference = getDefinition( "Foo", 21, 17 );
    assertEquals( labelFoo, reference );
    assertNotNull( reference );
  }

  public void testClassClass() {
    IDefinition reference = getDefinition( "aMethod", 35, 5 );
    assertNotNull( reference );
    assertEquals( classClass, reference );
  }

  public void testClassMethod() {
    IDefinition reference = getDefinition( "aMethod", 37, 5 );
    assertNotNull( reference );
    assertEquals( classMethod, reference );
  }

  public void testClassVar() {
    IDefinition reference = getDefinition( "aMethod", 36, 13 );
    assertNotNull( reference );
    assertEquals( classVar, reference );
  }

  public void testBlockClass() {
    IDefinition reference = getDefinition( "blockName", 47, 7 );
    assertNotNull( reference );
    assertEquals( blockClass, reference );
  }

  public void testBlockVar() {
    IDefinition reference = getDefinition( "blockName", 46, 7 );
    assertNotNull( reference );
    assertEquals( blockVar, reference );
  }

  public void testBlockLabel() {
    IDefinition reference = getDefinition( "blockName", 50, 15 );
    assertNotNull( reference );
    assertEquals( blockLabel, reference );
  }


  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { NameConflictTest.class.getName() });
  }
}
