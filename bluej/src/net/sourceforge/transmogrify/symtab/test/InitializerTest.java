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

// $Id: InitializerTest.java 1014 2001-11-30 03:28:10Z ajp $

import java.io.File;

import net.sourceforge.transmogrify.symtab.*;

public class InitializerTest extends DefinitionLookupTest {
  File _file;

  public InitializerTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    _file = new File("test/Initializer.java");
    createQueryEngine(new File[] { _file });
  }

  public void testLocalReference() throws Exception {
    IDefinition ref = getDefinition(_file, "x", 9, 5);
    IDefinition varX = getDefinition(_file, "x", 8, 9);

    assertNotNull("IDefinition for x not found", varX);
    assertEquals("Reference doesn't point to IDefinition", varX, ref);
  }
  public void testStaticLocalReferences() throws Exception {
    IDefinition ref = getDefinition(_file, "x", 15, 5);
    IDefinition varX = getDefinition(_file, "x", 14, 9);

    assertNotNull("IDefinition for x not found", varX);
    assertEquals("Reference doesn't point to IDefinition", varX, ref);
  }
  public void testClassMemberReference() throws Exception {
    IDefinition ref = getDefinition(_file, "b", 10, 5);
    IDefinition varB = getDefinition(_file, "b", 5, 7);

    assertNotNull("IDefinition for b not found", varB);
    assertEquals("Reference doesn't point to IDefinition", varB, ref);
  }
  public void testStaticClassMemberReference() throws Exception {
    IDefinition ref = getDefinition(_file, "y", 16, 5);
    IDefinition varY = getDefinition(_file, "y", 4, 14);

    assertNotNull("IDefinition for y not found", varY);
    assertEquals("Reference doesn't point to IDefinition", varY, ref);
  }
  public void testScope() throws Exception {
    Definition varX = (Definition)getDefinition(_file, "x", 8, 9);
    Definition varB = (Definition)getDefinition(_file, "b", 5, 7);
    Definition staticVarX = (Definition)getDefinition(_file, "x", 14, 9);

    Scope parent = varX.getParentScope();
    Scope classScope = varB.getParentScope();
    Scope otherBlock = staticVarX.getParentScope();

    assert("Block is not unique scope", !parent.equals(classScope));
    assert("Block is not unique scope", !parent.equals(otherBlock));
  }
  public void testStaticScope() throws Exception {
    Definition varX = (Definition)getDefinition(_file, "x", 8, 9);
    Definition varB = (Definition)getDefinition(_file, "b", 5, 7);
    Definition staticVarX = (Definition)getDefinition(_file, "x", 14, 9);

    Scope parent = staticVarX.getParentScope();
    Scope classScope = varB.getParentScope();
    Scope otherBlock = varX.getParentScope();

    assert("Block is not unique scope", !parent.equals(classScope));
    assert("Block is not unique scope", !parent.equals(otherBlock));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(InitializerTest.class);
  }
}
