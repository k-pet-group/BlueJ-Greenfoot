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

import junit.extensions.*;
import junit.framework.*;

import net.sourceforge.transmogrify.symtab.*;
import java.util.Vector;

public class MethodSignatureTest extends TestCase {

  MethodSignature signatureA = null;
  MethodSignature signatureB = null;

  ClassDef classA = new ClassDef( "foo", null, null);
  ClassDef classB = new ClassDef( "bar", null, null);

  public MethodSignatureTest( String name ) {
    super(name);
  }

  public void setUp() {

    ClassDef[] argTypes = new ClassDef[] { classA, classB };
    signatureA = new MethodSignature( argTypes );
  }

  public void testEquality() {
    ClassDef[] argTypes = new ClassDef[] { classA, classB };
    signatureB = new MethodSignature( argTypes );

    assert( signatureA.equals( signatureB ) );
  }

  public void testInequality() {
    ClassDef[] argTypes = new ClassDef[] { classA, classA };
    signatureB = new MethodSignature( argTypes );

    assert( !signatureB.equals( signatureA ) );
  }

  public void testVectorConstruction() {
    Vector argTypes = new Vector();
    argTypes.add( classA );
    argTypes.add( classB );
    signatureB = new MethodSignature( argTypes );

    assert( signatureB.equals( signatureA ) );
  }

  public void testToString() {
    String expected = "(foo, bar)";
    assertEquals(expected, signatureA.toString());
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { MethodSignatureTest.class.getName() });
  }
}
