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

// $Id: NameAmbiguityTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

public class NameAmbiguityTest extends DefinitionLookupTest {

  File file = new File( "test/JavaIsHideous.java" );

  IDefinition classFoo = null;
  IDefinition varFoo = null;
  IDefinition staticVar = null;
  IDefinition publicVar = null;

  public NameAmbiguityTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    createQueryEngine( new File[] {file} );

    classFoo = getDefinition( "Foo", 16, 7 );
    varFoo = getDefinition( "Foo", 6, 21 );
    staticVar = getDefinition( "x", 17, 24 );
    publicVar = getDefinition( "x", 13, 17 );
  }

  private IDefinition getDefinition( String name, int line, int column ) {
    return getDefinition( file, name, line, column );
  }

  public void tearDown() {}

  public void testFirstFooIsClass() {
    IDefinition reference = getDefinition( "Foo", 4, 12 );
    assertEquals( classFoo, reference );
    assertNotNull( reference );
  }

  public void testSecondFooIsVar() {
    IDefinition reference = getDefinition( "Foo", 8, 12 );
    assertEquals( varFoo, reference );
    assertNotNull( reference );
  }

  public void testFirstXIsStatic() {
    IDefinition reference = getDefinition( "x", 4, 16 );
    assertEquals( staticVar, reference );
    assertNotNull( reference );
  }

  public void testSecondXIsVar() {
    IDefinition reference = getDefinition( "x", 8, 16 );
    assertEquals( publicVar, reference );
    assertNotNull( reference );
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { NameAmbiguityTest.class.getName() });
  }
}
