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

import net.sourceforge.transmogrify.symtab.*;

import java.io.File;

public class DegenerateForTest extends DefinitionLookupTest {

  private File file;

  public DegenerateForTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/DegenerateFor.java");
    createQueryEngine(new File[] { file });
  }

  public void testResolutionOfIfStatement() throws Exception {
    IDefinition def = getDefinition(file, "x", 5, 11);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "x", 9, 11);
    assertEquals("Reference does not point at definition", def, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(DegenerateForTest.class);
  }

}
