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

import java.io.*;

import net.sourceforge.transmogrify.symtab.parser.*;

// $Id: CatchPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints a catch block
 */

public class CatchPrinter extends ChildIgnoringPrinter {
    public CatchPrinter(SymTabAST nodeToPrint)
    {
        super(nodeToPrint);
    }

    /**
    * prints the catch block to the PrettyPrinter
    *
    * @param printout the PrettyPrinter you would like to print to
    */
    public void printSelf(PrettyPrinter printout) throws IOException
    {
        SymTabAST parameterDef = nodeToPrint.getFirstChildOfType(JavaTokenTypes.PARAMETER_DEF);
        SymTabAST sList = nodeToPrint.getFirstChildOfType(JavaTokenTypes.SLIST);

        printout.print("catch");
        printPreBlockExpression(printout);
        printOpenParen(printout);
        PrinterFactory.makePrinter(parameterDef).print(printout);
        printCloseParen(printout);
        PrinterFactory.makePrinter(sList).print(printout);
        if (nodeToPrint.getNextSibling() == null)
        {
            if (nodeToPrint.getParent().getParent().getType() == JavaTokenTypes.LITERAL_finally)
            {
                printSiblingEnd(printout);
            }else
            {
                newline(printout);
            }
        }else
        {
            printSiblingEnd(printout);
        }
    }
}
