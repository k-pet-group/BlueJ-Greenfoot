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

import java.io.File;

public class ASTUtilTest extends TestCase {

  public ASTUtilTest(String name) {
  super(name);
  }

  public void testSpliceOneFile() throws Exception {
  File one = new File("test/symtab/util/SpliceFiles/One.java");
  File two = new File("test/symtab/util/SpliceFiles/Two.java");
  File three = new File("test/symtab/util/SpliceFiles/Three.java");
  File[] originals = new File[] {one, two, three};

  FileParser originalParser = new FileParser();
  originalParser.doFile(one);
  originalParser.doFile(two);
  originalParser.doFile(three);

  SymTabAST tree = originalParser.getTree();

  File twoNew = new File("test/symtab/util/SpliceFiles/Two.new.java");

  FileParser newParser = new FileParser();
  newParser.doFile(twoNew);
  SymTabAST newTree = newParser.getTree();
  // cheating to make the splice work right
  ((SymTabAST)newTree.getFirstChild()).setFile(two);

  ASTUtil.spliceFiles(newTree, tree);

  File[] spliced = new File[] {one, twoNew, three};
  FileParser expectedParser = new FileParser();
  expectedParser.doFile(one);
  expectedParser.doFile(twoNew);
  expectedParser.doFile(three);

  assert("AST trees not identical.",
       ASTUtil.treesBelowFilesAreEqual(expectedParser.getTree(), spliced,
                       tree, originals));
  }

  public void testSpliceNewFile() throws Exception {
  File one = new File("test/symtab/util/SpliceFiles/One.java");
  File two = new File("test/symtab/util/SpliceFiles/Two.java");
  File three = new File("test/symtab/util/SpliceFiles/Three.java");
  File[] originals = new File[] {one, two, three};

  FileParser originalParser = new FileParser();
  originalParser.doFile(two);
  originalParser.doFile(three);

  SymTabAST tree = originalParser.getTree();

  FileParser newParser = new FileParser();
  newParser.doFile(one);
  SymTabAST newTree = newParser.getTree();

  ASTUtil.spliceFiles(newTree, tree);

  FileParser expectedParser = new FileParser();
  expectedParser.doFile(one);
  expectedParser.doFile(two);
  expectedParser.doFile(three);

  assert("AST trees not identical.",
       ASTUtil.treesBelowFilesAreEqual(expectedParser.getTree(), originals,
                       tree, originals));

  }

  public void testSpliceMultipleFiles() throws Exception {
  File one = new File("test/symtab/util/SpliceFiles/One.java");
  File two = new File("test/symtab/util/SpliceFiles/Two.java");
  File three = new File("test/symtab/util/SpliceFiles/Three.java");
  File[] originals = new File[] {one, two, three};

  FileParser originalParser = new FileParser();
  originalParser.doFile(one);
  originalParser.doFile(two);
  originalParser.doFile(three);

  SymTabAST tree = originalParser.getTree();

  File oneNew = new File("test/symtab/util/SpliceFiles/One.new.java");
  File twoNew = new File("test/symtab/util/SpliceFiles/Two.new.java");

  FileParser newParser = new FileParser();
  newParser.doFile(twoNew);
  newParser.doFile(oneNew);
  SymTabAST newTree = newParser.getTree();
  // cheating to make the splice work right
  ((SymTabAST)newTree.getFirstChild()).setFile(two);
  ((SymTabAST)newTree.getFirstChild().getNextSibling()).setFile(one);

  ASTUtil.spliceFiles(newTree, tree);

  File[] spliced = new File[] {oneNew, twoNew, three};
  FileParser expectedParser = new FileParser();
  expectedParser.doFile(oneNew);
  expectedParser.doFile(twoNew);
  expectedParser.doFile(three);

  assert("AST trees not identical.",
       ASTUtil.treesBelowFilesAreEqual(expectedParser.getTree(), spliced,
                       tree, originals));
  }

  public static void main(String[] args) {
  junit.swingui.TestRunner.run(ASTUtilTest.class);
  }

}
