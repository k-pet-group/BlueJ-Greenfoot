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


public class CanRenameSymbolTest extends TestCase {

  File file;
  RenameVariable refactorer;

  public CanRenameSymbolTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/refactorer/RenameSymbol/Renamable.java");

    FileParser parser = new FileParser();
    parser.doFile(file);

    SymbolTable table = new TableMaker(parser.getTree()).getTable();
    refactorer = new RenameVariable();
    refactorer.setup(table);
  }

  public void testCanRenameVariable() throws Exception {
    Occurrence location = new Occurrence(file, 9, 5);
    assert("Should be able to refactor here.",
           refactorer.canRefactor(location));
  }

  public void testCanRenameMethod() throws Exception {
    Occurrence location = new Occurrence(file, 9, 10);
    assert("Should be able to refactor here.",
           refactorer.canRefactor(location));
  }

  public void testCantRenameWhitespace() throws Exception {
    Occurrence location = new Occurrence(file, 10, 3);
    assert("Should not be able to refactor here.",
           !refactorer.canRefactor(location));
  }

  public void testCantRenameClass() throws Exception {
    Occurrence location = new Occurrence(file, 3, 3);
    assert("Should not be able to refactor here.",
           !refactorer.canRefactor(location));
  }

  public void testCantRenameExternal() throws Exception {
    Occurrence location = new Occurrence(file, 11, 15);
    assert("Should not be able to refactor here.",
           !refactorer.canRefactor(location));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(CanRenameSymbolTest.class);
  }

}
