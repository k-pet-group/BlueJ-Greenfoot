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

import java.io.InputStream;
import antlr.Token;

public class SymTabLexer extends JavaLexer {
  protected int tokColumn = 1;
  protected int column = 1;

  public SymTabLexer(InputStream in) {
    super(in);
  }

  public void consume() throws antlr.CharStreamException {
    if ( inputState.guessing==0 ) {
      if (text.length()==0) {
        // remember token start column
        tokColumn = column;
      }
      if (LA(1)=='\n') { column = 1; }
      else { column++; }
    }
    super.consume();
  }

  protected Token makeToken(int t) {
    Token tok = super.makeToken(t);
    tok.setColumn(tokColumn);
    return tok;
  }
}