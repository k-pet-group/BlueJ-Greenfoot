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

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Writer;

// $Id: PrettyPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * <code>PrettyPrinter</code> contains some basic methods to assist with
 * the output of formatted strings.
 */
public class PrettyPrinter {
  private int indentLevel;
  private String tabString;
  private BufferedWriter output;
  private boolean beginLine;

  /**
   * constructor with output stream as a parameter
   * @param printout output stream
   */
  public PrettyPrinter(Writer printout) {
    tabString = makeTabString(PrinterProperties.getSpacesPerIndent());
    output = new BufferedWriter(printout);
    beginLine = true;
  }

  /**
   * returns a String with the specified number of spaces.
   * @param tabSize the number of spaces to put in the string
   *
   * @return String
   */
  private String makeTabString(int tabSize) {
    StringBuffer result = new StringBuffer();

    for (int i = 0; i < tabSize; i++) {
      result.append(" ");
    }

    return result.toString();
  }

  /**
   * increases the indent level by one.
   * @return <code>void</code>
   */
  public void indent() {
    indentLevel++;
  }

  /**
   * decreases the indent level by one.
   * @return <code>void</code>
   */
  public void unindent() {
    if (indentLevel > 0) {
      indentLevel--;
    }
  }

  /**
   * forces the pretty printer to begin indenting again
   * @return <code>void</code>
   */
  public void reset() {
    beginLine = true;
  }

  /**
   * prints the specified text to the OutputStream.  If it is the start
   * of a line it is indented, otherwise printing is done without special
   * processing.
   *
   * @param object the object to print
   * @return <code>void</code>
   */
  public void print(Object object) {
    print(object.toString());
  }

  /**
   * prints the specified text to the OutputStream.  If it is the start
   * of a line it is indented, otherwise printing is done without special
   * processing.
   *
   * @param text the text to print
   * @return <code>void</code>
   */
  public void print(String text) {
    try {
      if (beginLine) {
      tabify();
      beginLine = false;
      }
      output.write(text);
      output.flush();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * prints the specified object to the OutputStream.  If it is the start
   * of a new line it is indented.  A newline is appended to the end of the
   * output and the printer is told that the next thing printed should
   * be considered the beginning of a line.
   *
   * @param obj the object to print.
   * @return <code>void</code>
   */
  public void println(Object obj) {
    println(obj.toString());
  }

  /**
   * prints the specified text to the OutputStream.  If it is the start
   * of a new line it is indented.  A newline is appended to the end of the
   * output and the printer is told that the next thing printed should
   * be considered the beginning of a line.
   *
   * @param text the String to print.
   * @return <code>void</code>
   */
  public void println(String text) {
    try {
      if (beginLine) {
      tabify();
      }
      output.write(text);
      output.newLine();
      output.flush();
      reset();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * closes the printers output stream
   * @return <code>void</code>
   * @throws Exception
   */
  public void close() throws IOException {
    output.close();
  }

  /**
   * writes some appropriate number of tab strings to the output based on the indent level.
   */
  private void tabify() {
    try {
      for (int i = 0; i < indentLevel; i++) {
      output.write(tabString);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}
