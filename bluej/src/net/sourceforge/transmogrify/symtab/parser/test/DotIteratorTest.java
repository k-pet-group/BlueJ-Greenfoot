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

import junit.framework.*;

import antlr.*;
import antlr.collections.AST;

import net.sourceforge.transmogrify.symtab.parser.*;

// stop it! it's too silly
//import org.ramen.*;
//import jp.maruchan.ramen.ChickenRamenImpl;

public class DotIteratorTest extends TestCase {
  ASTFactory factory;

  public DotIteratorTest(String name) {
  super(name);
  }

  public void setUp() throws Exception {
  factory = new ASTFactory();
  factory.setASTNodeType(SymTabAST.class.getName());
  }

  public void testOne() {
  SymTabAST node = (SymTabAST)factory.create(JavaTokenTypes.IDENT, "foo");

  DotIterator it = new DotIterator(node);

  SymTabAST[] expected = { node };
  int i = 0;

  while(it.hasNext()) {
    assertEquals("incorrect node", expected[i], it.next());
    i++;
  }

  assertEquals("Not all nodes reached", 1, i);
  }

  public void testTwo() {
  SymTabAST nodeOne = (SymTabAST)factory.create(JavaTokenTypes.IDENT, "foo");
  SymTabAST nodeTwo = (SymTabAST)factory.create(JavaTokenTypes.IDENT, "bar");
  SymTabAST dot = (SymTabAST)factory.create(JavaTokenTypes.DOT, "");

  dot.addChild(nodeOne);
  dot.addChild(nodeTwo);

  DotIterator it = new DotIterator(dot);

  SymTabAST[] expected = { nodeOne, nodeTwo };
  int i = 0;

  while(it.hasNext()) {
    assertEquals("incorrect node", expected[i], it.next());
    i++;
  }

  assertEquals("Not all nodes reached", 2, i);
  }

  public void testThree() {
  SymTabAST nodeOne = (SymTabAST)factory.create(JavaTokenTypes.IDENT, "foo");
  SymTabAST nodeTwo = (SymTabAST)factory.create(JavaTokenTypes.IDENT, "bar");
  SymTabAST nodeThree =
    (SymTabAST)factory.create(JavaTokenTypes.IDENT, "baz");
  SymTabAST dotOne = (SymTabAST)factory.create(JavaTokenTypes.DOT, "");
  SymTabAST dotTwo = (SymTabAST)factory.create(JavaTokenTypes.DOT, "");

  dotOne.addChild(nodeOne);
  dotOne.addChild(nodeTwo);
  dotTwo.addChild(dotOne);
  dotTwo.addChild(nodeThree);

  DotIterator it = new DotIterator(dotTwo);

  SymTabAST[] expected = { nodeOne, nodeTwo, nodeThree };
  int i = 0;

  while(it.hasNext()) {
    assertEquals("incorrect text", expected[i], it.next());
    i++;
  }

  assertEquals("Not all nodes reached", 3, i);
  }

  public static void main(String[] args) {
  junit.swingui.TestRunner.main(new String[] { DotIteratorTest.class.getName() });
  }
}
