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

import junit.extensions.*;
import junit.framework.*;

import java.io.*;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import java.util.Vector;

public class MethodExceptionTest extends TestCase {
  QueryEngine query;
  File theFile = new File("test/TestMethod.java");

  public MethodExceptionTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();

      String [] args = { "test/TestMethod.java", "test/ExceptionA.java", "test/ExceptionB.java" };

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

  public void testExceptionList() {
    MethodDef def = (MethodDef)query.getDefinition("bigError", new Occurrence(theFile, 5, 15));

    IClass[] expected = { (IClass)query.getDefinition("ExceptionA", new Occurrence(theFile, 5, 44)),
                          (IClass)query.getDefinition("ExceptionB", new Occurrence(theFile, 5, 56)) };

    IClass[] exceptions = def.getExceptions();
    for (int i = 0; i < exceptions.length; i++) {
      assertEquals(expected[i], exceptions[i]);
    }
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { MethodExceptionTest.class.getName() });
  }
}
