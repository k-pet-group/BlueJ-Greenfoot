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

import java.io.*;
import net.sourceforge.transmogrify.symtab.*;

public class DoWhileLoopTest extends DefinitionLookupTest {
  File file;

  public DoWhileLoopTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/DoWhileLoop.java");

    createQueryEngine(new File[] { file });
  }

  public void testLookup() {
    IDefinition def = getDefinition(file, "x", 5, 9);
    IDefinition ref = getDefinition(file, "x", 8, 7);

    assertNotNull("definition not found", def);
    assertEquals("definitions do not match", def, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(DoWhileLoopTest.class);
  }
}
