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

// $Id: SwitchStatementTest.java 1014 2001-11-30 03:28:10Z ajp $

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;

import junit.framework.*;

public class SwitchStatementTest extends TestCase {
  QueryEngine query;
  File cur_file = new File("test/SwitchTest.java");

  public SwitchStatementTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();

      String [] args = { "test/SwitchTest.java" };

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

  public void testMethodVar() {

    Occurrence org_occ = new Occurrence(cur_file, 6, 9);
    IDefinition org_def = query.getDefinition("i", org_occ);

    assertNotNull(org_def);

    Occurrence cur_occ = new Occurrence(cur_file, 20, 9);
    IDefinition cur_def = query.getDefinition("i", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);

  }

  public void testScopeLocalReference() {

    Occurrence org_occ = new Occurrence(cur_file, 12, 13);
    IDefinition org_def = query.getDefinition("local", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 19, 9);
    IDefinition cur_def = query.getDefinition("local", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);
  }

  public void testClassVarReference() {
    Occurrence org_occ = new Occurrence(cur_file, 4, 8);
    IDefinition org_def = query.getDefinition("d", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 21, 9);
    IDefinition cur_def = query.getDefinition("d", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);
  }

  public void testReferenceOutsideScope() {

    Occurrence org_occ = new Occurrence(cur_file, 8, 9);
    IDefinition org_def = query.getDefinition("j", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 32, 7);
    IDefinition cur_def = query.getDefinition("j", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(org_def);
  }


  public void testReferenceInsideDefaultScope() {

    Occurrence org_occ = new Occurrence(cur_file, 12, 13);
    IDefinition org_def = query.getDefinition("local", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 25, 9);
    IDefinition cur_def = query.getDefinition("local", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(org_def);
  }


  public void testReferenceOutsideDefaultScope() {

    Occurrence org_occ = new Occurrence(cur_file, 6, 9);
    IDefinition org_def = query.getDefinition("i", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 26, 9);
    IDefinition cur_def = query.getDefinition("i", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(org_def);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { SwitchStatementTest.class.getName() });
  }
}
