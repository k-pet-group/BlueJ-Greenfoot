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

// $Id: TryCatchTest.java 1014 2001-11-30 03:28:10Z ajp $

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import junit.extensions.*;
import junit.framework.*;

public class TryCatchTest extends TestCase {
  QueryEngine query;
  File cur_file = new File("test/tryCatch.java");


  public TryCatchTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();

      String [] args = { "test/tryCatch.java" };

      for (int i = 0; i < args.length; i++) {
        fileParser.doFile(new File(args[i]));
      }

      TableMaker maker = new TableMaker( (SymTabAST)fileParser.getTree() );

      SymbolTable table = maker.getTable();

      query = new QueryEngine(table);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  public void tearDown() {}

  public void testRefernceInTry() {

    Occurrence org_occ = new Occurrence(cur_file, 12, 13);
    IDefinition org_def = query.getDefinition("c", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 14, 9);
    IDefinition cur_def = query.getDefinition("c", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);

  }

  public void testRefernceInCatch() {

    Occurrence org_occ = new Occurrence(cur_file, 17, 13);
    IDefinition org_def = query.getDefinition("c", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 19, 9);
    IDefinition cur_def = query.getDefinition("c", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);

  }

  public void testRefernceInFinally() {

    Occurrence org_occ = new Occurrence(cur_file, 27, 13);
    IDefinition org_def = query.getDefinition("c", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 29, 9);
    IDefinition cur_def = query.getDefinition("c", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);

  }

  public void testRefernceInTryWithNoFinally() {

    Occurrence org_occ = new Occurrence(cur_file, 39, 13);
    IDefinition org_def = query.getDefinition("c", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 41, 9);
    IDefinition cur_def = query.getDefinition("c", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);

  }

 public void testRefernceInCatchWithNoFinally() {

    Occurrence org_occ = new Occurrence(cur_file, 44, 13);
    IDefinition org_def = query.getDefinition("c", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 46, 9);
    IDefinition cur_def = query.getDefinition("c", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);

  }

 public void testRefernceIn2ndCatch() {

    Occurrence org_occ = new Occurrence(cur_file, 22, 13);
    IDefinition org_def = query.getDefinition("c", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 24, 9);
    IDefinition cur_def = query.getDefinition("c", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);

  }

 public void testRefernceInTryInsideMethod() {

    Occurrence org_occ = new Occurrence(cur_file, 36, 11);
    IDefinition org_def = query.getDefinition("b", org_occ);

    Occurrence cur_occ = new Occurrence(cur_file, 40, 9);
    IDefinition cur_def = query.getDefinition("b", cur_occ);

    assertEquals(org_def, cur_def);
    assertNotNull(cur_def);

  }



  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { TryCatchTest.class.getName() });
  }
}
