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
/** TESTS INTERFACES **/

// $Id: InterfaceTest.java 1014 2001-11-30 03:28:10Z ajp $

package net.sourceforge.transmogrify.symtab.test;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import junit.framework.*;

import java.io.*;

public class InterfaceTest extends TestCase {
  FileParser fileParser;
  PrintStream result;
  String sourceDir = "..\\myTest";

  public InterfaceTest(String name) {
    super(name);
  }

  private SymbolTable makeSymbolTable() throws Exception {
    SymbolTable table = null;

    try {
      // Opens the file specified by 'fileName', parses it
      // as necessary andd produces the resulting SymbolTable
      // This is then returned.
      File sourceFile = new File(sourceDir);

      FileParser fileParser = new FileParser();
      fileParser.doFile(sourceFile);

      TableMaker maker = new TableMaker( (SymTabAST)(fileParser.getTree()) );
      table = maker.getTable();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    return table;
  }

  public void setUp() {
  }

  public void tearDown() {}

  public void testInterfaceConstant() throws Exception {
  // check scope of interface constants

    // symbol table to test
    SymbolTable scrappy = makeSymbolTable();

    // file we're testing
    File cur_file = new File("..\\myTest\\itchy.java");

    QueryEngine query = new QueryEngine(scrappy);

    // check scope for interface constant, defined in Punchy
    Occurrence cur_occur = new Occurrence(cur_file, 15, 13);
    IDefinition cur_def = query.getDefinition("constant2", cur_occur);
    String testString = cur_def.getQualifiedName();
    assert( testString.equals("simple.itchy.constant2") );
  }

  public void testInterfaceMethod() throws Exception {
  // check scope of interface method

    // symbol table to test
    SymbolTable scrappy = makeSymbolTable();

    // file we're testing
    File cur_file = new File("..\\myTest\\itchy.java");

    QueryEngine query = new QueryEngine(scrappy);

    // check scope for interface constant, defined in Punchy
    Occurrence cur_occur = new Occurrence(cur_file, 18, 14);
    IDefinition cur_def = query.getDefinition("returnScent", cur_occur);
    String testString = cur_def.getQualifiedName();
    assert( testString.equals("simple.itchy.returnScent") );
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { InterfaceTest.class.getName() });
  }
}
