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


import java.io.File;

import junit.framework.*;

import net.sourceforge.transmogrify.refactorer.*;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.*;

public class PullUpFieldTest extends TestCase {

  private String path = "test/refactorer/PullUpField";

  File parent;
  File child;

  PullUpFieldRefactorer refactorer;

  public PullUpFieldTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    parent = new File(path + "/Parent.java");
    child = new File(path + "/Child.java");

    FileParser parser = new FileParser();
    parser.doFile(parent);
    parser.doFile(child);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    refactorer = new PullUpFieldRefactorer(new NullPrintManager());
    refactorer.setup(table);
  }

  public void testCanRefactorClassMember() {
    Occurrence location = new Occurrence(child, 5, 14);
    assert("Should be able to pull this field up.",
           refactorer.canRefactor(location));
  }

  public void testCantRefactorMethodVariable() {
    Occurrence location = new Occurrence(child, 10, 9);
    assert("Should not be able to pull this field up.",
           !refactorer.canRefactor(location));
  }

  public void testRefactorPublicVariable() throws Exception {
    refactorer.refactor(new Occurrence(child, 5, 14));

    FileParser parser = new FileParser();
    File newParent = new File(path + "/Parent.refactored-public.java");
    File newChild = new File(path + "/Child.refactored-public.java");

    parser.doFile(newParent);
    parser.doFile(newChild);

    assert("Refactored incorrectly.",
           ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(),
                                           new File[] {parent, child},
                                           parser.getTree(),
                                           new File[] {newParent, newChild}));
  }

  public void testRefactorProtectedVariable() throws Exception {
    refactorer.refactor(new Occurrence(child, 6, 17));

    FileParser parser = new FileParser();
    File newParent = new File(path + "/Parent.refactored-protected.java");
    File newChild = new File(path + "/Child.refactored-protected.java");

    parser.doFile(newParent);
    parser.doFile(newChild);

    assert("Refactored incorrectly.",
           ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(),
                                           new File[] {parent, child},
                                           parser.getTree(),
                                           new File[] {newParent, newChild}));
  }

  public void testRefactorPrivateVariable() throws Exception {
    refactorer.refactor(new Occurrence(child, 7, 15));

    FileParser parser = new FileParser();
    File newParent = new File(path + "/Parent.refactored-private.java");
    File newChild = new File(path + "/Child.refactored-private.java");

    parser.doFile(newParent);
    parser.doFile(newChild);

    assert("Refactored incorrectly.",
           ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(),
                                           new File[] {parent, child},
                                           parser.getTree(),
                                           new File[] {newParent, newChild}));
  }


  public static void main(String[] args) {
    junit.swingui.TestRunner.run(PullUpFieldTest.class);
  }

}
