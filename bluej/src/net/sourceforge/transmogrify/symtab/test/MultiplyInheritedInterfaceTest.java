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

public class MultiplyInheritedInterfaceTest extends DefinitionLookupTest {

  private File file;

  public MultiplyInheritedInterfaceTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/MultiplyInheritedInterface.java");
    createQueryEngine(new File[] { file });
  }

  public void testCompatability() throws Exception {
    IDefinition def = getDefinition(file, "method", 5, 15);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "method", 11, 5);
    assertEquals("Reference does not point to definition.", def, ref);
  }

  public void testReferenceToClassesInExtendsClause() throws Exception {
    IDefinition fooDef = getDefinition(file, "Foo", 16, 18);
    assertNotNull("Definition not found.", fooDef);

    IDefinition fooRef = getDefinition(file, "Foo", 20, 30);
    assertEquals("Reference does not point to definition.", fooDef, fooRef);


    IDefinition barDef = getDefinition(file, "Bar", 18, 18);
    assertNotNull("Definition not found.", barDef);

    IDefinition barRef = getDefinition(file, "Bar", 20, 35);
    assertEquals("Reference does not point to definition.", barDef, barRef);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(MultiplyInheritedInterfaceTest.class);
  }

}
