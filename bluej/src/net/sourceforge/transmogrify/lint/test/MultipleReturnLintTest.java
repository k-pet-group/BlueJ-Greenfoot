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

// $Id: MultipleReturnLintTest.java 1014 2001-11-30 03:28:10Z ajp $

package net.sourceforge.transmogrify.lint.test;

import java.io.File;
import java.util.*;

import net.sourceforge.transmogrify.lint.*;
import net.sourceforge.transmogrify.symtab.parser.*;

public class MultipleReturnLintTest extends AbstractLintTest {

  private File file;
  private Lint lint;

  public MultipleReturnLintTest(String name) {
    super(name);
  }

  private void parse(File file) throws Exception {
    lint = new MultipleReturnLint();
    Collection lints = new ArrayList();
    lints.add(lint);

    parse(file, lints);
  }

  public void testFindingMultipleReturns() throws Exception {
    file = new File("test/lint/MultipleReturn.java");
    parse(file);

    List expected = new ArrayList();
    SymTabAST dummyNode = null;
    Warning warning = null;

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(9);
    dummyNode.setColumn(7);
    warning = new Warning(MultipleReturnLint.WARNING, dummyNode);
    expected.add(warning);

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(12);
    dummyNode.setColumn(7);
    warning = new Warning(MultipleReturnLint.WARNING, dummyNode);
    expected.add(warning);

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(21);
    dummyNode.setColumn(7);
    warning = new Warning(MultipleReturnLint.WARNING, dummyNode);
    expected.add(warning);

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(24);
    dummyNode.setColumn(7);
    warning = new Warning(MultipleReturnLint.WARNING, dummyNode);
    expected.add(warning);

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(51);
    dummyNode.setColumn(7);
    warning = new Warning(MultipleReturnLint.WARNING, dummyNode);
    expected.add(warning);

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(60);
    dummyNode.setColumn(5);
    warning = new Warning(MultipleReturnLint.WARNING, dummyNode);
    expected.add(warning);

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(71);
    dummyNode.setColumn(11);
    warning = new Warning(MultipleReturnLint.WARNING, dummyNode);
    expected.add(warning);

    dummyNode = new SymTabAST();
    dummyNode.setFile(file);
    dummyNode.setLine(74);
    dummyNode.setColumn(11);
    warning = new Warning(MultipleReturnLint.WARNING, dummyNode);
    expected.add(warning);

    assertEquals("Incorrect warnings generated.",
                 expected, lint.getWarnings());
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(MultipleReturnLintTest.class);
  }

}

