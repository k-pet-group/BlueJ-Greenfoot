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

// $Id: IDefinition.java 1011 2001-11-22 10:36:26Z ajp $

import java.util.Iterator;

/**
 * implemented by all definitions of the <code>SymTabAST</code> tree, including
 * source/non-sourced node
 * @see net.sourceforge.transmogrify.symtab.Definition
 * @see net.sourceforge.transmogrify.symtab.ExternalDefinition
 */
public interface IDefinition {

  /**
   * verifies if this definition node is source/non-sourced(with no source-code)
   * @return <code>true</code> if the node is parsed from a source code
   *         <code>false</code> otherwise
   */
  public boolean isSourced();

  /**
   * gets the name that identified this particular definition/node
   * @return name for this definition
   */
  public String getName();

  /**
   * gets the fully qualified name of the definition, ie. dotted name for classes,
   * or method name with its signature for methods, etc
   * @return qualified name for this node
   */
  public String getQualifiedName();

  /**
   * adds any reference that this definition have to its collection of
   * <code>_references</code>
   * @param reference reference to be added which has <code>SymTabAST</code> node
   * @return <code>void</code>
   * @see net.sourceforge.transmogrify.symtab.antlr.SymTabAST
   */
  public void addReference(Reference reference);

  /**
   * gets the collection of references refer to by this definition
   * @return iterator of the references
   */
  public Iterator getReferences();

  /**
   * gets the number of references refer to by this definition
   * @return number of references
   */
  public int getNumReferences();
}