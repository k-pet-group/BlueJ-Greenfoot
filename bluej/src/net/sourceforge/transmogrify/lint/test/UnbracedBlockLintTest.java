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

// $Id: UnbracedBlockLintTest.java 1014 2001-11-30 03:28:10Z ajp $

package net.sourceforge.transmogrify.lint.test;

import java.io.File;
import java.util.*;

import net.sourceforge.transmogrify.lint.*;
import net.sourceforge.transmogrify.symtab.parser.*;

public class UnbracedBlockLintTest extends AbstractLintTest {

  private File file;
  private Lint lint;

  public UnbracedBlockLintTest(String name) {
    super(name);
  }

  private void parse(File file) throws Exception {
    lint = new UnbracedBlockLint();
    Collection lints = new ArrayList();
    lints.add(lint);

    parse(file, lints);
  }

  public void testUnbracedIf() throws Exception {
    file = new File("test/lint/UnbracedIf.java");
    parse(file);

    SymTabAST dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(8);
    dummyNode.setColumn(5);
    Warning warning = new Warning(UnbracedBlockLint.IF_WARNING, dummyNode);
    List expected = new ArrayList();
    expected.add(warning);

    assertEquals("Incorrect warnings generated.",
                 expected, lint.getWarnings());
  }

  public void testUnbracedWhile() throws Exception {
    file = new File("test/lint/UnbracedWhile.java");
    parse(file);

    SymTabAST dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(8);
    dummyNode.setColumn(5);
    Warning warning = new Warning(UnbracedBlockLint.WHILE_WARNING, dummyNode);
    List expected = new ArrayList();
    expected.add(warning);

    assertEquals("Incorrect warnings generated.",
                 expected, lint.getWarnings());
  }

  public void testUnbracedDo() throws Exception {
    file = new File("test/lint/UnbracedDo.java");
    parse(file);

    SymTabAST dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(8);
    dummyNode.setColumn(5);
    Warning warning = new Warning(UnbracedBlockLint.DO_WARNING, dummyNode);
    List expected = new ArrayList();
    expected.add(warning);

    assertEquals("Incorrect warnings generated.",
                 expected, lint.getWarnings());
  }

  public void testUnbracedElseClauses() throws Exception {
    file = new File("test/lint/UnbracedElse.java");
    parse(file);

    List expected = new ArrayList();

    SymTabAST dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(8);
    dummyNode.setColumn(5);
    Warning warning = new Warning(UnbracedBlockLint.ELSE_WARNING, dummyNode);
    expected.add(warning);

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(23);
    dummyNode.setColumn(10);
    warning = new Warning(UnbracedBlockLint.IF_WARNING, dummyNode);

    expected.add(warning);

    assertEquals("Incorrect warnings generated.",
                 expected, lint.getWarnings());

  }

  public void testUnbracedFor() throws Exception {
    file = new File("test/lint/UnbracedFor.java");
    parse(file);

    List expected = new ArrayList();

    SymTabAST dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(4);
    dummyNode.setColumn(5);
    Warning warning = new Warning(UnbracedBlockLint.FOR_WARNING, dummyNode);
    expected.add(warning);

    assertEquals("Incorrect warnings generated.",
                 expected, lint.getWarnings());
  }
}

