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
package net.sourceforge.transmogrify.refactorer;

// $Id: InlineTemp.java 1011 2001-11-22 10:36:26Z ajp $


import antlr.collections.AST;

import java.io.*;

import java.util.*;

import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import net.sourceforge.transmogrify.symtab.printer.*;


public class InlineTemp extends Transmogrifier {
  public InlineTemp() {
    super();
  }

  public InlineTemp(ASTPrintManager printManager) {
    super(printManager);
  }

  /**
   * implements "inline temp" [Fowler, 119]
   *
   * @param hook the <code>Hook</code> in which to perform the operation
   */
  public void apply(Hook hook) throws Exception {
    // declare temp as final, compile
    // find references to temp and replace them with rhs of assignment


    try {
      Occurrence location = hook.makeOccurrence();
      refactor(location);
    }
    catch (RefactoringException e) {
      hook.displayException(e, e.getMessage());
    }

    // REDTAG -- we'll want to do these in (almost?) every refactoring
    // consider template algorithm (or something)
    streamFiles();
  }

  public void refactor(Occurrence location) throws Exception {
    IDefinition definition = query.getDefinition(location);
    Iterator references = definition.getReferences();

    SymTabAST definitionNode = ((Definition)definition).getTreeNode();
    SymTabAST assign = getAssignment(definitionNode, references);
    SymTabAST rhs = getRHS(assign);

    while (references.hasNext()) {
      SymTabAST curNode = ((Reference)references.next()).getTreeNode();
      replaceNode(curNode, rhs.deepClone());
    }

    if (assign.getParent().getType() == JavaTokenTypes.EXPR) {
      removeNode(assign.getParent());
    }
    else {
      removeNode(assign);
    }
    removeNode(definitionNode);

  }

  public boolean canApply(Hook hook) {
    boolean result = false;
    Occurrence location = null;

    try {
      location = hook.makeOccurrence();
      result = canRefactor(location);
    }
    catch (Exception ignoreMe) {}

    result &= !table.isOutOfDate();

    return result;
  }

  public boolean canRefactor(Occurrence location) {
    boolean result = false;

    /* All of the following must be true to refactor:
     *   1. There is a definition at the location
     *   2. The definition is a VariableDef
     *   3. The variable is assigned at definition
     *   4. The variable is not a class member
     *   5. The variable is never reassigned
     */

    IDefinition def = query.getDefinition(location);
    if (def != null && def instanceof VariableDef) {
      VariableDef var = (VariableDef)def;

      result =
        (!isClassMember(var))
        && (isAssignedAtDefinition(var))
        && (!isReassigned(var));
    }

    return result;
  }

  private boolean isClassMember(VariableDef var) {
    return (var.getParentScope() instanceof ClassDef);
  }

  private boolean isAssignedAtDefinition(VariableDef var) {
    AST assignmentNode =
      var.getTreeNode().getFirstChildOfType(JavaTokenTypes.ASSIGN);
    return (assignmentNode != null);
  }

  private boolean isReassigned(VariableDef var) {
    boolean isReassigned = false;

    Iterator references = var.getReferences();
    while (references.hasNext() && !isReassigned) {
      Reference ref = (Reference)references.next();
      isReassigned = nodeIsLhsOfAssignment(ref.getTreeNode());
    }

    return isReassigned;
  }

  private boolean nodeIsLhsOfAssignment(SymTabAST node) {
    boolean result = false;

    SymTabAST parent = node.getParent();
    if (parent.getType() == JavaTokenTypes.ASSIGN) {
      if (parent.getFirstChild() == node) {
        result = true;
      }
    }

    return result;
  }

}
