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

import junit.framework.*;

import net.sourceforge.transmogrify.hook.HookImpl;

import net.sourceforge.transmogrify.refactorer.*;

import net.sourceforge.transmogrify.symtab.*;

import net.sourceforge.transmogrify.symtab.parser.*;

import net.sourceforge.transmogrify.symtab.printer.*;

public class ExtractMethodTest extends TestCase{
  String path;

  public ExtractMethodTest(String name){
    super(name);
  }

  public void setUp(){
    path = "test/";
  }

  public void tearDown(){
  }

  public void testExtractMethodNoArgs() throws Exception {
    File original = new File(path + "/ExtractMethodTester.java");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    Occurrence startLocation = new Occurrence(original, 15, 5);
    Occurrence endLocation = new Occurrence(original, 17, 5);
    refactorer.refactor(startLocation, endLocation, "printBanner");
    File compared = new File(path + "/ExtractMethodTester.refactored1.java");
    parser = new FileParser();
    parser.doFile(compared);
    assert("trees not equal", ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(), new File []{ original }, parser.getTree(), new File []{ compared }));
  }

  public void testExtractMethodOneArg() throws Exception {
    File original = new File(path + "/ExtractMethodTester.java");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    Occurrence startLocation = new Occurrence(original, 26, 5);
    Occurrence endLocation = new Occurrence(original, 27, 5);
    refactorer.refactor(startLocation, endLocation, "printDetails");
    File compared = new File(path + "/ExtractMethodTester.refactored2.java");
    parser = new FileParser();
    parser.doFile(compared);
    assert("trees not equal", ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(), new File []{ original }, parser.getTree(), new File []{ compared }));
  }

  public void testExtractMethodWithLocalDefNoParam() throws Exception {
    File original = new File(path + "/ExtractMethodTester2.java");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    Occurrence startLocation = new Occurrence(original, 8, 5);
    Occurrence endLocation = new Occurrence(original, 12, 5);
    refactorer.refactor(startLocation, endLocation, "fillDataSet");
    File compared = new File(path + "/ExtractMethodTester2.refactored1.java");
    parser = new FileParser();
    parser.doFile(compared);
    assert("trees not equal", ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(), new File []{ original }, parser.getTree(), new File []{ compared }));
  }

  public void testExtractMethodWithLocalDefParam() throws Exception {
    File original = new File(path + "/ExtractMethodTester2.java");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    Occurrence startLocation = new Occurrence(original, 10, 5);
    Occurrence endLocation = new Occurrence(original, 12, 5);
    refactorer.refactor(startLocation, endLocation, "fillDataSet");
    File compared = new File(path + "/ExtractMethodTester2.refactored2.java");
    parser = new FileParser();
    parser.doFile(compared);
    assert("trees not equal", ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(), new File []{ original }, parser.getTree(), new File []{ compared }));
  }

  public void testCantExtractMethodWithLocalDef() throws Exception {
    File original = new File(path + "/ExtractMethodTester2.java");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    Occurrence startLocation = new Occurrence(original, 29, 6);
    Occurrence endLocation = new Occurrence(original, 34, 5);
    try{
      refactorer.refactor(startLocation, endLocation, "fillDataSet");
      fail("should never get here");
    }
    catch(RefactoringException e){
      assertEquals("Definition of variable in selection, but it is referenced outside selection", e.getMessage());
    }
  }

  public void testStaticMethodExtraction() throws Exception {
    File original = new File(path + "/ExtractMethodTester3.java");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    Occurrence startLocation = new Occurrence(original, 6, 5);
    Occurrence endLocation = new Occurrence(original, 10, 5);
    refactorer.refactor(startLocation, endLocation, "foo");
    File compared = new File(path + "/ExtractMethodTester3.refactored1.java");
    parser = new FileParser();
    parser.doFile(compared);
    assert("trees not equal", ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(), new File []{ original }, parser.getTree(), new File []{ compared }));
  }

  public void testReturnVariableExtraction() throws Exception {
    File original = new File(path + "/ExtractMethodTester3.java");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    Occurrence startLocation = new Occurrence(original, 18, 5);
    Occurrence endLocation = new Occurrence(original, 20, 5);
    refactorer.refactor(startLocation, endLocation, "foo");
    File compared = new File(path + "/ExtractMethodTester3.refactored2.java");
    parser = new FileParser();
    parser.doFile(compared);
    assert("trees not equal", ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(), new File []{ original }, parser.getTree(), new File []{ compared }));
  }

  public void testCantReturnVariableExtraction() throws Exception {
    File original = new File(path + "/ExtractMethodTester3.java");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    Occurrence startLocation = new Occurrence(original, 29, 5);
    Occurrence endLocation = new Occurrence(original, 32, 5);
    try{
      refactorer.refactor(startLocation, endLocation, "foo");
      fail("should never get here");
    }
    catch(RefactoringException e){
      assertEquals("exception incorrect", "More than one variable referenced outside selection", e.getMessage());
    }
  }

  public void testBlockSelection() throws Exception {
    File original = new File(path + "/ExtractMethodTester3.java");
    HookImpl hook = new HookImpl();
    hook.openFile(path + "/ExtractMethodTester3.java");
    hook.selectText(18, 1, 21, 1);
    hook.setUserInput("foo");
    FileParser parser = new FileParser();
    parser.doFile(original);
    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    ExtractMethod refactorer = new ExtractMethod(new NullPrintManager());
    refactorer.setup(table);
    refactorer.apply(hook);
    File compared = new File(path + "/ExtractMethodTester3.refactored2.java");
    parser = new FileParser();
    parser.doFile(compared);
    assert("trees not equal", ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(), new File []{ original }, parser.getTree(), new File []{ compared }));
  }

  public static void main( String[] args){
    junit.swingui.TestRunner.run(ExtractMethodTest.class);
  }
}
