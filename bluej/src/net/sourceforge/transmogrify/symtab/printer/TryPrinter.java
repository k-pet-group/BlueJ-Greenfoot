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


// $Id: TryPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints a try block
 */

public class TryPrinter extends DefaultPrinter {
  public TryPrinter(SymTabAST nodeToPrint) {
    super(nodeToPrint);
  }

  /**
   * prints a try block to the printer
   *
   * @param printout the PrettyPrinter to print to
   */
  public void printSelf(PrettyPrinter printout) throws IOException {
    SymTabAST slist = nodeToPrint.getFirstChildOfType(JavaTokenTypes.SLIST);

    printout.print("try");
    PrinterFactory.makePrinter(slist).print(printout);
    printSiblingEnd(printout);

    SymTabASTIterator children = nodeToPrint.getChildren();
    // skip the slist
    children.next();

    while(children.hasNext()) {
      PrinterFactory.makePrinter(children.nextChild()).print(printout);
    }
  }
}
