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

// $Id: TryCatchEncloser.java 1011 2001-11-22 10:36:26Z ajp $
package net.sourceforge.transmogrify.twiddle;

import net.sourceforge.transmogrify.symtab.parser.TreeWalker;
import net.sourceforge.transmogrify.symtab.parser.JavaTokenTypes;
import net.sourceforge.transmogrify.symtab.parser.JavaRecognizer;
import net.sourceforge.transmogrify.symtab.parser.SymTabAST;

import net.sourceforge.transmogrify.symtab.Occurrence;
import net.sourceforge.transmogrify.symtab.IMethod;
import net.sourceforge.transmogrify.symtab.IDefinition;
import net.sourceforge.transmogrify.symtab.IClass;
import net.sourceforge.transmogrify.symtab.Typed;

import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;
import net.sourceforge.transmogrify.hook.StatementSpan;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Comparator;

public class TryCatchEncloser extends Transmogrifier {

  public void apply(Hook hook) throws Exception {
    StatementSpan span = new StatementSpan(hook);

    try {
      enclose(span.getStart(), span.getEnd());
    }
    catch (NoExceptionsThrownException e) {
      hook.displayMessage("No Exceptions Thrown", "No exceptions thrown in selected code.");
    }

    streamFiles();
  }

  public void enclose(Occurrence startLocation,
                      Occurrence endLocation) throws NoExceptionsThrownException {
    // REDTAG -- copied from ExtractMethod
    SymTabAST file = getFileNode(startLocation.getFile());

    SymTabAST startNode = file.getEnclosingNode(startLocation.getLine(),
                                                startLocation.getColumn());
    SymTabAST startExpr = findParentExpr(startNode);

    SymTabAST endNode = file.getEnclosingNode(endLocation.getLine(),
                                              endLocation.getColumn());
    SymTabAST endExpr = findParentExpr(endNode, startExpr);

    SymTabAST current = startExpr;
    SortedSet exceptions = new TreeSet(new ExceptionComparator());

    // for each expression between start and end
    while (current != endExpr.getNextSibling()) {
      // find all exceptions thrown
      exceptions.addAll(findExceptionsThrown(current));
      current = (SymTabAST)current.getNextSibling();
    }

    if (exceptions.size() > 0) {
      SymTabAST tryBlock = (SymTabAST)factory.create(JavaTokenTypes.LITERAL_try, "try");

      SymTabAST trySlist = (SymTabAST)factory.create(JavaTokenTypes.SLIST, "{");
      trySlist.setFirstChild(startExpr);
      tryBlock.setFirstChild(trySlist);

      SymTabAST rcurly = (SymTabAST)factory.create(JavaTokenTypes.RCURLY, "}");

      SymTabAST finalExpr = (SymTabAST)endExpr.getNextSibling();
      endExpr.setNextSibling(rcurly);

      SymTabAST previousBlock = trySlist;
      SymTabAST exception = null;
      SymTabAST catchBlock = null;
      SymTabAST parameterDef = null;
      SymTabAST modifiers = null;
      SymTabAST type = null;
      SymTabAST ident = null;
      SymTabAST slist = null;

      int suffix = 1;

      for (Iterator it = exceptions.iterator(); it.hasNext(); ) {
        exception = makeDotNode(((IDefinition)it.next()).getQualifiedName());
        catchBlock = (SymTabAST)factory.create(JavaTokenTypes.LITERAL_catch, "catch");

        parameterDef = (SymTabAST)factory.create(JavaTokenTypes.PARAMETER_DEF, JavaRecognizer._tokenNames[JavaTokenTypes.PARAMETER_DEF]);
        modifiers = (SymTabAST)factory.create(JavaTokenTypes.MODIFIERS, JavaRecognizer._tokenNames[JavaTokenTypes.MODIFIERS]);
        type = (SymTabAST)factory.create(JavaTokenTypes.TYPE, JavaRecognizer._tokenNames[JavaTokenTypes.TYPE]);
        ident = (SymTabAST)factory.create(JavaTokenTypes.IDENT, "e" + suffix);
        slist = (SymTabAST)factory.create(JavaTokenTypes.SLIST, "{");

        catchBlock.setFirstChild(parameterDef);
        parameterDef.setFirstChild(modifiers);
        parameterDef.setNextSibling(slist);
        slist.setFirstChild(rcurly);
        modifiers.setNextSibling(type);
        type.setFirstChild(exception);
        type.setNextSibling(ident);

        previousBlock.setNextSibling(catchBlock);
        catchBlock.setParent(tryBlock);
        previousBlock = catchBlock;
        suffix++;
      }

      tryBlock.setNextSibling(finalExpr);
      tryBlock.setParent(finalExpr.getParent());
      startExpr.getPreviousSibling().setNextSibling(tryBlock);
    }
    else {
      throw new NoExceptionsThrownException();
    }
  }

  public boolean canApply(Hook hook) {
    return hook.getParsedFiles().hasNext();
  }

  private SortedSet findExceptionsThrown(SymTabAST expr) {
    ExceptionWalker walker = new ExceptionWalker(expr);
    walker.walkTree();
    return walker.getExceptions();
  }

  class ExceptionWalker extends TreeWalker {
    SortedSet exceptions;

    public ExceptionWalker(SymTabAST root) {
      super(root);
      exceptions = new TreeSet();
    }

    public void walkNode(SymTabAST node) {
      switch(node.getType()) {
        case JavaTokenTypes.METHOD_CALL:
          IMethod def = (IMethod)findIdent(node).getDefinition();
          IClass[] thrownExceptions = def.getExceptions();
          for (int i = 0; i < thrownExceptions.length; i++) {
            exceptions.add(thrownExceptions[i]);
          }
          break;

        case JavaTokenTypes.LITERAL_throw:
          SymTabAST child = (SymTabAST)node.getFirstChild();
          exceptions.add(((Typed)findIdent(child).getDefinition()).getType());
          break;
      }

      super.walkChildren(node);
    }

    public SortedSet getExceptions() {
      return exceptions;
    }

    private SymTabAST findIdent(SymTabAST node) {
      IdentFinder finder = new IdentFinder(node);
      finder.walkTree();
      return finder.getResult();
    }
  }

  class IdentFinder extends TreeWalker {
    SymTabAST result;

    public IdentFinder(SymTabAST root) {
      super(root);
    }

    public void walkNode(SymTabAST node) {
      if (node.getType() == JavaTokenTypes.IDENT) {
        result = node;
      }
      else {
        super.walkChildren(node);
      }
    }

    public SymTabAST getResult() {
      return result;
    }
  }

  class ExceptionComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      int result = 0;

      IClass class1 = (IClass)o1;
      IClass class2 = (IClass)o2;

      if (class1.isCompatibleWith(class2)) {
        result = -1;
      }
      else if (class2.isCompatibleWith(class1)) {
        result = 1;
      }
      else {
        result = 0;
      }

      return result;
    }
  }
}