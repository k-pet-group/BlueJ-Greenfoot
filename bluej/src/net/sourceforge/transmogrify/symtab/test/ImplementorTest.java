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

// $Id: ImplementorTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.extensions.*;
import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;

import java.util.*;

public class ImplementorTest extends TestCase {
  QueryEngine query;

  public ImplementorTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();

      String [] args = { "test/Implementor.java", "test/Implementee.java" };

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

  public void testImplementingClassReference() {
    IDefinition def = query.getDefinition("Implementee",
                                         new Occurrence(new File("test/Implementee.java"),
                                                        3, 18));

    String [] expected = { "net.sourceforge.transmogrify.symtab.ClassDef[mystuff.Implementor]" };

    assertNotNull(def);
    List implementors = ((ClassDef)def).getImplementors();

    int i;
    for (i = 0; i < implementors.size(); i++) {
      assert("too many implementors", i < 1);
      assertEquals(expected[i], implementors.get(i).toString());
    }

    assert("not enough implementors", i==1);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { ImplementorTest.class.getName() });
  }
}
