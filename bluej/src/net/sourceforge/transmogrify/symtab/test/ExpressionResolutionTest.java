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

// $Id: ExpressionResolutionTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;

public class ExpressionResolutionTest extends DefinitionLookupTest {

  File file = new File ( "test/ExpressionResolution.java" );

  IDefinition refDef = null;
  IDefinition def = null;

  IDefinition intMethod = null;
  IDefinition doubleMethod = null;
  IDefinition booleanMethod = null;
  IDefinition charMethod = null;
  IDefinition objectMethod = null;
  IDefinition floatMethod = null;
  IDefinition anObject = null;

  public ExpressionResolutionTest( String name ) {
    super( name );
  }

  public void setUp() throws Exception {
    createQueryEngine( new File[] { file } );

    intMethod = getDefinition( "getsCalled", 16, 15 );
    doubleMethod = getDefinition( "getsCalled", 19, 15 );
    booleanMethod = getDefinition( "getsCalled", 22, 15 );
    charMethod = getDefinition( "getsCalled", 25, 15 );
    objectMethod = getDefinition( "getsCalled", 28, 15 );
    floatMethod = getDefinition( "getsCalled", 103, 15 );
    anObject = getDefinition( "anObject", 36, 26 );
  }

  private IDefinition getDefinition( String name, int line, int column ) {
    return getDefinition( file, name, line, column );
  }

  public void testGetDefinitionForThisSourceFile() {
    assertNotNull( intMethod );
  }

  public void testIdent() {
    refDef = getDefinition( "getsCalled", 39, 5 );
    assertEquals( intMethod, refDef );

    // reference in the method call
    def = getDefinition( "aInt", 32, 9 );
    refDef = getDefinition( "aInt", 39, 17 );
    assertEquals( def, refDef );

    refDef = getDefinition( "getsCalled", 41, 5 );
    assertEquals( objectMethod, refDef );
  }

  public void testMethodCall() {
    refDef = getDefinition( "getsCalled", 44, 5 );
    assertEquals( intMethod, refDef );

    // reference in the method call
    def = getDefinition( "returns", 10, 14 );
    refDef = getDefinition( "returns", 44, 17 );
    assertEquals( def, refDef );
  }

  public void testConstructor() {
    refDef = getDefinition( "getsCalled", 47, 5 );
    assertEquals( objectMethod, refDef );

    // reference in the method call
    def = getDefinition( "ExpressionResolution", 7, 10 );
    refDef = getDefinition( "ExpressionResolution", 47, 21 );
    assertEquals( def, refDef );
  }

  public void testDottedName() {
    refDef = getDefinition( "getsCalled", 50, 5 );
    assertEquals( intMethod, refDef );

    // references in the method call
    def = getDefinition( "foo", 5, 14 );
    refDef = getDefinition( "foo", 50, 26 );
    assertEquals( def, refDef );

    def = getDefinition( "anObject", 36, 26 );
    refDef = getDefinition( "anObject", 50, 17 );
    assertEquals( def, refDef );
  }

  public void testDottedMethod() {
    refDef = getDefinition( "getsCalled", 52, 5 );
    assertEquals( intMethod, refDef );

    // references in the method call
    def = getDefinition( "returns", 10, 14 );
    refDef = getDefinition( "returns", 52, 26 );
    assertEquals( def, refDef );

    def = getDefinition( "anObject", 36, 26 );
    refDef = getDefinition( "anObject", 52, 17 );
    assertEquals( def, refDef );
  }

  public void testNumericInt() {
    refDef = getDefinition( "getsCalled", 55, 5 );
    assertEquals( intMethod, refDef );
  }

  public void testLiteralDouble() {
    refDef = getDefinition( "getsCalled", 58, 5 );
    assertEquals(doubleMethod, refDef);
  }

  public void testBooleanExpressions() {
    refDef = getDefinition( "getsCalled", 61, 5 );
    assertEquals( booleanMethod, refDef );

    // test references in expression
    def = getDefinition( "aBoolean", 34, 13 );
    refDef = getDefinition( "aBoolean", 61, 17 );
    assertEquals( def, refDef );

    refDef = getDefinition( "getsCalled", 62, 5 );
    assertEquals( booleanMethod, refDef );

    // test references in expression
    def = getDefinition( "aBoolean", 34, 13 );
    refDef = getDefinition( "aBoolean", 62, 26 );
    assertEquals( def, refDef );

    // we'll assume the rest of the cases are covered by the above tests

    refDef = getDefinition( "getsCalled", 63, 5 );
    assertEquals( booleanMethod, refDef );

    refDef = getDefinition( "getsCalled", 64, 5 );
    assertEquals( booleanMethod, refDef );

    refDef = getDefinition( "getsCalled", 65, 5 );
    assertEquals( booleanMethod, refDef );

    refDef = getDefinition( "getsCalled", 66, 5 );
    assertEquals( booleanMethod, refDef );

    refDef = getDefinition( "getsCalled", 67, 5 );
    assertEquals( booleanMethod, refDef );

    refDef = getDefinition( "getsCalled", 68, 5 );
    assertEquals( booleanMethod, refDef );

    refDef = getDefinition( "getsCalled", 70, 5 );
    assertEquals( booleanMethod, refDef );
  }

  // I don't think we handle explicit class references right
  public void testChildrenOfInstanceOf() {

    refDef = getDefinition( "anObject", 70, 17 );
    assertNotNull( refDef );
    assertEquals( anObject, refDef );

    def = getDefinition( "ExpressionResolution", 3, 14 );
    refDef = getDefinition( "ExpressionResolution", 70, 37 );
    assertEquals( def, refDef );
    assertNotNull( refDef );
  }

  public void testBooleanLiterals() {
    refDef = getDefinition( "getsCalled", 73, 5 );
    assertEquals( booleanMethod, refDef );

    refDef = getDefinition( "getsCalled", 74, 5 );
    assertEquals( booleanMethod, refDef );
  }

  public void testIncrementDecrement() {
    refDef = getDefinition( "getsCalled", 77, 5 );
    assertEquals( intMethod, refDef );

    // test creation of incrementee reference
    def = getDefinition( "aInt", 32, 9 );
    refDef = getDefinition( "aInt", 77, 17 );
    assertEquals( def, refDef );

    refDef = getDefinition( "getsCalled", 78, 5 );
    assertEquals( doubleMethod, refDef );

    refDef = getDefinition( "getsCalled", 79, 5 );
    assertEquals( doubleMethod, refDef );

    refDef = getDefinition( "getsCalled", 80, 5 );
    assertEquals( intMethod, refDef );
  }

  public void testUnaryPlusMinus() {
    refDef = getDefinition( "getsCalled", 83, 5 );
    assertEquals( intMethod, refDef );

    // references in method parameters
    def = getDefinition( "aInt", 32, 9 );
    refDef = getDefinition( "aInt", 83, 19 );
    assertEquals( def, refDef );

    refDef = getDefinition( "getsCalled", 84, 5 );
    assertEquals( doubleMethod, refDef );
  }

  public void testArithmeticOperators() {
    refDef = getDefinition( "getsCalled", 87, 5 );
    assertEquals( intMethod, refDef );

    // don't check references here, since they have the same name

    refDef = getDefinition( "getsCalled", 88, 5 );
    assertEquals( doubleMethod, refDef );

    // references in expression
    def = getDefinition( "aInt", 32, 9 );
    refDef = getDefinition( "aInt", 88, 17 );
    assertEquals( def, refDef );

    def = getDefinition( "aDouble", 33, 12 );
    refDef = getDefinition( "aDouble", 88, 24 );
    assertEquals( def, refDef );

    refDef = getDefinition( "getsCalled", 89, 5 );
    assertEquals( doubleMethod, refDef );

    refDef = getDefinition( "getsCalled", 90, 5 );
    assertEquals( intMethod, refDef );

    refDef = getDefinition( "getsCalled", 91, 5 );
    assertEquals( intMethod, refDef );

    refDef = getDefinition( "getsCalled", 93, 5 );
    assertEquals( intMethod, refDef );

    refDef = getDefinition( "getsCalled", 94, 5 );
    assertEquals( intMethod, refDef );

    refDef = getDefinition( "getsCalled", 95, 5 );
    assertEquals( intMethod, refDef );
  }

  public void testMultipleExpression() {
    refDef = getDefinition( "getsCalled", 98, 5 );
    assertEquals( intMethod, refDef );

    def = getDefinition( "aInt", 32, 9 );
    refDef = getDefinition( "aInt", 98, 17 );
    assertEquals( def, refDef );

    def = getDefinition( "returns", 10, 14 );
    refDef = getDefinition( "returns", 98, 24 );
    assertEquals( def, refDef );

    def = getDefinition( "anObject", 36, 26 );
    refDef = getDefinition( "anObject", 98, 36 );
    assertEquals( def, refDef );

    def = getDefinition( "foo", 5, 14 );
    refDef = getDefinition( "foo", 98, 45 );
    assertEquals( def, refDef );
  }

  public void testCharacterLiterals() throws Exception {
    refDef = getDefinition( "getsCalled", 100, 5 );
    assertNotNull( refDef );
    assertEquals( charMethod, refDef);
  }

  public void testLogicalNot() throws Exception {
    refDef = getDefinition("getsCalled", 108, 5);
    assertNotNull("Definition not found.", refDef);
    assertEquals("Definition incorrect.", booleanMethod, refDef);
  }

  public void testLogicalNotChildren() throws Exception {
    IDefinition boooolDef = getDefinition("booool", 107, 13);
    IDefinition boooolRef = getDefinition("booool", 108, 17);
    assertNotNull("Definition not created.", boooolDef);
    assertEquals("Definition not found.", boooolDef, boooolRef);
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { ExpressionResolutionTest.class.getName() });
  }

}
