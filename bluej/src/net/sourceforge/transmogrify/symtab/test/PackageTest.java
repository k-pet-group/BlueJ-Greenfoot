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

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;
import java.util.Vector;

import junit.framework.*;

public class PackageTest extends TestCase {
  QueryEngine query;

  public PackageTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();

      String [] args = { "test/Implementor.java", "test/Implementee.java" };

      for (int i = 0; i < args.length; i++) {
        fileParser.doFile(new File(args[i]));
      }

      TableMaker maker = new TableMaker((SymTabAST)(fileParser.getTree()) );
      SymbolTable table = maker.getTable();

      query = new QueryEngine(table);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  public void tearDown() {}

  public void testFoo() {
    Definition def
      = (Definition)query.getDefinition("Implementor",
                                        new Occurrence(
                                          new File("test/Implementor.java"),
                                          3,
                                          14));

    scopeDef(def);

    def = (Definition)query.getDefinition("Implementee",
                                          new Occurrence(new File("test/Implementor.java"),
                                                         3, 37));

    scopeDef(def);
    // REDTAG -- what the hell is this doing???
    fail();
  }

  private void scopeDef(Definition def) {
    assertNotNull(def);
    while (def != null) {
      System.out.println("  Definition: " + def);
      def = def.getParentScope();
    }
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.run(PackageTest.class);
  }
}
