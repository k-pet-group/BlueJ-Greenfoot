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

// $Id: MethodReturnTypeTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.extensions.*;
import junit.framework.*;

import java.io.*;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

public class MethodReturnTypeTest extends TestCase {
  QueryEngine query;
  File theFile = new File("test/Parent.java");
  File otherFile = new File("test/MethodTester.java");

  public MethodReturnTypeTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    FileParser fileParser = new FileParser();

    String[] args = { "test/Parent.java",
                      "test/MethodTester.java" };

    for (int i = 0; i < args.length; i++) {
      fileParser.doFile(new File(args[i]));
    }

    TableMaker maker = new TableMaker( (SymTabAST)(fileParser.getTree() ));
    SymbolTable table = maker.getTable();

    query = new QueryEngine(table);
  }

  public void tearDown() {}

  public void testReturnType() throws Exception {
    IDefinition def = query.getDefinition("echo", new Occurrence(theFile, 10, 14));

    assertNotNull(def);
    assert("not a method", (def instanceof MethodDef));

    IDefinition returnType = ((MethodDef)def).getType();
    assertEquals("Return type incorrect.",
                 new ExternalClass(Integer.TYPE),
                 returnType);
  }

  public void testDefinitionOfReturnDottedClassType() throws Exception {
    IDefinition ref = query.getDefinition("MethodTester", new Occurrence(theFile, 14, 18));
    IDefinition def = query.getDefinition("MethodTester", new Occurrence(otherFile, 3, 14));

    assertNotNull(ref);
    assertEquals(ref,def);
  }

  public void testDefinitionOfReturnInnerClassType() throws Exception {
    IDefinition ref = query.getDefinition("InnerClass", new Occurrence(theFile, 18, 10));
    IDefinition def = query.getDefinition("InnerClass", new Occurrence(theFile, 22, 9));

    assertNotNull(ref);
    assertEquals(ref,def);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { MethodReturnTypeTest.class.getName() });
  }
}
