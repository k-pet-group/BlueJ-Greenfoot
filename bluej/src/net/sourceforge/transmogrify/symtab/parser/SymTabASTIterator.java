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
package net.sourceforge.transmogrify.symtab.parser;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator for the children of a tree node.
 *
 * @version 1.0
 * @since 1.0
 * @see Iterator
 */
public class SymTabASTIterator implements Iterator {
  private SymTabAST _current;

  /**
   * Creates a new <tt>SymTabASTIterator</tt>.
   *
   * @param parent the node whose children will be iterated over.
   */
  public SymTabASTIterator(SymTabAST parent) {
    _current = (SymTabAST)parent.getFirstChild();
  }

  /**
   * Whether the node has another child.  (In other words, returns
   * <tt>true</tt> if <tt>next</tt> would return an element rather than
   * throwing an exception.)
   *
   * @return the next child node.
   */
  public boolean hasNext() {
    return (_current != null);
  }

  /**
   * The next child node.
   *
   * @return the next child node.
   */
  public Object next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    Object result = _current;
    _current = (SymTabAST)_current.getNextSibling();

    return result;
  }

  /**
   * The next child node.
   *
   * @return the next child node.
   */
  public SymTabAST nextChild() {
    return (SymTabAST)next();
  }

  /**
   * Unsupported operation
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
