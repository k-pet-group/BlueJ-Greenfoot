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

import net.sourceforge.transmogrify.symtab.parser.SymTabAST;
import java.io.*;

//$Id: AssignPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints the and Assignment
 */

public class AssignPrinter extends ChildIgnoringPrinter {
  public AssignPrinter(SymTabAST nodeToPrint) {
    super(nodeToPrint);
  }

//A tag that is colored red used to symbolize the incompatentcy of a programmer
//or used to harold a piece of godforsakenly hidious code.
  // REDTAG -- also a binary operator, written for varaible init.

/**
 * prints the assignment to the PrettyPrinter.  If the left operand is null
 * we are printing the right side of a variable declaration, otherwise
 * we are printing a regular assignemnt.
 *
 * @param printout the printer object you would like to print to.
 */
  public void printSelf(PrettyPrinter printout) throws IOException {
    SymTabAST firstChild = (SymTabAST)nodeToPrint.getFirstChild();
    SymTabAST secondChild = (SymTabAST)firstChild.getNextSibling();

    if (secondChild == null) {
      // the assignment's lhs is a variable def
      // so the first child is the rhs
      printout.print(" = ");
      PrinterFactory.makePrinter(firstChild).print(printout);
    }
    else {
      PrinterFactory.makePrinter(firstChild).print(printout);
      printout.print(" = ");
      PrinterFactory.makePrinter(secondChild).print(printout);
    }
  }
}
