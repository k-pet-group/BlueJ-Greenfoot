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

import antlr.collections.AST;

import net.sourceforge.transmogrify.symtab.parser.SymTabAST;
import net.sourceforge.transmogrify.symtab.parser.ASTUtil;

// $Id: BlockDef.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>BlockDef</code> is a <code>Scope</code> which contains
 * information about everything that defines a nameless block of java code.
 * There are provisions for keeping the name of several anonymous
 * blocks of java code within the same parent scope to have unique names.
 *
 * @see Scope
 * @see Resolvable
 */

public class BlockDef extends DefaultScope {
  private static long id = 0;

  public BlockDef( Scope parentScope, SymTabAST node ) {
    super( "~Anonymous~" + id++, parentScope, node );
  }

}
