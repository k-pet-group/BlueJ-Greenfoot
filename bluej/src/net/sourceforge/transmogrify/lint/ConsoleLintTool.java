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

// $Id: ConsoleLintTool.java 1011 2001-11-22 10:36:26Z ajp $

package net.sourceforge.transmogrify.lint;

import java.io.File;
import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.*;

public class ConsoleLintTool extends AbstractLintTool {

  private SymTabAST _root;
  private List _lints;

  public ConsoleLintTool(SymTabAST root, List lints) {
    _root = root;
    _lints = lints;
  }

  protected SymTabAST getTree() {
    return _root;
  }

  protected List getLints() {
    return _lints;
  }

  public void displayWarnings() {
    Iterator it = _lints.iterator();
    while (it.hasNext()) {
      printWarningsForLint((Lint)it.next());
    }
  }

  private void printWarningsForLint(Lint lint) {
    Iterator it = lint.getWarnings().iterator();
    while (it.hasNext()) {
      System.out.println(it.next());
    }
  }

  public static void main(String[] args) {
    try {
      if (args.length < 2) {
        throw new IllegalArgumentException();
      }

      List lints = new ArrayList();
      for (int i = 0; i < args.length - 1; i++) {
        Class lintClass = Class.forName(args[i]);
        Lint lint = (Lint)lintClass.newInstance();
        lints.add(lint);
      }
      
      FileParser parser = new FileParser();
      parser.doFile(new File(args[args.length - 1]));
      SymTabAST root = parser.getTree();

      ConsoleLintTool lintTool = new ConsoleLintTool(root, lints);
      lintTool.lint();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

}
