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
import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.*;
import java.io.File;

public class GetFileNodeTest extends TestCase {

  private String path = "test/symtab/util/GetFileNode";

  public GetFileNodeTest(String name) {
  super(name);
  }

  public void testSingleFile() throws Exception {
  File file = new File(path + "/One.java");
  FileParser parser = new FileParser();
  parser.doFile(file);

  SymTabAST root = parser.getTree();
  SymTabAST fileShouldBe = (SymTabAST)root.getFirstChild();
  assertNotNull(fileShouldBe);
  SymTabAST fileFound = ASTUtil.getFileNode(root, file);

  assertNotNull(fileFound);
  assertEquals(fileShouldBe, fileFound);
  }

  public void testTwoFiles() throws Exception {
  File a = new File(path + "/TwoA.java");
  File b = new File(path + "/TwoB.java");

  FileParser parser = new FileParser();
  parser.doFile(a);
  parser.doFile(b);

  SymTabAST root = parser.getTree();
  SymTabAST aShouldBe = (SymTabAST)root.getFirstChild();
  assertNotNull(aShouldBe);
  SymTabAST bShouldBe = (SymTabAST)aShouldBe.getNextSibling();
  assertNotNull(bShouldBe);

  SymTabAST aFound = ASTUtil.getFileNode(root, a);
  assertNotNull(aFound);
  assertEquals(aShouldBe, aFound);

  SymTabAST bFound = ASTUtil.getFileNode(root, b);
  assertNotNull(bFound);
  assertEquals(bShouldBe, bFound);
  }

  public void testFileNotFound() throws Exception {
  File one = new File(path + "/One.java");
  File a = new File(path + "/TwoA.java");
  File b = new File(path + "/TwoB.java");

  FileParser parser = new FileParser();
  parser.doFile(a);
  parser.doFile(b);

  SymTabAST root = parser.getTree();
  SymTabAST oneFound = ASTUtil.getFileNode(root, one);

  assert(oneFound == null);
  }

  public void testDifferentFileObject() throws Exception {
  File a = new File(path + "/TwoA.java");
  File b = new File(path + "/TwoB.java");

  FileParser parser = new FileParser();
  parser.doFile(a);
  parser.doFile(b);

  SymTabAST root = parser.getTree();
  File aEvilTwin = new File(path + "/TwoA.java");

  SymTabAST shouldBe = (SymTabAST)root.getFirstChild();
  assertNotNull("Could not find file node.", shouldBe);

  SymTabAST actual = ASTUtil.getFileNode(root, aEvilTwin);
  assertEquals("Did not find node for specified file.", shouldBe, actual);
  }

  public void testDifferentFileObjectMadeFromPath() throws Exception {
  File a = new File(path + "/TwoA.java");
  File b = new File(path + "/TwoB.java");

  FileParser parser = new FileParser();
  parser.doFile(a);
  parser.doFile(b);

  SymTabAST root = parser.getTree();
  File aEvilTwin = new File(a.getPath());

  assertEquals("The files aren't equal, Ralph.", a, aEvilTwin);

  SymTabAST shouldBe = (SymTabAST)root.getFirstChild();
  assertNotNull("Could not find file node.", shouldBe);

  SymTabAST actual = ASTUtil.getFileNode(root, aEvilTwin);
  assertEquals("Did not find node for specified file.", shouldBe, actual);
  }

  public static void main(String [] args) {
  junit.swingui.TestRunner.main(new String [] { GetFileNodeTest.class.getName() });
  }

}