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
package net.sourceforge.transmogrify.symtab.parser.test;

import antlr.*;
import antlr.collections.*;

import java.io.*;

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;


public class SymTabASTTest extends TestCase {
  SymTabAST original;
  File _file;

  public SymTabASTTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    FileParser fp = new FileParser();
    try {
      _file = new File("test/One.java");
      fp.doFile(_file);

      original = (SymTabAST)fp.getTree();
    }
    catch(Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  public void tearDown() {}

  public void testDeepClone() {
    SymTabAST extraCrispy = original.deepClone();

    assert(extraCrispy.equalsTree(original));
    // make sure they are not object equal
    assert(extraCrispy != original);
  }

  public void testGetEnclosingNodeCursorInToken() throws Exception {
    SymTabAST fileNode = (SymTabAST)original.getFirstChild();
    SymTabAST enclosing = fileNode.getEnclosingNode(10, 8);
    assertNotNull("node not found", enclosing);
  }

  public void testGetEnclosingNodeCursorAtBoundary() throws Exception {
    SymTabAST fileNode = (SymTabAST)original.getFirstChild();
    SymTabAST enclosing = fileNode.getEnclosingNode(10, 12);
    assertNotNull("node not found", enclosing);
  }

  public void testGetEnclosingNodeCursorAtEndOfToken() throws Exception {
    SymTabAST fileNode = (SymTabAST)original.getFirstChild();
    SymTabAST enclosing = fileNode.getEnclosingNode(11, 17);
    assertNotNull("node not found", enclosing);
  }

  public void testGetEnclosingNodeCursorOutsideToken() throws Exception {
    SymTabAST fileNode = (SymTabAST)original.getFirstChild();
    SymTabAST enclosing = fileNode.getEnclosingNode(12, 1);
    assertNotNull("node not found", enclosing);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { SymTabASTTest.class.getName() });
  }
}
