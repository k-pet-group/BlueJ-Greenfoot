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

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import antlr.collections.AST;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

public class ScopeIndexTest extends TestCase {
  SymbolTable table;

  public ScopeIndexTest(String name) {
  super(name);
  }

  public void setUp() throws Exception {
  try {
    FileParser fileParser = new FileParser();

    fileParser.doFile(new File("test/Skimpy.java"));
    fileParser.doFile(new File("test/Blocks.java"));
    fileParser.doFile(new File("test/Parent.java"));
    fileParser.doFile(new File("test/VarTypes.java"));

    TableMaker maker = new TableMaker( (SymTabAST)fileParser.getTree() );
    table = maker.getTable();
  }
  catch (Exception e) {
    e.printStackTrace();
    fail();
  }
  }

  public void tearDown() {}

  public void testLookup() throws Exception {
  ScopeIndex index = table.getScopeIndex();
  Scope scope = index.lookup(new Occurrence(new File("test/Skimpy.java"), 19, 1));
  assertEquals(scope.getName(), "g");
  }

  public void testIndex() throws Exception {
    ScopeIndex index = table.getScopeIndex();

    Enumeration elts = index.getIndex().elements();

    while (elts.hasMoreElements()) {
      Vector vector = (Vector)elts.nextElement();

      for (int i = 0; i < vector.size(); i++) {
        Scope scope = (Scope)vector.elementAt(i);
      }
    }
  }

  public void testBlockLookup() throws Exception {
  ScopeIndex index = table.getScopeIndex();
  Scope scope = index.lookup(new Occurrence(new File("test/Blocks.java"), 10, 1));
  assert("names not similar", scope.getName().startsWith("~Anonymous~"));
  assertEquals(7, scope.getTreeNode().getSpan().getStartLine());
  }

  public void testLookup2() throws Exception {
  ScopeIndex index = table.getScopeIndex();
  Scope scope = index.lookup(new Occurrence(new File("test/Parent.java"), 11, 1));
  assertEquals("echo", scope.getName());
  assertEquals(10, scope.getTreeNode().getSpan().getStartLine());
  assertEquals(12, scope.getTreeNode().getSpan().getEndLine());
  }

  public void testLookup3() throws Exception {
  ScopeIndex index = table.getScopeIndex();
  Scope scope = index.lookup(new Occurrence(new File("test/VarTypes.java"), 7, 1));
  assertEquals("VarTypes", scope.getName());
  assertEquals(6, scope.getTreeNode().getSpan().getStartLine());
  assertEquals(12, scope.getTreeNode().getSpan().getEndLine());
  }

  public static void main(String [] args) {
  junit.swingui.TestRunner.main(new String [] { ScopeIndexTest.class.getName() });
  }
}
