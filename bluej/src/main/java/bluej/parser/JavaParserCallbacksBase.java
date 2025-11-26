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

public class JavaParserCallbacksBase { //  implements JavaParserCallbacks {
    // @Override
    protected void beginPackageStatement(LocatableToken token) {  }

    // @Override
    protected void gotPackage(List<LocatableToken> pkgTokens) { }

    // @Override
    protected void gotPackageSemi(LocatableToken token) { }

    // @Override
    protected void gotModifier(LocatableToken token) { }

    // @Override
    protected void modifiersConsumed() { }

    // @Override
    protected void beginElement(LocatableToken token) { }

    // @Override
    protected void endElement(LocatableToken token, boolean included) { }

    // @Override
    protected void beginMethodBody(LocatableToken token) { }

    // @Override
    protected void endMethodBody(LocatableToken token, boolean included) { }

    // @Override
    protected void endMethodDecl(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }

    // @Override
    protected void reachedCUstate(int i) { }

    // @Override
    protected void finishedCU(int state) { }

    // @Override
    protected void gotImportStmtSemi(LocatableToken token)
    {
        endElement(token, true);
    }

    // @Override
    protected void beginForLoop(LocatableToken token) { beginElement(token); }

    // @Override
    protected void beginForLoopBody(LocatableToken token) { }

    // @Override
    protected void endForLoopBody(LocatableToken token, boolean included) { }

    // @Override
    protected void endForLoop(LocatableToken token, boolean included) { }

    // @Override
    protected void beginWhileLoop(LocatableToken token) { }

    // @Override
    protected void beginWhileLoopBody(LocatableToken token) { }

    // @Override
    protected void endWhileLoopBody(LocatableToken token, boolean included) { }

    // @Override
    protected void endWhileLoop(LocatableToken token, boolean included) { }

    // @Override
    protected void beginIfStmt(LocatableToken token) { }

    // @Override
    protected void beginIfCondBlock(LocatableToken token) { }

    // @Override
    protected void endIfCondBlock(LocatableToken token, boolean included) { }

    // @Override
    protected void gotElseIf(LocatableToken token) {}

    // @Override
    protected void endIfStmt(LocatableToken token, boolean included) { }

    // @Override
    protected void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) { }

    // @Override
    protected void beginSwitchBlock(LocatableToken token) { }

    // @Override
    protected void endSwitchBlock(LocatableToken token) { }

    // @Override
    protected void endSwitchStmt(LocatableToken token, boolean included) { }

    // @Override
    protected void beginDoWhile(LocatableToken token) { beginElement(token); }

    // @Override
    protected void beginDoWhileBody(LocatableToken token) { }

    // @Override
    protected void endDoWhileBody(LocatableToken token, boolean included) { }

    // @Override
    protected void endDoWhile(LocatableToken token, boolean included) { }

    // @Override
    protected void beginTryCatchStmt(LocatableToken token, boolean hasResource) { }

    // @Override
    protected void beginTryBlock(LocatableToken token) { }

    // @Override
    protected void endTryBlock(LocatableToken token, boolean included) { }

    // @Override
    protected void endTryCatchStmt(LocatableToken token, boolean included) { }

    // @Override
    protected void beginSynchronizedBlock(LocatableToken token) { }

    // @Override
    protected void endSynchronizedBlock(LocatableToken token, boolean included) { }

    // @Override
    protected void beginArgumentList(LocatableToken token) { }

    // @Override
    protected void endArgument() { }

    // @Override
    protected void endArgumentList(LocatableToken token) { }

    // @Override
    protected void gotExprNew(LocatableToken token) { }

    // @Override
    protected void endExprNew(LocatableToken token, boolean included) { }

    // @Override
    protected void beginArrayInitList(LocatableToken token) { }

    // @Override
    protected void endArrayInitList(LocatableToken token) { }

    // @Override
    protected void beginAnonClassBody(LocatableToken token, boolean isEnumMember) { }

    // @Override
    protected void endAnonClassBody(LocatableToken token, boolean included) { }

    // @Override
    protected void beginStmtblockBody(LocatableToken token)
    {
        beginElement(token);
    }

    // @Override
    protected void endStmtblockBody(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }

    // @Override
    protected void beginInitBlock(LocatableToken first, LocatableToken lcurly) { }

    // @Override
    protected void endInitBlock(LocatableToken rcurly, boolean included) { }

    // @Override
    protected void beginTypeBody(LocatableToken leftCurlyToken) { }

    // @Override
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included) { }

    // @Override
    protected void gotDeclBegin(LocatableToken token) { beginElement(token); }

    // @Override
    protected void endDecl(LocatableToken token) { endElement(token, false); }

    // @Override
    protected void gotTypeDef(LocatableToken firstToken, int tdType) { }

    // @Override
    protected void gotTypeDefName(LocatableToken nameToken) { }

    // @Override
    protected void beginTypeDefExtends(LocatableToken extendsToken) { }

    // @Override
    protected void endTypeDefExtends() { }

    // @Override
    protected void beginTypeDefImplements(LocatableToken implementsToken) { }

    // @Override
    protected void endTypeDefImplements() { }

    // @Override
    protected void beginTypeDefPermits(LocatableToken permitsToken) { }

    // @Override
    protected void endTypeDefPermits() { }

    // @Override
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }

    // @Override
    protected void beginVariableDecl(LocatableToken first) { }

    // @Override
    protected void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) { }

    // @Override
    protected void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) { }

    // @Override
    protected void endVariable(LocatableToken token, boolean included) { }

    // @Override
    protected void endVariableDecls(LocatableToken token, boolean included) { }

    // @Override
    protected void beginForInitDecl(LocatableToken first) { }

    // @Override
    protected void gotForInit(LocatableToken first, LocatableToken idToken) { }

    // @Override
    protected void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) { }

    // @Override
    protected void endForInit(LocatableToken token, boolean included) { }

    // @Override
//    protected void endForInitDecl(LocatableToken token, boolean included) {
//        endForInit(token, included);
//    }

    // @Override
    protected void endForInitDecls(LocatableToken token, boolean included) { }

    // @Override
    protected void beginFieldDeclarations(LocatableToken first) { }

    // @Override
    protected void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) { }

    // @Override
    protected void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) { }

    // @Override
    protected void endField(LocatableToken token, boolean included) { }

    // @Override
    protected void endFieldDeclarations(LocatableToken token, boolean included) { }

    // @Override
    protected void gotTypeSpec(List<LocatableToken> tokens) { }

    // @Override
    protected void gotTypeCast(List<LocatableToken> tokens)
    {
        gotTypeSpec(tokens);
    }

    // @Override
    protected void beginExpression(LocatableToken token, boolean isLambdaBody) { }

    // @Override
    protected void endExpression(LocatableToken token, boolean emptyExpression) { }

    // @Override
    protected void gotLiteral(LocatableToken token) { }

    // @Override
    protected void gotPrimitiveTypeLiteral(LocatableToken token) { }

    // @Override
    protected void gotIdentifier(LocatableToken token) { }
    // @Override
    protected void gotIdentifierEOF(LocatableToken token) { gotIdentifier(token); }

    // @Override
    protected void gotMemberAccessEOF(LocatableToken token) { gotMemberAccess(token); }

    // @Override
    protected void gotCompoundIdent(LocatableToken token) { gotIdentifier(token); }
    // @Override
    protected void gotCompoundComponent(LocatableToken token) { gotMemberAccess(token); }
    // @Override
    protected void completeCompoundValue(LocatableToken token) { gotMemberAccess(token); }
    // @Override
    protected void completeCompoundValueEOF(LocatableToken token) { completeCompoundValue(token); }
    // @Override
    protected void completeCompoundClass(LocatableToken token) { gotMemberAccess(token); }

    // @Override
    protected void gotMemberAccess(LocatableToken token) { }

    // @Override
    protected void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) { }

    // @Override
    protected void gotMethodCall(LocatableToken token) { }

    // @Override
    protected void gotConstructorCall(LocatableToken token) { }

    // @Override
    protected void gotDotEOF(LocatableToken token)
    {
        gotBinaryOperator(token);
    }

    // @Override
    protected void gotStatementExpression() { }

    // @Override
    protected void gotClassLiteral(LocatableToken token) { }

    // @Override
    protected void gotBinaryOperator(LocatableToken token) { }

    // @Override
    protected void gotUnaryOperator(LocatableToken token) { }

    // @Override
    protected void gotQuestionOperator(LocatableToken token) { }

    // @Override
    protected void gotQuestionColon(LocatableToken token) { }

    // @Override
    protected void gotInstanceOfOperator(LocatableToken token) { }

    // @Override
    protected void gotInstanceOfVar(LocatableToken token) { }

    // @Override
    protected void gotArrayElementAccess() { }

    // @Override
    protected void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) { }

    // @Override
    protected void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) { }

    // @Override
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {}

    // @Override
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken, String name) {}

    // @Override
    protected void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {}

    // @Override
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) { }

    // @Override
    protected void gotArrayDeclarator() { }

    // @Override
    protected void gotNewArrayDeclarator(boolean withDimension) { }

    // @Override
    protected void gotAllMethodParameters() { }

    // @Override
    protected void gotTypeParam(LocatableToken idToken) { }

    // @Override
    protected void gotTypeParamBound(List<LocatableToken> tokens) { }

    // @Override
    protected void gotMethodTypeParamsBegin() { }

    // @Override
    protected void endMethodTypeParams() { }

    // @Override
    public void gotComment(LocatableToken token) { }
    // TODO: why the heck this one has to be public?


    // @Override
    protected void gotInnerType(LocatableToken start)
    {
    }


    // @Override
    protected void beginThrows(LocatableToken token) { }
    // @Override
    protected void endThrows() { }

    // @Override
    protected void gotTopLevelDecl(LocatableToken token)
    {
    }


    // @Override
    protected void beginSwitchCase(LocatableToken token) { }

    // @Override
    protected void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) { }

    // @Override
    protected void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) { }

    // @Override
    protected void gotSwitchDefault() { }

    // @Override
    protected void gotThrow(LocatableToken token) { }

    // @Override
    protected void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) { }

    // @Override
    protected void gotReturnStatement(boolean hasValue) { }

    // @Override
    protected void gotYieldStatement() { }

    // @Override
    protected void gotEmptyStatement() { }

    // @Override
    protected void gotCatchFinally(LocatableToken token) { }

    // @Override
    protected void gotMultiCatch(LocatableToken token) { }

    // @Override
    protected void gotCatchVarName(LocatableToken token) { }

    // @Override
    protected void gotAssert() { }

    // @Override
    protected void gotForTest(boolean isPresent) { }
    // @Override
    protected void gotForIncrement(boolean isPresent) { }

    // @Override
    protected void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) { }


    // @Override
    protected void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow)
    {
    }


    // @Override
    protected void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) { }

    // @Override
    protected void endLambdaBody(LocatableToken closeCurly) { }

    // @Override
    protected void gotPostOperator(LocatableToken token) { }

    // @Override
    protected void gotArrayTypeIdentifier(LocatableToken token)
    {
        gotIdentifier(token);
    }

    // @Override
    protected void gotParentIdentifier(LocatableToken token)
    {
        gotIdentifier(token);
    }


    // @Override
    protected void gotLambdaFormalParam() { }
    // @Override
    protected void gotLambdaFormalName(LocatableToken name) { }
    // @Override
    protected void gotLambdaFormalType(List<LocatableToken> type) { }

    // @Override
    protected void beginFormalParameter(LocatableToken token) { }

    // @Override
    protected void beginRecordParameters(LocatableToken parenToken) {}

    // @Override
    protected void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {}

    // @Override
    protected void endRecordParameters(LocatableToken closeParen) {}

    // @Override
    protected void error(String msg, int beginLine, int beginCol, int endLine, int endCol)
    {
        throw new ParseFailure("Parse error: (" + beginLine + ":" + beginCol + ") :" + msg);
    }
}
