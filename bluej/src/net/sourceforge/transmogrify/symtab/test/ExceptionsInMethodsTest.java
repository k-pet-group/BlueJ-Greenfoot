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

public class ExceptionsInMethodsTest extends DefinitionLookupTest {

  private File mainFile;
  private File exceptionsFile;

  public ExceptionsInMethodsTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    mainFile = new File("test/symtab/ExceptionsInMethods.java");
    exceptionsFile =
      new File("test/symtab/ExceptionsInMethodsException.java");

    createQueryEngine(new File[] {mainFile, exceptionsFile});
  }

  public void testSourcedException() throws Exception {
    IDefinition exceptionRef = getDefinition(mainFile,
                                            "ExceptionsInMethodsException",
                                            5, 42);
    IDefinition exceptionDef = getDefinition(exceptionsFile,
                                            "ExceptionsInMethodsException",
                                            3, 14);

    assertNotNull("Original definition not found.", exceptionDef);
    assertEquals("Reference does not point to definition.",
                 exceptionDef, exceptionRef);
  }

  public void testNonSourcedException() throws Exception {
    IDefinition exceptionRef = getDefinition(mainFile, "Exception", 5, 31);
    assertNotNull("Definition not found.", exceptionRef);
    assertEquals("Definition does not point to right class.",
                 new ExternalClass(java.lang.Exception.class), exceptionRef);
  }

  public void testQualifiedException() throws Exception {
    IDefinition exceptionRef = getDefinition(mainFile, "Exception", 9, 48);
    assertNotNull("Definition not found.", exceptionRef);
    assertEquals("Definition does not point to right class.",
                 new ExternalClass(java.lang.Exception.class), exceptionRef);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(ExceptionsInMethodsTest.class);
  }
}
