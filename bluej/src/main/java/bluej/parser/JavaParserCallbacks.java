/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2016,2017,2021,2022  Michael Kolling and John Rosenberg
 
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
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;


import bluej.parser.lexer.LocatableToken;

import java.util.List;

public class JavaParserCallbacks
{
    /**
     * Found a package X; statement
     * @param token The "package" token
     */
    protected void beginPackageStatement(LocatableToken token) {  }

    /**
     * We have the package name for this source, from a package statement at the top of the file.
     * @param pkgTokens The tokens making up the package name (including the dots)
     */
    protected void gotPackage(List<LocatableToken> pkgTokens) { }

    /**
     * We've seen the semicolon at the end of a "package" statement.
     * @param token The semicolon token
     */
    protected void gotPackageSemi(LocatableToken token) { }

    /** Saw a modifier (public,private etc) */
    protected void gotModifier(LocatableToken token) { }

    /**
     * Modifiers were consumed. This is called after the entity to which the modifiers apply
     * has been identified (eg gotTypeDef() called)
     */
    protected void modifiersConsumed() { }

    /** Beginning of some arbitrary grammatical element */
    protected void beginElement(LocatableToken token) { }

    /** End of some arbitrary grammatical element.
     *
     * @param token  The end token 
     * @param included  True if the end token is part of the element; false if it is part of the next element.
     */
    protected void endElement(LocatableToken token, boolean included) { }

    /**
     * Got the beginning (opening brace) of a method or constructor body.
     */
    protected void beginMethodBody(LocatableToken token) { }

    /**
     * End of a method or constructor body reached.
     */
    protected void endMethodBody(LocatableToken token, boolean included) { }

    /** End of a method or constructor declaration */
    protected void endMethodDecl(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }

    /**
     * Reached a compilation unit state.
     * State 1 = package statement parsed. State 2 = one or more type definitions parsed
     */
    protected void reachedCUstate(int i) { }

    /**
     * Finished parsing a compilation unit.
     * @param state Our last state: see reachedCUState for details
     */
    protected void finishedCU(int state) { }

    /** We've seen the semicolon at the end of an "import" statement */
    protected void gotImportStmtSemi(LocatableToken token)
    {
        endElement(token, true);
    }

    protected void beginForLoop(LocatableToken token) { beginElement(token); }

    protected void beginForLoopBody(LocatableToken token) { }

    protected void endForLoopBody(LocatableToken token, boolean included) { }

    protected void endForLoop(LocatableToken token, boolean included) { }

    protected void beginWhileLoop(LocatableToken token) { }

    protected void beginWhileLoopBody(LocatableToken token) { }

    protected void endWhileLoopBody(LocatableToken token, boolean included) { }

    protected void endWhileLoop(LocatableToken token, boolean included) { }

    protected void beginIfStmt(LocatableToken token) { }

    /** Begin an "if" conditional block (the part that is executed conditionally) */
    protected void beginIfCondBlock(LocatableToken token) { }

    protected void endIfCondBlock(LocatableToken token, boolean included) { }

    protected void gotElseIf(LocatableToken token) {}

    protected void endIfStmt(LocatableToken token, boolean included) { }

    protected void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) { }

    protected void beginSwitchBlock(LocatableToken token) { }

    protected void endSwitchBlock(LocatableToken token) { }

    protected void endSwitchStmt(LocatableToken token, boolean included) { }

    protected void beginDoWhile(LocatableToken token) { beginElement(token); }

    protected void beginDoWhileBody(LocatableToken token) { }

    protected void endDoWhileBody(LocatableToken token, boolean included) { }

    protected void endDoWhile(LocatableToken token, boolean included) { }

    protected void beginTryCatchSmt(LocatableToken token, boolean hasResource) { }

    protected void beginTryBlock(LocatableToken token) { }

    protected void endTryBlock(LocatableToken token, boolean included) { }

    protected void endTryCatchStmt(LocatableToken token, boolean included) { }

    protected void beginSynchronizedBlock(LocatableToken token) { }

    protected void endSynchronizedBlock(LocatableToken token, boolean included) { }

    /** A list of a parameters to a method or constructor */
    protected void beginArgumentList(LocatableToken token) { }

    /** An individual argument has ended */
    protected void endArgument() { }

    /** The end of the argument list has been reached. */
    protected void endArgumentList(LocatableToken token) { }

    /**
     * got a "new ..." expression. Will be followed by a type spec (gotTypeSpec())
     * and possibly by array size declarations, then endExprNew()
     */
    protected void gotExprNew(LocatableToken token) { }

    protected void endExprNew(LocatableToken token, boolean included) { }

    protected void beginArrayInitList(LocatableToken token) { }

    protected void endArrayInitList(LocatableToken token) { }

    /** An anonymous class body. Preceded by a type spec (see gotTypeSpec()) except in the case of an enum member body. */
    protected void beginAnonClassBody(LocatableToken token, boolean isEnumMember) { }

    protected void endAnonClassBody(LocatableToken token, boolean included) { }

    /**
     * Beginning of a statement block. This includes anonymous statement blocks, and static
     * initializer blocks
     */
    protected void beginStmtblockBody(LocatableToken token)
    {
        beginElement(token);
    }

    protected void endStmtblockBody(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }

    /**
     * Begin a (possibly static) initialisation block.
     * @param first   The first token (should be either "static" or the "{")
     * @param lcurly  The "{" token which opens the block body
     */
    protected void beginInitBlock(LocatableToken first, LocatableToken lcurly) { }

    /**
     * End of a (possibly static) initialisation block
     * @param rcurly    The last token (should be "}")
     * @param included  True if the last token is actually a "}"
     */
    protected void endInitBlock(LocatableToken rcurly, boolean included) { }

    /** Begin the type definition body. */
    protected void beginTypeBody(LocatableToken leftCurlyToken) { }

    /** End of type definition body. This should be a '}' unless an error occurred */
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included) { }

    /**
     * Got the beginning of a declaration - either a type, a field/variable, or a
     * method constructor, or an initialisation block. This will be followed by one of:
     *
     * <ul>
     * <li>gotTypeDef(...) - if a type definition
     * <li>gotMethodDeclaration(...) - if a method declaration
     * <li>gotConstructorDecl(...) - if a constructor declaration
     * <li>beginInitBlock(...) - if an initialiser block
     * <li>beginFieldDeclarations(...) - if a field declaration
     * <li>beginVariableDecl(...) - if a variable declaration
     * <li>endDecl(...) - if not a valid declaration
     * </ul>
     */
    protected void gotDeclBegin(LocatableToken token) { beginElement(token); }

    /**
     * End a declaration (unsuccessfully).
     */
    protected void endDecl(LocatableToken token) { endElement(token, false); }

    /**
     * Called when the current element is recognised as a type definition.
     * @param tdType  one of TYPEDEF_CLASS, _INTERFACE, _ANNOTATION or _ENUM
     */
    protected void gotTypeDef(LocatableToken firstToken, int tdType) { }

    /** Called when we have the identifier token for a class/interface/enum definition */
    protected void gotTypeDefName(LocatableToken nameToken) { }

    /** Called when we have seen the "extends" literal token */
    protected void beginTypeDefExtends(LocatableToken extendsToken) { }

    /** Called after we have seen the last type in an "extends" type list */
    protected void endTypeDefExtends() { }

    /** Called when we have seen the "implements" literal token */
    protected void beginTypeDefImplements(LocatableToken implementsToken) { }

    /** Called after we have seen the last type in an "implements" type list */
    protected void endTypeDefImplements() { }

    /** Called when we have seen the "permits" literal token */
    protected void beginTypeDefPermits(LocatableToken permitsToken) { }

    /** Called after we have seen the last type in a "permits" type list */
    protected void endTypeDefPermits() { }

    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }

    /**
     * Got a variable declaration, which might declare multiple variables. Each
     * variable will generate gotVariable() or gotSubsequentVar().
     * @param first  The first token in the declaration
     */
    protected void beginVariableDecl(LocatableToken first) { }

    /**
     * Got the (first) variable in a variable declaration.
     * @param first    The first token in the declaration
     * @param idToken  The token with the variable identifier
     * @param inited   Whether the variable is initialized as part of the declaration
     */
    protected void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) { }

    protected void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) { }

    protected void endVariable(LocatableToken token, boolean included) { }

    protected void endVariableDecls(LocatableToken token, boolean included) { }

    protected void beginForInitDecl(LocatableToken first) { }

    protected void gotForInit(LocatableToken first, LocatableToken idToken) { }

    protected void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) { }

    protected void endForInit(LocatableToken token, boolean included) { }

    protected void endForInitDecls(LocatableToken token, boolean included) { }

    /**
     * Got a field declaration, which might declare multiple fields. Each field will generate
     * gotField() or gotSubsequentField().
     * @param first  The first token in the declaration
     *
     */
    protected void beginFieldDeclarations(LocatableToken first) { }

    /**
     * Got a field (inside a type definition).
     * @param first     The first token that forms part of the field declaration
     * @param idToken   The token with the name of the field.
     * @param initExpressionFollows
     */
    protected void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) { }

    protected void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) { }

    /** End a single field declaration (but not necessarily the field declaration statement) */
    protected void endField(LocatableToken token, boolean included) { }

    /** End a field declaration statement */
    protected void endFieldDeclarations(LocatableToken token, boolean included) { }

    /** We've seen a type specification or something that looks a lot like one. */
    protected void gotTypeSpec(List<LocatableToken> tokens) { }

    /** Seen a type cast operator. The tokens list contains the type to which is cast. */
    protected void gotTypeCast(List<LocatableToken> tokens)
    {
        gotTypeSpec(tokens);
    }

    /** Saw the beginning of an expression */
    protected void beginExpression(LocatableToken token, boolean isLambdaBody) { }

    /** Reached the end of an expression. The given token is the first one past the end. */
    protected void endExpression(LocatableToken token, boolean emptyExpression) { }

    /** Saw a literal as part of an expression */
    protected void gotLiteral(LocatableToken token) { }

    /**
     * Saw a primitive type literal in an expression; usually occurs as "int.class"
     * or "int[].class" for example.
     * @param token  The primitive token
     */
    protected void gotPrimitiveTypeLiteral(LocatableToken token) { }

    /** Saw an identifier as (part of) an expression */
    protected void gotIdentifier(LocatableToken token) { }
    /**
     * Got an identifier (possibly part of a compound identifier) immediately followed by
     * end of input stream.
     */
    protected void gotIdentifierEOF(LocatableToken token) { gotIdentifier(token); }

    protected void gotMemberAccessEOF(LocatableToken token) { gotMemberAccess(token); }

    protected void gotCompoundIdent(LocatableToken token) { gotIdentifier(token); }
    protected void gotCompoundComponent(LocatableToken token) { gotMemberAccess(token); }
    protected void completeCompoundValue(LocatableToken token) { gotMemberAccess(token); }
    protected void completeCompoundValueEOF(LocatableToken token) { completeCompoundValue(token); }
    protected void completeCompoundClass(LocatableToken token) { gotMemberAccess(token); }

    protected void gotMemberAccess(LocatableToken token) { }

    /** Saw a member method call (expr.methodName()), token is the method name; arguments to follow */
    protected void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) { }

    /** Saw a "naked" method call - "methodName(...)" */
    protected void gotMethodCall(LocatableToken token) { }

    /** Saw a call to the constructor as this(...) or super(...) */
    protected void gotConstructorCall(LocatableToken token) { }

    /** Saw a dot operator followed by end-of-file */
    protected void gotDotEOF(LocatableToken token)
    {
        gotBinaryOperator(token);
    }

    protected void gotStatementExpression() { }

    protected void gotClassLiteral(LocatableToken token) { }

    /** Saw a binary operator as part of an expression */
    protected void gotBinaryOperator(LocatableToken token) { }

    protected void gotUnaryOperator(LocatableToken token) { }

    /** Saw a "?" operator. This will be followed by the left-hand-side expression
     * (demarked by beginExpression() and endExpression(), then gotQuestionColon) followed by a continuation
     * of the current expression (for the right-hand-side).
     */
    protected void gotQuestionOperator(LocatableToken token) { }

    protected void gotQuestionColon(LocatableToken token) { }

    /**
     * Saw the "instanceof" operator. The type spec will follow.
     */
    protected void gotInstanceOfOperator(LocatableToken token) { }

    /**
     * Saw a var name following an "instanceof". Called after gotInstanceOfOperator and gotTypeSpec.
     */
    protected void gotInstanceOfVar(LocatableToken token) { }

    protected void gotArrayElementAccess() { }

    protected void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) { }

    protected void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) { }

    /**
     * We've seen a constructor declaration. The token supplied is the constructor name.
     * The hiddenToken is the comment before the constructor.
     */
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {}

    /**
     * We've seen a method declaration; the token parameter is the method name;
     * the hiddenToken parameter is the comment before the method
     */
    protected void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {}

    /**
     * We saw a method (or constructor) parameter. The given token specifies the parameter name. 
     * The last type parsed by parseTypeSpec(boolean) is the parameter type, after any additonal
     * array declarators (see gotArrayDeclarator()) are applied.
     *
     * @param token   The token giving the parameter name
     * @param ellipsisToken  The token, if any, with the ellipsis indicating a varargs parameter. May be null.
     */
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) { }

    /**
     * Called when, after a parameter/field/variable name, array declarators "[]" are seen.
     * Will be called once for each set of "[]", immediately before gotField() or equivalent
     * is called.
     */
    protected void gotArrayDeclarator() { }

    /**
     * Called for the array components when we get "new xyz[]".
     */
    protected void gotNewArrayDeclarator(boolean withDimension) { }

    protected void gotAllMethodParameters() { }

    /**
     * Saw a type parameter for a class or method. If for a method, will be bracketed by
     * calls to {@code gotMethodTypeParamsBegin} and {@code endMethodTypeParams}
     * @param idToken  The token with the type parameter identifier
     */
    protected void gotTypeParam(LocatableToken idToken) { }

    protected void gotTypeParamBound(List<LocatableToken> tokens) { }

    protected void gotMethodTypeParamsBegin() { }

    protected void endMethodTypeParams() { }

    /**
     * Called by the lexer when it sees a comment.
     */
    public void gotComment(LocatableToken token) { }


    protected void gotInnerType(LocatableToken start)
    {
    }


    protected void beginThrows(LocatableToken token) { }
    protected void endThrows() { }

    protected void gotTopLevelDecl(LocatableToken token)
    {
    }


    protected void gotSwitchCase() { }

    protected void gotSwitchDefault() { }

    protected void gotThrow(LocatableToken token) { }

    protected void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) { }

    protected void gotReturnStatement(boolean hasValue) { }

    protected void gotYieldStatement() { }

    protected void gotEmptyStatement() { }

    protected void gotCatchFinally(LocatableToken token) { }

    protected void gotMultiCatch(LocatableToken token) { }

    protected void gotCatchVarName(LocatableToken token) { }

    protected void gotAssert() { }

    protected void gotForTest(boolean isPresent) { }
    protected void gotForIncrement(boolean isPresent) { }

    protected void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) { }


    protected void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow)
    {
    }


    /**
     * A lambda expression has been found and we are about to parse its body (the part after ->).
     * If lambdaIsBlock, a statement block body follows, otherwise an expression follows.
     */
    protected void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) { }

    /**
     * The end of the lambda body has been reached (either block or expression)
     */
    protected void endLambdaBody(LocatableToken closeCurly) { }

    protected void gotPostOperator(LocatableToken token) { }

    protected void gotArrayTypeIdentifier(LocatableToken token)
    {
        gotIdentifier(token);
    }

    protected void gotParentIdentifier(LocatableToken token)
    {
        gotIdentifier(token);
    }


    /**
     * Called when we find a lambda formal parameter, i.e. declaration of a parameter
     * like (int x) ->
     */
    protected void gotLambdaFormalParam() { }
    /**
     * Called when we find a lambda formal parameter name, i.e. the "x" in the 
     * declaration of a parameter like (int x)
     */
    protected void gotLambdaFormalName(LocatableToken name) { }
    /**
     * Called when we find a lambda formal parameter name, i.e. the "List&lt;Integer&gt;" in the 
     * declaration of a parameter like (List&lt;Integer&gt; x)
     */
    protected void gotLambdaFormalType(List<LocatableToken> type) { }

    protected void beginFormalParameter(LocatableToken token) { }

    /**
     * Called at the beginning of the record parameters in a header, i.e. the opening parenthesis
     * in record Point(int x, int y)
     * @param parenToken The opening-parenthesis token
     */
    protected void beginRecordParameters(LocatableToken parenToken) {}

    /**
     * Called when a record parameter has been encountered in a record header.
     * @param first The first token of that record parameter, either a modifier or the type.
     * @param idToken The token for the identifier (name) of the parameter.
     * @param varargsToken The token for the varargs on the parameter, or null if this is not a varargs parameter.
     */
    protected void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {}

    /**
     * Called at the end of the record parameters in a header, i.e. the closing parenthesis
     * in record Point(int x, int y)
     * @param closeParen The close-parenthesis token
     */
    protected void endRecordParameters(LocatableToken closeParen) {}
}
