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
package net.sourceforge.transmogrify.refactorer.test;

import java.io.*;
import java.util.Vector;

import junit.framework.*;

import net.sourceforge.transmogrify.refactorer.*;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.*;

public class RenameSymbolTest extends TestCase {

  private String path = "test/refactorer/RenameSymbol";

  public RenameSymbolTest(String name) {
    super(name);
  }

  public void testRenameVar() throws Exception {
    File file = new File(path + "/Simple.java");

    FileParser parser = new FileParser();
    parser.doFile(file);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    RenameVariable refactorer = new RenameVariable(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(file, 8, 5 );
    String name = "y";

    refactorer.refactor(location, name);
    SymTabAST refactoredTree = refactorer.getTree();

    assertNotNull( refactoredTree );

    File resultFile =
      new File(path + "/Simple.refactored.java");
    FileParser resultParser = new FileParser();
    resultParser.doFile(resultFile);

    File[] originals = new File[] { file };
    File[] results = new File[] { resultFile };

    assert(ASTUtil.treesBelowFilesAreEqual(refactoredTree, originals,
                                           resultParser.getTree(), results));
  }

  public void testRenameAtDefinition() throws Exception {
    File file = new File(path + "/Simple.java");

    FileParser parser = new FileParser();
    parser.doFile(file);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    RenameVariable refactorer = new RenameVariable(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(file, 5, 15 );
    String name = "y";

    refactorer.refactor(location, name);
    SymTabAST refactoredTree = refactorer.getTree();

    assertNotNull( refactoredTree );

    File resultFile =
      new File(path + "/Simple.refactored.java");
    FileParser resultParser = new FileParser();
    resultParser.doFile(resultFile);

    File[] originals = new File[] { file };
    File[] results = new File[] { resultFile };

    assert(ASTUtil.treesBelowFilesAreEqual(refactoredTree, originals,
                                           resultParser.getTree(), results));

  }

  public void testRenameVarTwoFilesWithUnderscore() throws Exception {
    File one = new File(path + "/FileOne.java");
    File two = new File(path + "/FileTwo.java");
    File[] files = new File[] { one, two };

    FileParser parser = new FileParser();
    parser.doFile(one);
    parser.doFile(two);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    RenameVariable refactorer = new RenameVariable(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(one, 4, 30);
    String name = "YOUR_MOMS_NAME";

    refactorer.refactor(location, name);
    SymTabAST refactoredTree = refactorer.getTree();

    assertNotNull(refactoredTree);

    File oneRef = new File(path + "/FileOne.refactored.java");
    File twoRef = new File(path + "/FileTwo.refactored.java");
    File[] filesRef = new File[] { oneRef, twoRef };

    parser = new FileParser();
    for ( int i = 0; i < filesRef.length; i++ ) {
      parser.doFile(filesRef[i]);
    }

    assert(ASTUtil.treesBelowFilesAreEqual(refactoredTree, files,
                                           parser.getTree(), filesRef));
  }

  public void testRenameMethod() throws Exception {
    File one = new File(path + "/MethodTester.java");
    File two = new File(path + "/MethodTesterTwo.java");
    File[] files = new File[] { one, two };

    FileParser parser = new FileParser();
    parser.doFile(one);
    parser.doFile(two);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    RenameVariable refactorer = new RenameVariable(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(two, 11, 8);
    String name = "newMethod";

    refactorer.refactor(location, name);
    SymTabAST refactoredTree = refactorer.getTree();

    assertNotNull( refactoredTree );

    File oneRef = new File(path + "/MethodTester.refactored.java");
    File twoRef = new File(path + "/MethodTesterTwo.refactored.java");
    File[] filesRef = new File[] { oneRef, twoRef };

    parser = new FileParser();
    for ( int i = 0; i < filesRef.length; i++ ) {
      parser.doFile(filesRef[i]);
    }

    assert(ASTUtil.treesBelowFilesAreEqual(refactoredTree, files,
                                           parser.getTree(), filesRef ));

  }

  public void testRenameMethodOtherFile() throws Exception {
    File one = new File(path + "/MethodTester.java");
    File two = new File(path + "/MethodTesterTwo.java");
    File[] files = new File[] { one, two };

    FileParser parser = new FileParser();
    parser.doFile(one);
    parser.doFile(two);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    RenameVariable refactorer = new RenameVariable(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(one, 7, 5);
    String name = "newMethod";

    refactorer.refactor(location, name);
    SymTabAST refactoredTree = refactorer.getTree();

    assertNotNull( refactoredTree );

    File oneRef = new File(path + "/MethodTester.refactored.java");
    File twoRef = new File(path + "/MethodTesterTwo.refactored.java");
    File[] filesRef = new File[] { oneRef, twoRef };

    parser = new FileParser();
    for ( int i = 0; i < filesRef.length; i++ ) {
      parser.doFile(filesRef[i]);
    }

    assert(ASTUtil.treesBelowFilesAreEqual(refactoredTree, files,
                                           parser.getTree(), filesRef ));

  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { RenameSymbolTest.class.getName() });
  }
}
