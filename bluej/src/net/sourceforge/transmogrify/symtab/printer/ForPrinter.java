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


// $Id: ForPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints a for loop
 */

public class ForPrinter extends ChildIgnoringPrinter {
  public ForPrinter(SymTabAST nodeToPrint) {
    super(nodeToPrint);
  }

  /**
   * prints a for loop to the PrettyPrinter
   *
   * @param printout the PrettyPrinter to print to
   */
  public void printSelf(PrettyPrinter printout) throws IOException {
    SymTabAST forInit = nodeToPrint.getFirstChildOfType(JavaTokenTypes.FOR_INIT);
    SymTabAST forCond = nodeToPrint.getFirstChildOfType(JavaTokenTypes.FOR_CONDITION);
    SymTabAST forIter = nodeToPrint.getFirstChildOfType(JavaTokenTypes.FOR_ITERATOR);
    SymTabAST body = (SymTabAST)forIter.getNextSibling();

    printout.print(nodeToPrint.getText());
    printPreBlockExpression(printout);
    printOpenParen(printout);
    if (forInit != null) {
      PrinterFactory.makePrinter(forInit).print(printout);
    }
    printout.print("; ");
    PrinterFactory.makePrinter(forCond).print(printout);
    printout.print("; ");
    PrinterFactory.makePrinter(forIter).print(printout);
    printCloseParen(printout);
    PrinterFactory.makePrinter(body).print(printout);
    if (body.getType() == JavaTokenTypes.EXPR) {
      endStatement(printout);
    }
    newline(printout);
  }
}
