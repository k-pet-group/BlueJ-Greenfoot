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

// $Id: VariableDefTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;

public class VariableDefTest extends DefinitionLookupTest {

  private File file = new File ( "test/VariableDef.java" );

  IDefinition refDef = null;

  public VariableDefTest( String name ) {
    super( name );
  }

  public void setUp() throws Exception {
    createQueryEngine(new File[] { file });
  }

  protected IDefinition getDefinition( String name, int lineNumber, int columnNumber ) {
    return super.getDefinition( file, name, lineNumber, columnNumber );
  }

  public void testPrimitiveType() throws Exception {
    IVariable ref = (IVariable)getDefinition("a", 5, 15);

    assertNotNull(ref);
    assertEquals("Type of variable incorrect.",
                 new ExternalClass(Integer.TYPE),
                 ref.getType());
  }

  public void testObjectType() throws Exception {
    refDef = getDefinition( "obj", 6, 36 );
    assertNotNull(refDef);

    assertEquals( getDefinition( "VariableDef2", 10, 7), ((VariableDef)refDef).getType() );
  }

  public void testExpressionInAssignment() throws Exception {
    refDef = getDefinition( "VariableDef2", 6, 46 );
    assertNotNull( refDef );

    assertEquals( getDefinition( "VariableDef2", 12, 10), refDef );
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { VariableDefTest.class.getName() });
  }

}
