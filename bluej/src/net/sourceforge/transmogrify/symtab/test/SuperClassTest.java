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

// $Id: SuperClassTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import java.io.*;

import java.util.*;

public class SuperClassTest extends TestCase {
  private SymbolTable table;
  private QueryEngine query;

  public SuperClassTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    try {
      FileParser fileParser = new FileParser();

      String [] args = { "test/Parent.java",
                         "test/Child.java",
                         "test/GrandChild.java",
                         "test/OtherChild.java" };

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

  public void testMethodParameterReference() {
    File theFile = new File("test/Parent.java");

    IDefinition ref = query.getDefinition("x", new Occurrence(theFile, 11, 12));
    IDefinition def = query.getDefinition("x", new Occurrence(theFile, 10, 23));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testInheritedMethodReference() {
    File theFile = new File("test/Child.java");
    File otherFile = new File("test/Parent.java");

    IDefinition ref = query.getDefinition("echo", new Occurrence(theFile, 6, 5));
    IDefinition def = query.getDefinition("echo", new Occurrence(otherFile, 10, 14));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testDoubleInheritedMethodReference() {
    File theFile = new File("test/GrandChild.java");
    File otherFile = new File("test/Parent.java");

    IDefinition ref = query.getDefinition("echo", new Occurrence(theFile, 6, 5));
    IDefinition def = query.getDefinition("echo", new Occurrence(otherFile, 10, 14));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testInheritedVariableReference() {
    File theFile = new File("test/Child.java");
    File otherFile = new File("test/Parent.java");

    IDefinition ref = query.getDefinition("z", new Occurrence(theFile, 7, 5));
    IDefinition def = query.getDefinition("z", new Occurrence(otherFile, 4, 7));

    assertNotNull(def);
    assertEquals(ref, def);
  }

  public void testSuperClass() {
    File theFile = new File("test/Child.java");

    IDefinition def = query.getDefinition("Child", new Occurrence(theFile, 3, 14));

    assertNotNull(def);
    assertEquals("net.sourceforge.transmogrify.symtab.ClassDef[hierarchy.Parent]", ((ClassDef)def).getSuperclass().toString());

  }

  public void testSubclasses() {
    File theFile = new File("test/Parent.java");

    IDefinition def = query.getDefinition("Parent", new Occurrence(theFile, 3, 14));

    assertNotNull(def);

    List extensions = ((ClassDef)def).getSubclasses();

    int i;

    for (i = 0; i < extensions.size(); i++) {
      System.out.println("reference " + i);
      assert("too many references", i<2);
      assertEquals("net.sourceforge.transmogrify.symtab.ClassDef[hierarchy.Parent]", ((ClassDef)extensions.get(i)).getSuperclass().toString());
    }

    assert("not enough references", i==2);
  }

  public void testObjectIsSuperClass() {
    File theFile = new File("../test/Parent.java");

    IDefinition def = query.getDefinition("Parent", new Occurrence(theFile, 3, 14));
    assertNotNull(def);
    IClass superclass = ((ClassDef)def).getSuperclass();
System.out.println(superclass);
    assertEquals("java.lang.Object is not superclass", new ExternalClass(java.lang.Object.class), superclass);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { SuperClassTest.class.getName() });
  }
}
