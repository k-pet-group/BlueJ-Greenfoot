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

public class PrimitiveHierarchyTest extends TestCase {

  public PrimitiveHierarchyTest(String name) {
    super(name);
  }

  private String method(double x) {
    return "double";
  }

  private String method(float x) {
    return "float";
  }

  private String method(long x) {
    return "long";
  }

  private String method(int x) {
    return "int";
  }

  private String method(char x) {
    return "char";
  }

  private String method(short x) {
    return "short";
  }

  private String method(byte x) {
    return "byte";
  }

  public void testCharAndByte() {
    char x = 0;
    byte y = 0;
    assertEquals("Incorrect type returned.", "int", method(x+y));
  }

  public void testCharAndShort() {
    char x = 0;
    short y = 0;
    assertEquals("Incorrect type returned.", "int", method(x+y));
  }

  public void testLongAndFloat() {
    long x = 0;
    float y = 0;
    assertEquals("Incorrect type returned.", "float", method(x+y));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(PrimitiveHierarchyTest.class);
  }

}
