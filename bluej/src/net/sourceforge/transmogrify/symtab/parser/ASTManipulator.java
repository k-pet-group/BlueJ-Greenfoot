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

// $Id: ASTManipulator.java 1011 2001-11-22 10:36:26Z ajp $
package net.sourceforge.transmogrify.symtab.parser;

import net.sourceforge.transmogrify.symtab.SymbolTable;
import net.sourceforge.transmogrify.symtab.QueryEngine;
import net.sourceforge.transmogrify.symtab.Reference;
import net.sourceforge.transmogrify.symtab.Occurrence;
import net.sourceforge.transmogrify.symtab.IClass;

import net.sourceforge.transmogrify.symtab.printer.FilePrintManager;
import net.sourceforge.transmogrify.symtab.printer.ASTPrintManager;
import net.sourceforge.transmogrify.symtab.printer.ASTStreamer;

import antlr.ASTFactory;
import antlr.collections.AST;

import java.io.File;
import java.io.IOException;

import java.util.Iterator;

public class ASTManipulator {
  protected SymTabAST root;
  protected SymbolTable table;
  protected QueryEngine query;
  protected ASTFactory factory;

  protected ASTPrintManager _manager;


  public ASTManipulator() {
    this(new FilePrintManager());
  }

  public ASTManipulator(ASTPrintManager manager) {
    _manager = manager;
  }

  /**
   * gets clone of root node
   * @return root node
   */
  public SymTabAST getTree() {
    return root.deepClone();
  }

  public void setup(SymbolTable table) {
    this.table = table;

    root = table.getTree();
    query = new QueryEngine(table);

    factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());
  }

  /**
   * replaces the text of the AST node of a reference
   *
   * @param ref the <code>Reference</code> to rename
   * @param newText the text to use
   */
  protected void renameReference(Reference ref, String newText) {
    addDirtyFile(ref.getTreeNode());
    ref.getTreeNode().setText(newText);
  }

  /**
   * removes a node from the AST
   *
   * @param node the node to remove
   */
  protected void removeNode(SymTabAST node) {
    addDirtyFile(node);
    SymTabAST prevSibling = node.getPreviousSibling();

    if (prevSibling != null) {
      prevSibling.setNextSibling(node.getNextSibling());
    }
    else {
      SymTabAST parent = node.getParent();
      parent.setFirstChild(node.getNextSibling());
    }

    node.setParent(null);
    node.setNextSibling(null);
    node.setPreviousSibling(null);
  }

  /**
   * replaces a node in the AST
   * // REDTAG -- what happens when the new node has siblings???
   *
   * @param node the node to replace
   * @param r the node to replace it with
   */
  protected void replaceNode(SymTabAST node, SymTabAST replace) {
    addDirtyFile(node);
    SymTabAST prevSibling = node.getPreviousSibling();
    SymTabAST parent = node.getParent();
    SymTabAST nextSibling = (SymTabAST)node.getNextSibling();

    if (prevSibling != null) {
      prevSibling.setNextSibling(replace);
      replace.setPreviousSibling(prevSibling);
    }
    else {
      parent.setFirstChild(replace);
    }

    replace.setParent(parent);
    if (nextSibling != null) {
      replace.setNextSibling(nextSibling);
      nextSibling.setPreviousSibling(replace);
    }
  }

  protected void clipAndReplaceNodes(SymTabAST startNode, SymTabAST nextSibling, SymTabAST replace) {
    addDirtyFile(startNode);
    replaceNode(startNode, replace);
    replace.setNextSibling(nextSibling);
  }

  protected void addMethod(SymTabAST methodDef, SymTabAST antecedentDefinition) {
    addDirtyFile(antecedentDefinition);
    SymTabAST parent = antecedentDefinition;

    while (parent.getType() != JavaTokenTypes.OBJBLOCK) {
      parent = parent.getParent();
    }

    removeNode(parent.getFirstChildOfType(JavaTokenTypes.RCURLY));
    addChild(parent, methodDef);
    addChild(parent, (SymTabAST)factory.create(JavaTokenTypes.RCURLY, "}"));
  }

  /**
   * inserts a node at the end the child list of a node
   *
   */
  protected void addChild(SymTabAST parent, SymTabAST newRightSibling) {
    addDirtyFile(parent);
    AST sibling = parent.getFirstChild();

    if (sibling == null) {
      parent.setFirstChild(newRightSibling);
    }
    else {
      while(sibling.getNextSibling() != null) {
        sibling = sibling.getNextSibling();
      }
      sibling.setNextSibling(newRightSibling);
      newRightSibling.setPreviousSibling((SymTabAST)sibling);
    }

    newRightSibling.setParent(parent);
  }

  protected void addFirstChild(SymTabAST parent, SymTabAST child) {
    addDirtyFile(parent);
    SymTabAST previousFirstChild = (SymTabAST)parent.getFirstChild();

    parent.setFirstChild(child);
    child.setNextSibling(previousFirstChild);
    child.setParent(parent);
    child.setPreviousSibling(null);

    if (previousFirstChild != null) {
      previousFirstChild.setPreviousSibling(child);
    }
  }
  /**
   * gets file node based on input <code>File</code>
   * @param file
   * @return node representation for the <code>file</code>
   */
  protected SymTabAST getFileNode(File file) {
    SymTabAST result = null;

    if (file != null) {
      result = ASTUtil.getFileNode(root, file);
    }

    return result;
  }

  /**
   * finds the *first* assignment to a definition -- there should never
   * be more than one!
   *
   * @param defNode the definition to find the assignment of
   */
  protected SymTabAST getAssignment(SymTabAST defNode, Iterator references) {
    // if the assignment is part of the definition
    SymTabAST result = defNode.getFirstChildOfType(JavaTokenTypes.ASSIGN);

    if (result == null) {
      while (references.hasNext()) {
        SymTabAST ident = ((Reference)references.next()).getTreeNode();
        if (ident.getParent().getType() == JavaTokenTypes.ASSIGN) {
          result = ident.getParent();
          break;
        }
      }
    }

    return result;
  }

  /**
   * gets the right-hand side of an expression.
   * some expressions have only a left-hand side (int var = 4;)
   * in these cases the lhs is used as the rhs
   *
   * @param sxpr the expression to process
   *
   * @return the right-hand side of the expression
   */
  protected SymTabAST getRHS(SymTabAST expr) {
    SymTabAST result;

    result = (SymTabAST)getLHS(expr).getNextSibling();

    if (result == null) {
      result = getLHS(expr);
    }

    // I don't think we ever want the EXPR node -- just the contents of it
    if ( result.getType() == JavaTokenTypes.EXPR ) {
      result = (SymTabAST)result.getFirstChild();
    }

    return result;
  }

  /**
   * gets the left hand side of the equation
   *
   * @param expr the expression to process
   *
   * @return the left hand side of the expression
   */
  protected SymTabAST getLHS(SymTabAST expr) {
    return (SymTabAST)expr.getFirstChild();
  }

  private SymTabAST getFileNodeForOccurrence(Occurrence occ) {
    // not sure if this is the job of the Refactorer, or of the SymbolTable.

    SymTabAST result = null;

    SymTabAST root = getTree();
    SymTabAST file = (SymTabAST)root.getFirstChild();
    while ( (file != null) && (result == null) ) {
      if ( file.getFile().equals(occ.getFile()) ) {
        result = file;
      }
      file = (SymTabAST)file.getNextSibling();
    }

    return result;
  }

  protected SymTabAST makeReturnStatement(SymTabAST expressionReturned) {
    ASTFactory factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());

    SymTabAST returnStmt =
      (SymTabAST)factory.create(JavaTokenTypes.LITERAL_return, "return");

    if (expressionReturned.getType() != JavaTokenTypes.EXPR) {
      SymTabAST expr = makeTextlessNode(JavaTokenTypes.EXPR);
      expr.setFirstChild(expressionReturned);
      returnStmt.setFirstChild(expr);
    }
    else {
      returnStmt.setFirstChild(expressionReturned);
    }

    return returnStmt;
  }

  protected SymTabAST makeTextlessNode(int nodeType) {
    ASTFactory factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());

    return (SymTabAST)factory.create(nodeType,
                                     JavaRecognizer._tokenNames[nodeType]);
  }

  // REDTAG -- doesn't dirty the file
  protected void addImportToFile(File file, IClass importedClass) {
    SymTabAST fileNode = ASTUtil.getFileNode(getTree(), file);
    SymTabAST node = (SymTabAST)fileNode.getFirstChild();
    if (node.getType() == JavaTokenTypes.PACKAGE_DEF) {
      node = (SymTabAST)node.getNextSibling();
    }

    SymTabAST importNode = makeImportClassNode(importedClass);
    node.prepend(importNode);
  }

  protected SymTabAST makeImportClassNode(IClass importedClass) {
    ASTFactory factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());

    SymTabAST result = (SymTabAST)factory.create(JavaTokenTypes.IMPORT,
                                                 "import");
    result.setFirstChild(makeQualifiedClassNameNode(importedClass));

    return result;
  }

  protected SymTabAST makeQualifiedClassNameNode(IClass theClass) {
    return makeDotNode(theClass.getQualifiedName());
  }

  protected SymTabAST makeDotNode(String dottedName) {
    SymTabAST result = null;

    ASTFactory factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());

    int lastDot = dottedName.lastIndexOf(".");
    if (lastDot > 0) {
      result = (SymTabAST)factory.create(JavaTokenTypes.DOT, ".");
      SymTabAST leftNode = makeDotNode(dottedName.substring(0, lastDot));
      result.setFirstChild(leftNode);
      leftNode.setNextSibling(makeDotNode(dottedName.substring(lastDot + 1)));
    }
    else {
      result = (SymTabAST)factory.create(JavaTokenTypes.IDENT, dottedName);
    }

    return result;
  }

  /**
   * marks this file node for this node dirty
   * @param node the modified node
   * @return <code>void</code>
   */
  public void addDirtyFile(SymTabAST node) {
    SymTabAST fileNode = getFileNode(node.getFile());
    // the only time fileNode will be null is when it is a
    // constructed node
    if (fileNode != null) {
      fileNode.dirty();
    }
  }

  protected SymTabAST findParentExpr(SymTabAST candidate) {
    SymTabAST result = null;
    switch(candidate.getType()) {
      case JavaTokenTypes.EXPR:
      case JavaTokenTypes.LITERAL_while:
      case JavaTokenTypes.LITERAL_for:
      case JavaTokenTypes.LITERAL_do:
      case JavaTokenTypes.LITERAL_if:
      case JavaTokenTypes.LITERAL_else:
      case JavaTokenTypes.LITERAL_switch:
      case JavaTokenTypes.LITERAL_try:
      case JavaTokenTypes.LITERAL_catch:
      case JavaTokenTypes.LITERAL_finally:
      case JavaTokenTypes.LITERAL_throw:
      case JavaTokenTypes.LITERAL_return:
      case JavaTokenTypes.SLIST:
      case JavaTokenTypes.VARIABLE_DEF:
        result = candidate;
        break;

      default:
        result = findParentExpr(candidate.getParent());
    }

    return result;
  }

  protected SymTabAST findParentExpr(SymTabAST candidate, SymTabAST sibling) {
    SymTabAST result = findParentExpr(candidate);
    SymTabAST current = sibling;
    boolean found = false;

    while (current != null && !found) {
      if (current == result) {
        found = true;
      }
      current = (SymTabAST)current.getNextSibling();
    }

    if (!found) {
      result = findParentExpr(result.getParent(), sibling);
    }

    return result;
  }

  protected void streamFiles() throws IOException {
    new ASTStreamer(_manager).streamFiles(root);
  }
}