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
package net.sourceforge.transmogrify.symtab.parser;

import java.io.File;

// $Id: SymTabToken.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * an extensions of <code>antlr.CommonToken</code> that includes extra
 * information about the location of the the token.  This information
 * includes the file and line number of the token
 *
 * To use this Token in your lexuer structure, assuming your
 * antlr.Lexer is called lexer, use
 *
 * lexer.setTokenObjectClass(SymTabToken.class.getName());
 *
 */

public class SymTabToken extends antlr.CommonHiddenStreamToken {
  protected File _file;
  protected int _line;
  protected int _column;

  /**
   * sets the file of the token
   *
   * @param file the new <code>File</code>
   */
  public void setFile(File file) {
    _file = file;
  }

  /**
   * gets the file of the token
   *
   * @return File
   */
  public File getFile() {
    return _file;
  }

  /**
   * sets the line of the token
   *
   * @param line the new line
   */
  public void setLine(int line) {
    _line = line;
  }

  /**
   * gets the line of the token
   *
   * @return int
   */
  public int getLine() {
    return _line;
  }

  /**
   * sets the column where the token begins
   *
   * @param column the new column
   */
  public void setColumn(int column) {
    _column = column;
  }

  /**
   * gets the column where the token begins
   *
   * @return the column where the token begins
   */
  public int getColumn() {
    return _column;
  }
}



