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

public class ArrayInitializerTest extends DefinitionLookupTest {

  File file;
  /**
   * ArrayInitializerTest constructor comment.
   * @param name java.lang.String
   */
  public ArrayInitializerTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/ArrayInitializer.java");
    createQueryEngine(new File [] { file });
  }
  public void testInitializeArrayWithReference() throws Exception {
    IDefinition refVar1 = getDefinition(file, "var1", 9, 27);
    IDefinition defVar1 = getDefinition(file, "var1", 6,12);
    assertNotNull("couldn't find definition", defVar1);
    assertEquals("References doesn't point to the definition", defVar1, refVar1);

    IDefinition refVar2 = getDefinition(file, "var2", 9, 33);
    IDefinition defVar2 = getDefinition(file, "var2", 7,12);
    assertNotNull("couldn't find definition", defVar2);
    assertEquals("References doesn't point to the definition", defVar2, refVar2);

  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.run(ArrayInitializerTest.class);
  }
}
