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


import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import java.io.File;

import net.sourceforge.transmogrify.symtab.parser.*;


public class ReferenceThreshold extends ReferenceTool {
  private Set _underreferencedDefinitions;
  private int _threshold;

  public ReferenceThreshold(SymbolTable table, int threshold) {
    super(table.getTree());
    _threshold = threshold;
    _underreferencedDefinitions = new TreeSet();
    go();
  }

  private void go() {
    collectUnderreferencedDefinitions();
  }

  private void collectUnderreferencedDefinitions() {
    handleNode(_tree);
  }

  protected void handleNode(SymTabAST node) {
    if (node.isMeaningful()) {
      IDefinition def = node.getDefinition();
      if (def != null &&
          def.getNumReferences() <= _threshold &&
          def instanceof Definition) {
        // this is inheritently hackish
        // basically we want to ignore
        // all test cases and main() methods
        if (def.getQualifiedName().indexOf("test") < 0 &&
            def.getQualifiedName().indexOf("main") < 0) {
          _underreferencedDefinitions.add(def);
        }
      }
    }

    walkChildren(node);
  }

  public Set getUnderreferencedDefinitions() {
    return _underreferencedDefinitions;
  }

  public static void main(String[] args) {
    FileParser parser = new FileParser();
    int firstFile = 0;
    int threshold = 1;

    try {
      threshold = Integer.valueOf(args[0]).intValue();
      firstFile = 1;
    }
    catch (NumberFormatException ignoreMe) {}

    try {
      for (int i = firstFile; i < args.length; i++) {
           parser.doFile(new File(args[i]));
      }

      SymTabAST tree = (SymTabAST)parser.getTree();
      SymbolTable table = new TableMaker( tree ).getTable();
      ReferenceThreshold counter = new ReferenceThreshold(table, threshold);

      System.out.println("The following definitions have no more than "
                         + threshold
                         + " references");

      Set defs = counter.getUnderreferencedDefinitions();

      for (Iterator it = defs.iterator(); it.hasNext(); ) {
        System.out.println(it.next());
      }
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
  }
}
