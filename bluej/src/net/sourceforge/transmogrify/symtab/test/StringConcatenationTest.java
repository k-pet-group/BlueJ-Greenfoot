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

// $Id: StringConcatenationTest.java 1014 2001-11-30 03:28:10Z ajp $

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

public class StringConcatenationTest extends DefinitionLookupTest {

  private File file = new File( "test/StringConcatenation.java" );

  private IDefinition method = null;

  public StringConcatenationTest( String name ) {
    super( name );
  }

  public void setUp() throws Exception {
    createQueryEngine( new File[] { file } );

    method = getDefinition( "method", 5, 15);
  }

  public IDefinition getDefinition( String name, int line, int column ) {
    return getDefinition( file, name, line, column );
  }

  public void testTwoLiterals() {
    IDefinition ref = getDefinition( "method", 11, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testLiteralThenVar() {
    IDefinition ref = getDefinition( "method", 12, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testVarThenliteral() {
    IDefinition ref = getDefinition( "method", 13, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testVarThenIntVar() {
    IDefinition ref = getDefinition( "method", 15, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testIntVarThenVar() {
    IDefinition ref = getDefinition( "method", 16, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testVarThenIntLiteral() {
    IDefinition ref = getDefinition( "method", 17, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testIntLiteralThenVar() {
    IDefinition ref = getDefinition( "method", 18, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testMultipleConcatenation() {
    IDefinition ref = getDefinition( "method", 20, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );

    ref = getDefinition( "method", 21, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );

    ref = getDefinition( "method", 22, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );

    ref = getDefinition( "method", 23, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testUnknownTypeFirst() {
    IDefinition ref = getDefinition( "method", 26, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public void testUnknownTypeSecond() {
    IDefinition ref = getDefinition( "method", 27, 5 );
    assertEquals( method, ref );
    assertNotNull( ref );
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { StringConcatenationTest.class.getName() });
  }

}

