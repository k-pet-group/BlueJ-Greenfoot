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
import net.sourceforge.transmogrify.symtab.PrimitiveClasses;

public class PrimitiveClassesTest extends TestCase{

  public PrimitiveClassesTest(String name) {
    super(name);
  }

  public void testCharCompatibleWithInt() {
    assert("Char should be compatible with int.",
           PrimitiveClasses.typesAreCompatible(PrimitiveClasses.INT,
                                               PrimitiveClasses.CHAR));
           
  }

  public void testIntIncompatibleWithChar() {
    assert("Int should be incompatible with char.",
           !PrimitiveClasses.typesAreCompatible(PrimitiveClasses.CHAR,
                                                PrimitiveClasses.INT));

  }

  public void testIntCompatibleWithInt() {
    assert("int should be compatible with int.",
           PrimitiveClasses.typesAreCompatible(PrimitiveClasses.INT,
                                               PrimitiveClasses.INT));

  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(PrimitiveClassesTest.class);
  }

}
