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

// $Id: ParameterReferencesTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.extensions.*;
import junit.framework.*;

import java.io.*;

import net.sourceforge.transmogrify.symtab.*;

import net.sourceforge.transmogrify.symtab.parser.JavaTokenTypes;

public class ParameterReferencesTest extends DefinitionLookupTest {
  File parameterArgsFile = new File("test/ParameterArgs.java");
  File importedClassArgFile = new File("test/MethodTester.java");
  File explicitClassArgFile = new File("test/Child.java");
  File parentOfChildFile = new File("test/Parent.java");

  File [] filez = { parameterArgsFile,
                    importedClassArgFile,
                    explicitClassArgFile,
                    parentOfChildFile };

  IDefinition classArgDef = null;
  IDefinition primitiveArgDef = null;
  IDefinition importedClassArgDef = null;
  IDefinition explicitClassArgDef = null;

  IDefinition argDef = null;
  IDefinition arg2Def = null;

  public ParameterReferencesTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    query = createQueryEngine(filez);

    classArgDef = query.getDefinition("Arg", new Occurrence(parameterArgsFile, 17, 7));
    importedClassArgDef = query.getDefinition("MethodTester", new Occurrence(importedClassArgFile, 3, 14));
    explicitClassArgDef = query.getDefinition("Child", new Occurrence(explicitClassArgFile, 3, 14));

    argDef  = query.getDefinition("arg", new Occurrence(parameterArgsFile, 8, 39));
    arg2Def = query.getDefinition("arg2", new Occurrence(parameterArgsFile, 9, 39));
  }

  public void tearDown() {}

  public void testClassArg() {
    IDefinition ref = query.getDefinition("Arg", new Occurrence(parameterArgsFile, 8, 35));

    assertNotNull(classArgDef);
    assertEquals(classArgDef, ref);
  }

  public void testTypeOfPrimitiveArg() {
    IVariable ref = (IVariable)query.getDefinition("arg2", new Occurrence(parameterArgsFile, 9, 39));

    IClass varType = ref.getType();

    assertEquals("Type of parameter incorrect.",
                 new ExternalClass(Integer.TYPE), varType);
  }

  public void testImportedClassArg() {
    IDefinition ref = query.getDefinition("MethodTester", new Occurrence(parameterArgsFile, 10, 35));

    assertNotNull(ref);
    assertEquals(ref, importedClassArgDef);
  }

  public void testExplicitClassArg() {
    IDefinition ref = query.getDefinition("Child", new Occurrence(parameterArgsFile, 11, 45));

    assertNotNull(ref);
    assertEquals(ref, explicitClassArgDef);
  }

  public void testReferenceInMethod() {
    IDefinition ref = query.getDefinition("arg", new Occurrence(parameterArgsFile, 13, 9));

    assertNotNull(ref);
    assertEquals(ref, argDef);
  }

  public void testReferenceInMethod2() {
    IDefinition ref = query.getDefinition("arg2", new Occurrence(parameterArgsFile, 12, 9));

    assertNotNull(ref);
    assertEquals(ref, arg2Def);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { ParameterReferencesTest.class.getName() });
  }
}
