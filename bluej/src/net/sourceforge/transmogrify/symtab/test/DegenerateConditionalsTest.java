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

public class DegenerateConditionalsTest extends DefinitionLookupTest {
  File file;
  IDefinition def;

  public DegenerateConditionalsTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/DegenerateConditionals.java");

    createQueryEngine(new File[] { file });
    def = getDefinition(file, "x", 4, 7);
    assertNotNull("definition not found", def);
  }

  public void testDegenerateIf() throws Exception {
    IDefinition ref = getDefinition(file, "x", 7, 15);
    assertEquals("Definitions do not match", def, ref);
  }

  public void testDegenerateElse() throws Exception {
    IDefinition ref = getDefinition(file, "x", 8, 10);
    assertEquals("Definitions do not match", def, ref);
  }

  public void testDegenerateFor() throws Exception {
    IDefinition ref = getDefinition(file, "x", 11, 7);
    assertEquals("Definitions do not match", def, ref);
  }

  public void testDegenerateWhile() throws Exception {
    IDefinition ref = getDefinition(file, "x", 14, 7);
    assertEquals("Definitions do not match", def, ref);
  }

  public void testDegenerateDoWhile() throws Exception {
    IDefinition ref = getDefinition(file, "x", 18, 7);
    assertEquals("Definitions do not match", def, ref);
  }

  public void testDegenerateIfElseIf() throws Exception {
    IDefinition ref = getDefinition(file, "x", 22, 21);
    assertEquals("Definitions do not match", def, ref);

    ref = getDefinition(file, "x", 23, 10);
    assertEquals("Definitions do not match", def, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(DegenerateConditionalsTest.class);
  }
}
