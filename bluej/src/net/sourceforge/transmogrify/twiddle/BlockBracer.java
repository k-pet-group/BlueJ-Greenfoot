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

// $Id: BlockBracer.java 1011 2001-11-22 10:36:26Z ajp $
package net.sourceforge.transmogrify.twiddle;

import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;

import net.sourceforge.transmogrify.symtab.parser.SymTabAST;
import net.sourceforge.transmogrify.symtab.parser.JavaTokenTypes;
import net.sourceforge.transmogrify.symtab.parser.TreeWalker;

import antlr.ASTFactory;

public class BlockBracer extends Transmogrifier {
  private static ASTFactory factory;

  static {
    factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());
  }

  public void apply(Hook hook) throws Exception {
    braceBlocks();
    streamFiles();
  }

  public boolean canApply(Hook hook) {
    return hook.getParsedFiles().hasNext();
  }

  public void braceBlocks() {
    // REDTAG
    // the easiest way to do this is to use the lint to find all
    // unbraced blocks, but this makes the twiddle package dependent
    // on the lint package, which is something we were trying to avoid
    // the result of this is that much functionality in UnbracedBlockLine
    // is duplicated here

    TreeWalker treeWalker = new BlockBracerTreeWalker(root);

    treeWalker.walkTree();
  }


  public void brace(SymTabAST node) {
    SymTabAST slist = (SymTabAST)factory.create(JavaTokenTypes.SLIST, "{");
    SymTabAST rcurly = (SymTabAST)factory.create(JavaTokenTypes.RCURLY, "}");

    slist.setFirstChild(node);
    SymTabAST next = (SymTabAST)node.getNextSibling();
    node.setNextSibling(rcurly);
    slist.setNextSibling(next);

    if (node.getPreviousSibling() != null) {
      node.getPreviousSibling().setNextSibling(slist);
    }
    else {
      node.getParent().setFirstChild(slist);
    }

    addDirtyFile(node);
  }

  class BlockBracerTreeWalker extends TreeWalker {
    public BlockBracerTreeWalker(SymTabAST tree) {
      super(tree);
    }

    public void walkNode(SymTabAST node) {
      switch(node.getType()) {

      case JavaTokenTypes.LITERAL_if:
        handleIf(node);
        break;

      case JavaTokenTypes.LITERAL_do:
        if (!doHasBraces(node)) {
          brace((SymTabAST)node.getFirstChild());
        }
        break;

      case JavaTokenTypes.LITERAL_while:
        if (!whileHasBraces(node)) {
          brace((SymTabAST)node.getFirstChild().getNextSibling());
        }
        break;

      case JavaTokenTypes.LITERAL_for:
        if (!forHasBraces(node)) {
          brace((SymTabAST)node.getFirstChild().getNextSibling().getNextSibling().getNextSibling());
        }
        break;
      }

      super.walkNode(node);
    }

    private void handleIf(SymTabAST node) {
      if (!ifHasBraces(node)) {
        brace((SymTabAST)node.getFirstChild().getNextSibling());
      }

      SymTabAST elseNode
        = (SymTabAST)node.getFirstChild().getNextSibling().getNextSibling();
      if (elseNode != null && elseNode.getType() != JavaTokenTypes.LITERAL_if) {
        if (elseNode.getType() != JavaTokenTypes.SLIST) {
          brace(elseNode);
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
}