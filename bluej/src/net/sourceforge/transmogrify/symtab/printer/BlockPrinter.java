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


//$Id: BlockPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints out a list of statements or expressions indented one level
 */

public class BlockPrinter extends DefaultPrinter {
    public BlockPrinter(SymTabAST nodeToPrint)
    {
        super(nodeToPrint);
    }

    /**
    * prints the block.  If the text node of the block is an opening
    * brace ( '{' ), opening and closing braces are printed.  The children
    * of the block are indented according to the PrettyPrinter.
    *
    * @param printout the printer object you would like to print to.
    */
    public void printSelf(PrettyPrinter printout) throws IOException
    {
        if (nodeToPrint.getText().equals("{"))
        {
            printOpenBrace(printout);
        }
        printout.indent();

        SymTabASTIterator children = nodeToPrint.getChildren();
        int previousType = 0;
        while(children.hasNext())
        {
            SymTabAST child = children.nextChild();
            int currentType = child.getType();
            if (PrinterProperties.hasNewLineBetweenExpressionGroups() && previousType != 0 && currentType != JavaTokenTypes.RCURLY)
            {
                if (currentType != previousType ||
                    currentType == JavaTokenTypes.LITERAL_if ||
                    currentType == JavaTokenTypes.LITERAL_try ||
                    currentType == JavaTokenTypes.LITERAL_do ||
                    currentType == JavaTokenTypes.LITERAL_for ||
                    currentType == JavaTokenTypes.LITERAL_while ||
                    currentType == JavaTokenTypes.LITERAL_synchronized ||
                    currentType == JavaTokenTypes.LITERAL_switch ||
                    currentType == JavaTokenTypes.LITERAL_finally)
                {
                    newline(printout);
                }
            }
            PrinterFactory.makePrinter(child).print(printout);
            switch(currentType)
            {
            case JavaTokenTypes.EXPR:
            case JavaTokenTypes.LITERAL_throw:
            case JavaTokenTypes.LITERAL_return:
                endStatement(printout);
                newline(printout);
                break;
            }
            previousType = currentType;
        }
    }

    public void printTrailingFormat(PrettyPrinter printout) throws IOException
    {
        printout.unindent();
        if (nodeToPrint.getText().equals("{"))
        {
            printCloseBrace(printout);
        }
    }
}
