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

// $Id: TrivialExtractMethod.java 1011 2001-11-22 10:36:26Z ajp $

import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

import java.util.*;

import antlr.*;

public class TrivialExtractMethod extends Transmogrifier {
  public TrivialExtractMethod() {
    super();
  }

  public void apply(Hook hook) throws Exception {
    try {
      refactor(hook.makeOccurrence());
    }
    catch (RefactoringException e) {
      hook.displayException(e, e.getMessage());
    }
  }

  public boolean canApply(Hook hook) {
    return !table.isOutOfDate();
  }

  public void refactor(Occurrence location) {
    IDefinition def = query.getDefinition(location);
    Iterator references = def.getReferences();


    SymTabAST definition = ((Definition)def).getTreeNode();
    SymTabAST rhs = getRHS(getAssignment(definition, references));
    SymTabAST type = definition.getFirstChildOfType(JavaTokenTypes.TYPE);
    SymTabAST ident = definition.getFirstChildOfType(JavaTokenTypes.IDENT);

    SymTabAST methodDef =
      (SymTabAST)factory.create(JavaTokenTypes.METHOD_DEF,
                                JavaRecognizer._tokenNames[JavaTokenTypes.METHOD_DEF]);

    SymTabAST modifiers =
      (SymTabAST)factory.create(JavaTokenTypes.MODIFIERS,
                                JavaRecognizer._tokenNames[JavaTokenTypes.MODIFIERS]);

    SymTabAST prv = (SymTabAST)factory.create(JavaTokenTypes.LITERAL_private, "private");

    SymTabAST returnType = type.deepClone();
    SymTabAST methodName = ident.deepClone();

    SymTabAST parameters = makeTextlessNode(JavaTokenTypes.PARAMETERS);
    SymTabAST slist = (SymTabAST)factory.create(JavaTokenTypes.SLIST, "{");

    SymTabAST expr = rhs.deepClone();
    SymTabAST returnStmt = makeReturnStatement(expr);

    methodDef.setFirstChild(modifiers);
    modifiers.setFirstChild(prv);
    modifiers.setNextSibling(returnType);
    returnType.setNextSibling(methodName);
    methodName.setNextSibling(parameters);
    parameters.setNextSibling(slist);
    slist.setFirstChild(returnStmt);
    addChild(slist, (SymTabAST)factory.create(JavaTokenTypes.RCURLY, "}"));

    addMethod(methodDef, definition);

    SymTabAST methodCall =
      (SymTabAST)factory.create(JavaTokenTypes.METHOD_CALL, "(");
    SymTabAST callName = ident.deepClone();
    SymTabAST callParams = makeTextlessNode(JavaTokenTypes.ELIST);

    methodCall.setFirstChild(callName);
    callName.setNextSibling(callParams);

    replaceNode(rhs, methodCall);

  }
}