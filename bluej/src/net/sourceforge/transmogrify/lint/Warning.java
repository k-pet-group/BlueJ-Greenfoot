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

// $Id: Warning.java 1011 2001-11-22 10:36:26Z ajp $

package net.sourceforge.transmogrify.lint;

import net.sourceforge.transmogrify.symtab.parser.SymTabAST;
import net.sourceforge.transmogrify.symtab.*;

public class Warning {

  private String _text;
  private SymTabAST _node;
  private Occurrence _occ;

  public Warning(String text, SymTabAST node) {
    _text = text;
    _node = node;
    _occ = new Occurrence(_node);
  }

  public String getText() {
    return _text;
  }

  public Occurrence getOccurrence() {
    return _occ;
  }

  public String toString() {
    return _text + ": " + "Line " + _occ.getLine() + ", column " + _occ.getColumn() + " in " + _occ.getFile();
  }

  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof Warning) {
      Warning warning = (Warning)o;
      result = (getOccurrence().equals(warning.getOccurrence())
                && getText().equals(warning.getText()));
    }

    return result;
  }

  public int hashCode() {
    return _node.hashCode();
  }
}
