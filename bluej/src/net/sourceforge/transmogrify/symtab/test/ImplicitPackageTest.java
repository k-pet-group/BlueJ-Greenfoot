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

// $Id: ImplicitPackageTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;
import junit.extensions.*;

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

public class ImplicitPackageTest extends DefinitionLookupTest {

  File fileOne = new File( "test/ImplicitPackage.java" );
  File fileTwo = new File( "test/ImplicitPackageTwo.java" );
  File fileThree = new File( "test/implicit_package/ImplicitPackageThree.java" );

  Definition classOne = null;
  Definition classTwo = null;
  Definition classThree = null;
  Definition method = null;

  IDefinition refDef = null;

  public ImplicitPackageTest( String name ) {
    super( name );
  }

  public void setUp() throws Exception {
    createQueryEngine(new File[] { fileThree, fileTwo, fileOne });

    classOne = (Definition)getDefinition( fileOne, "ImplicitPackage", 1, 14 );
    classTwo = (Definition)getDefinition( fileTwo, "ImplicitPackageTwo", 1, 14 );
    classThree = (Definition)getDefinition( fileThree, "ImplicitPackageThree", 1, 14 );
    method = (Definition)getDefinition( fileOne, "method", 3, 8 );
  }

  public void testPackagesInSameDirectory() {
    assertNotNull( classOne.getEnclosingPackage() );
    assertEquals( classOne.getEnclosingPackage(),
                  classTwo.getEnclosingPackage() );
  }

  public void testClassVisibility() {
    refDef = getDefinition( fileTwo, "ImplicitPackage", 3, 3 );
    assertNotNull( refDef );
    assertEquals( classOne, refDef );

    refDef = getDefinition( fileTwo, "method", 6, 9 );
    assertNotNull( refDef );
    assertEquals( method, refDef );
  }

  public void testPackagesInDifferentDirectories() {
    assertNotNull( classOne.getEnclosingPackage() );
    assert(classOne.getEnclosingPackage() != classThree.getEnclosingPackage());
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { ImplicitPackageTest.class.getName() });
  }



}
