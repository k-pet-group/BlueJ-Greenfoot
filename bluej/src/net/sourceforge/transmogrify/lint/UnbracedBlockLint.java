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

// $Id: UnbracedBlockLint.java 1011 2001-11-22 10:36:26Z ajp $

package net.sourceforge.transmogrify.lint;

import net.sourceforge.transmogrify.symtab.parser.*;

public class UnbracedBlockLint extends Lint {

  public static String IF_WARNING = "Unbraced if";
  public static String DO_WARNING = "Unbraced do";
  public static String WHILE_WARNING = "Unbraced while";
  public static String ELSE_WARNING = "Unbraced else";
  public static String FOR_WARNING = "Unbraced for";

  public void handleNode(SymTabAST node) {
    switch(node.getType()) {

    case JavaTokenTypes.LITERAL_if:
      handleIf(node);
      break;

    case JavaTokenTypes.LITERAL_do:
      if (!doHasBraces(node)) {
        addWarning(new Warning(DO_WARNING, node));
      }
      break;

    case JavaTokenTypes.LITERAL_while:
      if (!whileHasBraces(node)) {
        addWarning(new Warning(WHILE_WARNING, node));
      }
      break;

    case JavaTokenTypes.LITERAL_for:
      if (!forHasBraces(node)) {
        addWarning(new Warning(FOR_WARNING, node));
      }
      break;
    }
  }

  private void handleIf(SymTabAST node) {
    if (!ifHasBraces(node)) {
      addWarning(new Warning(IF_WARNING, node));
    }

    SymTabAST elseNode
      = (SymTabAST)node.getFirstChild().getNextSibling().getNextSibling();
    if (elseNode != null && elseNode.getType() != JavaTokenTypes.LITERAL_if) {
      if (elseNode.getType() != JavaTokenTypes.SLIST) {
        addWarning(new Warning(ELSE_WARNING, node));
      }
    }
  }

  private boolean ifHasBraces(SymTabAST node) {
    return secondChildIsSList(node);
  }

  private boolean whileHasBraces(SymTabAST node) {
    return secondChildIsSList(node);
  }

  private boolean secondChildIsSList(SymTabAST node) {
    boolean result = false;

    SymTabAST statements = (SymTabAST)node.getFirstChild().getNextSibling();
    if (statements.getType() == JavaTokenTypes.SLIST) {
      result = true;
    }

    return result;
  }

  private boolean doHasBraces(SymTabAST node) {
    boolean result = false;

    SymTabAST statements = (SymTabAST)node.getFirstChild();
    if (statements.getType() == JavaTokenTypes.SLIST) {
      result = true;
    }

    return result;
  }

  private boolean forHasBraces(SymTabAST node) {
    boolean result = false;

    SymTabAST statements
      = (SymTabAST)node.getFirstChild().getNextSibling().getNextSibling().getNextSibling();

    if (statements.getType() == JavaTokenTypes.SLIST) {
      result = true;
    }

    return result;
  }
}