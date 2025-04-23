/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009, 2012,2014,2022,2024  Michael Kolling and John Rosenberg
 
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

/**
 * This class defines callback methods that are called during parsing of Kotlin source.
 * Most methods have empty implementations and are intended to be overridden by subclasses.
 * 
 */
public class KotlinParserCallbacks
{
    public void beginPackageStatement(LocatableToken token)
    {
    }
    
    public void gotPackage(List<LocatableToken> pkgTokens)
    {
    }
    
    public void gotPackageSemi(LocatableToken token)
    {
    }
    
    public void gotModifier(LocatableToken token) {}
    
    public void modifiersConsumed()
    {
    }
    
    public void beginElement(LocatableToken token) {}
    
    public void endElement(LocatableToken token, boolean included)
    {
    }
    
    public void beginMethodBody(LocatableToken token)
    {
    }
    
    public void endMethodBody(LocatableToken token, boolean included)
    {
    }
    
    public void endMethodDecl(LocatableToken token, boolean included)
    {
    }
    
    public void reachedCUstate(int i)
    {
    }
    
    public void finishedCU(int state)
    {
    }
    
    public void gotImportStmtSemi(LocatableToken token)
    {
    }
    
    public void beginForLoop(LocatableToken token) {}
    
    public void beginForLoopBody(LocatableToken token) {}
    
    public void endForLoopBody(LocatableToken token, boolean included) {}
    
    public void endForLoop(LocatableToken token, boolean included) {}
    
    public void beginWhileLoop(LocatableToken token) {}
    
    public void beginWhileLoopBody(LocatableToken token) {}
    
    public void endWhileLoopBody(LocatableToken token, boolean included) {}
    
    public void endWhileLoop(LocatableToken token, boolean included) {}
    
    public void beginIfStmt(LocatableToken token) {}
    
    public void beginIfCondBlock(LocatableToken token) {}
    
    public void endIfCondBlock(LocatableToken token, boolean included) {}
    
    public void gotElseIf(LocatableToken token) {}
    
    public void endIfStmt(LocatableToken token, boolean included) {}
    
    public void beginWhenStmt(LocatableToken token) {}
    
    public void beginWhenBlock(LocatableToken token) {}
    
    public void endWhenBlock(LocatableToken token) {}
    
    public void endWhenStmt(LocatableToken token, boolean included) {}
    
    public void beginDoWhile(LocatableToken token) {}
    
    public void beginDoWhileBody(LocatableToken token) {}
    
    public void endDoWhileBody(LocatableToken token, boolean included) {}
    
    public void endDoWhile(LocatableToken token, boolean included) {}
    
    public void beginTryCatchSmt(LocatableToken token, boolean hasResource) {}
    
    public void beginTryBlock(LocatableToken token) {}
    
    public void endTryBlock(LocatableToken token, boolean included) {}
    
    public void endTryCatchStmt(LocatableToken token, boolean included) {}
    
    public void beginArgumentList(LocatableToken token) {}
    
    public void endArgument() {}
    
    public void endArgumentList(LocatableToken token) {}
    
    public void gotExprNew(LocatableToken token) {}
    
    public void endExprNew(LocatableToken token, boolean included) {}
    
    public void beginArrayInitList(LocatableToken token) {}
    
    public void endArrayInitList(LocatableToken token) {}
    
    public void beginAnonClassBody(LocatableToken token, boolean isEnumMember) {}
    
    public void endAnonClassBody(LocatableToken token, boolean included) {}
    
    public void beginStmtblockBody(LocatableToken token) {}
    
    public void endStmtblockBody(LocatableToken token, boolean included) {}
    
    public void beginInitBlock(LocatableToken first, LocatableToken lcurly) {}
    
    public void endInitBlock(LocatableToken rcurly, boolean included) {}
    
    public void beginTypeBody(LocatableToken leftCurlyToken) {}
    
    public void endTypeBody(LocatableToken endCurlyToken, boolean included) {}
    
    public void gotDeclBegin(LocatableToken token) {}
    
    public void endDecl(LocatableToken token) {}
    
    public void gotTypeDef(LocatableToken firstToken, int tdType) {}
    
    public void gotTypeDefName(LocatableToken nameToken) {}
    
    public void beginTypeDefExtends(LocatableToken extendsToken) {}
    
    public void endTypeDefExtends() {}
    
    public void beginTypeDefImplements(LocatableToken implementsToken) {}
    
    public void endTypeDefImplements() {}
    
    public void gotTypeDefEnd(LocatableToken token, boolean included) {}
    
    public void beginVariableDecl(LocatableToken first) {}
    
    public void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) {}
    
    public void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) {}
    
    public void endVariable(LocatableToken token, boolean included) {}
    
    public void endVariableDecls(LocatableToken token, boolean included) {}
    
    public void beginForInitDecl(LocatableToken first) {}
    
    public void gotForInit(LocatableToken first, LocatableToken idToken) {}
    
    public void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) {}
    
    public void endForInit(LocatableToken token, boolean included) {}
    
    public void endForInitDecls(LocatableToken token, boolean included) {}
    
    public void beginFieldDeclarations(LocatableToken first) {}
    
    public void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {}
    
    public void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) {}
    
    public void endField(LocatableToken token, boolean included) {}
    
    public void endFieldDeclarations(LocatableToken token, boolean included) {}
    
    public void gotTypeSpec(List<LocatableToken> tokens) {}
    
    public void gotTypeCast(List<LocatableToken> tokens) {}
    
    public void beginExpression(LocatableToken token, boolean isLambdaBody) {}
    
    public void endExpression(LocatableToken token, boolean emptyExpression) {}
    
    public void gotLiteral(LocatableToken token) {}
    
    public void gotPrimitiveTypeLiteral(LocatableToken token) {}
    
    public void gotIdentifier(LocatableToken token) {}
    
    public void gotIdentifierEOF(LocatableToken token) {}
    
    public void gotMemberAccessEOF(LocatableToken token) {}
    
    public void gotCompoundIdent(LocatableToken token) {}
    
    public void gotCompoundComponent(LocatableToken token) {}
    
    public void completeCompoundValue(LocatableToken token) {}
    
    public void completeCompoundValueEOF(LocatableToken token) {}
    
    public void completeCompoundClass(LocatableToken token) {}
    
    public void gotMemberAccess(LocatableToken token) {}
    
    public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {}
    
    public void gotMethodCall(LocatableToken token) {}
    
    public void gotConstructorCall(LocatableToken token) {}
    
    public void gotDotEOF(LocatableToken token) {}
    
    public void gotStatementExpression() {}
    
    public void gotClassLiteral(LocatableToken token) {}
    
    public void gotBinaryOperator(LocatableToken token) {}
    
    public void gotUnaryOperator(LocatableToken token) {}
    
    public void gotQuestionOperator(LocatableToken token) {}
    
    public void gotQuestionColon(LocatableToken token) {}
    
    public void gotIsOperator(LocatableToken token) {}
    
    public void gotAsOperator(LocatableToken token) {}
    
    public void gotArrayElementAccess() {}
    
    public void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) {}
    
    public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) {}
    
    public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {}
    
    public void gotFunctionDeclaration(LocatableToken token, LocatableToken hiddenToken) {}
    
    public void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) {}
    
    public void gotArrayDeclarator() {}
    
    public void gotNewArrayDeclarator(boolean withDimension) {}
    
    public void gotAllMethodParameters() {}
    
    public void gotTypeParam(LocatableToken idToken) {}
    
    public void gotTypeParamBound(List<LocatableToken> tokens) {}
    
    public void gotMethodTypeParamsBegin() {}
    
    public void endMethodTypeParams() {}
    
    public void gotComment(LocatableToken token) {}
    
    public void gotInnerType(LocatableToken start) {}
    
    public void beginWhenCase(LocatableToken token) {}
    
    public void gotWhenCaseType(LocatableToken token, boolean isArrowSyntax) {}
    
    public void endWhenCase(LocatableToken token, boolean wasArrowSyntax) {}
    
    public void gotWhenElse() {}
    
    public void gotThrow(LocatableToken token) {}
    
    public void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) {}
    
    public void gotReturnStatement(boolean hasValue) {}
    
    public void gotEmptyStatement() {}
    
    public void gotCatchFinally(LocatableToken token) {}
    
    public void gotMultiCatch(LocatableToken token) {}
    
    public void gotCatchVarName(LocatableToken token) {}
    
    public void gotForTest(boolean isPresent) {}
    
    public void gotForIncrement(boolean isPresent) {}
    
    public void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) {}
    
    public void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) {}
    
    public void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) {}
    
    public void endLambdaBody(LocatableToken closeCurly) {}
    
    public void gotPostOperator(LocatableToken token) {}
    
    public void gotArrayTypeIdentifier(LocatableToken token) {}
    
    public void gotParentIdentifier(LocatableToken token) {}
    
    public void gotLambdaFormalParam() {}
    
    public void gotLambdaFormalName(LocatableToken name) {}
    
    public void gotLambdaFormalType(List<LocatableToken> type) {}
    
    public void beginFormalParameter(LocatableToken token) {}
    
    // Kotlin-specific callbacks
    
    public void gotPropertyDeclaration(LocatableToken token, boolean isVal) {}
    
    public void gotPropertyName(LocatableToken token) {}
    
    public void gotPropertyType(List<LocatableToken> tokens) {}
    
    public void gotPropertyInitializer(LocatableToken token) {}
    
    public void endPropertyDeclaration(LocatableToken token) {}
    
    public void beginFunctionDeclaration(LocatableToken token) {}
    
    public void gotFunctionName(LocatableToken token) {}
    
    public void gotFunctionReturnType(List<LocatableToken> tokens) {}
    
    public void endFunctionDeclaration(LocatableToken token) {}
    
    public void beginCompanionObject(LocatableToken token) {}
    
    public void endCompanionObject(LocatableToken token) {}
    
    public void beginObjectDeclaration(LocatableToken token) {}
    
    public void gotObjectName(LocatableToken token) {}
    
    public void endObjectDeclaration(LocatableToken token) {}
    
    public void beginDataClass(LocatableToken token) {}
    
    public void endDataClass(LocatableToken token) {}
    
    public void beginTypeAlias(LocatableToken token) {}
    
    public void gotTypeAliasName(LocatableToken token) {}
    
    public void gotTypeAliasType(List<LocatableToken> tokens) {}
    
    public void endTypeAlias(LocatableToken token) {}
}