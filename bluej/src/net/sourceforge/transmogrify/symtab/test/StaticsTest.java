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

// $Id: StaticsTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import java.io.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;


public class StaticsTest extends DefinitionLookupTest {
  File staticsFile = new File("test/Statics.java");
  File oneFile = new File("test/One.java");
  File swooshFile = new File("test/Swoosh.java");
  File logoMakerFile = new File("test/LogoMaker.java");
  File fooFile = new File("test/Foo.java");

  IDefinition FOODef = null;
  IDefinition curvatureDef = null;
  IDefinition logoMakerDef = null;
  IDefinition swooshDef = null;
  IDefinition FooFOODef = null;
  IDefinition whateverDef = null;

  public StaticsTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    File [] filez = { staticsFile, oneFile, swooshFile, logoMakerFile, fooFile };
    query = createQueryEngine(filez);

    FOODef = query.getDefinition("FOO",
                                 new Occurrence(oneFile, 4, 14));
    curvatureDef = query.getDefinition("curvature",
                                       new Occurrence(swooshFile, 4, 21));
    logoMakerDef = query.getDefinition("LogoMaker",
                                       new Occurrence(logoMakerFile, 3, 14));
    swooshDef = query.getDefinition("Swoosh",
                                    new Occurrence(swooshFile, 3, 14));
    FooFOODef = query.getDefinition("FOO",
                                    new Occurrence(fooFile, 4, 21));
    whateverDef = query.getDefinition("whatever",
                                      new Occurrence(fooFile, 15, 22));
  }

  public void tearDown() {}

  public void testSingleStatic() {
    IDefinition ref = query.getDefinition("FOO",
                                         new Occurrence(staticsFile, 6, 9));

    assertNotNull(ref);
    assertEquals(ref, FOODef);
  }

  public void testSingleStaticOtherPackage() {
    IDefinition ref = query.getDefinition("FOO",
                                         new Occurrence(staticsFile, 7, 20));

    assertNotNull(FooFOODef);
    assertEquals(FooFOODef, ref);
  }

  public void testStaticMethod() {
    IDefinition ref = query.getDefinition("whatever",
                                         new Occurrence(staticsFile, 8, 20));

    assertNotNull(ref);
    assertEquals(ref, whateverDef);
  }

  public void testOtherPackageStatic() {
    IDefinition ref = query.getDefinition("curvature",
                                         new Occurrence(logoMakerFile, 7, 26));
    assertNotNull(ref);
    assertEquals(ref,curvatureDef);
  }

  public void testOtherPackageStaticNonTerminalReference() {
    IDefinition ref = query.getDefinition("LogoMaker",
                                         new Occurrence(swooshFile, 7, 24));
    assertNotNull(logoMakerDef);
    assertEquals(logoMakerDef, ref);
  }

  public void testSwooshDefined() {
    assertNotNull(swooshDef);
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { StaticsTest.class.getName() });
  }
}
