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

public class ProtectedMemberLookupTest extends DefinitionLookupTest {
  File file;

  public ProtectedMemberLookupTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/JavaLexer.java");

    createQueryEngine(new File[] { file });
  }

  public void testLookup() throws Exception {
    Class defClass = ClassLoader.getSystemClassLoader().loadClass("antlr.CharScanner");
    Class type = defClass.getDeclaredField("text").getType();
    IDefinition def = new ExternalClass(type);

    IVariable ref = (IVariable)getDefinition(file, "text", 139, 59);

    assertNotNull("definition not found", def);
    assertEquals("definitions do not match", def, ref.getType());
  }

  public void testLookup2() throws Exception {
    Class defClass = Class.forName("net.sourceforge.transmogrify.symtab.parser.JavaLexer");

    ExternalClass ext = new ExternalClass(defClass);

    IVariable var = ext.getVariableDefinition("text");
    assertNotNull(var);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(ProtectedMemberLookupTest.class);
  }
}
