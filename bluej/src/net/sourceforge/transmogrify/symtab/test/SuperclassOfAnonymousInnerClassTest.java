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
import java.util.*;

public class SuperclassOfAnonymousInnerClassTest extends DefinitionLookupTest {

  private File file;

  public SuperclassOfAnonymousInnerClassTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/SuperclassOfAnonymousInnerClass.java");
    createQueryEngine(new File[] { file });
  }

  public void testReferenceToConstructor() throws Exception {
    IClass superclass = new ExternalClass(java.util.HashSet.class);
    ISignature signature = new MethodSignature(new Vector());
    IDefinition constructor = superclass.getMethodDefinition("HashSet",
                                                             signature);
    IDefinition ref = getDefinition(file, "HashSet", 10, 17);
    assertEquals("Reference does not point to definition.",
                 constructor, ref);
  }

  public void testReferenceToMethodInSuperclass() throws Exception {
    IDefinition ref = getDefinition(file, "size", 12, 18);
    assertNotNull("Reference not created.", ref);
  }

  public void testReferenceToConstructorInAssignment() throws Exception {
    IClass superclass = new ExternalClass(java.util.HashSet.class);
    ISignature signature = new MethodSignature(new Vector());
    IDefinition constructor = superclass.getMethodDefinition("HashSet",
                                                             signature);
    IDefinition ref = getDefinition(file, "HashSet", 18, 23);
    assertEquals("Reference does not point to definition.",
                 constructor, ref);
  }

  public void testReferenceToMethodInSuperclassInAssignment() throws Exception {
    IDefinition ref = getDefinition(file, "size", 20, 18);
    assertNotNull("Reference not created.", ref);
  }

  public void testReferenceToInterfaceConstructor() throws Exception {
    IClass superclass
      = new ExternalClass(java.awt.event.ActionListener.class);
    ISignature signature = new MethodSignature(new Vector());
    IDefinition constructor = superclass.getMethodDefinition("ActionListener",
                                                             signature);

    IDefinition ref = getDefinition(file, "ActionListener", 27, 17);
    assertEquals("Reference does not point to definition.",
                 constructor, ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(SuperclassOfAnonymousInnerClassTest.class);
  }

}
