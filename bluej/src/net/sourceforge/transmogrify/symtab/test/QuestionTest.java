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
package net.sourceforge.transmogrify.symtab.test;

import java.io.File;
import net.sourceforge.transmogrify.symtab.*;

public class QuestionTest extends DefinitionLookupTest {
  File file;

  public QuestionTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/Question.java");

    createQueryEngine(new File[] { file });
  }

  public void testLookupInTest() throws Exception {
    IDefinition def = getDefinition(file, "x", 4, 7);
    IDefinition ref = getDefinition(file, "x", 10, 16);

    assertNotNull("definition not found", def);
    assertEquals("definitions do not match", def, ref);
  }

  public void testLookupInBranch() throws Exception {
    IDefinition def = getDefinition(file, "x", 4, 7);
    IDefinition ref = getDefinition(file, "x", 10, 21);

    assertNotNull("definition not found", def);
    assertEquals("definitions do not match", def, ref);
  }

  public void testResult() throws Exception {
    IDefinition def = getDefinition(file, "doSomething", 20, 15);
    IDefinition ref = getDefinition(file, "doSomething", 16, 5);

    assertNotNull("definition not found", def);
    assertEquals("definitions do not match", def, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(QuestionTest.class);
  }
}
