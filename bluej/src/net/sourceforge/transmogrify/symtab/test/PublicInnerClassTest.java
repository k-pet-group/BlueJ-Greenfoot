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

public class PublicInnerClassTest extends DefinitionLookupTest {

  private File file;

  public PublicInnerClassTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/PublicInnerClass.java");
    createQueryEngine(new File[] { file });
  }

  public void testContainingClassDefinition() throws Exception {
    IDefinition ref = getDefinition(file, "Map", 8, 5);
    IDefinition def = new ExternalClass(java.util.Map.class);
    assertEquals("Reference points to wrong definition.",
                 def, ref);
  }

  public void testInnerClassDefinition() throws Exception {
    IDefinition ref = getDefinition(file, "Entry", 8, 9);
    IDefinition def = new ExternalClass(java.util.Map.Entry.class);
    assertEquals("Reference points to wrong definition.",
                 def, ref);
  }

  public void testInnerClassMethod() throws Exception {
    IDefinition ref = getDefinition(file, "getKey", 9, 11);
    assertNotNull("Reference not created.", ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(PublicInnerClassTest.class);
  }

}
