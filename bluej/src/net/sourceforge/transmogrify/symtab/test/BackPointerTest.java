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
import antlr.*;
import antlr.collections.*;

import java.io.*;

public class BackPointerTest extends TestCase {
  AST tree;

  public BackPointerTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    FileParser fp = new FileParser();

    try {
      fp.doFile(new File("test/One.java"));
      tree = fp.getTree();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  public void tearDown() {}

  public void testParentage() {
    // node: CLASS_DEF
    SymTabAST parent = (SymTabAST)tree.getFirstChild().getFirstChild().getNextSibling();
    // node: [One, <IDENT>]
    SymTabAST nodeToTest = (SymTabAST)parent.getFirstChild().getNextSibling();

    assertEquals(parent, nodeToTest.getParent());
  }

  public void testPreviousSibling() {
    // node:
    SymTabAST leftSibling = (SymTabAST)tree.getFirstChild().getFirstChild().getNextSibling().getFirstChild();
    SymTabAST rightSibling = (SymTabAST)leftSibling.getNextSibling();

    assertEquals(leftSibling, rightSibling.getPreviousSibling());
    assert(leftSibling.getPreviousSibling() == null);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { BackPointerTest.class.getName() });
  }
}
