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

package net.sourceforge.transmogrify.symtab.test;

// $Id: ForLoopTest.java 1014 2001-11-30 03:28:10Z ajp $

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import junit.framework.*;

import java.io.*;
import java.util.Vector;

public class ForLoopTest extends TestCase {
  FileParser fileParser;
  PrintStream result;
  int numTestFiles = 1; // current number of test files in test Dir
  String testFiles[];

  public ForLoopTest(String name) {
    super(name);
  }

  public void loadTestFileArray(){
    testFiles = new String[numTestFiles];
    testFiles[0] = "test/ForLoops.java";
  }

  public Vector loadAllTestFiles() {
  // loads all test files in the test directory
  // into a vector that can then be used to make a Symbol
  // table. This Vector is then returned.

    Vector V = new Vector();
    for (int i = 0; i < numTestFiles; i++ ) {
      V.addElement(testFiles[i]);
    }
    return V;

  }

  private SymbolTable makeSymbolTable(Vector filez) throws Exception {
    // Opens the files listed filez
    // loaded to the SymbolTable returned
    SymbolTable table = null;

    try {
      FileParser fileParser = new FileParser();

      for (int i = 0; i < filez.size(); i++) {
        File sourceFile = new File((String)filez.elementAt(i));
        fileParser.doFile(sourceFile);
      }

      TableMaker maker = new TableMaker( (SymTabAST)(fileParser.getTree()) );
      table = maker.getTable();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    return table;

  }// end - SymbolTable()

  public void setUp() {
    //load test files for easy loading
    loadTestFileArray();
  }

  public void tearDown() {}

  public void testIndexVar() throws Exception {
  // check scope of Index Var
    Vector test = new Vector();

    // symbol table to test
    test.addElement(testFiles[0]);
    SymbolTable symTab = makeSymbolTable(test);

    // file we're testing
    File cur_file = new File(testFiles[0]);

    QueryEngine query = new QueryEngine(symTab);

    // check scope for the method this in scrappy.java
    Occurrence cur_occur = new Occurrence(cur_file, 8, 14);
    IDefinition cur_def = query.getDefinition("i", cur_occur);

    Occurrence other_occur = new Occurrence(cur_file, 10, 15);
    IDefinition other_def = query.getDefinition("i", other_occur);

    assertEquals(cur_def, other_def);
    assert(cur_def==other_def);
    assertNotNull(cur_occur);
  }

  public void testIndexVar2() throws Exception {
  // check scope of Index Var Not Equal outside scope
    Vector test = new Vector();

    // symbol table to test
    test.addElement(testFiles[0]);
    SymbolTable symTab = makeSymbolTable(test);

    // file we're testing
    File cur_file = new File(testFiles[0]);

    QueryEngine query = new QueryEngine(symTab);

    // check scope for the method this in scrappy.java
    Occurrence cur_occur = new Occurrence(cur_file, 8, 14);
    IDefinition cur_def = query.getDefinition("i", cur_occur);

    Occurrence other_occur = new Occurrence(cur_file, 13, 9);
    IDefinition other_def = query.getDefinition("i", other_occur);

    assert(cur_def!=other_def);
    assertNotNull(cur_occur);
  }

  public void testIndexVar3() throws Exception {
    // check scope of Index Var Not Equal outside scope
      Vector test = new Vector();

      // symbol table to test
      test.addElement(testFiles[0]);
      SymbolTable symTab = makeSymbolTable(test);

      // file we're testing
      File cur_file = new File(testFiles[0]);

      QueryEngine query = new QueryEngine(symTab);

      // check scope for the method this in scrappy.java
      Occurrence cur_occur = new Occurrence(cur_file, 8, 16);
      IDefinition cur_def = query.getDefinition("i", cur_occur);

      Occurrence other_occur = new Occurrence(cur_file, 20, 14);
      IDefinition other_def = query.getDefinition("i", other_occur);

      assert(cur_def!=other_def);
      assertNotNull(cur_occur);
    }

  public void testLoopVar() throws Exception {
  // check scope of Index Var Not Equal outside scope
    Vector test = new Vector();

    // symbol table to test
    test.addElement(testFiles[0]);
    SymbolTable symTab = makeSymbolTable(test);

    // file we're testing
    File cur_file = new File(testFiles[0]);

    QueryEngine query = new QueryEngine(symTab);

    // check scope for the method this in scrappy.java
    Occurrence cur_occur = new Occurrence(cur_file, 9, 11);
    IDefinition cur_def = query.getDefinition("myVar", cur_occur);

    Occurrence other_occur = new Occurrence(cur_file, 12, 9);
    IDefinition other_def = query.getDefinition("myVar", other_occur);

    assert(cur_def!=other_def);
    assertNotNull(cur_occur);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { ForLoopTest.class.getName() });
  }
}
