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

import net.sourceforge.transmogrify.symtab.parser.SymTabAST;

public abstract class ReferenceTool {
  protected SymTabAST _tree = null;

  public ReferenceTool(SymTabAST tree) {
    _tree = tree;
  }

  protected void walkChildren( SymTabAST node ) {
    if ( node.getFirstChild() != null ) {
      walkSelfAndSiblings( (SymTabAST)node.getFirstChild() );
    }
  }

  private void walkSelfAndSiblings( SymTabAST node ) {
    handleNode( node );
    SymTabAST sibling = (SymTabAST)node.getNextSibling();
    while ( sibling != null ) {
      handleNode( sibling );
      sibling = (SymTabAST)sibling.getNextSibling();
    }
  }

  protected abstract void handleNode(SymTabAST node);
}