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

package net.sourceforge.transmogrify.hook.test;

import java.io.File;

import junit.framework.TestCase;

import net.sourceforge.transmogrify.hook.HookImpl;


public class HookImplTest extends TestCase {
  HookImpl hook;

  public HookImplTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    hook = new HookImpl();
    hook.openFile("test/One.java");
  }

  public void testOffsetConversion() throws Exception {
    hook.setCaretPos(3,9);
    int expected = 28;
    assertEquals("", expected, hook.getCaretPos());
  }

  public void testSelection() throws Exception {
    hook.selectText(6,4,7,7);
    String expected = "rivate int real;\r\n  priv";
    String result = hook.getSelectedText();

    assertEquals("selected text incorrect", expected, result);
  }

  public void testLineAndColumnFromOffset() throws Exception {
    hook.setCaretPos(28);
    assertEquals("line number wrong", 3, hook.getCaretLineNumber());
    assertEquals("column wrong", 9, hook.getCaretOffset());
  }

  public void testGetCurrentFile() throws Exception {
    File testFile = new File("test/One.java");
    assertEquals(testFile.getPath(), hook.getCurrentFile());
  }


  public static void main(String[] args) {
    junit.swingui.TestRunner.run(HookImplTest.class);
  }
}