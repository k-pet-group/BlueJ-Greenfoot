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
package net.sourceforge.transmogrify.symtab;

// $Id: Main.java 1011 2001-11-22 10:36:26Z ajp $

import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.*;


import java.io.*;
import antlr.collections.AST;
import antlr.collections.ASTEnumeration;
import antlr.debug.misc.*;
import antlr.*;
import java.awt.event.*;

import java.util.Vector;

/**
 * an entry point for parsing files and constructing a symbol table
 * from the command line.  Processes each file or directory listed
 * on the command line.  Shows a nifty JTreePane that displays the
 * AST result of processing.
 */

public class Main {

  private static ASTFactory factory = new ASTFactory();
  private static AST root = factory.create(0, "AST ROOT");

  private static File currentFile;

  private static SymbolTable symbolTable = null;

  public static void main(String[] args) {
    try {
      if (args.length > 0) {
        System.err.println("Parsing...");

        FileParser fileParser = new FileParser();

        for (int i = 0; i < args.length; i++) {
          fileParser.doFile(new File(args[i]));
        }

        SymTabAST tree = (SymTabAST)(fileParser.getTree());
        TreePane.showTree( tree );

      }
      else {
        System.err.println("Usage: java " +
                           Main.class.getName() +
                           " <directory or file name>");
      }
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
}
