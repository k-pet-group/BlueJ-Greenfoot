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

// $Id: VarTypesTest.java 1014 2001-11-30 03:28:10Z ajp $

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import junit.framework.*;

import java.io.*;
import java.util.Vector;

public class VarTypesTest extends TestCase {
  FileParser fileParser;
  PrintStream result;
  String testFiles[] = {"test/VarTypes.java"};

  public VarTypesTest(String name) {
    super(name);
  }

  private SymbolTable makeSymbolTable() {
    // Opens the files listed filez
    // loaded to the SymbolTable returned
    SymbolTable table = null;

    try {
      FileParser fileParser = new FileParser();

      for (int i = 0; i < testFiles.length; i++) {
        fileParser.doFile(new File(testFiles[i]));
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

  public void setUp() {}

  public void tearDown() {}

  public void testIntVar() throws Exception {
  // check scope of Index Var

    // symbol table to test
    SymbolTable symTab = makeSymbolTable();

    // file we're testing
    File cur_file = new File(testFiles[0]);

    QueryEngine query = new QueryEngine(symTab);
    Occurrence cur_occur = new Occurrence(cur_file, 7, 9);
    IDefinition cur_def = query.getDefinition("var2", cur_occur);

    assertNotNull( cur_def );
    IClass c_def = ((VariableDef)cur_def).getType();
    assertNotNull(c_def);
    assertEquals("Type for int incorrect.",
                 new ExternalClass(Integer.TYPE),
                 c_def);
  }

  public void testFloatVar() throws Exception {
  // check scope of Index Var

    // symbol table to test
    SymbolTable symTab = makeSymbolTable();

    // file we're testing
    File cur_file = new File(testFiles[0]);

    QueryEngine query = new QueryEngine(symTab);
    Occurrence cur_occur = new Occurrence(cur_file, 9, 11);
    IDefinition cur_def = query.getDefinition("myFloat", cur_occur);

    assertNotNull( cur_def );
    IClass c_def = ((VariableDef)cur_def).getType();
    assertNotNull(c_def);
    assertEquals("Type for float incorrect.",
                 new ExternalClass(Float.TYPE),
                 c_def);
  }

  public void testCharVar() throws Exception {
  // check scope of Index Var

    // symbol table to test
    SymbolTable symTab = makeSymbolTable();

    // file we're testing
    File cur_file = new File(testFiles[0]);

    QueryEngine query = new QueryEngine(symTab);
    Occurrence cur_occur = new Occurrence(cur_file, 10, 10);
    IDefinition cur_def = query.getDefinition("myChar", cur_occur);

    assertNotNull(cur_def);
    IClass c_def = ((VariableDef)cur_def).getType();
    assertNotNull(c_def);
    assertEquals("Class for char incorrect.",
                 new ExternalClass(Character.TYPE),
                 c_def);
   }

  public void testStringVar() throws Exception {

      // symbol table to test
      SymbolTable symTab = makeSymbolTable();

      // file we're testing
      File cur_file = new File(testFiles[0]);

      QueryEngine query = new QueryEngine(symTab);
      Occurrence cur_occur = new Occurrence(cur_file, 11, 12);
      IDefinition cur_def = query.getDefinition("myString", cur_occur);

      assertNotNull( cur_def );
      IClass c_def = ((VariableDef)cur_def).getType();
      assertEquals("Class for String incorrect.",
                   new ExternalClass("".getClass()), c_def);
  }


  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { VarTypesTest.class.getName() });
  }
}
