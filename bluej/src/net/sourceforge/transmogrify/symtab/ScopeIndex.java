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

import java.util.Hashtable;
import java.util.Vector;


// $Id: ScopeIndex.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>ScopeIndex</code> provides methods for finding <code>Scope</code>s
 * related to a known <code>Occurrence</code>
 */
public class ScopeIndex {

  //This is a Hashtable full of Vectors.  The keys to this hashtable are filenames.
  //Each vector contains all of the scope objects from the specific file.
  private Hashtable indexOfFiles = new Hashtable();

  public Hashtable getIndex() {
    return indexOfFiles;
  }

  /**
   * returns the most specific <code>Scope</code> to which the specified
   * <code>Occurence</code> belongs.
   *
   * @param occ the <code>Occurrence</code> whose <code>Scope</code> we're interested in.
   * @return Scope
   */
  public Scope lookup(Occurrence occ) {
    String key = occ.getFile().getAbsolutePath();
    Vector scopeList = getFileVector(key);

    Scope result = findScope(scopeList, occ);

    return result;
  }

  /**
   * returns the most specific <code>Scope</code> to which the specified
   * <code>Occurence</code> belongs from the specified <code>Vector</code>
   * of <code>Scope</code>s.
   *
   * @param occ the <code>Occurrence</code> whose <code>Scope</code> we're interested in.
   * @param scopeList the <code>Vector</code> of <code>Scope</code>s to chose from.
   * @return Scope
   */
  public Scope findScope(Vector scopeList, Occurrence occ) {
    int i = 0;

    Scope bestSoFar = (Scope)scopeList.elementAt(i);

    while (!bestSoFar.getTreeNode().getSpan().contains(occ.getLine(), occ.getColumn())) {
      i++;
      bestSoFar = (Scope)scopeList.elementAt(i);
    }

    for ( ; i < scopeList.size(); i++) {
      Scope currentScope = (Scope)scopeList.elementAt(i);

      if (currentScope.getTreeNode().getSpan().contains(occ.getLine(), occ.getColumn())) {
        if (bestSoFar.getTreeNode().getSpan().contains(currentScope.getTreeNode().getSpan())) {
          bestSoFar = currentScope;
        }
      }
    }

    return bestSoFar;
  }

  /**
   * adds a <code>Scope</code> to the <code>ScopeIndex</code> for searching.
   *
   * @param scope the <code>Scope</code> to add.
   */
  public void addScope(Scope scope) {
    Vector fileVector = getFileVector(scope.getTreeNode().getFile().getAbsolutePath());

    fileVector.addElement(scope);
  }

  /**
   * returns the <code>Vector</code> containing the <code>Scope</code>s related
   * to the specified filename.
   *
   * @param fileName the fileName to find scopes for.
   * @return Vector
   */
  private Vector getFileVector(String fileName) {
    Vector result = (Vector)indexOfFiles.get(fileName);

    if (result == null) {
      result = new Vector();
      indexOfFiles.put(fileName, result);
    }

    return result;
  }
}