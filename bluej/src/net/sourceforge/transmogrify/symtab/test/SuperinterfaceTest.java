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
import java.io.*;

public class SuperinterfaceTest extends DefinitionLookupTest {
  File interfaceFile;
  File classFile;

  public SuperinterfaceTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    interfaceFile = new File("test/DefinesSomeFields.java");
    classFile = new File("test/UsesSomeFields.java");

    createQueryEngine(new File[] { interfaceFile, classFile });
  }

  public void testLookup() {
    IDefinition def = getDefinition(interfaceFile, "ONE", 4, 7);
    IDefinition ref = getDefinition(classFile, "ONE", 7, 10);

    assertNotNull("definition not found", def);
    assertEquals("definitions not equal", def, ref);
  }

  public void testProtectedMemberLookup() {
    IDefinition def = getDefinition(interfaceFile, "TWO", 5, 7);
    IDefinition ref = getDefinition(classFile, "TWO", 8, 10);

    assertNotNull("definition not found", def);
    assertEquals("definitions not equal", def, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(SuperinterfaceTest.class);
  }
}
