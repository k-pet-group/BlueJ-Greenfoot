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

public class PullUpFieldTestTwo extends TestCase {

  private String path = "test/refactorer/PullUpField";

  File parent;
  File child;

  PullUpFieldRefactorer refactorer;

  public PullUpFieldTestTwo(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    parent = new File(path + "/ParentTwo.java");
    child = new File(path + "/ChildTwo.java");

    FileParser parser = new FileParser();
    parser.doFile(parent);
    parser.doFile(child);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    refactorer = new PullUpFieldRefactorer(new NullPrintManager());
    refactorer.setup(table);
  }

  public void testCanRefactorUnknownClass() throws Exception {
    Occurrence location = new Occurrence(child, 9, 18);
    assert("Should be able to refactor here.",
           refactorer.canRefactor(location));
  }

  public void testCanRefactorKnownClass() throws Exception {
    Occurrence location = new Occurrence(child, 10, 17);
    assert("Should be able to pull up this field.",
           refactorer.canRefactor(location));
  }

  public void testCantRefactorAssignment() throws Exception {
    Occurrence location = new Occurrence(child, 8, 17);
    assert("Cannot pull up a field with an assignment in the declaration.",
           !refactorer.canRefactor(location));
  }

  public void testRefactorImportClass() throws Exception {
    Occurrence location = new Occurrence(child, 9, 18);
    refactorer.refactor(location);

    FileParser parser = new FileParser();
    File newParent = new File(path + "/ParentTwo.refactored-import.java");
    File newChild = new File(path + "/ChildTwo.refactored-import.java");
    parser.doFile(newParent);
    parser.doFile(newChild);

    assert("Refactoring performed incorrectly.",
           ASTUtil.treesBelowFilesAreEqual(refactorer.getTree(),
                                           new File[] {parent, child},
                                           parser.getTree(),
                                           new File[] {newParent, newChild}));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(PullUpFieldTestTwo.class);
  }

}
