package bluej.parser.psi.visitor;

import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.psi.CallbackRecorder;
import bluej.parser.psi.JavaParserCallbacks;

import java.util.List;

public class ForwardingCallbackRecorder extends CallbackRecorder {
    private final JavaParserCallbacks delegate;

    public ForwardingCallbackRecorder(JavaParserCallbacks delegate) {
        this.delegate = delegate;
    }
//
//
//    @Override
//    public JavaTokenFilter getTokenStream() {
//        super.getTokenStream();
//        this.delegate.getTokenStream();
//    }
//
//    @Override
//    public LocatableToken getLastToken() {
//        super.getLastToken();
//        this.delegate.getLastToken();
//    }
//
//    @Override
//    public void setLastToken(LocatableToken lastToken) {
//        super.setLastToken(lastToken);
//        this.delegate.setLastToken(lastToken);
//    }
//
//    @Override
//    public void setEmitRangeStart(int line, int column) {
//        super.setEmitRangeStart();
//        this.delegate.setEmitRangeStart();
//    }
//
//    @Override
//    public void clearEmitRangeStart() {
//        super.clearEmitRangeStart();
//        this.delegate.clearEmitRangeStart();
//    }
//
//    @Override
//    public boolean isInEmitRange(int line, int column) {
//        super.isInEmitRange();
//        this.delegate.isInEmitRange();
//    }

    @Override
    public void beginPackageStatement(LocatableToken token) {
        super.beginPackageStatement(token);
        this.delegate.beginPackageStatement(token);
    }

    @Override
    public void gotPackage(List<LocatableToken> pkgTokens) {
        super.gotPackage(pkgTokens);
        this.delegate.gotPackage(pkgTokens);
    }

    @Override
    public void gotPackageSemi(LocatableToken token) {
        super.gotPackageSemi(token);
        this.delegate.gotPackageSemi(token);
    }

    @Override
    public void gotModifier(LocatableToken token) {
        super.gotModifier(token);
        this.delegate.gotModifier(token);
    }

    @Override
    public void modifiersConsumed() {
        super.modifiersConsumed();
        this.delegate.modifiersConsumed();
    }

    @Override
    public void beginElement(LocatableToken token) {
        super.beginElement(token);
        this.delegate.beginElement(token);
    }

    @Override
    public void endElement(LocatableToken token, boolean included) {
        super.endElement(token, included);
        this.delegate.endElement(token, included);
    }

    @Override
    public void beginMethodBody(LocatableToken token) {
        super.beginMethodBody(token);
        this.delegate.beginMethodBody(token);
    }

    @Override
    public void endMethodBody(LocatableToken token, boolean included) {
        super.endMethodBody(token, included);
        this.delegate.endMethodBody(token, included);
    }

    @Override
    public void gotTypeDef(LocatableToken firstToken, int tdType) {
        super.gotTypeDef(firstToken, tdType);
        this.delegate.gotTypeDef(firstToken, tdType);
    }

    @Override
    public void gotTypeDefName(LocatableToken nameToken) {
        super.gotTypeDefName(nameToken);
        this.delegate.gotTypeDefName(nameToken);
    }

    @Override
    public void beginTypeBody(LocatableToken leftCurlyToken) {
        super.beginTypeBody(leftCurlyToken);
        this.delegate.beginTypeBody(leftCurlyToken);
    }

    @Override
    public void endTypeBody(LocatableToken endCurlyToken, boolean included) {
        super.endTypeBody(endCurlyToken, included);
        this.delegate.endTypeBody(endCurlyToken, included);
    }

    @Override
    public void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {
        super.gotMethodDeclaration(token, hiddenToken);
        this.delegate.gotMethodDeclaration(token, hiddenToken);
    }

    @Override
    public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {
        super.gotConstructorDecl(token, hiddenToken);
        this.delegate.gotConstructorDecl(token, hiddenToken);
    }

    @Override
    public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken, String name) {
        super.gotConstructorDecl(token, hiddenToken, name);
        this.delegate.gotConstructorDecl(token, hiddenToken, name);
    }

    @Override
    public void beginFieldDeclarations(LocatableToken first) {
        super.beginFieldDeclarations(first);
        this.delegate.beginFieldDeclarations(first);
    }

    @Override
    public void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {
        super.gotField(first, idToken, initExpressionFollows);
        this.delegate.gotField(first, idToken, initExpressionFollows);
    }

    @Override
    public void endFieldDeclarations(LocatableToken token, boolean included) {
        super.endFieldDeclarations(token, included);
        this.delegate.endFieldDeclarations(token, included);
    }

    @Override
    public void gotTypeSpec(List<LocatableToken> tokens) {
        super.gotTypeSpec(tokens);
        this.delegate.gotTypeSpec(tokens);
    }

    @Override
    public void gotImport(List<LocatableToken> tokens, boolean isStatic,
                          LocatableToken importToken, LocatableToken semiColonToken) {
        super.gotImport(tokens, isStatic, importToken, semiColonToken);
        this.delegate.gotImport(tokens, isStatic, importToken, semiColonToken);
    }

    @Override
    public void reachedCUstate(int state) {
        super.reachedCUstate(state);
        this.delegate.reachedCUstate(state);
    }

    @Override
    public void finishedCU(int state) {
        super.finishedCU(state);
        this.delegate.finishedCU(state);
    }

    @Override
    public void gotImportStmtSemi(LocatableToken token) {
        super.gotImportStmtSemi(token);
        this.delegate.gotImportStmtSemi(token);
    }

    @Override
    public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
                                  LocatableToken importToken, LocatableToken semiColonToken) {
        super.gotWildcardImport(tokens, isStatic, importToken, semiColonToken);
        this.delegate.gotWildcardImport(tokens, isStatic, importToken, semiColonToken);
    }

    @Override
    public void beginForLoop(LocatableToken token) {
        super.beginForLoop(token);
        this.delegate.beginForLoop(token);
    }

    @Override
    public void beginForLoopBody(LocatableToken token) {
        super.beginForLoopBody(token);
        this.delegate.beginForLoopBody(token);
    }

    @Override
    public void endForLoopBody(LocatableToken token, boolean included) {
        super.endForLoopBody(token, included);
        this.delegate.endForLoopBody(token, included);
    }

    @Override
    public void endForLoop(LocatableToken token, boolean included) {
        super.endForLoop(token, included);
        this.delegate.endForLoop(token, included);
    }

    @Override
    public void beginWhileLoop(LocatableToken token) {
        super.beginWhileLoop(token);
        this.delegate.beginWhileLoop(token);
    }

    @Override
    public void beginWhileLoopBody(LocatableToken token) {
        super.beginWhileLoopBody(token);
        this.delegate.beginWhileLoopBody(token);
    }

    @Override
    public void endWhileLoopBody(LocatableToken token, boolean included) {
        super.endWhileLoopBody(token, included);
        this.delegate.endWhileLoopBody(token, included);
    }

    @Override
    public void endWhileLoop(LocatableToken token, boolean included) {
        super.endWhileLoop(token, included);
        this.delegate.endWhileLoop(token, included);
    }

    @Override
    public void beginDoWhile(LocatableToken token) {
        super.beginDoWhile(token);
        this.delegate.beginDoWhile(token);
    }

    @Override
    public void beginDoWhileBody(LocatableToken token) {
        super.beginDoWhileBody(token);
        this.delegate.beginDoWhileBody(token);
    }

    @Override
    public void endDoWhileBody(LocatableToken token, boolean included) {
        super.endDoWhileBody(token, included);
        this.delegate.endDoWhileBody(token, included);
    }

    @Override
    public void endDoWhile(LocatableToken token, boolean included) {
        super.endDoWhile(token, included);
        this.delegate.endDoWhile(token, included);
    }

    @Override
    public void beginIfStmt(LocatableToken token) {
        super.beginIfStmt(token);
        this.delegate.beginIfStmt(token);
    }

    @Override
    public void beginIfCondBlock(LocatableToken token) {
        super.beginIfCondBlock(token);
        this.delegate.beginIfCondBlock(token);
    }

    @Override
    public void endIfCondBlock(LocatableToken token, boolean included) {
        super.endIfCondBlock(token, included);
        this.delegate.endIfCondBlock(token, included);
    }

    @Override
    public void gotElseIf(LocatableToken token) {
        super.gotElseIf(token);
        this.delegate.gotElseIf(token);
    }

    @Override
    public void endIfStmt(LocatableToken token, boolean included) {
        super.endIfStmt(token, included);
        this.delegate.endIfStmt(token, included);
    }

    @Override
    public void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) {
        super.beginSwitchStmt(token, isSwitchExpression);
        this.delegate.beginSwitchStmt(token, isSwitchExpression);
    }

    @Override
    public void beginSwitchBlock(LocatableToken token) {
        super.beginSwitchBlock(token);
        this.delegate.beginSwitchBlock(token);
    }

    @Override
    public void endSwitchBlock(LocatableToken token) {
        super.endSwitchBlock(token);
        this.delegate.endSwitchBlock(token);
    }

    @Override
    public void endSwitchStmt(LocatableToken token, boolean included) {
        super.endSwitchStmt(token, included);
        this.delegate.endSwitchStmt(token, included);
    }

    @Override
    public void beginSwitchCase(LocatableToken token) {
        super.beginSwitchCase(token);
        this.delegate.beginSwitchCase(token);
    }

    @Override
    public void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) {
        super.gotSwitchCaseType(token, isArrowSyntax);
        this.delegate.gotSwitchCaseType(token, isArrowSyntax);
    }

    @Override
    public void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) {
        super.endSwitchCase(token, wasArrowSyntax);
        this.delegate.endSwitchCase(token, wasArrowSyntax);
    }

    @Override
    public void gotSwitchDefault() {
        super.gotSwitchDefault();
        this.delegate.gotSwitchDefault();
    }

    @Override
    public void beginTryCatchStmt(LocatableToken token, boolean hasResource) {
        super.beginTryCatchStmt(token, hasResource);
        this.delegate.beginTryCatchStmt(token, hasResource);
    }

    @Override
    public void beginTryBlock(LocatableToken token) {
        super.beginTryBlock(token);
        this.delegate.beginTryBlock(token);
    }

    @Override
    public void endTryBlock(LocatableToken token, boolean included) {
        super.endTryBlock(token, included);
        this.delegate.endTryBlock(token, included);
    }

    @Override
    public void endTryCatchStmt(LocatableToken token, boolean included) {
        super.endTryCatchStmt(token, included);
        this.delegate.endTryCatchStmt(token, included);
    }

    @Override
    public void gotCatchFinally(LocatableToken token) {
        super.gotCatchFinally(token);
        this.delegate.gotCatchFinally(token);
    }

    @Override
    public void gotMultiCatch(LocatableToken token) {
        super.gotMultiCatch(token);
        this.delegate.gotMultiCatch(token);
    }

    @Override
    public void gotCatchVarName(LocatableToken token) {
        super.gotCatchVarName(token);
        this.delegate.gotCatchVarName(token);
    }

    @Override
    public void beginSynchronizedBlock(LocatableToken token) {
        super.beginSynchronizedBlock(token);
        this.delegate.beginSynchronizedBlock(token);
    }

    @Override
    public void endSynchronizedBlock(LocatableToken token, boolean included) {
        super.endSynchronizedBlock(token, included);
        this.delegate.endSynchronizedBlock(token, included);
    }

    @Override
    public void gotDeclBegin(LocatableToken token) {
        super.gotDeclBegin(token);
        this.delegate.gotDeclBegin(token);
    }

    @Override
    public void endDecl(LocatableToken token) {
        super.endDecl(token);
        this.delegate.endDecl(token);
    }

    @Override
    public void gotTypeDefEnd(LocatableToken token, boolean included) {
        super.gotTypeDefEnd(token, included);
        this.delegate.gotTypeDefEnd(token, included);
    }

    @Override
    public void beginTypeDefExtends(LocatableToken extendsToken) {
        super.beginTypeDefExtends(extendsToken);
        this.delegate.beginTypeDefExtends(extendsToken);
    }

    @Override
    public void endTypeDefExtends() {
        super.endTypeDefExtends();
        this.delegate.endTypeDefExtends();
    }

    @Override
    public void beginTypeDefImplements(LocatableToken implementsToken) {
        super.beginTypeDefImplements(implementsToken);
        this.delegate.beginTypeDefImplements(implementsToken);
    }

    @Override
    public void endTypeDefImplements() {
        super.endTypeDefImplements();
        this.delegate.endTypeDefImplements();
    }

    @Override
    public void beginTypeDefPermits(LocatableToken permitsToken) {
        super.beginTypeDefPermits(permitsToken);
        this.delegate.beginTypeDefPermits(permitsToken);
    }

    @Override
    public void endTypeDefPermits() {
        super.endTypeDefPermits();
        this.delegate.endTypeDefPermits();
    }

    @Override
    public void gotInnerType(LocatableToken start) {
        super.gotInnerType(start);
        this.delegate.gotInnerType(start);
    }

    @Override
    public void gotTopLevelDecl(LocatableToken token) {
        super.gotTopLevelDecl(token);
        this.delegate.gotTopLevelDecl(token);
    }

    @Override
    public void beginVariableDecl(LocatableToken first) {
        super.beginVariableDecl(first);
        this.delegate.beginVariableDecl(first);
    }

    @Override
    public void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) {
        super.gotVariableDecl(first, idToken, inited);
        this.delegate.gotVariableDecl(first, idToken, inited);
    }

    @Override
    public void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) {
        super.gotSubsequentVar(first, idToken, inited);
        this.delegate.gotSubsequentVar(first, idToken, inited);
    }

    @Override
    public void endVariable(LocatableToken token, boolean included) {
        super.endVariable(token, included);
        this.delegate.endVariable(token, included);
    }

    @Override
    public void endVariableDecls(LocatableToken token, boolean included) {
        super.endVariableDecls(token, included);
        this.delegate.endVariableDecls(token, included);
    }

    @Override
    public void beginForInitDecl(LocatableToken first) {
        super.beginForInitDecl(first);
        this.delegate.beginForInitDecl(first);
    }

    @Override
    public void gotForInit(LocatableToken first, LocatableToken idToken) {
        super.gotForInit(first, idToken);
        this.delegate.gotForInit(first, idToken);
    }

    @Override
    public void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        super.gotSubsequentForInit(first, idToken, initFollows);
        this.delegate.gotSubsequentForInit(first, idToken, initFollows);
    }

    @Override
    public void endForInit(LocatableToken token, boolean included) {
        super.endForInit(token, included);
        this.delegate.endForInit(token, included);
    }

//    @Override
//    public void endForInitDecl(LocatableToken token, boolean included) {
//        super.endForInitDecl(token, included);
//        this.delegate.endForInitDecl(token, included);
//    }

    @Override
    public void endForInitDecls(LocatableToken token, boolean included) {
        super.endForInitDecls(token, included);
        this.delegate.endForInitDecls(token, included);
    }

    @Override
    public void gotForTest(boolean isPresent) {
        super.gotForTest(isPresent);
        this.delegate.gotForTest(isPresent);
    }

    @Override
    public void gotForIncrement(boolean isPresent) {
        super.gotForIncrement(isPresent);
        this.delegate.gotForIncrement(isPresent);
    }

    @Override
    public void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) {
        super.determinedForLoop(forEachLoop, initExpressionFollows);
        this.delegate.determinedForLoop(forEachLoop, initExpressionFollows);
    }

    @Override
    public void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        super.gotSubsequentField(first, idToken, initFollows);
        this.delegate.gotSubsequentField(first, idToken, initFollows);
    }

    @Override
    public void endField(LocatableToken token, boolean included) {
        super.endField(token, included);
        this.delegate.endField(token, included);
    }

    @Override
    public void beginExpression(LocatableToken token, boolean isLambdaBody) {
        super.beginExpression(token, isLambdaBody);
        this.delegate.beginExpression(token, isLambdaBody);
    }

    @Override
    public void endExpression(LocatableToken token, boolean emptyExpression) {
        super.endExpression(token, emptyExpression);
        this.delegate.endExpression(token, emptyExpression);
    }

    @Override
    public void gotLiteral(LocatableToken token) {
        super.gotLiteral(token);
        this.delegate.gotLiteral(token);
    }

    @Override
    public void gotPrimitiveTypeLiteral(LocatableToken token) {
        super.gotPrimitiveTypeLiteral(token);
        this.delegate.gotPrimitiveTypeLiteral(token);
    }

    @Override
    public void gotIdentifier(LocatableToken token) {
        super.gotIdentifier(token);
        this.delegate.gotIdentifier(token);
    }

    @Override
    public void gotIdentifierEOF(LocatableToken token) {
        super.gotIdentifierEOF(token);
        this.delegate.gotIdentifierEOF(token);
    }

    @Override
    public void gotMemberAccessEOF(LocatableToken token) {
        super.gotMemberAccessEOF(token);
        this.delegate.gotMemberAccessEOF(token);
    }

    @Override
    public void gotCompoundIdent(LocatableToken token) {
        super.gotCompoundIdent(token);
        this.delegate.gotCompoundIdent(token);
    }

    @Override
    public void gotCompoundComponent(LocatableToken token) {
        super.gotCompoundComponent(token);
        this.delegate.gotCompoundComponent(token);
    }

    @Override
    public void completeCompoundValue(LocatableToken token) {
        super.completeCompoundValue(token);
        this.delegate.completeCompoundValue(token);
    }

    @Override
    public void completeCompoundValueEOF(LocatableToken token) {
        super.completeCompoundValueEOF(token);
        this.delegate.completeCompoundValueEOF(token);
    }

    @Override
    public void completeCompoundClass(LocatableToken token) {
        super.completeCompoundClass(token);
        this.delegate.completeCompoundClass(token);
    }

    @Override
    public void gotMemberAccess(LocatableToken token) {
        super.gotMemberAccess(token);
        this.delegate.gotMemberAccess(token);
    }

    @Override
    public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
        super.gotMemberCall(token, typeArgs);
        this.delegate.gotMemberCall(token, typeArgs);
    }

    @Override
    public void gotMethodCall(LocatableToken token) {
        super.gotMethodCall(token);
        this.delegate.gotMethodCall(token);
    }

    @Override
    public void gotConstructorCall(LocatableToken token) {
        super.gotConstructorCall(token);
        this.delegate.gotConstructorCall(token);
    }

    @Override
    public void gotDotEOF(LocatableToken token) {
        super.gotDotEOF(token);
        this.delegate.gotDotEOF(token);
    }

    @Override
    public void gotStatementExpression() {
        super.gotStatementExpression();
        this.delegate.gotStatementExpression();
    }

    @Override
    public void gotClassLiteral(LocatableToken token) {
        super.gotClassLiteral(token);
        this.delegate.gotClassLiteral(token);
    }

    @Override
    public void gotBinaryOperator(LocatableToken token) {
        super.gotBinaryOperator(token);
        this.delegate.gotBinaryOperator(token);
    }

    @Override
    public void gotUnaryOperator(LocatableToken token) {
        super.gotUnaryOperator(token);
        this.delegate.gotUnaryOperator(token);
    }

    @Override
    public void gotQuestionOperator(LocatableToken token) {
        super.gotQuestionOperator(token);
        this.delegate.gotQuestionOperator(token);
    }

    @Override
    public void gotQuestionColon(LocatableToken token) {
        super.gotQuestionColon(token);
        this.delegate.gotQuestionColon(token);
    }

    @Override
    public void gotInstanceOfOperator(LocatableToken token) {
        super.gotInstanceOfOperator(token);
        this.delegate.gotInstanceOfOperator(token);
    }

    @Override
    public void gotInstanceOfVar(LocatableToken token) {
        super.gotInstanceOfVar(token);
        this.delegate.gotInstanceOfVar(token);
    }

    @Override
    public void gotArrayElementAccess() {
        super.gotArrayElementAccess();
        this.delegate.gotArrayElementAccess();
    }

    @Override
    public void gotPostOperator(LocatableToken token) {
        super.gotPostOperator(token);
        this.delegate.gotPostOperator(token);
    }

    @Override
    public void gotTypeCast(List<LocatableToken> tokens) {
        super.gotTypeCast(tokens);
        this.delegate.gotTypeCast(tokens);
    }

    @Override
    public void gotArrayTypeIdentifier(LocatableToken token) {
        super.gotArrayTypeIdentifier(token);
        this.delegate.gotArrayTypeIdentifier(token);
    }

    @Override
    public void gotParentIdentifier(LocatableToken token) {
        super.gotParentIdentifier(token);
        this.delegate.gotParentIdentifier(token);
    }

    @Override
    public void beginArgumentList(LocatableToken token) {
        super.beginArgumentList(token);
        this.delegate.beginArgumentList(token);
    }

    @Override
    public void endArgument() {
        super.endArgument();
        this.delegate.endArgument();
    }

    @Override
    public void endArgumentList(LocatableToken token) {
        super.endArgumentList(token);
        this.delegate.endArgumentList(token);
    }

    @Override
    public void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) {
        super.gotMethodParameter(token, ellipsisToken);
        this.delegate.gotMethodParameter(token, ellipsisToken);
    }

    @Override
    public void gotArrayDeclarator() {
        super.gotArrayDeclarator();
        this.delegate.gotArrayDeclarator();
    }

    @Override
    public void gotNewArrayDeclarator(boolean withDimension) {
        super.gotNewArrayDeclarator(withDimension);
        this.delegate.gotNewArrayDeclarator(withDimension);
    }

    @Override
    public void gotAllMethodParameters() {
        super.gotAllMethodParameters();
        this.delegate.gotAllMethodParameters();
    }

    @Override
    public void beginFormalParameter(LocatableToken token) {
        super.beginFormalParameter(token);
        this.delegate.beginFormalParameter(token);
    }

    @Override
    public void gotTypeParam(LocatableToken idToken) {
        super.gotTypeParam(idToken);
        this.delegate.gotTypeParam(idToken);
    }

    @Override
    public void gotTypeParamBound(List<LocatableToken> tokens) {
        super.gotTypeParamBound(tokens);
        this.delegate.gotTypeParamBound(tokens);
    }

    @Override
    public void gotMethodTypeParamsBegin() {
        super.gotMethodTypeParamsBegin();
        this.delegate.gotMethodTypeParamsBegin();
    }

    @Override
    public void endMethodTypeParams() {
        super.endMethodTypeParams();
        this.delegate.endMethodTypeParams();
    }

    @Override
    public void gotExprNew(LocatableToken token) {
        super.gotExprNew(token);
        this.delegate.gotExprNew(token);
    }

    @Override
    public void endExprNew(LocatableToken token, boolean included) {
        super.endExprNew(token, included);
        this.delegate.endExprNew(token, included);
    }

    @Override
    public void beginArrayInitList(LocatableToken token) {
        super.beginArrayInitList(token);
        this.delegate.beginArrayInitList(token);
    }

    @Override
    public void endArrayInitList(LocatableToken token) {
        super.endArrayInitList(token);
        this.delegate.endArrayInitList(token);
    }

    @Override
    public void beginAnonClassBody(LocatableToken token, boolean isEnumMember) {
        super.beginAnonClassBody(token, isEnumMember);
        this.delegate.beginAnonClassBody(token, isEnumMember);
    }

    @Override
    public void endAnonClassBody(LocatableToken token, boolean included) {
        super.endAnonClassBody(token, included);
        this.delegate.endAnonClassBody(token, included);
    }

    @Override
    public void beginStmtblockBody(LocatableToken token) {
        super.beginStmtblockBody(token);
        this.delegate.beginStmtblockBody(token);
    }

    @Override
    public void endStmtblockBody(LocatableToken token, boolean included) {
        super.endStmtblockBody(token, included);
        this.delegate.endStmtblockBody(token, included);
    }

    @Override
    public void beginInitBlock(LocatableToken first, LocatableToken lcurly) {
        super.beginInitBlock(first, lcurly);
        this.delegate.beginInitBlock(first, lcurly);
    }

    @Override
    public void endInitBlock(LocatableToken rcurly, boolean included) {
        super.endInitBlock(rcurly, included);
        this.delegate.endInitBlock(rcurly, included);
    }

    @Override
    public void gotThrow(LocatableToken token) {
        super.gotThrow(token);
        this.delegate.gotThrow(token);
    }

    @Override
    public void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) {
        super.gotBreakContinue(keywordToken, labelToken);
        this.delegate.gotBreakContinue(keywordToken, labelToken);
    }

    @Override
    public void gotReturnStatement(boolean hasValue) {
        super.gotReturnStatement(hasValue);
        this.delegate.gotReturnStatement(hasValue);
    }

    @Override
    public void gotYieldStatement() {
        super.gotYieldStatement();
        this.delegate.gotYieldStatement();
    }

    @Override
    public void gotEmptyStatement() {
        super.gotEmptyStatement();
        this.delegate.gotEmptyStatement();
    }

    @Override
    public void gotAssert() {
        super.gotAssert();
        this.delegate.gotAssert();
    }

    @Override
    public void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) {
        super.gotAnnotation(annName, paramsFollow);
        this.delegate.gotAnnotation(annName, paramsFollow);
    }

    @Override
    public void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) {
        super.beginLambdaBody(lambdaIsBlock, openCurly);
        this.delegate.beginLambdaBody(lambdaIsBlock, openCurly);
    }

    @Override
    public void endLambdaBody(LocatableToken closeCurly) {
        super.endLambdaBody(closeCurly);
        this.delegate.endLambdaBody(closeCurly);
    }

    @Override
    public void gotLambdaFormalParam() {
        super.gotLambdaFormalParam();
        this.delegate.gotLambdaFormalParam();
    }

    @Override
    public void gotLambdaFormalName(LocatableToken name) {
        super.gotLambdaFormalName(name);
        this.delegate.gotLambdaFormalName(name);
    }

    @Override
    public void gotLambdaFormalType(List<LocatableToken> type) {
        super.gotLambdaFormalType(type);
        this.delegate.gotLambdaFormalType(type);
    }

    @Override
    public void beginRecordParameters(LocatableToken parenToken) {
        super.beginRecordParameters(parenToken);
        this.delegate.beginRecordParameters(parenToken);
    }

    @Override
    public void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {
        super.gotRecordParameter(first, idToken, varargsToken);
        this.delegate.gotRecordParameter(first, idToken, varargsToken);
    }

    @Override
    public void endRecordParameters(LocatableToken closeParen) {
        super.endRecordParameters(closeParen);
        this.delegate.endRecordParameters(closeParen);
    }

    @Override
    public void endMethodDecl(LocatableToken token, boolean included) {
        super.endMethodDecl(token, included);
        this.delegate.endMethodDecl(token, included);
    }

    @Override
    public void beginThrows(LocatableToken token) {
        super.beginThrows(token);
        this.delegate.beginThrows(token);
    }

    @Override
    public void endThrows() {
        super.endThrows();
        this.delegate.endThrows();
    }

    @Override
    public void gotComment(LocatableToken token) {
        super.gotComment(token);
        this.delegate.gotComment(token);
    }

    @Override
    public void error(String msg, int beginLine, int beginCol, int endLine, int endCol) {
        super.error(msg, beginLine, beginCol, endLine, endCol);
        this.delegate.error(msg, beginLine, beginCol, endLine, endCol);
    }
}
