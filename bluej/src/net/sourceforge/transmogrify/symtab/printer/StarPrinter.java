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

import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;


// $Id: StarPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints an AST node of type JavaTokenTypes.STAR
 */

public class StarPrinter extends ChildIgnoringPrinter {
  public StarPrinter(SymTabAST nodeToPrint) {
    super(nodeToPrint);
  }

  /**
   * prints an AST node of type JavaTokenTypes.STAR to the printer.
   * There are two flavors of these:<br>
   * <br>
   * the star is in an import statement<br>
   * the star is a binary operator<br>
   */
  public void printSelf(PrettyPrinter printout) throws IOException {
    SymTabAST lhs = (SymTabAST)nodeToPrint.getFirstChild();

    if (lhs != null) {
      SymTabAST rhs = (SymTabAST)lhs.getNextSibling();
      PrinterFactory.makePrinter(lhs).print(printout);
      printout.print(" " + nodeToPrint.getText() + " ");
      PrinterFactory.makePrinter(rhs).print(printout);
    }
    else {
      printout.print(nodeToPrint.getText());
    }
  }
}
