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

import net.sourceforge.transmogrify.refactorer.*;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.test.DefinitionLookupTest;

public class FindDefinitionRefactorerTest extends DefinitionLookupTest {
  File file;

  public FindDefinitionRefactorerTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/One.java");
    createQueryEngine(new File[] { file });
  }

  public void testFindDefinitionRightEdge() {
    FindDefinitionRefactorer refactorer = new FindDefinitionRefactorer();
    refactorer.setup(table);

    IDefinition expected = getDefinition(file, "complex", 7, 15);

    IDefinition def = refactorer.refactor(new Occurrence(file, 11, 5));
    assertEquals("definitions not the same", expected, def);
  }

  public void testFindDefinitionMiddle() {
    FindDefinitionRefactorer refactorer = new FindDefinitionRefactorer();
    refactorer.setup(table);

    IDefinition expected = getDefinition(file, "complex", 7, 15);

    IDefinition def = refactorer.refactor(new Occurrence(file, 11, 7));
    assertEquals("definitions not the same", expected, def);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(FindDefinitionRefactorerTest.class);
  }
}