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


public class LookupRefactorerTest extends TestCase {

  private File file;
  private ShowReferencesRefactorer refactorer;

  public LookupRefactorerTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/refactorer/CanLookup.java");

    FileParser parser = new FileParser();
    parser.doFile(file);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();

    refactorer = new ShowReferencesRefactorer();
    refactorer.setup(table);
  }

  public void testCanLookupVar() throws Exception {
    Occurrence location = new Occurrence(file, 8, 5);
    assert("Should be able to lookup references/definition here.",
           refactorer.canRefactor(location));
  }

  public void testCanLookupMethod() throws Exception {
    Occurrence location = new Occurrence(file, 7, 15);
    assert("Should be able to lookup references/definition here.",
           refactorer.canRefactor(location));
  }

  public void testCanLookupClass() throws Exception {
    Occurrence location = new Occurrence(file, 3, 14);
    assert("Should be able to lookup references/definition here.",
           refactorer.canRefactor(location));
  }

  public void testCantLookupExternalClass() throws Exception {
    Occurrence location = new Occurrence(file, 5, 3);
    assert("Cannot lookup external class.",
           !refactorer.canRefactor(location));
  }

  public void testCantLookupExternalMethod() throws Exception {
    Occurrence location = new Occurrence(file, 8, 9);
    assert("Cannot lookup external method.",
           !refactorer.canRefactor(location));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(LookupRefactorerTest.class);
  }

}
