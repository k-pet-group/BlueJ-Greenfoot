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

package net.sourceforge.transmogrify.symtab.printer.test;


import java.io.*;

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.printer.FileUtil;


public class FileUtilTest extends TestCase {
  File from;
  File otherFrom;
  File to;

  public FileUtilTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    from = new File("test/from.java");
    to = new File("test/to");
    otherFrom = new File("test/Pamby.java");
  }

  public void testCopy() throws Exception {
    FileUtil.copyFile(from, to);
    testEquality(from, to);
  }

  public void testBackupAndRestore() throws Exception {
    // backup
    FileUtil.copyFile(from, to);
    testEquality(from, to);
    // overwrite
    FileUtil.copyFile(otherFrom, from);
    testEquality(otherFrom, from);
    // restore
    FileUtil.copyFile(to, from);
    testEquality(to, from);
  }

  private void testEquality(File from, File to) throws Exception {
    String fromContents = FileUtil.getFileContents(from).toString();
    String toContents = FileUtil.getFileContents(to).toString();

    assertEquals(fromContents, toContents);
  }

  public void testFileContentsAreEqual() throws Exception {
    File from = new File("test/refactorer/util/from");
    File to = new File("test/refactorer/util/to");

    assert(FileUtil.fileContentsAreEqual(from, to));
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { FileUtilTest.class.getName() });
  }
}





