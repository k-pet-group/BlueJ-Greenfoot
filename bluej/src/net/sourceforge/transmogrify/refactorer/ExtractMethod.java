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

// $Id: ExtractMethod.java 1011 2001-11-22 10:36:26Z ajp $
import antlr.*;

import java.io.*;

import java.util.*;

import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;
import net.sourceforge.transmogrify.hook.StatementSpan;

import net.sourceforge.transmogrify.symtab.*;

import net.sourceforge.transmogrify.symtab.parser.*;

import net.sourceforge.transmogrify.symtab.printer.*;

public class ExtractMethod extends Transmogrifier {
  public ExtractMethod(ASTPrintManager manager){
    super(manager);
  }

  public ExtractMethod(){
    this(new FilePrintManager());
  }

  public void apply(Hook hook) throws Exception {
    StatementSpan span = new StatementSpan(hook);
    String name = hook.getUserInput("Rename Method", "New method name");
    refactor(span.getStart(), span.getEnd(), name);
    streamFiles();
  }

  public void refactor(Occurrence startLocation, Occurrence endLocation, String name) throws RefactoringException {
    SymTabAST file = getFileNode(startLocation.getFile());
    SymTabAST startNode = file.getEnclosingNode(startLocation.getLine(), startLocation.getColumn());
    SymTabAST startExpr = findParentExpr(startNode);
    SymTabAST endNode = file.getEnclosingNode(endLocation.getLine(), endLocation.getColumn());
    SymTabAST endExpr = findParentExpr(endNode, startExpr);
    Set localVars = new HashSet();
    SymTabAST current = startExpr;
    // for each EXPR node, find references to local variables
    while(current != endExpr.getNextSibling()){
      localVars.addAll(getLocalVars(current));
      current = (SymTabAST)current.getNextSibling();
    }
    Set parameterNodes = new HashSet();
    SymTabAST variableToReturn = null;
    for(Iterator it = localVars.iterator(); it.hasNext(); ){
      current = (SymTabAST)it.next();
      if(isAssignedTo(current, startExpr, endExpr)){
        if(!defInSelection(current, startExpr, endExpr)){
          if(isReferencedAfterSelection(current, endExpr)){
            if(variableToReturn == null){
              variableToReturn = current;
            }
            else{
              throw new RefactoringException("More than one variable referenced outside selection");
            }
          }
          addParameter(parameterNodes, current);
        }
        else{
          if(isReferencedAfterSelection(current, endExpr)){
            throw new RefactoringException("Definition of variable in selection, but it is referenced outside selection");
          }
          // don't need parameter
        }
      }
      else{
        addParameter(parameterNodes, current);
      }
    }
    SymTabAST objBlock = getParentObjBlock(startExpr);
    SymTabAST methodName = (SymTabAST)factory.create(JavaTokenTypes.IDENT, name);
    SymTabAST modifiers = findParentMethod(startExpr).getFirstChildOfType(JavaTokenTypes.MODIFIERS).deepClone();
    SymTabAST returnType = makeReturnType(variableToReturn);
    SymTabAST parameters = makeParameters(parameterNodes);
    SymTabAST slist = (SymTabAST)factory.create(JavaTokenTypes.SLIST, "{");
    slist.setFirstChild(startExpr);
    SymTabAST restOfBlock = (SymTabAST)endExpr.getNextSibling();
    endExpr.setNextSibling(null);
    SymTabAST methodDef = makeMethod(methodName, modifiers, returnType, parameters, slist, variableToReturn);
    SymTabAST elist = (SymTabAST)factory.create(JavaTokenTypes.ELIST, JavaRecognizer._tokenNames[JavaTokenTypes.ELIST]);
    SymTabAST prev = null;
    for(Iterator it = parameterNodes.iterator(); it.hasNext(); ){
      SymTabAST exprNode = (SymTabAST)factory.create(JavaTokenTypes.EXPR, JavaRecognizer._tokenNames[JavaTokenTypes.EXPR]);
      SymTabAST ident = ((SymTabAST)it.next()).deepClone();
      ident.setNextSibling(null);
      exprNode.setFirstChild(ident);
      if(prev == null){
        elist.setFirstChild(exprNode);
      }
      else{
        prev.setNextSibling(exprNode);
      }
      prev = exprNode;
    }
    SymTabAST methodCall = makeMethodCall(methodName.deepClone(), elist, variableToReturn);
    clipAndReplaceNodes(startExpr, restOfBlock, methodCall);
    removeNode(objBlock.getFirstChildOfType(JavaTokenTypes.RCURLY));
    addChild(objBlock, methodDef);
    addChild(objBlock, (SymTabAST)factory.create(JavaTokenTypes.RCURLY, "}"));
  }

  public boolean canApply(Hook hook){
    return !table.isOutOfDate();
  }

  private void addParameter(Set set, SymTabAST parameter){
    boolean found = false;
    for(Iterator it = set.iterator(); it.hasNext() && !found; ){
      SymTabAST current = (SymTabAST)it.next();
      if(current.getText().equals(parameter.getText())){
        found = true;
      }
    }
    if(!found){
      set.add(parameter);
    }
  }

  private SymTabAST makeMethodCall(SymTabAST name, SymTabAST elist, SymTabAST variableToReturn){
    SymTabAST result = (SymTabAST)factory.create(JavaTokenTypes.EXPR, JavaRecognizer._tokenNames[JavaTokenTypes.EXPR]);
    SymTabAST methodCall = (SymTabAST)factory.create(JavaTokenTypes.METHOD_CALL, "(");
    methodCall.setFirstChild(name);
    name.setNextSibling(elist);
    if(variableToReturn == null){
      result.setFirstChild(methodCall);
    }
    else{
      SymTabAST assign = (SymTabAST)factory.create(JavaTokenTypes.ASSIGN, "=");
      SymTabAST lhs = variableToReturn.deepClone();
      assign.setFirstChild(lhs);
      lhs.setNextSibling(methodCall);
      result.setFirstChild(assign);
    }
    return result;
  }

  private SymTabAST getParentObjBlock(SymTabAST expr){
    SymTabAST current = expr;
    while(current.getType() != JavaTokenTypes.OBJBLOCK){
      current = current.getParent();
    }
    return current;
  }

  private SymTabAST makeMethod(SymTabAST methodName, SymTabAST modifiers, SymTabAST returnType, SymTabAST parameters, SymTabAST slist, SymTabAST variableToReturn){
    SymTabAST method = (SymTabAST)factory.create(JavaTokenTypes.METHOD_DEF, JavaRecognizer._tokenNames[JavaTokenTypes.METHOD_DEF]);
    SymTabAST type = (SymTabAST)factory.create(JavaTokenTypes.TYPE, JavaRecognizer._tokenNames[JavaTokenTypes.TYPE]);
    method.setFirstChild(modifiers);
    modifiers.setNextSibling(type);
    type.setFirstChild(returnType);
    type.setNextSibling(methodName);
    methodName.setNextSibling(parameters);
    parameters.setNextSibling(slist);
    if(variableToReturn != null){
      SymTabAST returnStmt = (SymTabAST)factory.create(JavaTokenTypes.LITERAL_return, "return");
      SymTabAST expr = (SymTabAST)factory.create(JavaTokenTypes.EXPR, JavaRecognizer._tokenNames[JavaTokenTypes.EXPR]);
      returnStmt.setFirstChild(expr);
      expr.setFirstChild(variableToReturn);
      addChild(slist, returnStmt);
    }
    addChild(slist, (SymTabAST)factory.create(JavaTokenTypes.RCURLY, "}"));
    return method;
  }

  private Set getLocalVars(SymTabAST expr){
    Set result = new HashSet();
    SymTabAST current = (SymTabAST)expr.getFirstChild();
    while(current != null){
      if(current.getType() == JavaTokenTypes.IDENT){
        if(isLocal(current)){
          // need to know the type
          result.add(current);
        }
      }
      result.addAll(getLocalVars(current));
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private boolean isLocal(SymTabAST candidate){
    boolean result = true;
    // if the definition of the ident is in the same scope
    // (or a parent scope lower than class level)
    // it is local
    if(candidate.getDefinition() instanceof Definition){
      IDefinition def = candidate.getDefinition();
      if(!def.isSourced()){
        result = false;
      }
      else if(def instanceof IClass){
        result = false;
      }
      else{
        IClass enclosingClass = ((Definition)def).getEnclosingClass();
        if(enclosingClass == ((Definition)def).getParentScope()){
          result = false;
        }
        else{
          IClass parent = enclosingClass.getSuperclass();
          while(parent != null && parent instanceof IClass){
            if(parent == ((Definition)def).getParentScope()){
              result = false;
              break;
            }
            parent = parent.getSuperclass();
          }
        }
      }
    }
    else{
      // non-sourced (external) definitions are never local
      result = false;
    }
    return result;
  }

  private SymTabAST makeParameters(Set localVars){
    SymTabAST parameters = (SymTabAST)factory.create(JavaTokenTypes.PARAMETERS, JavaRecognizer._tokenNames[JavaTokenTypes.PARAMETERS]);
    Iterator it = localVars.iterator();
    SymTabAST prev = null;
    while(it.hasNext()){
      SymTabAST current = (SymTabAST)it.next();
      SymTabAST parameterDef = (SymTabAST)factory.create(JavaTokenTypes.PARAMETER_DEF, JavaRecognizer._tokenNames[JavaTokenTypes.PARAMETER_DEF]);
      SymTabAST typeNode = (SymTabAST)factory.create(JavaTokenTypes.TYPE, JavaRecognizer._tokenNames[JavaTokenTypes.TYPE]);
      SymTabAST modifiers = (SymTabAST)factory.create(JavaTokenTypes.MODIFIERS, JavaRecognizer._tokenNames[JavaTokenTypes.MODIFIERS]);
      SymTabAST ident = (SymTabAST)factory.create(JavaTokenTypes.IDENT, current.getText());
      SymTabAST type = getType(current);
      parameterDef.setFirstChild(modifiers);
      modifiers.setNextSibling(typeNode);
      typeNode.setNextSibling(ident);
      typeNode.setFirstChild(type);
      if(prev != null){
        prev.setNextSibling(parameterDef);
      }
      else{
        parameters.setFirstChild(parameterDef);
      }
      prev = parameterDef;
    }
    return parameters;
  }

  private SymTabAST getType(SymTabAST node){
    IClass typeDef = ((Typed)node.getDefinition()).getType();
    SymTabAST type = null;
    if(typeDef.isPrimitive()){
      type = LiteralResolver.getASTNode(typeDef);
    }
    else{
      type = (SymTabAST)factory.create(JavaTokenTypes.IDENT, typeDef.getName());
    }
    return type;
  }

  private boolean isAssignedTo(SymTabAST variable, SymTabAST start, SymTabAST end){
    boolean result = false;
    SymTabAST current = start;
    while(current != end.getNextSibling()){
      if(isAssignedToHelper(variable, current)){
        result = true;
        break;
      }
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private boolean isAssignedToHelper(SymTabAST candidate, SymTabAST node){
    boolean result = false;
    SymTabAST current = node;
    while(current != null){
      if(current.getType() == JavaTokenTypes.VARIABLE_DEF){
        SymTabAST assign = current.getFirstChildOfType(JavaTokenTypes.ASSIGN);
        if(assign != null){
          result = true;
          break;
        }
      }
      if(isAssignment(current) || isCrementor(current)){
        SymTabAST lhs = getLHS(current);
        if(candidate.getDefinition().equals(lhs.getDefinition())){
          result = true;
          break;
        }
      }
      if(isAssignedToHelper(candidate, (SymTabAST)current.getFirstChild())){
        result = true;
        break;
      }
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private boolean defInSelection(SymTabAST variable, SymTabAST start, SymTabAST end){
    boolean result = false;
    SymTabAST current = start;
    while(current != end.getNextSibling()){
      if(defInSelectionHelper(variable, current)){
        result = true;
        break;
      }
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private boolean defInSelectionHelper(SymTabAST variable, SymTabAST node){
    boolean result = false;
    SymTabAST current = node;
    while(current != null){
      if(current.getType() == JavaTokenTypes.VARIABLE_DEF){
        SymTabAST ident = current.getFirstChildOfType(JavaTokenTypes.IDENT);
        if(ident.getText().equals(variable.getText())){
          result = true;
          break;
        }
      }
      if(defInSelectionHelper(variable, (SymTabAST)current.getFirstChild())){
        result = true;
        break;
      }
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private boolean isAssignment(SymTabAST node){
    boolean result = false;
    switch(node.getType()){
      case JavaTokenTypes.ASSIGN:
      case JavaTokenTypes.PLUS_ASSIGN:
      case JavaTokenTypes.MINUS_ASSIGN:
      case JavaTokenTypes.STAR_ASSIGN:
      case JavaTokenTypes.DIV_ASSIGN:
      case JavaTokenTypes.MOD_ASSIGN:
      case JavaTokenTypes.SR_ASSIGN:
      case JavaTokenTypes.BSR_ASSIGN:
      case JavaTokenTypes.SL_ASSIGN:
      case JavaTokenTypes.BAND_ASSIGN:
      case JavaTokenTypes.BXOR_ASSIGN:
      case JavaTokenTypes.BOR_ASSIGN:
        result = true;
        break;
    }
    return result;
  }

  private boolean isCrementor(SymTabAST node){
    boolean result = false;
    switch(node.getType()){
      case JavaTokenTypes.INC:
      case JavaTokenTypes.DEC:
      case JavaTokenTypes.POST_INC:
      case JavaTokenTypes.POST_DEC:
        result = true;
        break;
    }
    return result;
  }

  private boolean isAssignedFromAfterSelection(SymTabAST variable, SymTabAST endSelectionExpr){
    boolean result = false;
    SymTabAST current = (SymTabAST)endSelectionExpr.getNextSibling();
    while(current != null){
      if(isAssignedFromAfterSelectionHelper(variable, current)){
        result = true;
        break;
      }
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private boolean isAssignedFromAfterSelectionHelper(SymTabAST candidate, SymTabAST expr){
    boolean result = false;
    SymTabAST current = (SymTabAST)expr.getFirstChild();
    while(current != null){
      if(isAssignment(current)){
        SymTabAST rhs = getRHS(current);
        if(candidate.getDefinition().equals(rhs.getDefinition())){
          result = true;
          break;
        }
      }
      if(isAssignedFromAfterSelectionHelper(candidate, current)){
        result = true;
        break;
      }
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private boolean isReferencedAfterSelection(SymTabAST variable, SymTabAST endExpr){
    boolean result = false;
    SymTabAST current = (SymTabAST)endExpr.getNextSibling();
    while(current != null){
      if(isReferencedAfterSelectionHelper(variable, current)){
        result = true;
        break;
      }
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private boolean isReferencedAfterSelectionHelper(SymTabAST variable, SymTabAST node){
    boolean result = false;
    SymTabAST current = (SymTabAST)node.getFirstChild();
    while(current != null){
      if(variable.getDefinition().equals(current.getDefinition())){
        result = true;
        break;
      }
      if(isReferencedAfterSelectionHelper(variable, current)){
        result = true;
        break;
      }
      current = (SymTabAST)current.getNextSibling();
    }
    return result;
  }

  private SymTabAST findParentMethod(SymTabAST candidate){
    SymTabAST result = null;
    if(candidate.getType() == JavaTokenTypes.METHOD_DEF || candidate.getType() == JavaTokenTypes.CTOR_DEF){
      result = candidate;
    }
    else{
      result = findParentMethod((SymTabAST)candidate.getParent());
    }
    return result;
  }

  private SymTabAST makeReturnType(SymTabAST variableToReturn){
    SymTabAST result = null;
    if(variableToReturn == null){
      result = (SymTabAST)factory.create(JavaTokenTypes.LITERAL_void, "void");
    }
    else{
      result = getType(variableToReturn);
    }
    return result;
  }
}
