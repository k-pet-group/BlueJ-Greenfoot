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

// $Id: WhileLoopTest.java 1014 2001-11-30 03:28:10Z ajp $

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

import net.sourceforge.transmogrify.symtab.parser.*;

import junit.extensions.*;
import junit.framework.*;

public class WhileLoopTest extends TestCase {
  QueryEngine query;
  File theFile = new File("test/TestWhileLoop.java");

  public WhileLoopTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();

      String [] args = { "test/TestWhileLoop.java" };

      for (int i = 0; i < args.length; i++) {
        fileParser.doFile(new File(args[i]));
      }

      TableMaker maker = new TableMaker( (SymTabAST)(fileParser.getTree()) );

      SymbolTable table = maker.getTable();

      query = new QueryEngine(table);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  public void tearDown() {}

  public void testRefernceInCondition() {
    IDefinition ref = query.getDefinition("x", new Occurrence(theFile, 9, 12));
    IDefinition def = query.getDefinition("x", new Occurrence(theFile, 4, 7));

    assertNotNull(def);
    assertEquals(ref,def);
  }

  public void testScopeLocalReference() {
    IDefinition ref = query.getDefinition("y", new Occurrence(theFile, 11, 7));
    IDefinition def = query.getDefinition("y", new Occurrence(theFile, 10, 14));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testClassVarReference() {
    IDefinition ref = query.getDefinition("x", new Occurrence(theFile, 12, 7));
    IDefinition def = query.getDefinition("x", new Occurrence(theFile, 4, 7));
    assertNotNull(def);
    assertEquals(ref,def);
  }

  public void testReferenceOutsideScope() {
    IDefinition ref = query.getDefinition("y", new Occurrence(theFile, 15, 5));
    IDefinition def = query.getDefinition("y", new Occurrence(theFile, 7, 13));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { WhileLoopTest.class.getName() });
  }
}
