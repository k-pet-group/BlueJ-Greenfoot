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
package net.sourceforge.transmogrify.symtab;

import java.io.File;

import net.sourceforge.transmogrify.symtab.parser.SymTabAST;

// $Id: Occurrence.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>Occurrence</code> contains file and line number information.
 * It is used to denote the location of various <code>Definintion</code>s
 * and <code>Reference</code>s
 *
 * @see Reference
 * @see Definition
 */

public class Occurrence implements Comparable {
  private File _file;
  private int _line;
  private int _column;

  public Occurrence(File file, int line, int column) {
    _file = file;
    _line = line;
    _column = column;
  }

  public Occurrence(SymTabAST node) {
    _file = node.getFile();
    _line = node.getLine();
    _column = node.getColumn();
  }

  /**
   * returns the File of this <code>Occurrence</code>
   *
   * @return File
   */
  public File getFile() {
    return _file;
  }

  /**
   * returns the line number of this <code>Occurrence</code>
   *
   * @return the line number of this <code>Occurrence</code>
   */
  public int getLine() {
    return _line;
  }

  /**
   * returns the column that this token starts at
   *
   * @return the column that this token starts at
   */
  public int getColumn() {
    return _column;
  }

  public int compareTo(Object o) {
    if (!(o instanceof Occurrence)) {
      throw new ClassCastException(getClass().getName());
    }

    Occurrence other = (Occurrence)o;

    int result = 0;

    result = getFile().compareTo(other.getFile());

    if (result == 0) {
      result = getLine() - other.getLine();
    }
    if (result == 0) {
      result = getColumn() - other.getColumn();
    }

    return result;
  }

  public boolean equals(Object o) {
    boolean result = false;
    
    if (o instanceof Occurrence) {
      Occurrence occ = (Occurrence)o;
      result = (getFile().equals(occ.getFile())
                && getLine() == occ.getLine()
                && getColumn() == occ.getColumn());
    }

    return result;
  }

  public int hashCode() {
    return getFile().hashCode();
  }

  public String toString() {
    return "[" + getFile() + ":" + getLine() + "," + getColumn() + "]";
  }

}
