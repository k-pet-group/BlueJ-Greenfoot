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

import net.sourceforge.transmogrify.symtab.printer.*;

public class FilePrintManagerTest extends TestCase {

  private ASTPrintManager _printManager = null;
  private File original = new File("test/refactorer/print/file");
  private File backupResult = new File("test/refactorer/print/backup");
  private File outputResult = new File("test/refactorer/print/output");

  public FilePrintManagerTest(String name) {
    super(name);
  }

  public void setUp() {
    _printManager = new FilePrintManager();
  }

  public void tearDown() throws Exception {
    // the duplicate code in the two tests should be here
  }

  private File getBackupFile() throws Exception {
    return new File("test/refactorer/print/file.bak");
  }

  public void testBackupCreated() throws Exception {
    _printManager.getWriter(original);
    assert(FileUtil.fileContentsAreEqual(backupResult, getBackupFile()));

    FileUtil.copyFile(getBackupFile(), original);
    getBackupFile().delete();
  }

  public void testPrintsToFile() throws Exception {
    Writer writer = _printManager.getWriter(original);
    writer.write("modified");
    writer.close();

    assert(FileUtil.fileContentsAreEqual(original, outputResult));

    FileUtil.copyFile(getBackupFile(), original);
    getBackupFile().delete();
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { FilePrintManagerTest.class.getName() });
  }

}
