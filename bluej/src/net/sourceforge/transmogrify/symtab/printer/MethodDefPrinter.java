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

// $Id: MethodDefPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints a method definition
 */

public class MethodDefPrinter extends ChildIgnoringPrinter {
  public MethodDefPrinter(SymTabAST nodeToPrint) {
    super(nodeToPrint);
  }

  /**
   * prints a method definition to the printer
   *
   * @param printout the PrettyPrinter to print to
   */
  public void printSelf(PrettyPrinter printout) throws IOException {
    SymTabAST modifiers = nodeToPrint.getFirstChildOfType(JavaTokenTypes.MODIFIERS);
    SymTabAST type = nodeToPrint.getFirstChildOfType(JavaTokenTypes.TYPE);
    SymTabAST ident = nodeToPrint.getFirstChildOfType(JavaTokenTypes.IDENT);
    SymTabAST params = nodeToPrint.getFirstChildOfType(JavaTokenTypes.PARAMETERS);
    SymTabAST throwsClause = nodeToPrint.getFirstChildOfType(JavaTokenTypes.LITERAL_throws);
    SymTabAST sList = nodeToPrint.getFirstChildOfType(JavaTokenTypes.SLIST);

    PrinterFactory.makePrinter(modifiers).print(printout);
    PrinterFactory.makePrinter(type).print(printout);
    printout.print(" ");
    PrinterFactory.makePrinter(ident).print(printout);
    printOpenParen(printout);
    PrinterFactory.makePrinter(params).print(printout);
    printCloseParen(printout);

    if (throwsClause != null) {
      PrinterFactory.makePrinter(throwsClause).print(printout);
    }

    if (sList != null) {
      PrinterFactory.makePrinter(sList).print(printout);
    }
    else {
      endStatement(printout);
    }
    newline(printout);
  }
}
