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

public class SynchronizedBlockTest extends DefinitionLookupTest {

  private File file;

  public SynchronizedBlockTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/SynchronizedBlock.java");
    createQueryEngine(new File[] { file });
  }

  public void testReferenceInSynchronizedStatement() throws Exception {
    IDefinition def = getDefinition(file, "o", 5, 10);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "o", 8, 18);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testReferenceInSynchronizedBlock() throws Exception {
    IDefinition def = getDefinition(file, "otherMethod", 13, 15);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "otherMethod", 9, 7);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(SynchronizedBlockTest.class);
  }

}

