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

import java.util.*;

/**
 * An iterator for dot ('.') delimited tokens.
 *
 * @version 1.0
 * @since 1.0
 * @see Iterator
 */
public class DotIterator implements Iterator {
  Iterator _nodesIter;
  List _nodes;

  public DotIterator(SymTabAST topNode) {
    _nodes = new ArrayList();
    makeNodes(topNode);
    _nodesIter = _nodes.iterator();
  }

  private void makeNodes(SymTabAST node) {
    if (node.getType() == JavaTokenTypes.DOT) {
      SymTabAST left  = (SymTabAST)node.getFirstChild();
      SymTabAST right = (SymTabAST)left.getNextSibling();

      makeNodes(left);
      makeNodes(right);
    }
    else {
      _nodes.add(node);
    }
  }

  /**
   * Returns true if the iteration has more elements. (In other words,
   * returns true if next would return an element rather than throwing an
   * exception.)
   *
   * @return <tt>true</tt> if the iterator has more elements.
   */
  public boolean hasNext() {
    return _nodesIter.hasNext();
  }

  /**
   * Returns the next portion of the dotted name.
   *
   * @return the next node in the dotted name.
   */
  public Object next() {
    return _nodesIter.next();
  }

  /**
   * Returns the next portion of the dotted name.
   *
   * @return the next node in the dotted name.
   */
  public SymTabAST nextNode() {
    return (SymTabAST)_nodesIter.next();
  }

  /**
   * Unsupported operation.
   *
   */
  public void remove()  {
    throw new UnsupportedOperationException();
  }
}
