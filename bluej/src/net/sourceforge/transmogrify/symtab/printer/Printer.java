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

import antlr.*;

import net.sourceforge.transmogrify.symtab.parser.*;

import java.io.*;

// $Id: Printer.java 1014 2001-11-30 03:28:10Z ajp $

public abstract class Printer {
  protected SymTabAST nodeToPrint;


  private ASTFactory factory;

  /**
   * constructor with node to be printed
   * @param node node to be printed
   */
  public Printer(SymTabAST node)
  {
    nodeToPrint = node;
    factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());
  }

  /**
   * prints the node with input <code>PrettyPrinter</code>
   * @param out object used to print the node
   * @return <code>void</code>
   * @throws IOException
   */
  public final void print(PrettyPrinter out) throws IOException
  {
    printHiddenBefore(out, ((SymTabAST)nodeToPrint).getHiddenBefore());
    printLeadingFormat(out);
    printSelf(out);
    printTrailingFormat(out);
  }

  private final void printHiddenBefore(PrettyPrinter out,
    CommonHiddenStreamToken tok) throws IOException
  {
    if (tok != null)
    {
      //      printHiddenBefore(out, tok.getHiddenBefore());
      printHidden(out, tok);
    }
  }

  private final void printHidden(PrettyPrinter out,
    CommonHiddenStreamToken tok) throws IOException
  {

    SymTabAST node = (SymTabAST)factory.create(0, "");
    node.initialize(tok);
    PrinterFactory.makePrinter(node).print(out);
  }

  protected final void endStatement(PrettyPrinter out)
  {
    out.print(";");
  }

  protected final void newline(PrettyPrinter out)
  {
    out.println("");
  }

  protected final void printOpenBrace(PrettyPrinter out)
  {
    if (PrinterProperties.hasOpenBraceOnNewLine())
    {
      newline(out);
    }else if (PrinterProperties.hasSpaceOutsideBraces())
    {
      out.print(" ");
    }
    out.print("{");
    newline(out);
  }

  protected final void printCloseBrace(PrettyPrinter out)
  {
    out.print("}");
    if (PrinterProperties.hasSpaceOutsideBraces())
    {
      out.print(" ");
    }
  }

  protected final void printPreBlockExpression(PrettyPrinter out)
  {
    if (PrinterProperties.hasSpaceBeforeBlockExpression())
    {
      out.print(" ");
    }
  }

  protected final void printOpenParen(PrettyPrinter out)
  {
    out.print("(");
    if (PrinterProperties.hasSpaceInsideParens())
    {
      out.print(" ");
    }
  }

  protected final void printCloseParen(PrettyPrinter out)
  {
    if (PrinterProperties.hasSpaceInsideParens())
    {
      out.print(" ");
    }
    out.print(")");
  }

  protected final void printSiblingEnd(PrettyPrinter out)
  {
    if (PrinterProperties.hasSiblingBlockOnNewLine())
    {
      newline(out);
    }
  }

  protected abstract void printSelf(PrettyPrinter out) throws IOException;
  protected abstract void printLeadingFormat(PrettyPrinter out) throws IOException;
  protected abstract void printTrailingFormat(PrettyPrinter out) throws IOException;
}
