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

import java.util.Vector;

import net.sourceforge.transmogrify.symtab.parser.*;


// $Id: LabelDef.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>LabelDef</code> is a <code>Definition</code> that contains information
 * about the definition of a Label.
 *
 * @see Definition
 */

public class LabelDef extends Definition {

  public LabelDef( String name, Scope parentScope, SymTabAST node ) {
    super( name, parentScope, node );
  }

}
