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


public class CanInlineTempTest extends TestCase {

  File file;
  InlineTemp refactorer;

  public CanInlineTempTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/refactorer/InlineTemp/Inlineable.java");

    FileParser parser = new FileParser();
    parser.doFile(file);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    refactorer = new InlineTemp();
    refactorer.setup(table);
  }

  public void testCanInlineTemp() throws Exception {
    Occurrence location = new Occurrence(file, 7, 13);
    assert("Should be able to refactor here.",
           refactorer.canRefactor(location));
  }

  public void testCantInlineReassignedVar() throws Exception {
    Occurrence location = new Occurrence(file, 11, 13);
    assert("Cannot inline a variable that is reassigned later.",
           !refactorer.canRefactor(location));
  }

  public void testCantInlineClassVar() throws Exception {
    Occurrence location = new Occurrence(file, 3, 10);
    assert("Cannot inline a class variable.",
           !refactorer.canRefactor(location));
  }

  public void testCantInlineUnassignedDeclaration() throws Exception {
    Occurrence location = new Occurrence(file, 15, 13);
    assert("Cannot inline a variable not assigned at declaration.",
           !refactorer.canRefactor(location));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(CanInlineTempTest.class);
  }
}
