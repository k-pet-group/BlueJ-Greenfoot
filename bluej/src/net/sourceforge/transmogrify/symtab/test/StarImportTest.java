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

// $Id: StarImportTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

public class StarImportTest extends DefinitionLookupTest {

  private File importingFile = new File( "test/StarImports.java" );
  private File importedOneFile = new File( "test/StarImportsOne.java" );
  private File importedTwoFile = new File( "test/StarImportsTwo.java" );

  File[] files = new File[] { importingFile,
            importedOneFile,
            importedTwoFile };

  private ClassDef importingClass = null;
  private ClassDef importedClass = null;

  public StarImportTest( String name ) {
    super( name );
  }

  public void setUp() throws Exception {
    query = createQueryEngine( files );

    importingClass = (ClassDef)(getDefinition( importingFile,
                 "StarImports", 5, 14 ));

  }

  public void testAllClassesGetImported() {
    IDefinition classOne = getDefinition( importedOneFile,
           "StarImportsOne", 3, 14 );
    VariableDef defOne = (VariableDef)(getDefinition( importingFile,
                  "one", 7, 18 ));
    assertNotNull( defOne );
    IClass defOneClass = defOne.getType();

    assertEquals( classOne, defOneClass );
    assertNotNull( classOne );


    IDefinition classTwo = getDefinition( importedTwoFile,
           "StarImportsTwo", 3, 14 );
    VariableDef defTwo = (VariableDef)(getDefinition( importingFile,
                  "two", 8, 18 ));
    assertNotNull( defTwo );
    IClass defTwoClass = defTwo.getType();

    assertEquals( classTwo, defTwoClass );
    assertNotNull( classTwo );
  }

  public void testSubPackageNotImported() {
    assertNotNull( importingClass );

    IClass notImported =
      importingClass.getClassDefinition("starImportsThree");

    assertNull(notImported);
  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { StarImportTest.class.getName() });
  }
}
