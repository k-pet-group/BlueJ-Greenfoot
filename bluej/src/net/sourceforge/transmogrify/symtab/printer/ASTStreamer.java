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

// $Id: ASTStreamer.java 1014 2001-11-30 03:28:10Z ajp $
package net.sourceforge.transmogrify.symtab.printer;

import net.sourceforge.transmogrify.symtab.parser.SymTabAST;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class ASTStreamer {
  private ASTPrintManager _printManager;

  public ASTStreamer() {
    this(new FilePrintManager());
  }

  public ASTStreamer(ASTPrintManager printManager) {
    _printManager = printManager;
  }

  /**
   * spits all files in the AST to disk
   * @return <code>void</code>
   * @throws <code>Exception</code>
   */
  public void streamFiles(SymTabAST root) throws IOException {
    SymTabAST node = (SymTabAST)root.getFirstChild();
    while (node != null) {
      if (node.isDirty()) {
        // don't bother to clear the dirty flag because
        // the whole tree is thrown away
        File file = node.getFile();
        Writer writer = _printManager.getWriter(file);

        PrettyPrinter output = new PrettyPrinter(writer);
        PrinterFactory.makePrinter(node).print(output);
        output.close();
      }

      node = (SymTabAST)node.getNextSibling();
    }
  }
}