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
import net.sourceforge.transmogrify.symtab.IDefinition;

public class MethodsWithNullTest extends DefinitionLookupTest {

  private File file;
  private IDefinition methodOne;
  private IDefinition methodTwo;

  public MethodsWithNullTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/MethodsWithNull.java");
    createQueryEngine(new File[] { file });

    methodOne = getDefinition(file, "method", 5, 15);
    methodTwo = getDefinition(file, "method", 7, 15);
  }

  public void testOneNull() throws Exception {
    IDefinition def = getDefinition(file, "method", 10, 5);
    assertNotNull(def);
    assertEquals("Should equal method(String).", methodOne, def);
  }

  public void testTwoNulls() throws Exception {
    IDefinition def = getDefinition(file, "method", 11, 5);
    assertNotNull(def);
    assertEquals("Should equal method(String, String).", methodTwo, def);
  }

  public void testObjectAndNull() throws Exception {
    IDefinition def = getDefinition(file, "method", 12, 5);
    assertNotNull(def);
    assertEquals("Should equal method(String, String).", methodTwo, def);
  }

  public void testNullAndObject() throws Exception {
    IDefinition def = getDefinition(file, "method", 13, 5);
    assertNotNull(def);
    assertEquals("Should equal method(String, String).", methodTwo, def);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(MethodsWithNullTest.class);
  }

}
