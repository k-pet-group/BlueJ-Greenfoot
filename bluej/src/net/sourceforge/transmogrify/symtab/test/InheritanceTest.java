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
/** TESTS BOTH INHERITANCE AND SELF REFERENCING WITH THIS **/

// $Id: InheritanceTest.java 1014 2001-11-30 03:28:10Z ajp $

package net.sourceforge.transmogrify.symtab.test;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import junit.extensions.*;
import junit.framework.*;

import java.io.*;
import java.util.*;

public class InheritanceTest extends DefinitionLookupTest {
  private File testFile = new File( "test/Inheritance.java" );

  private ClassDef parent = null;
  private ClassDef child = null;
  private IDefinition grandchild = null;
  private IDefinition otherchild = null;
  private IDefinition parentMethod = null;
  private IDefinition parentOverridden = null;
  private IDefinition childOverridden = null;
  private IDefinition parentX = null;

  public InheritanceTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    File[] testFiles = new File[] { testFile };
    query = createQueryEngine( testFiles );

    parent = (ClassDef)(getDefinition( testFile, "Inheritance", 3, 14));
    child = (ClassDef)(getDefinition( testFile, "Child", 14, 7 ));
    grandchild = getDefinition( testFile, "Grandchild", 29, 7 );
    otherchild = getDefinition( testFile, "AnotherChild", 41, 7 );
    parentMethod = getDefinition( testFile, "method", 7, 18 );
    parentOverridden = getDefinition( testFile, "overridden", 10, 18 );
    childOverridden = getDefinition( testFile, "overridden", 16, 18 );
    parentX = getDefinition( testFile, "x", 5, 17 );
  }

  public void tearDown() {}

  public void testChildHasSuperclass() throws Exception {
    assertEquals( parent, child.getSuperclass() );
  }

  public void testSubclasses() throws Exception {
    List children = parent.getSubclasses();

    assertEquals( 2, children.size() );

    for ( int i = 0; i < children.size(); i++ ) {
      ClassDef aChild = (ClassDef)(children.get(i));
      if ( (aChild != child) && (aChild != otherchild) ) {
  System.out.println( aChild );
  fail();
      }
    }
  }

  public void testExtendsClauseReferencesParent() throws Exception {
    IDefinition extendedClass = getDefinition( testFile, "Inheritance", 14, 21 );
    assertNotNull("Extended class should not be null.", extendedClass );
    assertEquals( parent, extendedClass );
  }

  public void testInheritedMethod() throws Exception {
    IDefinition method = getDefinition( testFile, "method", 23, 5 );
    assertNotNull("Inherited method not found.", method );
    assertEquals( parentMethod, method );
  }

  public void testInheritedVar() throws Exception {
    IDefinition var = getDefinition( testFile, "x", 22, 5 );
    assertNotNull( var );
    assertEquals( parentX, var );
  }

  public void testOverriddenMethod() throws Exception {
    IDefinition method = getDefinition( testFile, "overridden", 24, 5 );
    assertNotNull( method );
    assertEquals( childOverridden, method );
  }

  public void testTwiceInheritedMethod(){
    IDefinition method = getDefinition( testFile, "method", 35, 5 );
    assertNotNull( method );
    assertEquals( parentMethod, method );
  }

  public void testTwiceInheritedVar() throws Exception {
    IDefinition var = getDefinition( testFile, "x", 34, 5 );
    assertNotNull( var );
    assertEquals( parentX, var );
  }

  public void testInheritedOverriddenMethod() throws Exception {
    IDefinition method = getDefinition( testFile, "overridden", 36, 5 );
    assertNotNull( method );
    assertEquals( childOverridden, method );
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { InheritanceTest.class.getName() });
  }
}
