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

// $Id: UnusedParameterLintTest.java 1014 2001-11-30 03:28:10Z ajp $

package net.sourceforge.transmogrify.lint.test;

import java.io.File;
import java.util.*;

import net.sourceforge.transmogrify.lint.*;
import net.sourceforge.transmogrify.symtab.parser.*;

public class UnusedParameterLintTest extends AbstractLintTest {

  private Lint lint;

  public UnusedParameterLintTest(String name) {
    super(name);
  }

  private void parse(File file) throws Exception {
    lint = new UnusedParameterLint();
    Collection lints = new ArrayList();
    lints.add(lint);
    
    parse(file, lints);
  }

  public void testUnusedVariables() throws Exception {
    File file = new File("test/lint/UnusedParameters.java");

    parse(file);

    List expected = new ArrayList();
    expected.add(new Warning(UnusedParameterLint.PARAMETER_MESSAGE, 
                             makeDummyNode(file, 14, 33)));

    assertEquals("List of warnings incorrect.", expected, lint.getWarnings());
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(UnusedParameterLintTest.class);
  }

}

