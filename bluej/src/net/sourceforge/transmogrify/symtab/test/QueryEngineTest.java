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

// $Id: QueryEngineTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import antlr.collections.AST;

import java.io.*;
import java.util.*;

public class QueryEngineTest extends TestCase {
  private SymbolTable table;
  private File theFile;

  static String FILE_NAME = "test/QueryEngine.java";
  static int defLine = 4;
  static int refLine = 21;
  static int numRefs = 3;

  public QueryEngineTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    theFile = new File(FILE_NAME);

    FileParser fileParser = new FileParser();

    fileParser.doFile(theFile);

    TableMaker maker = new TableMaker( (SymTabAST)(fileParser.getTree()) );
    table = maker.getTable();
  }

  public void tearDown() {}

  public void testFindReferenceAtDefinition() throws Exception {
    QueryEngine engine = new QueryEngine(table);
    Reference ref = engine.getSymbol("x", new Occurrence(theFile, 5, 7));
    assertNotNull(ref);
  }

  public void testFindReferenceAtReference() throws Exception {
    QueryEngine engine = new QueryEngine(table);
    Reference ref = engine.getSymbol("x", new Occurrence(theFile, 8, 5));
    assertNotNull(ref);
  }

  public void testGetDefinitionFromReference() throws Exception {
    QueryEngine engine = new QueryEngine(table);
    IDefinition ref_def = engine.getDefinition("x",
                                              new Occurrence(theFile, 8, 5));

    IDefinition def_def = engine.getDefinition("x",
                                              new Occurrence(theFile, 5, 7));

    assertNotNull(def_def);
    assertEquals("Reference does not point to definition", def_def, ref_def);
  }

  public void testGetReferencesFromReference() throws Exception {
    QueryEngine engine = new QueryEngine(table);

    int[] lines = { 5, 8, 9 };

    Iterator references = engine.getReferences("x",
                                               new Occurrence(theFile, 8, 5));

    int[] foundLines = new int[3];
    int i = 0;
    while (references.hasNext()) {
      Reference ref = (Reference)(references.next());
      foundLines[i++] = ref.getLine();
      assertEquals(ref.getName(), "x");
    }

    assertEquals("wrong number of references", 3, i);

    Arrays.sort(lines);
    Arrays.sort(foundLines);

    for (i = 0; i < lines.length; i++) {
      assertEquals(lines[i], foundLines[i]);
    }
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { QueryEngineTest.class.getName() });
  }
}

