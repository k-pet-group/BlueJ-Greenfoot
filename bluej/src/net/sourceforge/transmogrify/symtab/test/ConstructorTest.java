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

// $Id: ConstructorTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;
import junit.extensions.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;

public class ConstructorTest extends TestCase {

  private File file = new File( "test/Constructors.java" );
  private QueryEngine query = null;

  private IDefinition refDef = null;

  private IDefinition noArgConstructor = null;
  private IDefinition doubleConstructor = null;
  private IDefinition shortConstructor = null;
  private IDefinition cockConstructor = null;

  public ConstructorTest( String name ) {
    super( name );
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();
      fileParser.doFile( file );

      TableMaker maker = new TableMaker( (SymTabAST)(fileParser.getTree()) );
      SymbolTable table = maker.getTable();

      query = new QueryEngine( table );

      noArgConstructor = getDefinition( "Constructors", 5, 10 );
      doubleConstructor = getDefinition( "Constructors", 8, 10 );
      shortConstructor = getDefinition( "Constructors", 11, 10 );
      cockConstructor = getDefinition( "Constructors", 14, 10 );
    }
    catch ( Exception e ) {
      e.printStackTrace();
      fail();
    }

  }

  private IDefinition getDefinition( String name, int lineNumber, int columnNumber ) {
    return query.getDefinition( name, new Occurrence( file, lineNumber, columnNumber ) );
  }

  public void testNoArgConstructor() {
    refDef = getDefinition( "Constructors", 30, 16 );
    assertNotNull( refDef );
    assertEquals( noArgConstructor, refDef );
  }

  public void testDoubleConstructor() {
    refDef = getDefinition( "Constructors", 31, 16 );
    assertNotNull( refDef );
    assertEquals( doubleConstructor, refDef );
  }

  public void testIntegerResolvesToDouble() {
    refDef = getDefinition( "Constructors", 32, 16 );
    assertNotNull( refDef );
    assertEquals( doubleConstructor, refDef );
  }

  public void testCockConstructor() {
    refDef = getDefinition( "Constructors", 33, 16 );
    assertNotNull( refDef );
    assertEquals( cockConstructor, refDef );
  }

  public void testSonOfCockResolvesToCock() {
    refDef = getDefinition( "Constructors", 34, 16 );
    assertNotNull( refDef );
    assertEquals( cockConstructor, refDef );
  }

  public void testConstructorInVariableDefinition() {
    refDef = getDefinition( "Constructors", 22, 28 );
    assertNotNull( refDef );
    assertEquals( noArgConstructor, refDef );
  }

  public void testImplicitConstructor() {
    refDef = getDefinition( "method", 56, 5 );
    assertNotNull( refDef );

    assertEquals( getDefinition( "method", 49, 15 ), refDef );
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { ConstructorTest.class.getName() });
  }

}




