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

import net.sourceforge.transmogrify.symtab.parser.*;

import java.util.*;

// $Id: Definition.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>Definition</code> contains basic information for everything
 * that is defined in a java source file.  A definition has a list
 * of <code>Reference</code>s, an <code>Occurrence</code>, a name, and
 * a parent <code>Scope</code>.
 *
 * @see Occurrence
 * @see Scope
 * @see Reference
 */

public abstract class Definition implements IDefinition, Comparable {
  private String _name = null;
  private Scope _parentScope = null;
  private SymTabAST _node = null;
  private SortedSet _references;

  private Occurrence _occurrence = null;

  public Definition(String name, Scope parentScope, SymTabAST node) {
    _name = name;
    _parentScope = parentScope;
    _node = node;
    _references = new TreeSet();

    if ( node != null ) {
      _occurrence = new Occurrence( _node.getFile(),
                                    ASTUtil.getLine( _node ),
                                    ASTUtil.getColumn( _node ));
    }
  }

  public boolean isSourced() {
    return true;
  }

  /**
   * returns the name of the definition
   *
   * @return String
   */

  public String getName() {
    return _name;
  }

  /**
   * returns the node in the AST that represents this definition
   *
   * @return the node in the AST that represents this definition
   */
  public SymTabAST getTreeNode() {
    return _node;
  }

  /**
   * Adds a <code>Reference</code> to the collection of <code>Reference</code>s
   *
   * @param reference the <code>Reference</code> to add
   */
  public void addReference( Reference reference ) {
    _references.add( reference );
  }

  /**
   * Returns the <code>Reference</code>s to this definition
   *
   * @return the <code>Reference</code>s to this definition
   */
  public Iterator getReferences() {
    return _references.iterator();
  }

  public int getNumReferences() {
    return _references.size();
  }

  /**
   * Returns the scope in which this definition is defined
   *
   * @return the scope in which this definition is defined
   */
  public Scope getParentScope() {
    return _parentScope;
  }

  /**
   * returns the fully qualifed name of this defintion.  The name of
   * the parentScope and all of its parents are considered when constructing
   * this name.
   *
   * @return the fully qualified name of this definition
   */
  public String getQualifiedName() {
    String nameToUse = _name;
    String result;

    if (_name == null) {
      nameToUse = "~NO NAME~";
    }

    if (getParentScope() != null &&
         !(getParentScope() instanceof BaseScope)) {
      result = getParentScope().getQualifiedName() + "." + nameToUse;
    }
    else {
      result = nameToUse;
    }
    return result;
  }

  /**
   * Returns the <code>Occurrence</code> for the location of this definition
   *
   * @return the <code>Occurrence</code> for the location of this definition
   */
  public Occurrence getOccurrence() {
    return _occurrence;
  }

  /**
   * Returns the <code>ClassDef</code> that this scope is contained in.
   *
   * @return the <code>ClassDef</code> this definition is contained in
   */
  public ClassDef getEnclosingClass() {
    ClassDef result = null;

    if ( getParentScope() != null ) {
      result = getParentScope().getEnclosingClass();
    }

    return result;
  }

  public IPackage getEnclosingPackage() {
    IPackage result = null;
    if (getParentScope() != null) {
      result = getParentScope().getEnclosingPackage();
    }

    return result;
  }

  /**
   * returns a String representation of the definition. This string includes
   * the class of the defintion and its qualified name
   *
   * @return String
   */
  public String toString() {
    return getClass().getName() + "[" + getQualifiedName() + "]";
  }

  public int compareTo(Object o) {
    int result = 0;

    if (!(o instanceof Definition)) {
      throw new ClassCastException(o.getClass().getName());
    }
    else {
      result = getQualifiedName().compareTo(((Definition)o).getQualifiedName());
    }

    return result;
  }

}
