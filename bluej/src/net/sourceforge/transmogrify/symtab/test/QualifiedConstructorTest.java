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

import java.util.Vector;

public class QualifiedConstructorTest extends DefinitionLookupTest {

  private File file;

  public QualifiedConstructorTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/QualifiedConstructors.java");
    createQueryEngine(new File[] { file });
  }

  public void testNoArgConstructor() throws Exception {
    IClass date = new ExternalClass(java.util.Date.class);
    ISignature signature = new MethodSignature(new Vector());
    IDefinition constructor = date.getMethodDefinition("Date", signature);
    assertNotNull("External definition not found.", constructor);

    IDefinition ref = getDefinition(file, "Date", 8, 41);
    assertEquals("Reference does not point at definition.", constructor, ref);
  }

  public void testOtherConstructor() throws Exception {
    IClass date = new ExternalClass(java.util.Date.class);
    Vector arguments = new Vector();
    arguments.add(new ExternalClass(Long.TYPE));
    ISignature signature = new MethodSignature(arguments);
    IDefinition constructor = date.getMethodDefinition("Date", signature);
    assertNotNull("External definition not found.", constructor);

    IDefinition ref = getDefinition(file, "Date", 9, 44);
    assertEquals("Reference does not point at definition.", constructor, ref);
  }

  public void testReferenceForArgumentInConstructor() throws Exception {
    IDefinition def = getDefinition(file, "x", 6, 10);
    assertNotNull("Definition could not be found.", def);

    IDefinition ref = getDefinition(file, "x", 9, 49);
    assertEquals("Reference does not point at definition.", def, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(QualifiedConstructorTest.class);
  }

}
