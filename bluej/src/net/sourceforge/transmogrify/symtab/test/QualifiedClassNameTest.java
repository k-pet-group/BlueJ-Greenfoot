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

import junit.framework.*;

import java.io.File;
import net.sourceforge.transmogrify.symtab.*;

public class QualifiedClassNameTest extends DefinitionLookupTest {

  private File testFile = new File("test/QualifiedClassName.java");

  public QualifiedClassNameTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    File[] testFiles = new File[] { testFile };
    query = createQueryEngine( testFiles );
  }

  public void testNonSourcedClass() throws Exception {
    IClass stringDef = new ExternalClass("".getClass());
    IVariable def = (IVariable)getDefinition(testFile, "nonSourced", 5, 28);
    IClass defType = def.getType();
    assertEquals("Classes not equal.", stringDef, defType);
  }

  public void testSourcedClass() throws Exception {
    IClass sourcedDef = (IClass)getDefinition(testFile,
                                              "QualifiedClassName", 3, 14);
    IVariable def = (IVariable)getDefinition(testFile, "sourced", 6, 35);
    IClass defType = def.getType();
    assertEquals("Classes not equal.", sourcedDef, defType);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(QualifiedClassNameTest.class);
  }

}
