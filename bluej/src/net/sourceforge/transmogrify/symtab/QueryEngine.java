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
import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.*;

// $Id: QueryEngine.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * a wrapper around a <code>SymbolTable</code> that makes Definition and
 * reference lookup easier
 */
public class QueryEngine {
  private SymbolTable symbolTable;
  private ScopeIndex index;

  public QueryEngine(SymbolTable symbolTable) {
    this.symbolTable = symbolTable;
    setIndex();
  }

  /**
   * sets the scope index that this <code>QueryEngine</code> uses
   */
  private void setIndex() {
    index = symbolTable.getScopeIndex();
  }

  /**
   * gets a symbol from the associated symbol table
   *
   * @param name the name of the symbol to get
   * @param location the location of that symbol
   *
   * @return Object the (possibly null) result of the lookup
   */
  public Reference getSymbol(String name, Occurrence location) {
    Scope enclosingScope = index.lookup(location);
    Reference result = enclosingScope.getSymbol(name, location);

    // REDTAG -- for cases like a label on the same line as the
    //           block it names, e.g. 'bar: for(int i = 0; ...'
    if (result == null) {
      result = enclosingScope.getParentScope().getSymbol(name, location);
    }

    return result;
  }

  /**
   * gets the definition of the given symbol
   *
   * @param name the name of the symbol to consider
   * @param location the <code>Occurrence</code> that represents the
   *                 location of the symbol
   *
   * @return Definition the (possibly null) result of the lookup
   */
  public IDefinition getDefinition(String name, Occurrence location) {
    Reference symbol = getSymbol(name, location);

    //if (symbol != null) {
    //  System.out.println("  found " + name);
    //}
    //else {
    //  System.out.println("  !could not find " + name);
    //}

    return resolveDefinition(symbol);
  }

  public IDefinition getDefinition(Occurrence location) {
    IDefinition result = null;

    SymTabAST node = getWordNodeAtOccurrence(location);
    if ( node != null ) {
      result = node.getDefinition();
    }

    return result;
  }

  private IDefinition resolveDefinition(Reference reference) {
    IDefinition result = null;

    if ( reference != null ) {
      result = reference.getDefinition();
    }

    return result;
  }

  /**
   * gets a collection of references determined by a symbol and location
   *
   * @param name the name of the symbol to consider
   * @param location the <code>Occurrence</code> that represents its location
   *
   * @return
   */
  public Iterator getReferences(String name, Occurrence location) {
    Reference symbol = getSymbol(name, location);
    return resolveReferences(symbol);
  }

  public Iterator getReferences(Occurrence location) {
    Iterator result = null;

    SymTabAST node = getWordNodeAtOccurrence(location);
    if ( node != null && node.getDefinition() != null ) {
      result = node.getDefinition().getReferences();
    }

    return result;
  }

  private Iterator resolveReferences(Reference reference) {
    return reference.getDefinition().getReferences();
  }

  public SymTabAST getFileNode(File file) {
    return ASTUtil.getFileNode(symbolTable.getTree(), file);
  }

  private SymTabAST getWordNodeAtOccurrence(Occurrence location) {
    SymTabAST result = null;

    SymTabAST fileNode = getFileNode(location.getFile());
    if ( fileNode != null ) {
      SymTabAST node = fileNode.getEnclosingNode(location.getLine(),
                                                 location.getColumn());

      if ( (node != null) && (node.getType() == JavaTokenTypes.IDENT) ) {
        result = node;
      }
    }

    return result;
  }

  public String getWordAtOccurrence(Occurrence location ) {
    String result = null;

    SymTabAST node = getWordNodeAtOccurrence(location);
    if ( node != null ) {
      result = node.getText();
    }

    return result;
  }

}




