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

import junit.extensions.*;
import junit.framework.*;

import net.sourceforge.transmogrify.refactorer.*;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.*;

public class InlineTempTest extends TestCase {
  private String path = "test/refactorer/InlineTemp";

  public InlineTempTest(String name) {
    super(name);
  }

  public void testDefAndAssignOnSameLine() throws Exception {
    File original = new File(path + "/InlineTempTesterTwo.java");

    FileParser parser = new FileParser();
    parser.doFile(original);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    InlineTemp refactorer = new InlineTemp(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(original, 7, 14);
    refactorer.refactor(location);

    File compared = new File(path + "/InlineTempTesterTwo.refactored.java");
    parser = new FileParser();
    parser.doFile(compared);

    assert(ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(),
                                           new File[] { original },
                                           parser.getTree(),
                                           new File[] {compared }));
  }

  public void testDefAndAssignOnDifferentLinesUsingAssign() throws Exception {
    File original = new File(path + "/InlineTempTester.java");
    FileParser parser = new FileParser();
    parser.doFile(original);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    InlineTemp refactorer = new InlineTemp(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(original, 8, 7);
    refactorer.refactor(location);

    File compared = new File(path + "/InlineTempTester.refactored.java");
    parser = new FileParser();
    parser.doFile(compared);

    assert(ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(),
                                           new File[] { original },
                                           parser.getTree(),
                                           new File[] {compared }));
  }

  public void testDefAndAssignOnDifferentLinesUsingDef() throws Exception {
    File original = new File(path + "/InlineTempTester.java");

    FileParser parser = new FileParser();
    parser.doFile(original);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    InlineTemp refactorer = new InlineTemp(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(original, 7, 15);
    refactorer.refactor(location);

    File compared = new File(path + "/InlineTempTester.refactored.java");
    parser = new FileParser();
    parser.doFile(compared);

    assert(ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(),
                                           new File[] { original },
                                           parser.getTree(),
                                           new File[] {compared }));
  }

  public void testMoreComplexExpression() throws Exception {
    File original = new File(path + "/InlineTempTesterThree.java");

    FileParser parser = new FileParser();
    parser.doFile(original);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    InlineTemp refactorer = new InlineTemp(new NullPrintManager());
    refactorer.setup(table);

    Occurrence location = new Occurrence(original, 8, 12);
    refactorer.refactor(location);

    File compared = new File(path + "/InlineTempTesterThree.refactored.java");
    parser = new FileParser();
    parser.doFile(compared);

    assert(ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(),
                                           new File[] { original },
                                           parser.getTree(),
                                           new File[] {compared }));
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { InlineTempTest.class.getName() });
  }
}
