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
package net.sourceforge.transmogrify.symtab.printer;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;

public class FileUtil {

  public static void copyFile (File fromFile, File toFile) throws IOException {
    FileReader from = new FileReader(fromFile);
    FileWriter to = new FileWriter(toFile);

    char[] buffer = new char[4096];
    int bytes_read;

    while((bytes_read = from.read(buffer)) != -1) {
      to.write(buffer, 0, bytes_read);
    }
    to.flush();
    to.close();
    from.close();
  }

  public static boolean fileContentsAreEqual(File a, File b) {
    // returns equality of files at the granularity of a line
    // that is to say, files with identical lines, but different line breaks
    // will evaluate to being equal
    boolean result = false;

    try {
      String astr = getFileContents(a).toString();
      String bstr = getFileContents(b).toString();

      if ( astr == null && bstr == null ) {
        result = true;
      }
      else if ( astr != null && bstr != null ) {
        result = astr.equals(bstr);
      }
      else {
        result = false;
      }
    }
    catch ( Exception e ) {}
    return result;
  }

  public static StringBuffer getFileContents(File file) throws Exception {
    StringBuffer result = new StringBuffer();
    BufferedReader reader = null;

    reader = new BufferedReader(new FileReader(file));
    String line = reader.readLine();
    while ( line != null ) {
      result.append(line);
      result.append("\n");
      line = reader.readLine();
    }

    return result;
  }

}
