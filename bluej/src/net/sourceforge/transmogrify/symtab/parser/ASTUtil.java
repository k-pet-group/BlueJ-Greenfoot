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

import net.sourceforge.transmogrify.symtab.*;
import java.io.*;

// $Id: ASTUtil.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>ASTUtil</code> is a <code>Utility Class</code> that contains utility code
 * for managing our SymTabAST.
 *
 * @see Definition
 * @see TypedDef
 */

public class ASTUtil {
  /**
   * replaces a node in the AST
   * // REDTAG -- what happens when the new node has siblings???
   *
   * @param node the node to replace
   * @param r the node to replace it with
   */
  public static void replaceNode(SymTabAST replacedNode, SymTabAST newNode) {
    SymTabAST replace = newNode.deepClone();

    SymTabAST prevSibling = replacedNode.getPreviousSibling();
    SymTabAST parent = replacedNode.getParent();
    SymTabAST nextSibling = (SymTabAST)replacedNode.getNextSibling();

    replace.setPreviousSibling(prevSibling);
    if (prevSibling != null) {
      prevSibling.setNextSibling(replace);
    }
    else {
      parent.setFirstChild(replace);
    }
    replace.setParent(parent);

    replace.setNextSibling(nextSibling);
    if (nextSibling != null) {
      nextSibling.setPreviousSibling(replace);
    }
  }


  /**
   * builds and returns the first <code>occurence</code> in the AST that has
   * a valid line number.
   *
   * @see Occurrence
   *
   * @return Occurrence
   * @param tree The SymTabAST to be searched for a valid node for which to return an
   * occcurence
   * @param symboloTable contains the info for the occurrence being inspected
   */
  public static Occurrence getOccurrence(SymTabAST tree, SymbolTable symbolTable) {
    return new Occurrence(symbolTable.getCurrentFile(), getLine(tree), getColumn(tree));
  }

  /**
   * gets a line number for the tree;  if the current SymTabAST node does not have one associated
   * with it, traverse its children until a line number is found.  Failure results in line
   * number value of 0.
   *
   * @param tree the SymTabAST to process
   *
   * @return int the resulting line number (0 if none is found)
   */
  public static int getLine(SymTabAST tree) {
    SymTabAST indexedNode = (SymTabAST)tree;

    // find a node that actually has line number info
    if (indexedNode.getLine() == 0) {
      indexedNode = (SymTabAST)indexedNode.getFirstChild();

      while(indexedNode != null && indexedNode.getLine() == 0) {
        indexedNode = (SymTabAST)indexedNode.getNextSibling();
      }

      if (indexedNode == null) {
        // we're screwed
        indexedNode = (SymTabAST)tree;
      }
    }

    return indexedNode.getLine();
  }

  /**
   * gets a column number for the tree;  if the current SymTabAST node does not have one associated
   * with it, traverse its children until a column number is found.  Failure results in column
   * number value of 0.
   *
   * @param tree the SymTabAST to process
   *
   * @return int the resulting line number (0 if none is found)
   */
  public static int getColumn(SymTabAST tree) {
    SymTabAST indexedNode = (SymTabAST)tree;

    // find a node that actually has line number info
    // REDTAG -- a label's ':' is a real token and has (the wrong) column info
    // because it is the parent of the ident node that people will want
    if (indexedNode.getColumn() == 0 || indexedNode.getType() == JavaTokenTypes.LABELED_STAT) {
      indexedNode = (SymTabAST)indexedNode.getFirstChild();

      while(indexedNode != null && indexedNode.getColumn() == 0) {
        indexedNode = (SymTabAST)indexedNode.getNextSibling();
      }

      if (indexedNode == null) {
        // we're screwed
        indexedNode = (SymTabAST)tree;
      }
    }

    return indexedNode.getColumn();
  }

  /**
   * Builds a ClassDef based on the the Node of tpye JavaTokenType.TYPE in the
   * SymTabAST and returns this.
   *
   * @see JavaTokenType
   *
   * @return ClassDef
   * @param tree the SymTabAST contaning all Definitions
   * @param symboloTable contains the info for the defintion being returned
   */
  private static ClassDef getType(SymTabAST tree, SymbolTable symbolTable) {
    // is this referenced correctly?

    ClassDef result = null;

    SymTabAST typeNode = tree.getFirstChildOfType(JavaTokenTypes.TYPE);

    if (typeNode != null) {
      //result = new DummyClass((SymTabAST)tree,
      //                        ASTUtil.constructDottedName(typeNode.getFirstChild()),
      //                        ASTUtil.getOccurrence(typeNode, symbolTable));
    }

    return result;
  }

  /**
   * Builds the dotted name String representation of the object contained within
   * the SymTabAST.
   *
   * @return String
   * @param tree the SymTabAST contaning the entire hierarcy of the object
   */
  public static String constructDottedName(SymTabAST tree) {
    String result;

    if (tree.getType() == JavaTokenTypes.DOT) {
      SymTabAST left  = (SymTabAST)tree.getFirstChild();
      SymTabAST right = (SymTabAST)left.getNextSibling();

      result = constructDottedName(left) + "." + constructDottedName(right);
    }
    else if (tree.getType() == JavaTokenTypes.ARRAY_DECLARATOR) {
      StringBuffer buf = new StringBuffer();
      SymTabAST left  = (SymTabAST)tree.getFirstChild();
      SymTabAST right = (SymTabAST)left.getNextSibling();

      buf.append(constructDottedName(left));

      if (right != null) {
        buf.append(".");
        buf.append(constructDottedName(right));
      }

      buf.append(" []");

      result = buf.toString();
    }
    else if (tree.getType() == JavaTokenTypes.METHOD_CALL) {
      result = constructDottedName((SymTabAST)tree.getFirstChild()) + "()";
    }
    else {
      result = tree.getText();
    }

    return result;
  }

  /**
   * Returns the Package name in the hierarchy represented by the SymTabAST.
   *
   * @return String
   * @param tree the SymTabAST contaning the entire hierarcy of the object
   */
  public static String constructPackage(SymTabAST tree) {
    String fullName = constructDottedName(tree);

    return fullName.substring(0,fullName.lastIndexOf("."));
  }

   /**
   * Returns the top Class name in the hierarchy represented by the SymTabAST.
   *
   * @return String
   * @param tree the SymTabAST contaning the entire hierarcy of the object
   */
  public static String constructClass(SymTabAST tree) {
    String fullName = constructDottedName(tree);

    return fullName.substring(fullName.lastIndexOf(".")+1, fullName.length());
  }

  public static boolean treesBelowFilesAreEqual(SymTabAST firstRoot,
                                                File[] firstFiles,
                                                SymTabAST secondRoot,
                                                File[] secondFiles) {
    boolean result = true;

    if ( firstFiles.length == secondFiles.length ) {
      for ( int i = 0; i < firstFiles.length; i++ ) {
        SymTabAST firstTree =
          (SymTabAST)getFileNode(firstRoot, firstFiles[i]).getFirstChild();
        SymTabAST secondTree =
          (SymTabAST)getFileNode(secondRoot, secondFiles[i]).getFirstChild();

        if ( !firstTree.equalsList(secondTree) ) {
          result = false;
          break;
        }
      }
    }
    else {
      result = false;
    }

    return result;
  }

  public static SymTabAST getFileNode(SymTabAST root, File file) {
    SymTabAST result = null;

    SymTabAST fileNode = (SymTabAST)root.getFirstChild();
    while ( fileNode != null && result == null ) {
      if ( file.equals(fileNode.getFile()) ) {
        result = fileNode;
      }
      fileNode = (SymTabAST)fileNode.getNextSibling();
    }

    return result;
  }

  public static SymTabAST spliceFiles(SymTabAST newTree, SymTabAST oldTree) {
    SymTabAST current = (SymTabAST)newTree.getFirstChild();
    SymTabAST next = null;

    while (current != null) {
      next = (SymTabAST)current.getNextSibling();

      SymTabAST nodeToReplace
        = ASTUtil.getFileNode(oldTree, current.getFile());

      if (nodeToReplace == null) {
        oldTree.addFirstChild(current);
      }
      else {
        System.out.println("replacing " + nodeToReplace + " with " + current);
        replaceNode(nodeToReplace, current);
      }

      current = next;
    }

    return oldTree;
  }

  public static SymTabAST getFirstImport(SymTabAST fileNode) {
    SymTabAST result = null;

    SymTabAST child = (SymTabAST)fileNode.getFirstChild();
    while (child != null && result == null) {
      if (child.getType() == JavaTokenTypes.IMPORT) {
        result = child;
      }
      child = (SymTabAST)child.getNextSibling();
    }

    return result;
  }
}
