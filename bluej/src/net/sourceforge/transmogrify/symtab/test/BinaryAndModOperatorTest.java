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

public class BinaryAndModOperatorTest extends DefinitionLookupTest {

  private File file;

  public BinaryAndModOperatorTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/BinaryAndModOperator.java");
    createQueryEngine(new File[] { file });
  }

  public void testResolutionOfShift() throws Exception {
    IDefinition def = getDefinition(file, "anInt", 14, 9);
    assertNotNull("Definition not found.", def);

    IDefinition ref = null;

    ref = getDefinition(file, "anInt", 19, 12);
    assertEquals("Reference does not point at definition.", def, ref);

    ref = getDefinition(file, "anInt", 22, 12);
    assertEquals("Reference does not point at definition.", def, ref);

    ref = getDefinition(file, "anInt", 23, 12);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testResultOfShiftShort() throws Exception {
    IDefinition def = getDefinition(file, "method", 8, 15);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "method", 18, 5);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testResultOfShiftInt() throws Exception {
    IDefinition def = getDefinition(file, "method", 8, 15);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "method", 19, 5);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testResultOfShiftLong() throws Exception {
    IDefinition def = getDefinition(file, "method", 5, 15);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "method", 20, 5);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testResolutionOfModOperator() throws Exception {
    IDefinition def = getDefinition(file, "anInt", 14, 9);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "anInt", 25, 12);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testResultOfModOperator() throws Exception {
    IDefinition def = getDefinition(file, "method", 42, 15);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "method", 27, 5);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testResolutionOfBitwiseOperator() throws Exception {
    IDefinition def = getDefinition(file, "aShort", 37, 11);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "aShort", 38, 12);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testResultOfBitwiseOnShort() throws Exception {
    IDefinition def = getDefinition(file, "method", 8, 15);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "method", 38, 5);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public void testResultOfBitwiseOnBoolean() throws Exception {
    IDefinition def = getDefinition(file, "method", 34, 15);
    assertNotNull("Definition not found.", def);

    IDefinition ref = getDefinition(file, "method", 39, 5);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(BinaryAndModOperatorTest.class);
  }

}
