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

// $Id: IfElseTest.java 1014 2001-11-30 03:28:10Z ajp $

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;

import junit.extensions.*;
import junit.framework.*;

public class IfElseTest extends TestCase {
  QueryEngine query;
  File theFile = new File("test/TestIfElse.java");

  public IfElseTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();

      String [] args = { "test/TestIfElse.java" };

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

  public void testScopeLocalVarIf() {

    IDefinition ref = query.getDefinition("x", new Occurrence(theFile, 16, 7));
    IDefinition def = query.getDefinition("x", new Occurrence(theFile, 14, 11));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testScopeLocalVarElseIf() {
    IDefinition ref = query.getDefinition("x", new Occurrence(theFile, 21, 7));
    IDefinition def = query.getDefinition("x", new Occurrence(theFile, 19, 11));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testScopeLocalVarElse() {
    IDefinition ref = query.getDefinition("y", new Occurrence(theFile, 26, 7));
    IDefinition def = query.getDefinition("y", new Occurrence(theFile, 24, 14));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testReferenceInCondition() {
    IDefinition ref = query.getDefinition("num", new Occurrence(theFile, 8, 11));
    IDefinition def = query.getDefinition("num", new Occurrence(theFile, 6, 9));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testReferenceToOutsideVar() {
    IDefinition ref = query.getDefinition("num", new Occurrence(theFile, 10, 7));
    IDefinition def = query.getDefinition("num", new Occurrence(theFile, 6, 9));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testReferenceToOutsideVar2() {
    IDefinition ref = query.getDefinition("val", new Occurrence(theFile, 9, 7));
    IDefinition def = query.getDefinition("val", new Occurrence(theFile, 5, 13));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { IfElseTest.class.getName() });
  }
}
