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


// $Id: NewPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints a statement of type JavaTokenTypes.LITERAL_new
 */

public class NewPrinter extends ChildIgnoringPrinter {
  public NewPrinter(SymTabAST nodeToPrint) {
    super(nodeToPrint);
  }

  /**
   * prints a statement of type JavaTokenTypes.LITERAL_new to the printer.
   * There are three flavors of these:<br>
   * <br>
   * those that have an object block<br>
   * those that do not<br>
   * those thar are array initializers<br>
   *
   * @param printout the PrettyPrinter to print to
   */
  public void printSelf(PrettyPrinter printout) throws IOException {
    // the ident could be a DOT or a propert IDENT node
    SymTabAST ident = (SymTabAST)nodeToPrint.getFirstChild();
    SymTabAST eList = nodeToPrint.getFirstChildOfType(JavaTokenTypes.ELIST);
    SymTabAST objBlock = nodeToPrint.getFirstChildOfType(JavaTokenTypes.OBJBLOCK);

    printout.print(nodeToPrint.getText() + " ");

    if (eList != null) {
      PrinterFactory.makePrinter(ident).print(printout);
      printOpenParen(printout);
      PrinterFactory.makePrinter(eList).print(printout);
      printCloseParen(printout);

      if (objBlock != null) {
        new AnonymousInnerClassPrinter(objBlock).print(printout);
      }
    }
    else {
      // its an array thingy (as far as we know...)
      SymTabAST type = (SymTabAST)nodeToPrint.getFirstChild();
      SymTabAST arrayDecl = (SymTabAST)type.getNextSibling();
      SymTabAST arrayInit = (SymTabAST)arrayDecl.getNextSibling();

      PrinterFactory.makePrinter(type).print(printout);
      PrinterFactory.makePrinter(arrayDecl).print(printout);
      if (arrayInit != null) {
        PrinterFactory.makePrinter(arrayInit).print(printout);
      }
    }
  }
}
