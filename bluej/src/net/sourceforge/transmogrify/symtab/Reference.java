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

import java.io.File;

import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.PrettyPrinter;

// $Id: Reference.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * represents a place where a definition is used.  There are two flavors
 * of references -- resolved (those that have a definition associated with
 * them) and unresolved (those that don't have a definition associated).
 * The goal of the resolution step is to get all of the references in the
 * symbol table to fall into the resolved category.
 */

public class Reference implements Comparable {

  private SymTabAST _node;
  private Occurrence _occurrence;

  public Reference( SymTabAST node ) {
    _node = node;
    _occurrence = new Occurrence( _node.getFile(), ASTUtil.getLine(_node), ASTUtil.getColumn(_node) );
  }

  private void method() {
  }

  /**
   * gets the definition associated with this reference
   *
   * @return Definition the (possibly null) definition associated with
   *                    this reference
   */
  public IDefinition getDefinition() {
    return _node.getDefinition();
  }

  /**
   * return the node that was passed in during ctor
   */
  public SymTabAST getTreeNode() {
    return _node;
  }

  /**
   * gets the occurrence of this reference
   *
   * @return Occurrence
   */
  public Occurrence getOccurrence() {
    return _occurrence;
  }

  /**
   * gets the line where the node resides
   */
  public int getLine() {
    return getOccurrence().getLine();
  }

  /**
   * gets the column for where the node resides
   */
  public int getColumn() {
    return getOccurrence().getColumn();
  }

  /**
   * gets the enclosing file for the node
   */
  public File getFile() {
    return getOccurrence().getFile();
  }

  /**
   * gets the name of the reference
   *
   * @return String the name of the definition associated with this reference
   *                if this reference is resolved, else null
   */
  public String getName() {
    return _node.getName();
  }

  /**
   * sends information about this object to a pretty printer
   *
   * @param printer the <code>PrettyPrinter</code> to send the information
   *                to
   */
  public void report(PrettyPrinter printer) {
    printer.println("at " + getOccurrence());
  }

  /**
   * returns a string representation of the reference.
   *
   * @return String
   */
  public String toString() {
    return getOccurrence().toString();
  }

  /**
   * returns whether the <code>Reference</code>s are equal
   *
   * @return whether the <code>Reference</code>s are equal
   */
   // REDTAG -- not finished
  public boolean equals(Object obj){
    boolean result = false;
    if (obj instanceof Reference) {
      result = getOccurrence().equals(((Reference)obj).getOccurrence());
    }
    return result;
  }

  public int compareTo(Object o) {
    if (!(o instanceof Reference)) {
      throw new ClassCastException(getClass().getName());
    }

    return getOccurrence().compareTo(((Reference)o).getOccurrence());
  }
}
