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

// $Id: Resolver.java 1011 2001-11-22 10:36:26Z ajp $

import antlr.*;
import antlr.collections.AST;

import net.sourceforge.transmogrify.symtab.parser.*;

import java.io.File;
import java.util.*;

/**
 * The resolver is responsible for traversing all the various
 * definitions in a symbol table and resolving references in them.
 *
 * @see SymbolTable
 */

public class Resolver extends DefinitionTraverser {

  /**
   * constructor with <code>SymbolTable</code> to be resolved
   */
  public Resolver(SymbolTable symbolTable) {
    super(symbolTable);
  }

  /**
   * resolves the symbol table
   * @return <code>void</code>
   * @see #traverse()
   */
  public void resolve() {
    traverse();
  }

  protected void handleSList(SymTabAST node, Scope scope){
      SymTabASTIterator iterator = node.getChildren();
      while (iterator.hasNext()){
          SymTabAST current = iterator.nextChild();
          resolveExpression(current, scope, null, true);
      }
  }

  protected void handleAnonymousInnerClass(AnonymousInnerClass innerClass) {
    SymTabAST objblock = innerClass.getTreeNode();
    SymTabAST expression = (SymTabAST)objblock.getFirstChild();
    while (expression != null) {
      resolveExpression(expression, innerClass, null, true);
      expression = (SymTabAST)expression.getNextSibling();
    }
  }

  /**
   * processes a <code>ClassDef</code> and resolves references in it
   *
   * @param classDef the <code>ClassDef</code> to process
   */
  protected void handleClass( ClassDef classDef ) {
    SymTabAST node = classDef.getTreeNode();

    if ( node != null ) {
      SymTabAST nameNode = node.getFirstChildOfType(JavaTokenTypes.IDENT);
      nameNode.setDefinition( classDef, classDef, true );

      SymTabAST extendsClause =
        node.getFirstChildOfType(JavaTokenTypes.EXTENDS_CLAUSE);
      SymTabAST extendedClassNode = (SymTabAST)extendsClause.getFirstChild();

      while ( extendedClassNode != null ) {
        IClass superClass = resolveClass(extendedClassNode, classDef,
                                         null, true);
        extendedClassNode.setDefinition(superClass,
                                        classDef,
                                        true);
        extendedClassNode = (SymTabAST)extendedClassNode.getNextSibling();
      }

      SymTabAST implementsNode
        = node.getFirstChildOfType(JavaTokenTypes.IMPLEMENTS_CLAUSE);

      if (implementsNode != null) {
        SymTabAST interfaceNode = (SymTabAST)(implementsNode.getFirstChild());
        while (interfaceNode != null) {
          resolveClass(interfaceNode, classDef, null, true);
          interfaceNode = (SymTabAST)(interfaceNode.getNextSibling());
        }
      }
    }
  }

  /**
   * processes a <code>MethodDef</code> and resolves references in it
   *
   * @param method the <code>MethodDef</code> to process
   */
  protected void handleMethod(MethodDef method) {
    SymTabAST node = method.getTreeNode();

    SymTabAST nameNode = node.getFirstChildOfType(JavaTokenTypes.IDENT);
    nameNode.setDefinition( method, method, true );

    // references to classes in return type
    SymTabAST returnTypeNode = node.getFirstChildOfType(JavaTokenTypes.TYPE);

    if (returnTypeNode != null) {
      // this is not a constructor
      resolveExpression(returnTypeNode, method, null, true);
    }

    SymTabAST throwsNode =
      node.getFirstChildOfType(JavaTokenTypes.LITERAL_throws);
    if (throwsNode != null) {
      SymTabAST exception = (SymTabAST)throwsNode.getFirstChild();
      while (exception != null) {
        resolveClass(exception, method, null, true);
        exception = (SymTabAST)exception.getNextSibling();
      }
    }

    // references to classes in parameters

    // the body -- this would be better its own function
    SymTabAST slist = node.getFirstChildOfType(JavaTokenTypes.SLIST);

    if ( slist != null ) {
      handleSList(slist, method);
    }
  }

  /**
   * processes a <code>BlockDef</code> and resolves references in it
   *
   * @param block the <code>BlockDef</code> to process
   */
  protected void handleBlock( BlockDef block ) {
    SymTabAST node = block.getTreeNode();

    switch( node.getType() ) {

    case JavaTokenTypes.LITERAL_for:
      handleFor( block );
      break;

    case JavaTokenTypes.LITERAL_if:
      handleIf( block );
      break;

    case JavaTokenTypes.LITERAL_while:
      handleWhileAndSynchronized( block );
      break;

    case JavaTokenTypes.LITERAL_do:
      handleDoWhile(block);
      break;

    case JavaTokenTypes.LITERAL_try:
    case JavaTokenTypes.LITERAL_finally:
      SymTabAST slist = node.getFirstChildOfType(JavaTokenTypes.SLIST);

      handleSList(slist, block);
      break;

    case JavaTokenTypes.LITERAL_catch:
      handleCatch( block );
      break;

    case JavaTokenTypes.LITERAL_switch:
      handleSwitch( block );
      break;

    case JavaTokenTypes.SLIST:
      handleSList(node, block);
      break;

    case JavaTokenTypes.EXPR:
      resolveExpression( node, block, null, true );
      break;

    case JavaTokenTypes.INSTANCE_INIT:
    case JavaTokenTypes.STATIC_INIT:
      handleSList((SymTabAST)node.getFirstChild(), block);
      break;

    case JavaTokenTypes.LITERAL_synchronized:
      handleWhileAndSynchronized(block);
      break;

    default:
      System.out.println( "Unhandled block " + block + " of type " + node.getType());
    }
  }

  /**
   * processes a switch statement and resolves references in it
   *
   * @param block the <code>BlockDef</code> to process
   */
  private void handleSwitch( BlockDef block ) {
    SymTabAST node = block.getTreeNode();

    SymTabAST expr = node.getFirstChildOfType(JavaTokenTypes.EXPR);
    resolveExpression(expr, block, null, true);

    SymTabAST caseGroup = (SymTabAST)(expr.getNextSibling());
    while (caseGroup.getType() == JavaTokenTypes.CASE_GROUP) {
      SymTabAST caseNode =
      caseGroup.getFirstChildOfType(JavaTokenTypes.LITERAL_case);
      while (caseNode != null &&
             caseNode.getType() == JavaTokenTypes.LITERAL_case) {
          resolveExpression((SymTabAST)caseNode.getFirstChild(),
                            block, null, true);
          caseNode = (SymTabAST)caseNode.getNextSibling();
      }

      SymTabAST caseSlist = caseGroup.getFirstChildOfType(JavaTokenTypes.SLIST);
      handleSList(caseSlist, block);

      caseGroup = (SymTabAST)(caseGroup.getNextSibling());
    }
  }

  /**
   * processes a catch block and resolves references in it
   *
   * @param block the <code>BlockDef</code> to process
   */
  private void handleCatch( BlockDef block) {
    SymTabAST node = block.getTreeNode();

    SymTabAST slist = node.getFirstChildOfType(JavaTokenTypes.SLIST);
    handleSList(slist, block);
  }

  /**
   * processes a for loop and resolves references in it
   *
   * @param block the <code>BlockDef</code> to process
   */
  private void handleFor( BlockDef block ) {
    SymTabAST node = block.getTreeNode();

    SymTabAST init = node.getFirstChildOfType(JavaTokenTypes.FOR_INIT);
    // only need to handle the elist case.  if the init node is a variable
    // definition, the variable def will be handled later on in the resolution
    if(init.getFirstChild() != null) {
      if ( init.getFirstChild().getType() == JavaTokenTypes.ELIST ) {
        resolveExpression( (SymTabAST)(init.getFirstChild()), block, null, true );
      }
    }

    SymTabAST cond = node.getFirstChildOfType(JavaTokenTypes.FOR_CONDITION);
    if(cond.getFirstChild() != null) {
      resolveExpression( (SymTabAST)(cond.getFirstChild()), block, null, true );
    }

    SymTabAST iterator = node.getFirstChildOfType(JavaTokenTypes.FOR_ITERATOR);
    if(iterator.getFirstChild() != null) {
      resolveExpression( (SymTabAST)(iterator.getFirstChild()), block, null, true );
    }

    //could be an SLIST, EXPR or an EMPTY_STAT
    SymTabAST body = (SymTabAST)(iterator.getNextSibling());
    if (body.getType() == JavaTokenTypes.SLIST){
        handleSList(body, block);
    }else{
        resolveExpression(body, block, null, true);
    }

  }

  /**
   * processes an if statement and resolves references in it
   *
   * @param block the <code>BlockDef</code> to process
   */
  private void handleIf(BlockDef block) {
    SymTabAST node = block.getTreeNode();

    SymTabAST conditional = (SymTabAST)(node.getFirstChild());
    resolveExpression( conditional, block, null, true );

    SymTabAST body = (SymTabAST)conditional.getNextSibling();
    if (body != null && body.getType() == JavaTokenTypes.SLIST) {
        handleSList(body, block);
    }else{
        resolveExpression( body, block, null, true );
    }

    SymTabAST elseBody = (SymTabAST)body.getNextSibling();
   /*
    if (elseBody != null && elseBody.getType() == JavaTokenTypes.SLIST) {
        handleSList(elseBody, block);
    }else{
        resolveExpression(elseBody, block, null, true);
    }
    */
    if (elseBody != null){
        resolveExpression(elseBody, block.getParentScope(), null, true);
    }
  }

  /**
   * processes a while loop and resolves references in it
   *
   * @param block the <code>BlockDef</code> to process
   */
  private void handleWhileAndSynchronized(BlockDef block) {
    SymTabAST node = block.getTreeNode();

    SymTabAST condition = (SymTabAST)(node.getFirstChild());
    SymTabAST slist = (SymTabAST)(condition.getNextSibling());

    resolveExpression(condition, block, null, true);
    handleSList(slist, block);
  }

  private void handleDoWhile(BlockDef block) {
    SymTabAST node = block.getTreeNode();

    SymTabAST slist = (SymTabAST)node.getFirstChild();
    SymTabAST condition = (SymTabAST)slist.getNextSibling();

    handleSList(slist, block);
    resolveExpression(condition, block, null, true);
  }

  /**
   * processes a variable definition and resolves references in it
   *
   * @param variable the <code>VariableDef</code> to process
   */
  protected void handleVariable( VariableDef variable ) {
    SymTabAST node = variable.getTreeNode();
    Scope location = variable.getParentScope();

    SymTabAST nameNode = node.getFirstChildOfType(JavaTokenTypes.IDENT);
    nameNode.setDefinition( variable, location, true );

    SymTabAST typeNode = node.getFirstChildOfType(JavaTokenTypes.TYPE);
    resolveType(typeNode, location, null, true);

    SymTabAST assignmentNode = node.getFirstChildOfType(JavaTokenTypes.ASSIGN);
    if ( assignmentNode != null ) {
      resolveExpression( (SymTabAST)(assignmentNode.getFirstChild()),
         variable.getParentScope(),
         null, true );
    }
  }

  /**
   * processes a label and resolves references in it
   *
   * @param label the <code>LabelDef</code> to process
   */
  protected void handleLabel( LabelDef label ) {
    SymTabAST node = label.getTreeNode();
    ((SymTabAST)node.getFirstChild()).setDefinition(label, label.getParentScope(), true);
  }

  /**
   * Resolves Java expressions, returning the type to which the expression
   * evalutes.  If this is the reference creation phase, any references found during resolution are created and
   * resolved.
   *
   * @param expression the <code>SymTabAST</code> representing the expression
   * @param location the <code>Scope</code> in which the expression occours.
   * @param context the <code>Scope</code> in which the search for the
   *                definition will start
   * @param referencePhase whether or not this is the reference phase of
   *                       table construction
   *
   * @return the <code>ClassDef</code> representing the type to which the
   *         expression evalutes.
   */
  public IClass resolveExpression(SymTabAST expression, Scope location,
          IClass context, boolean referencePhase) {
    IClass result = null;

    try {

    switch (expression.getType()) {

    case JavaTokenTypes.TYPECAST:
      result = resolveTypecast(expression, location, context, referencePhase);
      break;
    case JavaTokenTypes.EXPR:
    case JavaTokenTypes.LITERAL_return:
      if (expression.getFirstChild() != null) {
        result = resolveExpression((SymTabAST)expression.getFirstChild(),
                                   location, context, referencePhase);
      }
      else {
        // YOU WRITE BAD CODE!
      }
      break;

    case JavaTokenTypes.ELIST:

      SymTabAST child = (SymTabAST)(expression.getFirstChild());
      while ( child != null ) {
        resolveExpression( child, location, context, referencePhase );
        child = (SymTabAST)(child.getNextSibling());
      }
      break;

    case JavaTokenTypes.IDENT:
      result = resolveIdent( expression, location, context, referencePhase );
      break;

    case JavaTokenTypes.TYPE:
      result = resolveType( expression, location, context, referencePhase );
      break;

    case JavaTokenTypes.METHOD_CALL:
      result = resolveMethod(expression,
                             location,
                             context,
                             referencePhase);
      break;

    case JavaTokenTypes.LITERAL_this:
      result = resolveLiteralThis( expression, location, context );
      break;

    case JavaTokenTypes.LITERAL_super:
      result = resolveLiteralSuper( expression, location, context );
      break;

    case JavaTokenTypes.DOT:
      result = resolveDottedName(expression, location, context, referencePhase);
      break;

    case JavaTokenTypes.LITERAL_new:
      result = resolveNew( expression, location, context, referencePhase );
      break;

    case JavaTokenTypes.LITERAL_boolean:
    case JavaTokenTypes.LITERAL_double:
    case JavaTokenTypes.LITERAL_float:
    case JavaTokenTypes.LITERAL_long:
    case JavaTokenTypes.LITERAL_int:
    case JavaTokenTypes.LITERAL_short:
    case JavaTokenTypes.LITERAL_byte:
    case JavaTokenTypes.LITERAL_char:
      result = resolvePrimitiveType( expression, location, context, referencePhase );
      break;

    case JavaTokenTypes.NUM_INT:
      result = resolveNumInt( expression, location, context );
      break;

    case JavaTokenTypes.NUM_FLOAT:
      result = resolveNumFloat( expression, location, context );
      break;

    case JavaTokenTypes.STRING_LITERAL:
      result = resolveStringLiteral( expression, location, context );
      break;

    case JavaTokenTypes.CHAR_LITERAL:
      result = resolveCharLiteral( expression, location, context );
      break;

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
      resolveAssignment( expression, location, context, referencePhase );
      break;

    case JavaTokenTypes.LOR:
    case JavaTokenTypes.LAND:
    case JavaTokenTypes.NOT_EQUAL:
    case JavaTokenTypes.EQUAL:
    case JavaTokenTypes.LT:
    case JavaTokenTypes.GT:
    case JavaTokenTypes.LE:
    case JavaTokenTypes.GE:
      result = resolveBooleanExpression( expression, location, context, referencePhase );
      break;

    case JavaTokenTypes.LITERAL_instanceof:
      result = resolveInstanceOf( expression, location, context, referencePhase );
      break;

    case JavaTokenTypes.LITERAL_true:
    case JavaTokenTypes.LITERAL_false:
      result = resolveBooleanLiteral( expression, location, context );
      break;

    case JavaTokenTypes.LNOT:
      result = resolveBooleanUnary(expression,
                                   location,
                                   context,
                                   referencePhase);
      break;

    case JavaTokenTypes.INC:
    case JavaTokenTypes.POST_INC:
    case JavaTokenTypes.DEC:
    case JavaTokenTypes.POST_DEC:
    case JavaTokenTypes.UNARY_PLUS:
    case JavaTokenTypes.UNARY_MINUS:
      result = resolveUnaryExpression( expression, location, context, referencePhase );
      break;

    case JavaTokenTypes.PLUS:
    case JavaTokenTypes.MINUS:
    case JavaTokenTypes.DIV:
    case JavaTokenTypes.STAR:
    case JavaTokenTypes.BAND:
    case JavaTokenTypes.BOR:
    case JavaTokenTypes.BXOR:
    case JavaTokenTypes.MOD:
      result = resolveArithmeticExpression(expression, location, context,
                                           referencePhase );
      break;

    case JavaTokenTypes.LITERAL_break:
    case JavaTokenTypes.LITERAL_continue:
      resolveGoto(expression, location, context, referencePhase);
      break;

    case JavaTokenTypes.LPAREN:
      result = resolveExpression( (SymTabAST)(expression.getFirstChild()),
                                  location, context, referencePhase );
      break;

    case JavaTokenTypes.INDEX_OP:
      result = resolveArrayAccess( expression, location, context,
                                   referencePhase );
      break;

    case JavaTokenTypes.LITERAL_null:
      result = new NullClass();
      break;

    case JavaTokenTypes.QUESTION:
      result = resolveQuestion(expression, location, context,
                               referencePhase);
      break;

    case JavaTokenTypes.LITERAL_class:
      result = resolveLiteralClass();
      break;

    case JavaTokenTypes.ARRAY_INIT:
      resolveArrayInitializer(expression, location, context,
                              referencePhase);
      break;

    case JavaTokenTypes.LITERAL_throw:
      resolveThrowExpression(expression, location, context, referencePhase);
      break;

    case JavaTokenTypes.SL:
    case JavaTokenTypes.SR:
    case JavaTokenTypes.BSR:
      result = resolveShiftOperator(expression, location,
                                    context, referencePhase);
      break;

    case JavaTokenTypes.BNOT:
      resolveBitwiseNot(expression, location, context, referencePhase);
      break;

    case JavaTokenTypes.EMPTY_STAT:
    case JavaTokenTypes.ML_COMMENT:
    case JavaTokenTypes.SL_COMMENT:
    case JavaTokenTypes.VARIABLE_DEF:
    case JavaTokenTypes.METHOD_DEF:
    case JavaTokenTypes.CLASS_DEF:
    case JavaTokenTypes.LITERAL_for:
    case JavaTokenTypes.LITERAL_while:
    case JavaTokenTypes.LITERAL_if:
    case JavaTokenTypes.LITERAL_void:
    case JavaTokenTypes.LITERAL_interface:
    case JavaTokenTypes.LITERAL_do:
    case JavaTokenTypes.LITERAL_switch:
    case JavaTokenTypes.LITERAL_static:
    case JavaTokenTypes.LITERAL_transient:
    case JavaTokenTypes.LITERAL_native:
    case JavaTokenTypes.LITERAL_threadsafe:
    case JavaTokenTypes.LITERAL_synchronized:
    case JavaTokenTypes.LITERAL_volatile:
    case JavaTokenTypes.LITERAL_try:
    case JavaTokenTypes.LITERAL_catch:
    case JavaTokenTypes.LITERAL_finally:
    case JavaTokenTypes.LABELED_STAT:
    case JavaTokenTypes.RCURLY:
    case JavaTokenTypes.SLIST:
      break;

    default:
      System.out.println( "Unhandled expression type: "
                          + expression.getType() );

    }

    }
    catch (Exception e) {
      e.printStackTrace();
      result = new UnknownClass(expression.getText(), expression);
      System.out.println("Error resolving near " + expression);
    }

    return result;
  }

  private IClass resolveTypecast(SymTabAST node, Scope location, IClass context,
                                 boolean referencePhase) {
    SymTabAST typeNode = (SymTabAST)node.getFirstChild();
    SymTabAST exprNode = (SymTabAST)typeNode.getNextSibling();

    IClass type = null;

    if (typeNode.getFirstChild().getType()
        == JavaTokenTypes.ARRAY_DECLARATOR) {
      type = new ArrayDef(resolveType((SymTabAST)typeNode.getFirstChild(),
                                      location, context, referencePhase));
    }
    else {
      type = resolveType(typeNode, location, context, referencePhase);
    }

    resolveExpression(exprNode, location, context, referencePhase);
    ((SymTabAST)typeNode.getFirstChild()).setDefinition(type, location, referencePhase);

    return type;
  }

  private IClass resolveArrayAccess(SymTabAST node,
                                    Scope location,
                                    IClass context,
                                    boolean referencePhase) {

    SymTabAST arrayNode = (SymTabAST)(node.getFirstChild());
    SymTabAST exprNode = (SymTabAST)(arrayNode.getNextSibling());

    ArrayDef array = (ArrayDef)resolveExpression( arrayNode, location,
                                                  context, referencePhase );
    resolveExpression( exprNode, location, context, referencePhase );

    return array.getType();
  }

  private IClass resolveLiteralClass() {
    return new ExternalClass(Class.class);
  }

  /**
   * Resolves any dotted reference, returning the <code>Scope</code>
   * identified by the reference.
   *
   * @param tree the root node of the dotted reference
   * @param location the <code>Scope</code> in which the expression occours.
   * @param context the <code>Scope</code> in which the search for the
   *                definition will start
   * @return the <code>Scope</code> indentified by the reference
   */
  private IClass resolveDottedName(SymTabAST tree,
                                   Scope location,
                                   IClass context,
                                   boolean referencePhase) {
    IClass result = null;

    IClass localContext = context;
    String name = null;

    DotIterator it = new DotIterator(tree);
    while (it.hasNext()) {
      SymTabAST node = it.nextNode();
      localContext = resolveExpression(node, location,
                                       localContext, referencePhase);
      if (localContext == null) {
        node.setMeaningfulness(false);
        name = node.getText();
        while (localContext == null && it.hasNext()) {
          SymTabAST next = it.nextNode();
          name = name + "." + next.getText();
          localContext = location.getClassDefinition(name);
          if (localContext != null && referencePhase) {
            next.setDefinition(localContext, location, referencePhase);
          }
          else {
            next.setMeaningfulness(false);
          }
        }
      }
    }

    if (localContext != null) {
      result = localContext;
    }
    else {
      result = new UnknownClass(name, tree);
    }

    return result;
  }

  /**
   * Resolves a method call.
   *
   * @param methodNode the <code>SymTabAST</code> for the METHOD_CALL node
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   * @param referencePhase whether or not this is the reference phase of
   *                       table construction
   *
   * @return the <code>ClassDef</code> for the type returned by the method
   */
  private IClass resolveMethod(SymTabAST methodNode,
                               Scope location,
                               IClass context,
                               boolean referencePhase) {
    IClass result = new UnknownClass(methodNode.getText(), methodNode);
    IClass newContext = null;

    if (context == null) {
      newContext = location.getEnclosingClass();
    }
    else {
      newContext = context;
    }

    String name = null;
    boolean createReference = true;

    SymTabAST nameNode = (SymTabAST)(methodNode.getFirstChild() );
    SymTabAST parametersNode = (SymTabAST)(nameNode.getNextSibling() );

    ISignature signature = resolveParameters(parametersNode,
                                             location,
                                             context,
                                             referencePhase);

    if ( nameNode.getType() == JavaTokenTypes.IDENT ) {
      name = nameNode.getText();
    }
    else if (nameNode.getType() == JavaTokenTypes.LITERAL_super) {
      IClass superclass = location.getEnclosingClass().getSuperclass();
      newContext = superclass;
      name = superclass.getName();
      createReference = false;
    }
    else if ( nameNode.getType() == JavaTokenTypes.LITERAL_this) {
      newContext = location.getEnclosingClass();
      name = newContext.getName();
      createReference = false;
    }
    else {
      // REDTAG -- doing dotted name resolution on its own
      SymTabAST contextNode = (SymTabAST)(nameNode.getFirstChild());
      nameNode = (SymTabAST)(contextNode.getNextSibling());

      name = nameNode.getText();
      newContext = resolveExpression(contextNode,
                                     location,
                                     context,
                                     referencePhase);
    }

    if ( newContext != null ) {
      IMethod method = newContext.getMethodDefinition(name, signature);

      if ( method != null ) {
        if ( createReference && referencePhase) {
          nameNode.setDefinition( method, location, referencePhase);
        }
        result = method.getType();
      }
    }

    if (result == null) {
      result = new UnknownClass(methodNode.getText(), methodNode);
    }

    return result;
  }

  /**
   * resolves a literal "this"
   *
   * @param expression the <code>SymTabAST</code> of the expression
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the resulting scope of the expression (the type to which it evaluates)
   */
  private IClass resolveLiteralThis( SymTabAST thisNode, Scope location,
    IClass context ) {
    return location.getEnclosingClass();
  }

  /**
   * resolves a literal "super"
   *
   * @param expression the <code>SymTabAST</code> of the expression
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the resulting scope of the expression (the type to which it evaluates)
   */
  private IClass resolveLiteralSuper(SymTabAST superNode, Scope location,
           IClass context ) {
    return location.getEnclosingClass().getSuperclass();
  }

  private boolean newIsConstructor( SymTabAST newNode ) {
    boolean result = false;

    SymTabAST typeNode = (SymTabAST)(newNode.getFirstChild().getNextSibling());
    if ( typeNode.getType() == JavaTokenTypes.ELIST) {
      result = true;
    }
    return result;

  }

  /**
   * resolves and expression of type JavaTokenTypes.TYPE
   *
   * @param expression the <code>SymTabAST</code> of the expression
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   * @param referencePhase whether or not this is the reference phase of
   *                       table construction
   * @return the resulting scope of the expression (the type to which it evaluates)
   * @see #resolveDottedName(SymTabAST, Scope, IClass, boolean)
   * @see #resolveClassIdent(SymTabAST, Scope, IClass, boolean)
   */
  public IClass resolveType(SymTabAST expr, Scope location,
        IClass context, boolean referencePhase ) {
    IClass result = null;
    SymTabAST nameNode = (SymTabAST)expr.getFirstChild();

    if (nameNode.getType() == JavaTokenTypes.DOT) {
      result = resolveDottedName(nameNode,
             location,
             context,
             referencePhase);
    }
    else {
      result = resolveClassIdent(nameNode,
            location,
            context,
            referencePhase);
    }

    return result;
  }

  /**
   * resolves Class type expression
   * @param expr node to be resolved
   * @param location scope of the <code>expr</code>
   * @param context context of the <code>expr</code> if exists
   * @param referencePhase <code>true</code> if this method is used to during
   *                                         finding reference phase
   *                       <code>false</code> otherwise
   * @return <code>IClass</code> representing the type to which the
   *         expression evalutes.
   * @see #resolveDottedName(SymTabAST, Scope, IClass, boolean)
   */
  public IClass resolveClass(SymTabAST expr, Scope location,
                             IClass context, boolean referencePhase) {

    IClass result = resolveDottedName(expr, location, context, referencePhase);
    if (result != null && referencePhase) {
      expr.setDefinition(result, location, referencePhase);
    }

    return result;
  }

  /**
   * resolves expression with <code>JavaTokenTypes<code> other than <code>DOT</code>
   * @param expr expression to be resolved
   * @param location scope of the expression
   * @param context context of the expression if any
   * @param referencePhase <code>true</code> if this method is used to during
   *                                         finding reference phase
   *                       <code>false</code> otherwise
   * @return <code>IClass</code> representing the type to which the
   *         expression evalutes.
   */
  public IClass resolveClassIdent(SymTabAST expr, Scope location,
          IClass context, boolean referencePhase) {

    IClass result = location.getClassDefinition(expr.getText());
    if (result != null) {
      expr.setDefinition(result, location, referencePhase);
    }

    return result;
  }

  private IClass resolveNew( SymTabAST newNode, Scope location,
   IClass context, boolean referencePhase ) {

    IClass result;

    if (newIsConstructor(newNode)) {
      result = resolveConstructor(newNode, location, context, referencePhase);
    }
    else {
      result = resolveNewArray(newNode, location, context, referencePhase );
    }

    return result;
  }

  private IClass resolveNewArray(SymTabAST newNode, Scope location,
         IClass context, boolean referencePhase) {
    IClass arrayType;

    SymTabAST typeNode = (SymTabAST)(newNode.getFirstChild());
    SymTabAST declaratorNode = (SymTabAST)(typeNode.getNextSibling());
    SymTabAST initializerNode = (SymTabAST)(declaratorNode.getNextSibling());

    arrayType = resolveClass(typeNode, location, context, referencePhase);

    if ( declaratorNode.getFirstChild() != null ) {
      resolveExpression(((SymTabAST)declaratorNode.getFirstChild()),
      location, context, referencePhase);
    }

    if ( initializerNode != null ) {
      resolveArrayInitializer(initializerNode, location, context, referencePhase);
    }

    return new ArrayDef(arrayType);
  }

  private IClass resolveQuestion(SymTabAST question, Scope location,
         IClass context, boolean referencePhase) {
    SymTabAST test = (SymTabAST)question.getFirstChild();
    SymTabAST leftBranch = (SymTabAST)test.getNextSibling();
    SymTabAST rightBranch = (SymTabAST)leftBranch.getNextSibling();

    resolveExpression(test, location, context, referencePhase);
    IClass leftClass = resolveExpression(leftBranch, location, context,
             referencePhase);
    IClass rightClass = resolveExpression(rightBranch, location, context,
              referencePhase);

    return moreGeneral(leftClass, rightClass);
  }

  private IClass moreGeneral(IClass a, IClass b) {
    return (a.isCompatibleWith(b))?b:a;
  }

  /**
   * Resolves a constructor call.
   *
   * @param tree the root node of the constructor call
   * @return the <code>ClassDef</code> for the class instantiated by the
   *         constructor
   */
  private IClass resolveConstructor( SymTabAST constructor, Scope location,
                                     IClass context, boolean referencePhase ) {

    IClass classConstructed = null;

    SymTabAST nameNode = (SymTabAST)(constructor.getFirstChild());
    SymTabAST parametersNode = (SymTabAST)(nameNode.getNextSibling());
    SymTabAST nameIdent = null;
    if (nameNode.getType() == JavaTokenTypes.IDENT) {
      nameIdent = nameNode;
    }
    else {
      nameIdent = (SymTabAST)nameNode.getFirstChild().getNextSibling();
    }

    classConstructed = resolveClass(nameNode, location,
                                    context, false);
    if ( classConstructed != null ) {
      MethodSignature signature = resolveParameters(parametersNode,
                                                    location,
                                                    context,
                                                    referencePhase);

      IMethod constructorDef =
        classConstructed.getMethodDefinition(nameIdent.getText(), signature);

      if (constructorDef != null && referencePhase) {
        nameIdent.setDefinition(constructorDef, location, referencePhase);
      }
    }

    return classConstructed;
  }


  /**
   * Resolves the types found in a method call. Any references found
   * in the process are created.  Returns a <code>MethodSignature</code> for
   * the types of the parameters.
   *
   * @param elist The <code>SymTabAST</code> for the list of parameters
   * @return the signature of the parameters
   */
  private MethodSignature resolveParameters(SymTabAST elist,
                                            Scope location,
                                            IClass context,
                                            boolean referencePhase) {
    Vector parameters = new Vector();

    SymTabAST expr = (SymTabAST)(elist.getFirstChild());
    while ( expr != null ) {
      IClass parameter =
        (IClass)resolveExpression((SymTabAST)(expr.getFirstChild()),
                                  location, context, referencePhase);
      parameters.add(parameter);

      expr = (SymTabAST)(expr.getNextSibling());
    }

    return new MethodSignature(parameters);
  }

  /**
   * Resolves an IDENT node of an AST, creating the appropriate reference and
   * returning the scope of the identifer.
   *
   * @param ident the IDENT node
   * @param location the <code>Scope</code> in which the IDENT is found
   * @return the <code>Scope</code> the identifier identifies
   */
  private IClass resolveIdent(SymTabAST ident,
                              Scope location,
        IClass context,
        boolean referencePhase) {

    IClass result = null;
    IDefinition def = null;
    String name = ident.getText();

    // look for var
    if (context != null) {
      def = context.getVariableDefinition(name);
    }
    else {
      def = location.getVariableDefinition(name);
    }

    if (def != null) {
      result = ((IVariable)def).getType();
    }
    else {
      // look for class
      if (context != null) {
        result = context.getClassDefinition(name);
      }
      else {
        result = location.getClassDefinition(name);
      }
      def = result;
    }

    if (def != null) {
      ident.setDefinition(def, location, referencePhase);
    }

    return result;
  }

  /**
   * Resolves a (binary) boolean expression.  The left and right sides of the
   * expression
   * are resolved in the process.
   *
   * @param expression the <code>SymTabAST</code> representing the boolean
   *                   expression.
   * @return the <code>Scope</code> for the boolean primitive type.
   */
  private IClass resolveBooleanExpression( SymTabAST expression,
    Scope location, IClass context, boolean referencePhase) {
    IClass result = null;

    SymTabAST leftChild = (SymTabAST)(expression.getFirstChild());
    resolveExpression( leftChild, location, context, referencePhase );
    SymTabAST rightChild = (SymTabAST)(leftChild.getNextSibling());
    resolveExpression( rightChild, location, context, referencePhase );

    result = LiteralResolver.getResolver().getDefinition(JavaTokenTypes.LITERAL_boolean);

    return result;
  }

  /**
   * resolves references in an assignment expression
   *
   * @param expression the <code>SymTabAST</code> of the expression
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the resulting scope of the expression (the type to which it evaluates)
   */
  private IClass resolveAssignment( SymTabAST expression, Scope location,
     IClass context, boolean referencePhase ) {
    IClass result = null;

    SymTabAST leftNode = (SymTabAST)(expression.getFirstChild());
    SymTabAST rightNode = (SymTabAST)(leftNode.getNextSibling());

    result = resolveExpression( leftNode, location, context, referencePhase );
    resolveExpression( rightNode, location, context, referencePhase );

    return result;
  }

  /**
   * Resolves a unary expression.  Returns the type of the expression,
   * creating any references found along the way.  Unary expressions are
   * increment (x++), decrement (x--), unary plus (+x), and unary minus (-x)
   *
   * @param expression the <code>SymTabAST</code> of the unary expression.
   * @return the <code>Scope</code> for the type to which the expression
   * evalutes.
   */
  private IClass resolveUnaryExpression( SymTabAST expression, Scope location,
   IClass context, boolean referencePhase ) {
    SymTabAST operatee = (SymTabAST)(expression.getFirstChild());
    return resolveExpression(operatee, location, context, referencePhase);
  }

  /**
   * Resolves an arithmetic expression.  Returns the <code>Scope</code> for
   * the type to which the expression resolves.  Any references found during
   * resolution are created and resolved.
   *
   * @param expression the <code>SymTabAST</code> representing the arithmetic
   *                   expression.
   *
   * @return the <code>Scope</code> for the type to which the expression
   *         evaluates.
   */
  private IClass resolveArithmeticExpression( SymTabAST expression,
             Scope location,
             IClass context,
             boolean referencePhase ) {
    IClass result = null;

    SymTabAST leftChild = (SymTabAST)(expression.getFirstChild());
    SymTabAST rightChild = (SymTabAST)(leftChild.getNextSibling());

    IClass leftType = (IClass)(resolveExpression( leftChild, location,
        context, referencePhase ));
    IClass rightType = (IClass)(resolveExpression( rightChild, location,
         context, referencePhase ));

    result = binaryResultType( leftType, rightType );

    return result;
  }

  /**
   * Returns the <code>ClassDef</code> for the type to which arithmetic
   * expressions evaluate.
   *
   * @param a the <code>ClassDef</code> of the first operand.
   * @param b the <code>ClassDef</code> of the second operand.
   *
   * @return the <code>ClassDef</code> to which the expression evaluates.
   */
  private IClass binaryResultType(IClass a, IClass b) {

    IClass result = null;

    // These may or may not be in line with the rules set forth in the java
    // language specification.  Not being in line would be a BadThing(r).

    IClass string = new ExternalClass(java.lang.String.class);

    if (a.equals(string) || b.equals(string)) {
      result = string;
    }
    else if (a.equals(PrimitiveClasses.BOOLEAN)) {
      result = PrimitiveClasses.BOOLEAN;
    }
    else {
      result = PrimitiveClasses.binaryPromote((ExternalClass)a,
                                              (ExternalClass)b);
    }

    return result;
  }

  /**
   * resolves references in an instanceof expression
   *
   * @param expression the <code>SymTabAST</code> of the expression
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the resulting scope of the expression (the type to which it evaluates)
   */
  private IClass resolveInstanceOf( SymTabAST expression, Scope location,
           IClass context, boolean referencePhase ) {
    SymTabAST leftNode = (SymTabAST)(expression.getFirstChild());
    SymTabAST rightNode = (SymTabAST)(leftNode.getNextSibling());

    resolveExpression( leftNode, location, context, referencePhase );

    SymTabAST classNameNode = (SymTabAST)(rightNode.getFirstChild());
    resolveClass(classNameNode, location, context, referencePhase);

    LiteralResolver literalResolver = LiteralResolver.getResolver();
    return literalResolver.getDefinition(JavaTokenTypes.LITERAL_boolean);
  }

  /**
   * resolves references in a a break statement
   *
   * @param expression the <code>SymTabAST</code> for the expression
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the <code>Scope</code> for the int primitive type
   */
  private IClass resolveGoto( SymTabAST expression, Scope location,
    IClass context, boolean referencePhase ) {
    SymTabAST label = (SymTabAST)(expression.getFirstChild());
    if ( label != null ) {
      LabelDef def = location.getLabelDefinition(label.getText());
      if (def != null) {
        label.setDefinition(def, location, referencePhase);
      }
    }

    return null;
  }

  private IClass resolvePrimitiveType( SymTabAST primitive, Scope location,
    IClass context, boolean referencePhase ) {
    IClass result = LiteralResolver.getResolver().getDefinition(primitive.getType());

    primitive.setDefinition(result, location, referencePhase);
    return result;
  }

  /**
   * Returns the <code>ClassDef</code> of the int primitive type.  This may
   * need to be amended, based on the Java Language spec, to return a long
   * if the literal is larger than an int can hold.
   *
   * @param expression the <code>SymTabAST</code> for the integer literal
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the <code>Scope</code> for the int primitive type
   */
  private IClass resolveNumInt(SymTabAST expression, Scope location,
                               IClass context) {
    return PrimitiveClasses.INT;
  }

  /**
   * Returns the <code>ClassDef</code> type of the float primitive type.
   * This may need to be amended, based on the Java Language spec, to return
   * a double if the literal is larger than a float can hold.
   *
   * @param expression the <code>SymTabAST</code> for the floating point
    literal
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the <code>Scope</code> for the float primitive type
   */
  private IClass resolveNumFloat( SymTabAST expression, Scope location,
      IClass context ) {
   return PrimitiveClasses.DOUBLE;
  }

  /**
   * Returns the <code>ClassDef</code> type of a string literal
   *
   * @param expression the <code>SymTabAST</code> for a string literal
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the <code>Scope</code> type of a string literal
   */
  private IClass resolveStringLiteral( SymTabAST expression, Scope location,
          IClass context ) {
    return LiteralResolver.getResolver().getDefinition(JavaTokenTypes.STRING_LITERAL);
  }

  /**
   * Returns the <code>ClassDef</code> type of a character literal
   *
   * @param expression the <code>SymTabAST</code> for a string literal
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the <code>Scope</code> type of a character literal
   */
  private IClass resolveCharLiteral( SymTabAST expression, Scope location,
    IClass context ) {
    return LiteralResolver.getResolver().getDefinition(JavaTokenTypes.LITERAL_char);
  }

  /**
   * Describe <code>resolveBooleanLiteral</code> method here.
   *
   * @param expression the <code>SymTabAST</code> of the expression
   * @param location the <code>Scope</code> where the expression occurs
   * @param context the <code>Scope</code> in which the expression occurs
   *                (where the search for a defintion begins)
   *
   * @return the <code>Scope</code> for the boolean primitive.
   */
  private IClass resolveBooleanLiteral( SymTabAST expression, Scope location,
     IClass context ) {
    return LiteralResolver.getDefinition(JavaTokenTypes.LITERAL_boolean);
  }

  private IClass resolveBooleanUnary(SymTabAST expression,
           Scope location,
           IClass context,
           boolean referencePhase) {
    SymTabAST child = (SymTabAST)expression.getFirstChild();
    resolveExpression(child, location, context, referencePhase);

    return LiteralResolver.getDefinition(JavaTokenTypes.LITERAL_boolean);
  }

  /**
   * Resolves a constructor call.
   *
   * @param tree the root node of the constructor call
   * @return the <code>ClassDef</code> for the class instantiated by the
   *         constructor
   */
  private void resolveArrayInitializer(SymTabAST initializerNode, Scope location,
           IClass context, boolean referencePhase ) {
    SymTabAST child = (SymTabAST)(initializerNode.getFirstChild());
    while ( child != null ) {
      resolveExpression( child, location, context, referencePhase );
      child = (SymTabAST)(child.getNextSibling());
    }
  }

  /**
   * Resolves a constructor call.
   *
   * @param tree the root node of the constructor call
   * @return the <code>ClassDef</code> for the class instantiated by the
   *         constructor
   */
  private void resolveThrowExpression( SymTabAST throwNode, Scope location,
          IClass context, boolean referencePhase ) {


    SymTabAST nameNode = (SymTabAST)(throwNode.getFirstChild());
    resolveExpression(nameNode, location, context, referencePhase);
  }

  private IClass resolveShiftOperator(SymTabAST expression,
                                       Scope location,
                                       IClass context,
                                       boolean referencePhase) {
    IClass result = null;

    SymTabAST leftChild = (SymTabAST)expression.getFirstChild();
    SymTabAST rightChild = (SymTabAST)leftChild.getNextSibling();

    result = resolveExpression(leftChild, location, context, referencePhase);
    resolveExpression(rightChild, location, context, referencePhase);

    result = PrimitiveClasses.unaryPromote(result);

    return result;
  }

  private IClass resolveBitwiseNot(SymTabAST expression,
                                   Scope location,
                                   IClass context,
                                   boolean referencePhase) {
    IClass result = null;
    SymTabAST child = (SymTabAST)expression.getFirstChild();
    result = resolveExpression(child, location, context, referencePhase);

    result = PrimitiveClasses.unaryPromote(result);

    return result;
  }

}
