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

// $Id: CastingTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;
import junit.extensions.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;

public class CastingTest extends DefinitionLookupTest {

  private File file = new File ( "test/Casting.java" );

  IDefinition refDef = null;

  public CastingTest( String name ) {
    super( name );
  }

  public void setUp() throws Exception {
    createQueryEngine(new File[] { file });
  }

  protected IDefinition getDefinition( String name, int lineNumber, int columnNumber ) {
    return getDefinition( file, name, lineNumber, columnNumber );
  }

  public void testCastingType() throws Exception {
    refDef = getDefinition( "Casting", 11, 13 );
    assertNotNull( refDef );

    assertEquals( getDefinition( "Casting", 1, 14 ), refDef );
  }

  public void testMethodCall() throws Exception {
    refDef = getDefinition( "method", 11, 5 );
    assertNotNull( refDef );

    assertEquals( getDefinition( "method", 3, 15 ), refDef );
  }

  public void testCastedObject() throws Exception {
    refDef = getDefinition( "obj", 11, 21 );
    assertNotNull(refDef);

    assertEquals( getDefinition( "obj", 10, 12 ), refDef );
  }

  public void testCastToArrayType() throws Exception {
    IDefinition def = getDefinition("method", 19, 15);
    refDef = getDefinition("method", 16, 5);

    assertNotNull("definition not found", def);
    assertEquals("Definitions do not match", def, refDef);
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { CastingTest.class.getName() });
  }
}
