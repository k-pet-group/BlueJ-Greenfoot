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

// $Id: CommaForLoopsTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;
import junit.extensions.*;

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

public class CommaForLoopsTest extends DefinitionLookupTest {

  private File file = new File( "test/CommaForLoops.java" );
  private File[] files = new File[] { file };

  IDefinition def;

  public CommaForLoopsTest( String name ) {
    super( name );
  }

  public void setUp() throws Exception {
    createQueryEngine( files );
    def = null;
  }

  public void testMultipleVariableDeclaration() throws Exception {
    def = getDefinition(file, "a", 5, 18);
    assertNotNull(def);
    assertEquals(getDefinition(file, "a", 4, 13), def);

    def = getDefinition(file, "b", 6, 14);
    assertNotNull(def);
    assertEquals(getDefinition(file, "b", 4, 17), def);

    def = getDefinition(file, "c", 7, 14);
    assertNotNull(def);
    assertEquals(getDefinition(file, "c", 4, 21), def);
  }

  public void testMultipleIterators() throws Exception {
    def = getDefinition(file, "a", 4, 45);
    assertNotNull(def);
    assertEquals(getDefinition(file, "a", 4, 13), def);

    def = getDefinition(file, "b", 4, 49);
    assertNotNull(def);
    assertEquals(getDefinition(file, "b", 4, 17), def);

    def = getDefinition(file, "c", 4, 53);
    assertNotNull(def);
    assertEquals(getDefinition(file, "c", 4, 21), def);
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { CommaForLoopsTest.class.getName() });
  }

}


