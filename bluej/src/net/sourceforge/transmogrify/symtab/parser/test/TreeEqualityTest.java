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


import java.io.File;

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.parser.*;


public class TreeEqualityTest extends TestCase {

  private String path = "test/symtab/util/TreeEquality";

  public TreeEqualityTest(String name) {
  super(name);
  }

  public void testIdenticalSourceFiles() throws Exception {
  File original = new File(path + "/Simple.java");
  FileParser originalParser = new FileParser();
  originalParser.doFile(original);

  File compared = new File(path + "/SimpleIdentical.java");
  FileParser comparedParser = new FileParser();
  comparedParser.doFile(compared);

  assert(ASTUtil.treesBelowFilesAreEqual(originalParser.getTree(),
                       new File[] { original },
                       comparedParser.getTree(),
                       new File[] { compared }));
  }

  public void testSyntacticallyIdenticalSourceFiles() throws Exception {
  File original = new File(path + "/Simple.java");
  FileParser originalParser = new FileParser();
  originalParser.doFile(original);

  File compared = new File(path + "/SimpleUgly.java");
  FileParser comparedParser = new FileParser();
  comparedParser.doFile(compared);

  assert(ASTUtil.treesBelowFilesAreEqual(originalParser.getTree(),
                       new File[] { original },
                       comparedParser.getTree(),
                       new File[] { compared }));
  }

  public void testSyntacticallyDifferentSourceFiles() throws Exception {
  File original = new File(path + "/Simple.java");
  FileParser originalParser = new FileParser();
  originalParser.doFile(original);

  File compared = new File(path + "/SimpleDifferent.java");
  FileParser comparedParser = new FileParser();
  comparedParser.doFile(compared);

  assert(!ASTUtil.treesBelowFilesAreEqual(originalParser.getTree(),
                      new File[] { original },
                      comparedParser.getTree(),
                      new File[] { compared }));
  }

  public void testMultipleSourceFiles() throws Exception {
  File aOriginal = new File(path + "/AOriginal.java");
  File bOriginal = new File(path + "/BOriginal.java");
  File cOriginal = new File(path + "/COriginal.java");
  File[] originals = new File[] { aOriginal, bOriginal, cOriginal };

  FileParser originalParser = new FileParser();
  for ( int i = 0; i < originals.length; i++ ) {
    originalParser.doFile(originals[i]);
  }

  File aCompared = new File(path + "/ACompared.java");
  File bCompared = new File(path + "/BCompared.java");
  File cCompared = new File(path + "/CCompared.java");
  File[] compared = new File[] { aCompared, bCompared, cCompared };

  FileParser comparedParser = new FileParser();
  for ( int i = 0; i < compared.length; i++ ) {
    comparedParser.doFile(compared[i]);
  }

  //TreePane.showTree(originalParser.getTree());
  //TreePane.showTree(comparedParser.getTree());

  assert(ASTUtil.treesBelowFilesAreEqual(originalParser.getTree(),
                       originals,
                       comparedParser.getTree(),
                       compared));
  }

  public void testMultipleSourceFilesOutOfOrder() throws Exception {
  File aOriginal = new File(path + "/AOriginal.java");
  File bOriginal = new File(path + "/BOriginal.java");
  File cOriginal = new File(path + "/COriginal.java");
  File[] originals = new File[] { aOriginal, bOriginal, cOriginal };

  FileParser originalParser = new FileParser();
  originalParser.doFile(bOriginal);
  originalParser.doFile(aOriginal);
  originalParser.doFile(cOriginal);

  File aCompared = new File(path + "/ACompared.java");
  File bCompared = new File(path + "/BCompared.java");
  File cCompared = new File(path + "/CCompared.java");
  File[] compared = new File[] { aCompared, bCompared, cCompared };

  FileParser comparedParser = new FileParser();
  comparedParser.doFile(cCompared);
  comparedParser.doFile(bCompared);
  comparedParser.doFile(aCompared);

  assert(ASTUtil.treesBelowFilesAreEqual(originalParser.getTree(),
                       originals,
                       comparedParser.getTree(),
                       compared));
 }

  public void testMultipleDifferentSourceFiles() throws Exception {
  File aOriginal = new File(path + "/AOriginal.java");
  File bOriginal = new File(path + "/BOriginal.java");
  File cOriginal = new File(path + "/COriginal.java");
  File[] originals = new File[] { aOriginal, bOriginal, cOriginal };

  FileParser originalParser = new FileParser();
  for ( int i = 0; i < originals.length; i++ ) {
    originalParser.doFile(originals[i]);
  }

  File aCompared = new File(path + "/ACompared.java");
  File bCompared = new File(path + "/BCompared.java");
  File cCompared = new File(path + "/CDifferent.java");
  File[] compared = new File[] { aCompared, bCompared, cCompared };

  FileParser comparedParser = new FileParser();
  for ( int i = 0; i < compared.length; i++ ) {
    comparedParser.doFile(compared[i]);
  }

  assert(!ASTUtil.treesBelowFilesAreEqual(originalParser.getTree(),
                      originals,
                      comparedParser.getTree(),
                      compared));
  }

  public static void main(String [] args) {
  junit.swingui.TestRunner.main(new String[] { TreeEqualityTest.class.getName() });
  }


}