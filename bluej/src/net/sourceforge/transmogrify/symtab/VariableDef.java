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

import antlr.*;
import antlr.collections.*;

import net.sourceforge.transmogrify.symtab.parser.*;

// $Id: VariableDef.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>VariableDef</code> is a <code>Definition</code> that contains
 * information about the definition of a variable.
 *
 * @see Definition
 * @see TypedDef
 */
public class VariableDef extends Definition implements IVariable {

  public static final int PRIVATE_VISIBILITY = 0;
  public static final int PROTECTED_VISIBILITY = 1;
  public static final int PUBLIC_VISIBILITY = 2;
  public static final int DEFAULT_VISIBILITY = 3;

  private IClass _type = null;

  public VariableDef(String name, Scope parentScope, SymTabAST node) {
    super(name, parentScope, node);
  }

  /**
   * Returns the <code>Type</code> of the variable.
   *
   * @see TypedDef
   *
   * @return the <code>Type</code> of the variable
   */
  public IClass getType() {
    return _type;
  }

  /**
   * Sets the type of the variable.
   *
   * @see TypedDef
   *
   * @param def the <code>Type</code> object that represents the type of the
   *            variable.
   */
  public void setType(IClass type) {
    _type = type;
  }

  public int getVisibility() {
    int result = DEFAULT_VISIBILITY;

    SymTabAST visibilityNode = getVisibilityNode();
    if (visibilityNode != null) {
      if (visibilityNode.getType() == JavaTokenTypes.LITERAL_private) {
        result = PRIVATE_VISIBILITY;
      }
      else if (visibilityNode.getType() == JavaTokenTypes.LITERAL_protected) {
        result = PROTECTED_VISIBILITY;
      }
      else if (visibilityNode.getType() == JavaTokenTypes.LITERAL_public) {
        result = PUBLIC_VISIBILITY;
      }
    }

    return result;
  }

  private SymTabAST getVisibilityNode() {
    SymTabAST result = null;

    SymTabAST modifiersNode =
      getTreeNode().getFirstChildOfType(JavaTokenTypes.MODIFIERS);
    SymTabAST modifier = (SymTabAST)modifiersNode.getFirstChild();
    while (modifier != null) {
      if (isVisibilityNode(modifier)) {
        result = modifier;
        break;
      }
      modifier = (SymTabAST)modifier.getNextSibling();
    }

    return result;
  }

  private boolean isVisibilityNode(SymTabAST node) {
    return (node.getType() == JavaTokenTypes.LITERAL_public ||
            node.getType() == JavaTokenTypes.LITERAL_protected ||
            node.getType() == JavaTokenTypes.LITERAL_private);
  }

  public void setVisibility(int visibility) {
    ASTFactory factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());

    SymTabAST newVisibilityNode = null;
    if (visibility == PRIVATE_VISIBILITY) {
      newVisibilityNode =
        (SymTabAST)factory.create(JavaTokenTypes.LITERAL_private,
                                  "private");
    }
    else if (visibility == PROTECTED_VISIBILITY) {
      newVisibilityNode =
        (SymTabAST)factory.create(JavaTokenTypes.LITERAL_protected,
                                  "protected");
    }
    else if (visibility == PUBLIC_VISIBILITY) {
      newVisibilityNode =
        (SymTabAST)factory.create(JavaTokenTypes.LITERAL_public,
                                  "public");
    }

    SymTabAST visibilityNode = getVisibilityNode();
    SymTabAST modifiersNode =
      getTreeNode().getFirstChildOfType(JavaTokenTypes.MODIFIERS);

    newVisibilityNode.setParent(modifiersNode);
    if (visibilityNode != null) {
      modifiersNode.replaceChild(visibilityNode, newVisibilityNode);
    }
    else {
      modifiersNode.addFirstChild(newVisibilityNode);
    }
  }

  public boolean isAssignedAtDeclaration() {
    boolean result = false;

    if (getTreeNode().getFirstChildOfType(JavaTokenTypes.ASSIGN) != null) {
      result = true;
    }

    return result;
  }

}

