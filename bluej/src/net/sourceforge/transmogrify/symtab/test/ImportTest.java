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

// $Id: ImportTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;
import junit.extensions.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import java.io.*;

public class ImportTest extends TestCase {

  private File importingFile = new File( "test/Imports.java" );
  private File importedFile = new File( "test/Imported.java" );
  private File importedTooFile = new File( "test/ImportedToo.java" );

  private QueryEngine query = null;

  private IDefinition importedClass = null;
  private ClassDef importingClass = null;

  public ImportTest( String name ) {
    super( name );
  }

  private IDefinition getDefinition( File file, String name, int lineNumber, int columnNumber ) {
    return query.getDefinition( name, new Occurrence( file, lineNumber, columnNumber ) );
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();
      fileParser.doFile( importingFile );
      fileParser.doFile( importedFile );
      fileParser.doFile( importedTooFile );

      TableMaker maker = new TableMaker( (SymTabAST)(fileParser.getTree()) );
      SymbolTable table = maker.getTable();

      query = new QueryEngine( table );

      importingClass = (ClassDef)(getDefinition( importingFile,
             "Imports", 6, 14));
    }
    catch ( Exception e ) {
      e.printStackTrace();
      fail();
    }
  }

  public void testVariablesOfImportedClasses() {
    importedClass = getDefinition( importedFile, "Imported", 3, 14 );
    VariableDef variable = (VariableDef)(getDefinition( importingFile,
              "mailOrderBride",
              8, 12 ) );
    assertNotNull( variable );

    assertEquals( importedClass, variable.getType() );

  }

  public static void main( String[] args ) {
    junit.swingui.TestRunner.main(new String[] { ImportTest.class.getName() });
  }

}
