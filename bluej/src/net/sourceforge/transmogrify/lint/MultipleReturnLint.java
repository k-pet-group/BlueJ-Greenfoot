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

// $Id: MultipleReturnLint.java 1011 2001-11-22 10:36:26Z ajp $

package net.sourceforge.transmogrify.lint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.transmogrify.symtab.parser.*;

public class MultipleReturnLint extends Lint {

  public static String WARNING = "Multiple return statements";

  public void handleNode(SymTabAST node) {
    if (node.getType() == JavaTokenTypes.METHOD_DEF) {
      SymTabAST slist = node.getFirstChildOfType(JavaTokenTypes.SLIST);
      if (slist != null) {
        MethodDefTreeWalker walker = new MethodDefTreeWalker(slist);
        walker.walkTree();
        
        List returnStatements = walker.getReturnStatements();
        if (returnStatements.size() > 1) {
          Iterator it = returnStatements.iterator();
          while (it.hasNext()) {
            addWarning(new Warning(WARNING, (SymTabAST)it.next()));
          }
        }
      }
    }
  }

  class MethodDefTreeWalker extends TreeWalker {
    private List returnStatements = new ArrayList();

    public MethodDefTreeWalker(SymTabAST node) {
      super(node);
    }

    public void walkNode(SymTabAST node) {
      if (!isInnerClass(node)) {
        if (node.getType() == JavaTokenTypes.LITERAL_return) {
          returnStatements.add(node);
        }
        super.walkNode(node);
      }
    }

    public List getReturnStatements() {
      return returnStatements;
    }

    private boolean isInnerClass(SymTabAST node) {
      boolean result = false;

      if (node.getType() == JavaTokenTypes.CLASS_DEF
          || isAnonymousInnerClass(node)) {
        result = true;
      }

      return result;
    }

    private boolean isAnonymousInnerClass(SymTabAST node) {
      boolean result = false;

      SymTabAST parent = node.getParent();
      if (parent.getType() == JavaTokenTypes.LITERAL_new) {
        if (parent.getFirstChildOfType(JavaTokenTypes.OBJBLOCK) != null) {
          result = true;
        }
      }

      return result;
    }
  }

}
