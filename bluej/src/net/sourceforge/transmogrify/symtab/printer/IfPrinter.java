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


// $Id: IfPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints an if statement
 */

public class IfPrinter extends ChildIgnoringPrinter {
    public IfPrinter(SymTabAST nodeToPrint)
    {
        super(nodeToPrint);
    }

    /**
    * prints an if statement to the PrettyPrinter.  Handles three
    * cases for the else block:<br>
    * <br>
    * else branch is null<br>
    * else branch is an if block<br>
    * else branch is an slist ( an actual 'else' block )<br>
    *
    * @param printout the PrettyPrinter to print to
    */
    public void printSelf(PrettyPrinter printout) throws IOException
    {
        SymTabAST exprNode = nodeToPrint.getFirstChildOfType(JavaTokenTypes.EXPR);
        SymTabAST ifBranch = (SymTabAST)exprNode.getNextSibling();
        SymTabAST elseBranch = (SymTabAST)ifBranch.getNextSibling();

        printout.print(nodeToPrint.getText());
        printPreBlockExpression(printout);
        printOpenParen(printout);
        PrinterFactory.makePrinter(exprNode).print(printout);
        printCloseParen(printout);

        printBranch(ifBranch, printout);

        if (elseBranch == null)
        {
            newline(printout);
        }else
        {
            printSiblingEnd(printout);
            printout.print("else");
            if (elseBranch.getType() != JavaTokenTypes.SLIST)
            {
                printout.print(" ");
            }
            printBranch(elseBranch, printout);
            if (elseBranch.getType() != JavaTokenTypes.LITERAL_if)
            {
                newline(printout);
            }
        }

    }

    private void printBranch(SymTabAST branch, PrettyPrinter printout) throws IOException
    {
        switch(branch.getType())
        {
        case JavaTokenTypes.SLIST:
        case JavaTokenTypes.LITERAL_if:
        case JavaTokenTypes.EMPTY_STAT:
            PrinterFactory.makePrinter(branch).print(printout);
            break;

        case JavaTokenTypes.EXPR:
        case JavaTokenTypes.LITERAL_return:
        case JavaTokenTypes.LITERAL_throw:
            printout.indent();
            PrinterFactory.makePrinter(branch).print(printout);
            printout.unindent();
            endStatement(printout);
            break;

        }

    }
}
